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
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.InetAddress;
import java.net.InetSocketAddress;

/**
 * Implementation of the TELNET protocol from Internet STD 8.
 * 
 * <p>
 * This class is a low-level interface designed for performance over safety, and
 * must be used carefully by the caller with respect to synchronization and
 * order of operations. Generally, you shouldn't use this class directly unless
 * you know what you're doing.
 * </p>
 * 
 * @author cu5
 */
public class TELNETProtocol implements Closeable {
    // Special byte constants.
    private static final byte CODE_NUL = (byte) 0;
    private static final byte CODE_LF = (byte) 10;
    private static final byte CODE_CR = (byte) 13;

    private static final byte CODE_SUB_SE = (byte) 240;

    private static final byte CODE_NOP = (byte) 241;
    private static final byte CODE_DataMark = (byte) 242;
    private static final byte CODE_BRK = (byte) 243;
    private static final byte CODE_FUN_IP = (byte) 244;
    private static final byte CODE_FUN_AO = (byte) 245;
    private static final byte CODE_FUN_AYT = (byte) 246;
    private static final byte CODE_FUN_EC = (byte) 247;
    private static final byte CODE_FUN_EL = (byte) 248;
    private static final byte CODE_GA = (byte) 249;

    private static final byte CODE_SUB_SB = (byte) 250;

    private static final byte CODE_OPT_WILL = (byte) 251;
    private static final byte CODE_OPT_WONT = (byte) 252;
    private static final byte CODE_OPT_DO = (byte) 253;
    private static final byte CODE_OPT_DONT = (byte) 254;

    private static final byte CODE_IAC = (byte) 255;

    /**
     * TELNET byte stream state.
     */
    private static enum StreamState {
        // In initial state, expect data or IAC.
        START,

        // Saw CR, expect NUL or LF.
        CR,

        // Saw IAC, expect command.
        IAC,

        // Saw option negotiation command, expect option code.
        OPTION,

        // Saw option sub-negotiation begin command, expect option code.
        NEGOTIATION_BEGIN,

        // In option sub-negotiation, expect data or IAC.
        NEGOTIATION,

        // Saw IAC in option sub-negotiation, expect IAC or end command.
        NEGOTIATION_IAC;
    }

    private final SocketConnection socket;

    private final byte[] rbuf;
    private int roff, rlen;

    private final byte[] wbuf;
    private int wlen;

    private final Reader reader;
    private final Writer writer;

    /**
     * @param address
     *            remote address
     * @param proxy
     *            TODO: replace this with a generic configuration object
     * 
     * @throws IOException
     */
    public TELNETProtocol(SocketConnection socket) throws IOException {
        this.socket = socket;

        rbuf = socket.getReceiveBuffer();
        reader = new InputStreamReader(new InputFilter(), "UTF-8");

        wbuf = socket.getSendBuffer();
        writer = new OutputStreamWriter(new OutputFilter(), "UTF-8");
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

    public void shutdownOutput() throws IOException {
        socket.write(wlen);
        wlen = 0;

        socket.shutdownOutput();
    }

    public void flush() throws IOException {
        socket.write(wlen);
        wlen = 0;

        socket.flush();
    }

    public Reader getReader() {
        return reader;
    }

    public Writer getWriter() {
        return writer;
    }

    private final class InputFilter extends InputStream {
        private StreamState state = StreamState.START;
        private byte savedByte;

        @Override
        public int read() throws IOException {
            do {
                final int next = nextData();
                if (next != 256) {
                    return next;
                }
            } while (fill());

            return -1;
        }

        @Override
        public int read(byte[] buf, int off, int len) throws IOException {
            int ii = 0;

            while (ii < len) {
                final int next = nextData();
                if (next == 256) {
                    // Try to fill buffer without blocking.
                    if (socket.available() == 0 && ii != 0) {
                        // Would block, and we read at least one byte.
                        break;
                    }

                    fill();
                } else {
                    // Got next byte.
                    buf[off++] = (byte) next;
                    ii++;
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

        private int nextData() {
            while (roff < rlen) {
                final byte nextByte = rbuf[roff++];

                switch (state) {
                case START:
                    switch (nextByte) {
                    case CODE_CR:
                        state = StreamState.CR;
                        break;

                    case CODE_IAC:
                        state = StreamState.IAC;
                        break;

                    default:
                        return nextByte;
                    }
                    break;

                case CR:
                    state = StreamState.START;
                    switch (nextByte) {
                    case CODE_LF:
                        return CODE_LF;

                    case CODE_NUL:
                        break;

                    default:
                        // Not in compliance with RFC, but we'll be generous.
                        --roff;
                        break;
                    }
                    return CODE_CR;

                case IAC:
                    switch (nextByte) {
                    case CODE_IAC:
                        state = StreamState.START;
                        return CODE_IAC;

                    case CODE_OPT_WILL:
                    case CODE_OPT_WONT:
                    case CODE_OPT_DO:
                    case CODE_OPT_DONT:
                        state = StreamState.OPTION;
                        savedByte = nextByte;
                        break;

                    case CODE_SUB_SB:
                        state = StreamState.NEGOTIATION_BEGIN;
                        break;

                    case CODE_NOP:
                    case CODE_DataMark:
                    case CODE_BRK:
                    case CODE_FUN_IP:
                    case CODE_FUN_AO:
                    case CODE_FUN_AYT:
                    case CODE_FUN_EC:
                    case CODE_FUN_EL:
                    case CODE_GA:
                        // TODO: Other TELNET commands.
                        state = StreamState.START;
                        break;

                    default:
                        // Not in compliance with RFC, but we'll be generous.
                        state = StreamState.START;
                        break;
                    }
                    break;

                case OPTION:
                    state = StreamState.START;
                    switch (savedByte) {
                    case CODE_OPT_WILL:
                    case CODE_OPT_WONT:
                    case CODE_OPT_DO:
                    case CODE_OPT_DONT:
                        // TODO: Option negotiation.
                        break;

                    default:
                        // If this ever happens, we screwed up.
                        throw new AssertionError();
                    }
                    break;

                case NEGOTIATION_BEGIN:
                    state = StreamState.NEGOTIATION;
                    // TODO: Option sub-negotiation code.
                    break;

                case NEGOTIATION:
                    if (nextByte == CODE_IAC) {
                        state = StreamState.NEGOTIATION_IAC;
                    } else {
                        // TODO: Option sub-negotiation parameter.
                    }
                    break;

                case NEGOTIATION_IAC:
                    switch (nextByte) {
                    case CODE_SUB_SE:
                        state = StreamState.START;
                        // TODO: Option sub-negotiation termination.
                        break;

                    case CODE_IAC:
                        state = StreamState.NEGOTIATION;
                        // TODO: Add IAC to parameter.
                        break;

                    default:
                        // Not in compliance with RFC, but we'll be generous.
                        state = StreamState.NEGOTIATION;
                        break;
                    }
                    break;
                }
            }

            return 256; // -1 == (byte) 255
        }
    }

    private final class OutputFilter extends OutputStream {
        @Override
        public void write(int octet) throws IOException {
            nextData((byte) octet);
        }

        @Override
        public void write(byte[] buf, int off, int len) throws IOException {
            for (int ii = 0; ii < len; ii++) {
                nextData(buf[off++]);
            }
        }

        private void writeByte(final byte nextByte) throws IOException {
            assert wlen >= 0 && wlen < wbuf.length;

            wbuf[wlen++] = nextByte;

            if (wlen == wbuf.length) {
                socket.write(wlen);
                wlen = 0;
            }
        }

        private void nextData(final byte nextByte) throws IOException {
            switch (nextByte) {
            case CODE_LF:
                writeByte(CODE_CR);
                writeByte(CODE_LF);
                break;

            case CODE_CR:
                writeByte(CODE_CR);
                writeByte(CODE_NUL);
                break;

            case CODE_IAC:
                writeByte(CODE_IAC);
                writeByte(CODE_IAC);
                break;

            default:
                writeByte(nextByte);
                break;
            }
        }
    }

    public static void main(String[] args) throws Exception {
        InetAddress address = InetAddress.getByName(args[0]);
        int port = Integer.parseInt(args[1]);

        InetSocketAddress socketAddress = new InetSocketAddress(address, port);
        SocketConnection socket = new SocketConnection(socketAddress, null);

        TELNETProtocol connection = null;
        try {
            connection = new TELNETProtocol(socket);
        } finally {
            if (connection == null) {
                socket.close();
            }
        }

        try {
            Reader reader = connection.getReader();

            char[] rbuf = new char[4096];
            int rlen;
            while ((rlen = reader.read(rbuf)) != -1) {
                for (int ii = 0; ii < rlen; ii++) {
                    System.out.print(rbuf[ii]);
                }
            }
        } finally {
            connection.close();
        }
    }
}
