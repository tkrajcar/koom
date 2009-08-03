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
import java.util.regex.Pattern;

import net.feem.koom.net.ServerNVT;
import net.feem.koom.net.SocketConnection;
import net.feem.koom.net.TELNETProtocol;
import net.feem.koom.services.Utility;

/**
 * Text-based session controller.
 * 
 * @author cu5
 */
class Controller implements Runnable {
    // Matches strings of the form "connect WORLD SECRET", ignoring whitespace.
    private static final Pattern loginPat = Pattern.compile(
            "\\s*connect\\s+(\\S+)\\s+(\\S+)\\s*", Pattern.CASE_INSENSITIVE);

    private final SocketConnection socket;
    private final TELNETProtocol telnet;
    private final ServerNVT server;

    public Controller(SocketConnection socket) {
        this.socket = socket;
        this.telnet = new TELNETProtocol(socket);
        this.server = new ServerNVT(telnet);
    }

    @Override
    public void run() {
        // Wait for the initial command.
        try {
            waitForCommand();
        } catch (IOException ex) {
            // Terminate the connection now.
            Utility.close(telnet);
            return;
        }

        // Proceed to main I/O loop.
    }

    private void waitForCommand() throws IOException {
        // Wait up to 5 seconds at a time.
        socket.setTimeout(5000);

        // Allow 3 attempts.
        server.read(cbuf, off, len);

        // OK, they blew it.
    }

    // The magic string.
    private String getMagic() {

    }
}
