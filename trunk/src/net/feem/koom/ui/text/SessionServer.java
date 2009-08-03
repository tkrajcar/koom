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
package net.feem.koom.ui.text;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

import net.feem.koom.net.SocketConnection;
import net.feem.koom.net.SocketServer;
import net.feem.koom.services.Utility;

/**
 * Server for text-based interface.
 * 
 * @author cu5
 */
public class SessionServer implements Closeable, Runnable {
    private final SocketServer server;

    public SessionServer(SocketAddress address) throws IOException {
        server = new SocketServer(address);
    }

    @Override
    public void close() throws IOException {
        server.close();
    }

    @Override
    public void run() {
        System.err.println("Starting session server");

        while (true) {
            // Wait for connection.
            SocketConnection socket;
            try {
                socket = server.accept();
            } catch (IOException ex) {
                // Error while waiting for connections.
                break;
            }

            System.err.println("Accepted new connection");

            // Spawn controller thread.
            Controller control = new Controller(socket);

            ThreadGroup group = new ThreadGroup("Text UI");
            Thread thread = new Thread(group, control, "Text Controller");
            thread.setPriority(Utility.getPriority(1));

            thread.start();
        }

        System.err.println("Stopping session server");
    }

    public static void main(String[] args) throws IOException {
        // FIXME: Make this configurable.
        final String SERVER_HOST = "localhost";
        final int SERVER_PORT = 4200;

        SocketAddress address = new InetSocketAddress(SERVER_HOST, SERVER_PORT);
        SessionServer server = new SessionServer(address);

        Thread thread = new Thread(server, "Text Server");
        // TODO: Server will normally be a daemon thread.
        // thread.setDaemon(true);

        thread.start();
    }
}
