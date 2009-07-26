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

/**
 * A high level interface to the TELNET protocol. You should generally interface
 * with this class, in preference to using {@link TELNETProtocol} directly.
 * 
 * @author cu5
 */
public class NetworkVT {
    private static enum InputState {
        // Waiting for input.
        START,

        // End of input.
        STOP, STOP_UNTERMINATED,

        // Waiting for reset.
        RESET
    }

    private final TELNETProtocol proto;

    private final Reader reader;
    private final StringBuilder input = new StringBuilder(16384);
    private InputState inputState = InputState.START;
    private boolean sawCR;

    private final Writer writer;
    private final TELNETEventHandler outputHandler;

    private boolean sawGA;
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

        // Suggest initial options.
        outputHandler = proto.getOutputEventHandler();

        optionNAWS.tryEnable();
    }

    public void setWindowSize(int rows, int columns) throws IOException {
        optionNAWS.setWindowSize(rows, columns);
    }

    /**
     * Returns the next line, or <code>null</code> if the next "line" is
     * unterminated. An incomplete line can be retrieved with the
     * {@link readForced} method.
     * 
     * <p>
     * The returned line does not contain the line terminator.
     * </p>
     * 
     * <p>
     * Note that the returned value is a {@link CharSequence} with possible
     * limited lifetime; it is likely change on the next call to any
     * <code>read*</code> method.
     * </p>
     * 
     * @return a complete line, or <code>null</code> if unterminated
     * 
     * @throws IOException
     *             if there was an underlying I/O error
     */
    public CharSequence readLine() throws IOException {
        if (nextLine()) {
            inputState = InputState.RESET;
            return input;
        } else {
            return null;
        }
    }

    /**
     * Returns the next record, regardless if it's terminated or not.
     * 
     * <p>
     * The returned line does not contain any line terminator.
     * </p>
     * 
     * <p>
     * Note that the returned value is a {@link CharSequence} with possible
     * limited lifetime; it is likely change on the next call to any
     * <code>read*</code> method.
     * </p>
     * 
     * @return the next record, regardless of termination
     * 
     * @throws IOException
     *             if there was an underlying I/O error
     */
    public CharSequence readForced() throws IOException {
        nextLine();
        inputState = InputState.RESET;
        return input;
    }

    /**
     * Writes a complete line.
     * 
     * @param line
     *            line character sequence (without termination)
     * 
     * @throws IOException
     *             if there was an underlying I/O error
     */
    public void writeLine(CharSequence line) throws IOException {
        synchronized (proto) {
            for (int ii = 0; ii < line.length(); ii++) {
                final char nextChar = line.charAt(ii);
                writer.append(nextChar);

                if (nextChar == '\r') {
                    // Bare CR.
                    writer.append('\0');
                }
            }

            writer.append("\r\n");
            writer.flush();
            proto.flush();
        }
    }

    //
    // Gets the next line from the remote end.
    //
    private boolean nextLine() throws IOException {
        // Figure out what to do.
        switch (inputState) {
        case STOP:
        case STOP_UNTERMINATED:
            // Already have line.
            return true;

        case RESET:
            // Start on next line.
            input.setLength(0);
            inputState = InputState.START;
            break;

        default:
            // Continue reading characters.
            break;
        }

        // Read characters.
        try {
            while (true) {
                final int nextChar = reader.read();
                if (nextChar == -1) {
                    // End of stream. Record ends now.
                    break;
                }

                if (sawCR) {
                    sawCR = false;
                    switch (nextChar) {
                    case '\0':
                        // Bare CR.
                        input.append('\r');
                        continue;

                    case '\n':
                        // Line terminated.
                        inputState = InputState.STOP;
                        return true;

                    default:
                        // Not in compliance with RFC, but we'll be generous.
                        input.append('\r');
                        break;
                    }
                }

                switch (nextChar) {
                case '\r':
                    sawCR = true;
                    break;

                case '\n':
                    // Not in compliance with RFC, but a fairly common mistake.
                    inputState = InputState.STOP;
                    return true;

                default:
                    input.append((char) nextChar);
                    break;
                }
            }
        } catch (StreamStateException ex) {
            if (sawGA) {
                // Go Ahead. Record ends now.
                sawGA = false;
                proto.clear();
            } else {
                // Shouldn't happen.
                throw new AssertionError(ex);
            }
        }

        // Record unterminated.
        inputState = InputState.STOP_UNTERMINATED;
        return false;
    }

    private final class InputEventHandler implements TELNETEventHandler {
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
                if (option == optionNAWS.getOption()) {
                    optionNAWS.enable();
                } else {
                    // Unsupported option.
                    outputHandler.processWONT(option);
                    proto.flush();
                }
            }
        }

        @Override
        public void processDONT(int option) throws IOException {
            synchronized (proto) {
                if (option == optionNAWS.getOption()) {
                    optionNAWS.disable();
                } else {
                    // Already WONTing unsupported local option.
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
        public void beginParam(int option) throws IOException {
            // Ignoring for now.
        }

        @Override
        public void endParam() throws IOException {
            // Ignoring for now.
        }

        @Override
        public void appendParam(byte nextByte) throws IOException {
            // Ignoring for now.
        }
    }

    /**
     * Negotiate About Window Size option (RFC 1073).
     */
    private final class NAWSOption extends TELNETOption {
        // Unknown window size; call setWindowSize() to initialize.
        private int rows = 0;
        private int cols = 0;

        private NAWSOption() {
            super(31);
        }

        public void setWindowSize(int rows, int cols) throws IOException {
            if (rows < 1 || rows > 65535 || cols < 1 || cols > 65535) {
                throw new IllegalArgumentException("Invalid window size");
            }

            if (isEnabled()) {
                sendWindowSize(rows, cols);
            }

            this.rows = rows;
            this.cols = cols;
        }

        @Override
        protected void requestEnable() throws IOException {
            outputHandler.processWILL(getOption());
            proto.flush();
        }

        @Override
        protected void requestDisable() throws IOException {
            outputHandler.processWONT(getOption());
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
