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
import java.net.ServerSocket;
import java.net.SocketAddress;

/**
 * A simple server for modest uses.
 * 
 * @author cu5
 */
public class SocketServer implements Closeable {
    private final ServerSocket socket;

    public SocketServer(SocketAddress address) throws IOException {
        socket = new ServerSocket();

        socket.setReuseAddress(true);
        socket.bind(address);
    }

    @Override
    public void close() throws IOException {
        socket.close();
    }

    public SocketConnection accept() throws IOException {
        return new SocketConnection(socket.accept());
    }
}
