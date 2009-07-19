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
package net.feem.koom.ui.swing;

import javax.swing.SwingUtilities;

import net.feem.koom.Session;
import net.feem.koom.ui.UserInterface;

/**
 * A Swing-based user interface instance. Each Swing-based user interface
 * consists of a collection of windows, rooted around a terminal emulator
 * window, with several subsidiary windows for tactical displays.
 * 
 * @author cu5
 */
public class SwingUserInterface implements UserInterface {
    private final Terminal terminal = new Terminal(this);

    private Session session;

    /**
     * Construct
     */
    public SwingUserInterface() {
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                terminal.pack();
                terminal.setVisible(true);
            }
        });

        // TODO: Wait around forever to manage the user interface.
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.feem.koom.ui.UserInterface#attach(net.feem.koom.Session)
     */
    @Override
    public void setSession(Session session) {
        synchronized (this) {
            this.session = session;

            // Notify children of session change.
            terminal.setSession(session);
        }
    }
}
