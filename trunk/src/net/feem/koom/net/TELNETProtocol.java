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

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Implementation of the TELNET protocol from Internet STD 8.
 * 
 * <p>
 * Since transforming CRs and end of lines in this class would result in a loss
 * of information about line termination, the user is responsible for
 * transforming to and from the standard CR NUL and CR LF sequences.
 * </p>
 * 
 * <p>
 * This class is a low-level interface designed for performance over safety, and
 * must be used carefully by the caller with respect to synchronization and
 * order of operations. Generally, you shouldn't use this class directly unless
 * you know what you're doing; see {@link NetworkVT} instead.
 * </p>
 * 
 * @author cu5
 */
public class TELNETProtocol implements Closeable {
    //
    // TELNET command codes from Internet STD 8.
    //
    public static final byte CODE_SE = (byte) 240;

    public static final byte CODE_NOP = (byte) 241;
    public static final byte CODE_DataMark = (byte) 242;
    public static final byte CODE_BRK = (byte) 243;
    public static final byte CODE_FUN_IP = (byte) 244;
    public static final byte CODE_FUN_AO = (byte) 245;
    public static final byte CODE_FUN_AYT = (byte) 246;
    public static final byte CODE_FUN_EC = (byte) 247;
    public static final byte CODE_FUN_EL = (byte) 248;
    public static final byte CODE_GA = (byte) 249;

    public static final byte CODE_WILL = (byte) 251;
    public static final byte CODE_WONT = (byte) 252;
    public static final byte CODE_DO = (byte) 253;
    public static final byte CODE_DONT = (byte) 254;

    public static final byte CODE_SB = (byte) 250;

    public static final byte CODE_IAC = (byte) 255;

    /**
     * TELNET command state.
     */
    private static enum CommandState {
        // In initial state, expect data or IAC.
        START,

        // Saw IAC, expect command.
        IAC,

        // Saw option negotiation command, expect option code.
        OPTION_WILL, OPTION_WONT, OPTION_DO, OPTION_DONT,

        // Saw option sub-negotiation begin command, expect option code.
        NEGOTIATION_BEGIN,

        // In option sub-negotiation, expect data or IAC.
        NEGOTIATION,

        // Saw IAC in option sub-negotiation, expect IAC or end command.
        NEGOTIATION_IAC;
    }

    private final SocketConnection socket;

    private final InputFilter in;
    private TELNETEventHandler inHandler;

    private final OutputFilter out;
    private final OutputEventHandler outHandler = new OutputEventHandler();

    static private int getUnsigned(byte signed) {
        return 0xFF & signed;
    }

    /**
     * Constructs a TELNET protocol handler.
     * 
     * @param socket
     *            remote connection
     * @param handler
     *            TELNET stream event handler
     * 
     * @throws IOException
     *             on I/O errors
     */
    public TELNETProtocol(SocketConnection socket) throws IOException {
        this.socket = socket;

        in = new InputFilter(socket.getReceiveBuffer());
        out = new OutputFilter(socket.getSendBuffer());
    }

    /**
     * Closes the protocol and the underlying connection.
     */
    @Override
    public void close() throws IOException {
        //
        // Caller beware: write buffer data intentionally discarded here. Use
        // shutdownOutput() or flush() first if you care.
        //
        socket.close();
    }

    /**
     * Sets the handler for reading input events.
     * 
     * <p>
     * An input event handler must be set before any input operations.
     * </p>
     * 
     * @param handler
     *            input event handler
     */
    public void setInputHandler(TELNETEventHandler handler) {
        inHandler = handler;
    }

    /**
     * Gets the handler for writing output events.
     * 
     * @return output event handler
     */
    public TELNETEventHandler getOutputEventHandler() {
        return outHandler;
    }

    /**
     * Clears input state change indicator after a {@link StreamStateException},
     * allowing input processing to continue.
     */
    public void clear() {
        in.changed = null;
    }

    public void shutdownOutput() throws IOException {
        out.emptyBuffer();
        socket.shutdownOutput();
    }

    public void flush() throws IOException {
        out.emptyBuffer();
        socket.flush();
    }

    public InputStream getInputStream() {
        return in;
    }

    public OutputStream getOutputStream() {
        return out;
    }

    //
    // Translate TELNET input stream to raw data bytes.
    //
    private final class InputFilter extends InputStream {
        private static final int NEED_DATA = 128;

        private final byte[] rbuf;
        private int roff, rlen;

        private CommandState state = CommandState.START;
        private StreamStateException changed;

        private InputFilter(byte[] rbuf) {
            this.rbuf = rbuf;
        }

        @Override
        public int available() {
            // Because we might consume an arbitrary number of bytes, it's
            // non-trivial to determine this, so there isn't much point.
            return 0;
        }

        @Override
        public int read() throws IOException {
            checkState();

            do {
                final int next = nextData();
                if (next != NEED_DATA) {
                    return 0xFF & next;
                }
            } while (fill());

            return -1;
        }

        @Override
        public int read(byte[] buf, int off, int len) throws IOException {
            checkState();

            int ii = 0;

            try {
                while (ii < len) {
                    final int next = nextData();
                    if (next == NEED_DATA) {
                        // Try to fill buffer.
                        if (socket.available() == 0) {
                            // Would block.
                            if (ii != 0) {
                                break;
                            }
                        }

                        if (!fill()) {
                            // End of stream.
                            if (ii == 0) {
                                return -1;
                            }
                            break;
                        }
                    } else {
                        // Got next byte.
                        buf[off++] = (byte) next;
                        ii++;
                    }
                }
            } catch (IOException ex) {
                // Can't continue reading.
                if (ii == 0) {
                    throw ex;
                }
            }

            return ii;
        }

        @Override
        public long skip(long count) throws IOException {
            checkState();

            // We still need to process every single byte, but we don't need to
            // save it anywhere. Since the caller has expressed no interest in
            // the skipped content, we also block to satisfy the request unless
            // we reach an actual end of stream.
            long ii = 0;

            try {
                while (ii < count) {
                    final int next = nextData();
                    if (next == NEED_DATA) {
                        // Try to fill buffer.
                        if (!fill()) {
                            // End of stream.
                            break;
                        }
                    } else {
                        // Got next byte.
                        ii++;
                    }
                }
            } catch (IOException ex) {
                // Can't continue reading.
                if (ii == 0) {
                    throw ex;
                }
            }

            return ii;
        }

        private boolean fill() throws IOException {
            assert roff == rlen;

            final int len = socket.read();
            if (len == -1) {
                return false;
            }

            roff = 0;
            rlen = len;
            return true;
        }

        private void checkState() throws StreamStateException {
            if (changed != null) {
                // Re-throw exception until reset.
                throw changed;
            }
        }

        private int nextData() throws IOException {
            try {
                return nextDataUnwrapped();
            } catch (StreamStateException ex) {
                changed = ex;
                throw ex;
            }
        }

        private int nextDataUnwrapped() throws IOException {
            while (roff < rlen) {
                final byte nextByte = rbuf[roff++];

                switch (state) {
                //
                // Look for command sequences.
                //
                case START:
                    if (nextByte == CODE_IAC) {
                        state = CommandState.IAC;
                    } else {
                        return nextByte;
                    }
                    break;

                //
                // Process command sequence.
                //
                case IAC:
                    switch (nextByte) {
                    case CODE_IAC:
                        state = CommandState.START;
                        return CODE_IAC;

                    case CODE_WILL:
                        state = CommandState.OPTION_WILL;
                        break;

                    case CODE_WONT:
                        state = CommandState.OPTION_WONT;
                        break;

                    case CODE_DO:
                        state = CommandState.OPTION_DO;
                        break;

                    case CODE_DONT:
                        state = CommandState.OPTION_DONT;
                        break;

                    case CODE_SB:
                        state = CommandState.NEGOTIATION_BEGIN;
                        break;

                    case CODE_DataMark:
                        // Java doesn't support TCP Urgent data, and DM is
                        // somewhat useless anyway. We can safely treat DM
                        // as a no-op.
                    case CODE_NOP:
                        // Do nothing.
                        state = CommandState.START;
                        break;

                    default:
                        // Deliver all other commands to event handler.
                        state = CommandState.START;
                        inHandler.processCommand(nextByte);
                        break;
                    }
                    break;

                //
                // Process options.
                //
                case OPTION_WILL:
                    state = CommandState.START;
                    inHandler.processWILL(getUnsigned(nextByte));
                    break;

                case OPTION_WONT:
                    state = CommandState.START;
                    inHandler.processWONT(getUnsigned(nextByte));
                    break;

                case OPTION_DO:
                    state = CommandState.START;
                    inHandler.processDO(getUnsigned(nextByte));
                    break;

                case OPTION_DONT:
                    state = CommandState.START;
                    inHandler.processDONT(getUnsigned(nextByte));
                    break;

                //
                // Process option sub-negotiation.
                //
                case NEGOTIATION_BEGIN:
                    state = CommandState.NEGOTIATION;
                    inHandler.beginParam(getUnsigned(nextByte));
                    break;

                case NEGOTIATION:
                    if (nextByte == CODE_IAC) {
                        state = CommandState.NEGOTIATION_IAC;
                    } else {
                        inHandler.appendParam(nextByte);
                    }
                    break;

                case NEGOTIATION_IAC:
                    switch (nextByte) {
                    case CODE_SE:
                        state = CommandState.START;
                        inHandler.endParam();
                        break;

                    case CODE_IAC:
                        state = CommandState.NEGOTIATION;
                        inHandler.appendParam(CODE_IAC);
                        break;

                    default:
                        // Not in compliance with RFC, but we'll be generous.
                        state = CommandState.NEGOTIATION;
                        inHandler.appendParam(CODE_IAC);
                        inHandler.appendParam(nextByte);
                        break;
                    }
                    break;
                }
            }

            return NEED_DATA;
        }
    }

    //
    // Translate raw data bytes to TELNET output stream.
    //
    private final class OutputFilter extends OutputStream {
        private final byte[] wbuf;
        private int wlen;

        private OutputFilter(byte[] wbuf) {
            this.wbuf = wbuf;
        }

        @Override
        public void flush() throws IOException {
            // This doesn't flush all the way to the socket, just empties the
            // write buffer.
            emptyBuffer();
        }

        @Override
        public void write(int nextByte) throws IOException {
            nextData((byte) nextByte);
        }

        @Override
        public void write(byte[] buf, int off, int len) throws IOException {
            for (int ii = 0; ii < len; ii++) {
                nextData(buf[off++]);
            }
        }

        private void reserve(int len) throws IOException {
            if (wlen + len > wbuf.length) {
                emptyBuffer();
            }
        }

        private void nextData(final byte nextByte) throws IOException {
            assert wlen >= 0 && wlen <= wbuf.length;

            // Since writes can throw exceptions, we must check that the buffer
            // has enough space first, then write to the buffer. Otherwise, a
            // previous write could have already filled the buffer.
            if (nextByte == CODE_IAC) {
                // Escaping IAC requires two bytes.
                reserve(2);
                wbuf[wlen++] = CODE_IAC;
            } else if (wlen == wbuf.length) {
                emptyBuffer();
            }

            wbuf[wlen++] = nextByte;
        }

        private void emptyBuffer() throws IOException {
            socket.write(wlen);
            wlen = 0;
        }
    }

    //
    // Event handler for adding commands to the output stream.
    //
    private final class OutputEventHandler implements TELNETEventHandler {
        @Override
        public void processCommand(byte code) throws IOException {
            writeCommand(code);
        }

        @Override
        public void processWILL(int option) throws IOException {
            writeOption(CODE_WILL, option);
        }

        @Override
        public void processWONT(int option) throws IOException {
            writeOption(CODE_WONT, option);
        }

        @Override
        public void processDO(int option) throws IOException {
            writeOption(CODE_DO, option);
        }

        @Override
        public void processDONT(int option) throws IOException {
            writeOption(CODE_DONT, option);
        }

        @Override
        public void beginParam(int option) throws IOException {
            writeOption(CODE_SB, option);
        }

        @Override
        public void endParam() throws IOException {
            writeCommand(CODE_SE);
        }

        @Override
        public void appendParam(byte nextByte) throws IOException {
            out.nextData(nextByte);
        }

        private void writeCommand(byte code) throws IOException {
            out.reserve(2);
            out.wbuf[out.wlen++] = CODE_IAC;
            out.wbuf[out.wlen++] = code;
        }

        private void writeOption(byte code, int option) throws IOException {
            assert option >= 0 && option < 256;

            out.reserve(3);
            out.wbuf[out.wlen++] = CODE_IAC;
            out.wbuf[out.wlen++] = code;
            out.wbuf[out.wlen++] = (byte) option;
        }
    }
}
