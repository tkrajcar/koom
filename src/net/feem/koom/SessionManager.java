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

import net.feem.koom.ui.UserInterface;
import net.feem.koom.ui.swing.SwingUserInterface;

/**
 * A session manager controls all the sessions and user interfaces for a given
 * execution of Koom.
 * 
 * @author cu5
 */
public class SessionManager implements Runnable {
    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run() {
        // There's only one user interface for now, but possibly several later.
        UserInterface mainUI = new SwingUserInterface();
        new Thread(mainUI, "MainUI").start();

        // Each session goes in its own thread group.
        Session mainSession = new Session();
        ThreadGroup mainGroup = new ThreadGroup("MainSession");
        new Thread(mainGroup, mainSession, "MainSession").start();

        // We tell the mainUI to use the main session.
        mainUI.setSession(mainSession);

        // TODO: Wait around forever to manage sessions.
    }
}
