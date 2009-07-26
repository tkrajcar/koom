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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;

/**
 * @author cu5
 */
public class DebugClient {
    private static NetworkVT terminal;
    private static volatile boolean terminating;

    public static void main(String[] args) throws Exception {
        // Create terminal.
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
            terminal = new NetworkVT(connection);
        } finally {
            if (terminal == null) {
                connection.close();
            }
        }

        // Display input from connection.
        terminal.setWindowSize(24, 80);

        new Thread(new Runnable() {
            public void run() {
                readThread();
            }
        }).start();

        // Feed input to connection.
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                System.in));
        int count = 0;
        try {
            while (!terminating) {
                // FIXME: Can block here.
                final String line = reader.readLine();
                if (line == null) {
                    break;
                }

                if (count == 0) {
                    count = 1;
                    terminal.setWindowSize(25, 80);
                }

                // FIXME: Can block here.
                terminal.writeLine(line);
            }

            connection.shutdownOutput();
        } finally {
            terminating = true;
            connection.close();
        }

        System.exit(0);
    }

    private static void readThread() {
        try {
            while (!terminating) {
                final CharSequence line = terminal.readLine();
                if (line == null) {
                    System.out.format("PROMPT[" + terminal.readForced() + "]");
                } else {
                    System.out.println(line);
                }
            }
        } catch (IOException ex) {
            // Can't read anymore.
            ex.printStackTrace();
        } finally {
            System.err.println("READER TERMINATING");
            terminating = true;
        }
    }
}
