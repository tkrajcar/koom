/*
 * Copyright 2009 Tim Krajcar <allegro@conmolto.org>.
 *
 * This file is part of Koom, a BattleTech MUX graphical HUD client.
 *
 * Koom is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Koom is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Koom.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.feem.koom.net;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;

import net.feem.koom.services.Utility;

/**
 * A high level interface to the TELNET protocol. You should generally interface
 * with this class, in preference to using {@link TELNETProtocol} directly.
 * 
 * @author cu5
 */
public class NetworkVT {
    private static final byte[] TERMINAL_TYPE = Utility.getASCII("KOOM");

    private static enum InputState {
        // Waiting for input.
        START,

        // End of input.
        STOP, STOP_UNTERMINATED, STOP_EOF;
    }

    private final TELNETProtocol proto;

    private final Reader reader;
    private InputState inputState = InputState.START;
    private boolean sawCR;
    private int savedChar = -1;

    private final Writer writer;
    private final TELNETEventHandler outputHandler;

    private boolean sawGA;
    private final TTYPEOption optionTTYPE = new TTYPEOption();
    private final NAWSOption optionNAWS = new NAWSOption();

    public NetworkVT(TELNETProtocol proto) throws IOException {
        this.proto = proto;
        proto.setInputHandler(new InputEventHandler());

        try {
            reader = new InputStreamReader(proto.getInputStream(), "UTF-8");
            writer = new OutputStreamWriter(proto.getOutputStream(), "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            // Every JVM should support UTF-8.
            throw new AssertionError(ex);
        }

        outputHandler = proto.getOutputEventHandler();
    }

    public void setWindowSize(int rows, int columns) throws IOException {
        optionNAWS.setWindowSize(rows, columns);
    }

    /**
     * Reads characters from the remote end. In addition to the usual reasons, a
     * read may end early because a record terminator was encountered. (This
     * implies a zero length record is possible.)
     * 
     * <p>
     * Besides the usual end of line, a record may be terminated by an explicit
     * marker, the end of stream, a style boundary, and possibly others. Check
     * the terminal state after the read to disambiguate the various cases.
     * </p>
     * 
     * <p>
     * In particular, {@link readIsRecord()} will indicate if the returned data
     * terminates a record (including lines), and {@link readIsLine()} will
     * indicate if the returned data terminates a line.
     * </p>
     * 
     * @param cbuf
     *            character array
     * @param off
     *            offset into character array
     * @param len
     *            number of characters to write
     * 
     * @return the number of characters read, or -1 if end of stream
     * 
     * @throws IOException
     *             if there was an underlying I/O error
     */
    public int read(char[] cbuf, int off, int len) throws IOException {
        int ii = 0;

        inputState = InputState.START;

        // Clean up any unfinished business.
        if (savedChar != -1) {
            if (len > 0) {
                cbuf[off++] = (char) savedChar;
                ii = 1;
                savedChar = -1;
            } else {
                return 0;
            }
        }

        // Read characters up until the next boundary.
        try {
            LOOP: while (ii < len) {
                if (!reader.ready() && ii != 0) {
                    break LOOP;
                }

                final int nextChar = reader.read();
                if (nextChar == -1) {
                    // End of stream.
                    if (ii == 0) {
                        return -1;
                    }

                    inputState = InputState.STOP_EOF;
                    break LOOP;
                }

                switch (nextChar) {
                case '\0':
                    if (sawCR) {
                        sawCR = false;
                        cbuf[off++] = '\r';
                        ii++;
                    } else {
                        cbuf[off++] = '\0';
                        ii++;
                    }
                    break;

                case '\r':
                    if (sawCR) {
                        // Not in compliance with RFC, but we'll be generous.
                        cbuf[off++] = '\r';
                        ii++;
                    } else {
                        sawCR = true;
                    }
                    break;

                case '\n':
                    // Not in compliance with RFC, but a fairly common mistake.
                    // Our behavior is more useful to a MU* client.
                    sawCR = false;
                    inputState = InputState.STOP;
                    break LOOP;

                default:
                    if (sawCR) {
                        // Not in compliance with RFC, but we'll be generous.
                        sawCR = false;
                        cbuf[off++] = '\r';
                        ii++;

                        if (ii == len) {
                            savedChar = nextChar;
                            break LOOP;
                        }
                    }

                    cbuf[off++] = (char) nextChar;
                    ii++;
                    break;
                }
            }
        } catch (StreamStateException ex) {
            if (sawGA) {
                // Go Ahead.
                proto.clear();
                sawGA = false;
            } else {
                // Not supposed to happen.
                throw new AssertionError("Unexpected state change");
            }

            inputState = InputState.STOP_UNTERMINATED;
        } catch (IOException ex) {
            // I/O error.
            if (ii == 0) {
                throw ex;
            }
        }

        // Finish read.
        return ii;
    }

    /**
     * Tests if the most recent read ended with a record terminator of any type.
     * 
     * @return if the record ended with any kind of terminator
     */
    public boolean readIsRecord() {
        switch (inputState) {
        case STOP:
        case STOP_UNTERMINATED:
        case STOP_EOF:
            return true;

        default:
            return false;
        }
    }

    /**
     * Tests if the most recent read ended with a line terminator.
     * 
     * @return if the record ended with a line terminator
     */
    public boolean readIsLine() {
        switch (inputState) {
        case STOP:
            return true;

        default:
            return false;
        }
    }

    /**
     * Writes characters to the remote end, adding line termination.
     * 
     * @param cbuf
     *            character array
     * @param off
     *            offset into character array
     * @param len
     *            number of characters to write
     * 
     * @throws IOException
     *             if there was an underlying I/O error
     */
    public void writeLine(char[] cbuf, int off, int len) throws IOException {
        synchronized (proto) {
            for (int ii = 0; ii < len; ii++) {
                final char nextChar = cbuf[off++];
                writer.write(nextChar);

                if (nextChar == '\r') {
                    // Bare CR.
                    writer.write('\0');
                }
            }

            writer.write("\r\n");
        }
    }

    /**
     * Flushes any buffered data to the remote end.
     * 
     * @throws IOException
     *             if there was an underlying I/O error
     */
    public void flush() throws IOException {
        synchronized (proto) {
            writer.flush();
            proto.flush();
        }
    }

    private final class InputEventHandler implements TELNETEventHandler {
        private TELNETOption subOption;

        @Override
        public void processCommand(byte code) throws IOException {
            if (code == TELNETProtocol.CODE_GA) {
                sawGA = true;
                throw new StreamStateException();
            }
        }

        @Override
        public void processDO(int option) throws IOException {
            synchronized (proto) {
                switch (option) {
                case TTYPEOption.OPTION_CODE:
                    optionTTYPE.enable();
                    break;

                case NAWSOption.OPTION_CODE:
                    optionNAWS.enable();
                    break;

                default:
                    // Unsupported option.
                    outputHandler.processWONT(option);
                    proto.flush();
                }
            }
        }

        @Override
        public void processDONT(int option) throws IOException {
            synchronized (proto) {
                switch (option) {
                case TTYPEOption.OPTION_CODE:
                    optionTTYPE.disable();
                    break;

                case NAWSOption.OPTION_CODE:
                    optionNAWS.disable();
                    break;

                default:
                    // Already WONTing unsupported local option.
                    break;
                }
            }
        }

        @Override
        public void processWILL(int option) throws IOException {
            synchronized (proto) {
                // Unsupported remote option.
                outputHandler.processDONT(option);
                proto.flush();
            }
        }

        @Override
        public void processWONT(int option) throws IOException {
            // Already DONTing unsupported remote option.
        }

        @Override
        public void beginParam(int option) {
            switch (option) {
            case TTYPEOption.OPTION_CODE:
                subOption = optionTTYPE;
                break;

            default:
                // Unsupported option.
                subOption = null;
                return;
            }

            subOption.beginParam();
        }

        @Override
        public void endParam() throws IOException {
            if (subOption == null) {
                // Unsupported option.
            } else {
                subOption.endParam();
            }
        }

        @Override
        public void appendParam(byte nextByte) {
            if (subOption == null) {
                // Unsupported option.
            } else {
                subOption.appendParam(nextByte);
            }
        }
    }

    /**
     * Terminal Type option (RFC 1091).
     */
    private final class TTYPEOption extends TELNETOption {
        private static final int OPTION_CODE = 24;

        private static final byte SUB_IS = (byte) 0;
        private static final byte SUB_SEND = (byte) 1;

        private int paramCount;

        private TTYPEOption() {
            super(OPTION_CODE);
        }

        @Override
        public void beginParam() {
            paramCount = 1;
        }

        @Override
        public void endParam() throws IOException {
            if (paramCount == 2) {
                // Send TTYPE.
                paramCount = 0;

                synchronized (proto) {
                    outputHandler.beginParam(OPTION_CODE);
                    outputHandler.appendParam(SUB_IS);

                    for (byte nextByte : TERMINAL_TYPE) {
                        outputHandler.appendParam(nextByte);
                    }

                    outputHandler.endParam();
                    proto.flush();
                }
            }
        }

        @Override
        public void appendParam(byte nextByte) {
            if (paramCount == 1 && nextByte == SUB_SEND) {
                // Request for TTYPE from server.
                paramCount = 2;
            } else {
                // Parameter parsing failed.
                paramCount = -1;
            }
        }

        @Override
        protected void requestEnable() throws IOException {
            outputHandler.processWILL(OPTION_CODE);
            proto.flush();
        }

        @Override
        protected void requestDisable() throws IOException {
            outputHandler.processWONT(OPTION_CODE);
            proto.flush();
        }
    }

    /**
     * Negotiate About Window Size option (RFC 1073).
     */
    private final class NAWSOption extends TELNETOption {
        private static final int OPTION_CODE = 31;

        // Unknown window size; call setWindowSize() to initialize.
        private int rows = 0;
        private int cols = 0;

        private NAWSOption() {
            super(OPTION_CODE);
        }

        public void setWindowSize(int rows, int cols) throws IOException {
            if (rows < 1 || rows > 65535 || cols < 1 || cols > 65535) {
                throw new IllegalArgumentException("Invalid window size");
            }

            if (rows == this.rows && cols == this.cols) {
                // No change.
                return;
            }

            if (isEnabled()) {
                synchronized (proto) {
                    sendWindowSize(rows, cols);
                }
            }

            this.rows = rows;
            this.cols = cols;
        }

        @Override
        protected void requestEnable() throws IOException {
            outputHandler.processWILL(OPTION_CODE);
            proto.flush();
        }

        @Override
        protected void requestDisable() throws IOException {
            outputHandler.processWONT(OPTION_CODE);
            proto.flush();
        }

        @Override
        protected void doEnable() throws IOException {
            sendWindowSize(rows, cols);
        }

        private void sendWindowSize(int rows, int cols) throws IOException {
            if (rows == 0 && cols == 0) {
                // Unknown window size, don't send anything.
                return;
            }

            // Parameter: WIDTH[1] WIDTH[0] HEIGHT[1] HEIGHT[0]
            outputHandler.beginParam(getOption());

            outputHandler.appendParam((byte) (cols >>> 8));
            outputHandler.appendParam((byte) cols);
            outputHandler.appendParam((byte) (rows >>> 8));
            outputHandler.appendParam((byte) rows);

            outputHandler.endParam();

            proto.flush();
        }
    }
}
