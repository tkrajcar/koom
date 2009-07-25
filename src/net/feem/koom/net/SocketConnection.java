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
     * Creates a connection over a network socket.
     * 
     * @param address
     *            remote address
     * @param proxy
     *            TODO: replace this with a generic configuration object
     * 
     * @throws IOException
     */
    public SocketConnection(SocketAddress address, Proxy proxy)
            throws IOException {
        if (proxy == null) {
            socket = new Socket();
        } else {
            socket = new Socket(proxy);
        }

        boolean success = false;

        try {
            socket.setTcpNoDelay(true); // make configurable?
            socket.setKeepAlive(true); // definitely make configurable

            socket.connect(address);

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

    public byte[] getReceiveBuffer() {
        return rbuf;
    }

    public byte[] getSendBuffer() {
        return wbuf;
    }

    public int read() throws IOException {
        return in.read(rbuf);
    }

    public void write(int len) throws IOException {
        out.write(wbuf, 0, len);
    }
}
