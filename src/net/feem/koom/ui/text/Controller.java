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

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.feem.koom.net.ServerNVT;
import net.feem.koom.net.SocketConnection;
import net.feem.koom.net.TELNETProtocol;
import net.feem.koom.services.Utility;
import net.feem.koom.world.World;

/**
 * Text-based session controller.
 * 
 * @author cu5
 */
class Controller implements Runnable {
    // Allow up to 3 attempts.
    private static final int MAX_ATTEMPTS = 3;

    // Wait up to roughly 30 seconds for success.
    private static final long MAX_COMMAND_TIME = 30000;

    // Matches strings of the form "connect WORLD SECRET", ignoring whitespace.
    private static final Pattern loginPat = Pattern.compile(
            "\\s*con(?:nect)?\\s+(\\S+)\\s+(\\S+)\\s*",
            Pattern.CASE_INSENSITIVE);

    private final SocketConnection socket;
    private final TELNETProtocol telnet;
    private final ServerNVT server;

    private static void scrub(StringBuilder sb) {
        for (int ii = 0; ii < sb.length(); ii++) {
            sb.setCharAt(ii, '\0');
        }

        sb.setLength(0);
    }

    public Controller(SocketConnection socket) {
        this.socket = socket;
        this.telnet = new TELNETProtocol(socket);
        this.server = new ServerNVT(telnet);
    }

    @Override
    public void run() {
        try {
            // Wait for the initial command.
            World world = waitForHandshake();
            if (world == null) {
                System.out.println("Login failed");
                return;
            }

            System.out.println("Connecting to " + world.getName());

            // Proceed to main I/O loop.
        } catch (IOException ex) {
            // FIXME: Report error.
            ex.printStackTrace();
        } finally {
            Utility.close(telnet);
        }

        System.out.println("Connection closed");
    }

    private World waitForHandshake() throws IOException {
        // Read the initial login command.
        StringBuilder sb = new StringBuilder(128);
        char[] cbuf = new char[128];

        socket.setTimeout(1000);
        try {
            return readLogin(sb, cbuf);
        } finally {
            socket.setTimeout(0);

            scrub(sb);

            for (int ii = 0; ii < cbuf.length; ii++) {
                cbuf[ii] = '\0';
            }
        }
    }

    private World readLogin(StringBuilder sb, char[] cbuf) throws IOException {
        // Protect against excessive resource consumption.
        int attempts = 0;
        final long start = System.currentTimeMillis();

        while (true) {
            // Wait for I/O.
            try {
                int len = server.read(cbuf, 0, cbuf.length);
                if (len == -1) {
                    // End of stream.
                    break;
                }

                sb.append(cbuf, 0, len);
            } catch (SocketTimeoutException ex) {
                // Interrupt handled by checking the time later.
            }

            if (server.readIsLine()) {
                // Check attempt.
                Matcher mat = loginPat.matcher(sb);
                if (mat.matches()) {
                    // TODO: Implement world management.
                    World world = new World(mat.group(1));
                    world.setSecret(mat.group(2).toCharArray());
                    if (world.checkAuth(sb, mat.start(2), mat.end(2))) {
                        return world;
                    }
                }

                // Reset for next attempt.
                if (++attempts == MAX_ATTEMPTS) {
                    // Reached maximum number of attempts.
                    break;
                }

                scrub(sb);
            } else if (System.currentTimeMillis() - start > MAX_COMMAND_TIME) {
                // Exceeded maximum time.
                break;
            }
        }

        // OK, they blew it.
        return null;
    }
}
