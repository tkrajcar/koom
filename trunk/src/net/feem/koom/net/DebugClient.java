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
import java.util.ArrayList;
import java.util.List;

/**
 * @author cu5
 */
public class DebugClient {
    private static TELNETProtocol connection;
    private static NetworkVT terminal;

    private static final List<String> output = new ArrayList<String>();
    private static final List<String> working = new ArrayList<String>();

    public static void main(String[] args) throws Exception {
        // Create terminal.
        InetAddress address = InetAddress.getByName(args[0]);
        int port = Integer.parseInt(args[1]);

        InetSocketAddress socketAddress = new InetSocketAddress(address, port);
        SocketConnection socket = new SocketConnection(socketAddress, null);

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

        Thread input = new Thread(new Runnable() {
            public void run() {
                inputThread();
            }
        });
        input.setDaemon(true);
        input.start();

        new Thread(new Runnable() {
            public void run() {
                readThread();
            }
        }).start();

        new Thread(new Runnable() {
            public void run() {
                writeThread();
            }
        }).start();
    }

    private static void inputThread() {
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                System.in));
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                synchronized (output) {
                    output.add(line);
                    output.notify();
                }
            }
        } catch (IOException ex) {
            // Can't input anymore.
            ex.printStackTrace();
        } finally {
            System.err.println("INPUT TERMINATING");
            terminate();
        }
    }

    private static void readThread() {
        StringBuilder line = new StringBuilder(16384);
        char[] cbuf = new char[32];

        try {
            while (true) {
                final int count = terminal.read(cbuf, 0, cbuf.length);
                if (count == -1) {
                    // End of stream.
                    break;
                }

                for (int ii = 0; ii < count; ii++) {
                    System.out.print(cbuf[ii]);
                }

                line.append(cbuf, 0, count);

                if (terminal.readIsRecord()) {
                    if (terminal.readIsLine()) {
                        System.out.println();
                        // System.out.format("[LINE:%s]%n", line);
                    } else {
                        System.out.format("[PROMPT:%s]%n", line);
                    }

                    line.setLength(0);
                }
            }
        } catch (IOException ex) {
            // Can't read anymore.
            ex.printStackTrace();
        } finally {
            System.err.println("READER TERMINATING");
            terminate();
        }
    }

    private static void writeThread() {
        boolean changed = true;

        try {
            LOOP: while (true) {
                synchronized (output) {
                    while (output.isEmpty()) {
                        try {
                            output.wait();
                        } catch (InterruptedException ex) {
                            // Don't care.
                        }
                    }

                    working.addAll(output);
                    output.clear();
                }

                if (changed) {
                    System.err.println("CHANGING WINDOW SIZE");
                    terminal.setWindowSize(25, 80);
                    changed = false;
                }

                for (String line : working) {
                    if (line == null) {
                        break LOOP;
                    }

                    terminal.writeLine(line.toCharArray(), 0, line.length());
                    terminal.flush();
                }

                working.clear();
            }

            connection.shutdownOutput();
        } catch (IOException ex) {
            // Can't write anymore.
            ex.printStackTrace();
        } finally {
            System.err.println("WRITER TERMINATING");
            terminate();
        }
    }

    private static void terminate() {
        synchronized (output) {
            output.add(null);
            output.notify();
        }
    }
}
