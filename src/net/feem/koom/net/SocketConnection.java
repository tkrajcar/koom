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
import java.net.Proxy;
import java.net.Socket;
import java.net.SocketAddress;

/**
 * @author cu5
 */
public class SocketConnection implements Closeable {
    private final Socket socket;

    private final InputStream in;
    private final byte[] rbuf;

    private final OutputStream out;
    private final byte[] wbuf;

    /**
     * Establishes a buffer size, subject to a minimum value.
     * 
     * @param suggested
     *            suggested size
     * 
     * @return actual size
     */
    private static int getSize(int suggested) {
        return (suggested < 8192) ? 8192 : suggested;
    }

    /**
     * Gets a connected socket from a socket address and proxy configuration.
     */
    private static Socket getSocket(SocketAddress address, Proxy proxy)
            throws IOException {
        Socket socket;
        if (proxy == null) {
            socket = new Socket();
        } else {
            socket = new Socket(proxy);
        }

        socket.connect(address);
        return socket;
    }

    /**
     * Creates a <code>SocketConnection</code> from an existing socket.
     * 
     * @param socket
     *            an existing socket
     * 
     * @throws IOException
     *             if there's an I/O error
     */
    public SocketConnection(Socket socket) throws IOException {
        this.socket = socket;

        boolean success = false;
        try {
            socket.setTcpNoDelay(true); // make configurable?
            socket.setKeepAlive(true); // definitely make configurable

            in = socket.getInputStream();
            rbuf = new byte[getSize(socket.getReceiveBufferSize())];

            out = socket.getOutputStream();
            wbuf = new byte[getSize(socket.getSendBufferSize())];

            success = true;
        } finally {
            if (!success) {
                socket.close();
            }
        }
    }

    /**
     * Creates a connection over a network socket.
     * 
     * @param address
     *            remote address
     * @param proxy
     *            TODO: replace this with a generic configuration object
     * 
     * @throws IOException
     *             if there's an I/O error
     */
    public SocketConnection(SocketAddress address, Proxy proxy)
            throws IOException {
        this(getSocket(address, proxy));
    }

    @Override
    public void close() throws IOException {
        socket.close();
    }

    public void shutdownOutput() throws IOException {
        socket.shutdownOutput();
    }

    public int available() throws IOException {
        return in.available();
    }

    public void flush() throws IOException {
        out.flush();
    }

    public void setTimeout(int timeout) throws IOException {
        socket.setSoTimeout(timeout);
    }

    /**
     * @return receive buffer of at least 256 bytes
     */
    public byte[] getReceiveBuffer() {
        return rbuf;
    }

    /**
     * @return send buffer of at least 256 bytes
     */
    public byte[] getSendBuffer() {
        return wbuf;
    }

    /**
     * Reads in bytes to fill the receive buffer. The existing buffer contents
     * are discarded. Blocks until at least one byte has been read, the end of
     * stream is reached, or an exception is thrown.
     * 
     * @return number of bytes read, or -1 if end of stream
     * 
     * @throws IOException
     *             if there was an I/O error
     */
    public int read() throws IOException {
        return in.read(rbuf);
    }

    /**
     * Writes out the given number of bytes from the send buffer. Will block
     * until all bytes are written, or an exception is thrown.
     * 
     * @param len
     *            number of bytes to write
     * 
     * @throws IOException
     *             if there was an I/O error
     */
    public void write(int len) throws IOException {
        out.write(wbuf, 0, len);
    }
}
