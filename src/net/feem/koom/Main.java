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
package net.feem.koom;

import net.feem.koom.services.Version;

/**
 * Launcher for the Koom client.
 * 
 * @author cu5
 */
public class Main {
    /**
     * Initial entry point into the Koom client. This method is just responsible
     * for starting the session manager thread, then returning. The JVM won't
     * actually terminate until all the non-daemon threads have exited.
     * 
     * @param args
     */
    public static void main(String[] args) {
        System.out.format("Starting Koom %s (build %s)...%n", Version
                .getVersion(), Version.getBuild());

        SessionManager manager = new SessionManager();

        // TODO: Configure manager using command line arguments here.

        new Thread(manager, "SessionManager").start();
    }
}
