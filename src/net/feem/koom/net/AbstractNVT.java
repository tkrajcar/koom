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
 * Base class for other network VT implementations. A network VT provides a high
 * level interface to the TELNET protocol. You should generally interface with
 * classes derived from this one, in preference to using {@link TELNETProtocol}
 * directly.
 * 
 * @author cu5
 */
public abstract class AbstractNVT {
    private static enum InputState {
        // Waiting for input.
        START,

        // Stream configuration changed.
        CONFIG,

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

    protected AbstractNVT(TELNETProtocol proto) {
        this.proto = proto;

        try {
            reader = new InputStreamReader(proto.getInputStream(), "UTF-8");
            writer = new OutputStreamWriter(proto.getOutputStream(), "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            // Every JVM should support UTF-8.
            throw new AssertionError(ex);
        }

        outputHandler = proto.getOutputEventHandler();
    }

    /**
     * Gets the object used for synchronizing writes. Note that this includes
     * synchronizing modifications to state that affects writes.
     * 
     * @return lock object
     */
    public Object getWriteLock() {
        return this;
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
            while (ii < len) {
                if (!reader.ready() && ii != 0) {
                    // Would block, and we already have data.
                    break;
                }

                final int nextChar = reader.read();
                if (nextChar == -1) {
                    // End of stream.
                    if (ii == 0) {
                        return -1;
                    }

                    inputState = InputState.STOP_EOF;
                    return ii;
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
                    return ii;

                default:
                    if (sawCR) {
                        // Not in compliance with RFC, but we'll be generous.
                        sawCR = false;
                        cbuf[off++] = '\r';
                        ii++;

                        if (ii == len) {
                            savedChar = nextChar;
                            return ii;
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

                inputState = InputState.STOP_UNTERMINATED;
                return ii;
            }

            // Stream reconfigured.
            inputState = InputState.CONFIG;
        } catch (IOException ex) {
            // I/O error.
            if (ii == 0) {
                throw ex;
            }
        }

        // Make deferred configuration changes before returning.
        if (doReconfig()) {
            inputState = InputState.CONFIG;
        }

        return ii;
    }

    /**
     * Tests if the most recent read ended because of a reconfiguration event.
     * 
     * @return if there was a reconfiguration event
     */
    public boolean readIsReconfig() {
        switch (inputState) {
        case CONFIG:
            return true;

        default:
            return false;
        }
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
        synchronized (getWriteLock()) {
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
        synchronized (getWriteLock()) {
            flushInput();
            flushProtocol();
        }
    }

    protected class InputEventHandler implements TELNETEventHandler {
        private TELNETOption subOption;

        @Override
        public void processCommand(byte code) throws IOException {
            if (code == TELNETProtocol.CODE_GA) {
                sawGA = true;
                throw new StreamStateException();
            } else {
                // Treat the other commands as no-ops.
            }
        }

        @Override
        public void processDO(int option) throws IOException {
            synchronized (getWriteLock()) {
                // Unsupported option.
                flushInput();
                outputHandler.processWONT(option);
                flushProtocol();
            }
        }

        @Override
        public void processDONT(int option) throws IOException {
            // Already WONTing unsupported local option.
        }

        @Override
        public void processWILL(int option) throws IOException {
            synchronized (getWriteLock()) {
                // Unsupported remote option.
                flushInput();
                outputHandler.processDONT(option);
                flushProtocol();
            }
        }

        @Override
        public void processWONT(int option) throws IOException {
            // Already DONTing unsupported remote option.
        }

        @Override
        public void beginParam(int option) throws IOException {
            if (subOption == null) {
                // Unsupported option.
            } else {
                subOption.beginParam();
            }
        }

        @Override
        public void endParam() throws IOException {
            if (subOption == null) {
                // Unsupported option.
            } else {
                try {
                    subOption.endParam();
                } finally {
                    setSubOption(null);
                }
            }
        }

        @Override
        public void appendParam(byte nextByte) throws IOException {
            if (subOption == null) {
                // Unsupported option.
            } else {
                subOption.appendParam(nextByte);
            }
        }

        protected void setSubOption(TELNETOption subOption) {
            this.subOption = subOption;
        }
    }

    /**
     * Flushes user input to the TELNET protocol. This ensures any user input is
     * written out before we write directly to the {@link TELNETProtocol}.
     * 
     * @throws IOException
     *             if there's an I/O error
     */
    protected void flushInput() throws IOException {
        writer.flush();
    }

    /**
     * Flushes data from the TELNET protocol to the network. This ensures any
     * buffered data is written out to the network.
     * 
     * @throws IOException
     *             if there's an I/O error
     */
    protected void flushProtocol() throws IOException {
        proto.flush();
    }

    /**
     * Called when it's safe to perform deferred reconfiguration.
     * 
     * @return if reconfiguration was performed
     */
    protected boolean doReconfig() {
        // Nothing to reconfig by default.
        return false;
    }
}
