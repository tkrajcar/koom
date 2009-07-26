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

import static net.feem.koom.net.TELNETProtocol.CODE_DO;
import static net.feem.koom.net.TELNETProtocol.CODE_DONT;
import static net.feem.koom.net.TELNETProtocol.CODE_WILL;
import static net.feem.koom.net.TELNETProtocol.CODE_WONT;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Deque;
import java.util.LinkedList;

/**
 * @author cu5
 */
public class DebugClient {
    private static TELNETProtocol connection;

    public static void main(String[] args) throws Exception {
        InetAddress address = InetAddress.getByName(args[0]);
        int port = Integer.parseInt(args[1]);

        InetSocketAddress socketAddress = new InetSocketAddress(address, port);
        SocketConnection socket = new SocketConnection(socketAddress, null);

        TELNETEventHandler handler = null;
        try {
            handler = new TestingEventHandler();
            connection = new TELNETProtocol(socket, handler);
        } finally {
            if (connection == null) {
                socket.close();
            }
        }

        new Thread(new Runnable() {
            public void run() {
                writeThread();
            }
        }).start();

        try {
            Reader reader = new InputStreamReader(connection.getInputStream(),
                    "UTF-8");

            char[] rbuf = new char[4096];
            int rlen;
            while (true) {
                try {
                    rlen = reader.read(rbuf);
                    if (rlen == -1) {
                        break;
                    }
                } catch (StreamStateException ex) {
                    System.err.println("STATE CHANGE");
                    connection.clear();
                    continue;
                }

                for (int ii = 0; ii < rlen; ii++) {
                    System.out.print(rbuf[ii]);
                }
            }
        } finally {
            connection.close();
        }
    }

    private static Deque<OptionCommand> events = new LinkedList<OptionCommand>();

    private static void writeThread() {
        final TELNETEventHandler handler = connection.getOutputEventHandler();

        try {
            Writer writer = new OutputStreamWriter(
                    connection.getOutputStream(), "UTF-8");

            synchronized (events) {
                while (true) {
                    while (events.isEmpty()) {
                        try {
                            events.wait();
                        } catch (InterruptedException ex) {
                            // Don't care.
                        }
                    }

                    while (!events.isEmpty()) {
                        OptionCommand event = events.removeFirst();

                        switch (event.code) {
                        case CODE_WILL:
                            handler.processWILL(event.option);
                            break;

                        case CODE_WONT:
                            handler.processWONT(event.option);
                            break;

                        case CODE_DO:
                            handler.processDO(event.option);
                            break;

                        case CODE_DONT:
                            handler.processDONT(event.option);
                            break;
                        }
                    }

                    connection.flush();
                }
            }

            // writer.write("connect username password\r\n");
            // writer.flush();
        } catch (IOException ex) {
        } finally {
            // connection.close();
        }
    }

    private static void addEvent(byte code, int option) {
        synchronized (events) {
            events.addLast(new OptionCommand(code, option));
            events.notify();
        }
    }

    private static class OptionCommand {
        final byte code;
        final int option;

        OptionCommand(byte code, int option) {
            this.code = code;
            this.option = option;
        }
    }

    private static class TestingEventHandler implements TELNETEventHandler {
        //
        // Miscellaneous incoming command processing.
        //
        public void processCommand(byte code) {
            System.err.format("recv cmd  %d\n", code);
        }

        //
        // Incoming option negotiation. Return true if the option modifies input
        // processing.
        //
        private final boolean dummyOption = false;

        public void processWILL(int option) throws StreamStateException {
            System.err.format("recv WILL %d%n", option);
            negotiateRemote(option, false, dummyOption, true);
        }

        public void processWONT(int option) throws StreamStateException {
            System.err.format("recv WONT %d%n", option);
            negotiateRemote(option, false, dummyOption, false);
        }

        public void processDO(int option) throws StreamStateException {
            System.err.format("recv DO   %d%n", option);
            negotiateLocal(option, false, dummyOption, true);
            throw new StreamStateException(); // do it for giggles!
        }

        public void processDONT(int option) throws StreamStateException {
            System.err.format("recv DONT %d%n", option);
            negotiateLocal(option, false, dummyOption, false);
        }

        //
        // Option sub-negotiation.
        //
        public void beginParam(int code) {
            System.err.format("recv SB   %d%n", code);
        }

        public void endParam() throws StreamStateException {
            System.err.println();
            System.err.println("recv SE");
        }

        public void appendParam(byte nextByte) {
            System.err.format("%02X ", nextByte);
        }

        //
        // Generate responses for option negotiation.
        //
        private void negotiateLocal(int option, boolean can, boolean current,
                boolean next) {
            if (current == next) {
                // Don't respond if we're already in the next state.
                return;
            }

            if (can && next) {
                // Accept.
                addEvent(CODE_WILL, option);
            } else {
                // Reject.
                addEvent(CODE_WONT, option);
            }
        }

        private void negotiateRemote(int option, boolean can, boolean current,
                boolean next) {
            if (current == next) {
                // Don't respond if they're already in the next state.
                return;
            }

            if (can && next) {
                // Accept.
                addEvent(CODE_DO, option);
            } else {
                // Reject.
                addEvent(CODE_DONT, option);
            }
        }
    }
}
