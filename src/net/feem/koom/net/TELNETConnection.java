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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.net.SocketAddress;

/**
 * @author cu5
 */
public class TELNETConnection implements Closeable {
    private final Socket socket;
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
    public TELNETConnection(SocketAddress address, Proxy proxy)
            throws IOException {
        if (proxy == null) {
            socket = new Socket();
        } else {
            socket = new Socket(proxy);
        }

        boolean success = false;

        try {
            socket.setTcpNoDelay(true); // make configurable?
            socket.setKeepAlive(true); // make configurable

            socket.connect(address);

            InputStream in = socket.getInputStream();
            in = new BufferedInputStream(in, socket.getReceiveBufferSize());
            reader = new InputStreamReader(new InputFilter(in), "UTF-8");

            OutputStream out = socket.getOutputStream();
            out = new BufferedOutputStream(out, socket.getSendBufferSize());
            writer = new OutputStreamWriter(new OutputFilter(out), "UTF-8");

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

    public Reader getReader() {
        return reader;
    }

    public Writer getWriter() {
        return writer;
    }

    private class InputFilter extends FilterInputStream {
        public InputFilter(InputStream in) {
            super(in);
        }

        @Override
        public int read() throws IOException {
            int next;

            do {
                next = in.read();
            } while (next == 0xFF);

            return next;
        }

        @Override
        public int read(byte[] buf, int off, int len) throws IOException {
            int ii = off;

            while (ii < len) {
                buf[ii++] = (byte) read();

                if (available() == 0) {
                    break;
                }
            }

            return (ii - off);
        }
    }

    private class OutputFilter extends FilterOutputStream {
        public OutputFilter(OutputStream out) throws IOException {
            super(out);
        }

        @Override
        public void write(int octet) throws IOException {
            if (octet == 0xFF) {
                out.write(0xFF);
            }

            out.write(octet);
        }

        @Override
        public void write(byte[] buf, int off, int len) throws IOException {
            for (int ii = off; ii < len; ii++) {
                out.write(buf[ii]);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        InetAddress address = InetAddress.getByName(args[0]);
        int port = Integer.parseInt(args[1]);

        InetSocketAddress socketAddress = new InetSocketAddress(address, port);
        TELNETConnection connection = new TELNETConnection(socketAddress, null);

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
