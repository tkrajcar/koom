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

import javax.swing.JComponent;

import net.feem.koom.Session;
import net.feem.koom.SessionManager;
import net.feem.koom.ui.BindingManager;
import net.feem.koom.ui.UserInterface;

/**
 * A Swing-based user interface instance. Each Swing-based user interface
 * consists of a collection of windows, rooted around a terminal emulator
 * window, with several subsidiary windows for tactical displays.
 * 
 * @author cu5
 */
public class SwingUserInterface implements UserInterface {
    private final SessionManager manager;
    private final BindingManager bindings;
    private boolean terminated;

    private final Terminal terminal;

    /**
     * Construct user interface.
     */
    public SwingUserInterface(SessionManager manager) {
        this.manager = manager;
        this.bindings = new BindingManager(manager.getBindings());

        this.terminal = new Terminal(this);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run() {
        // TODO: Spawn child threads within this thread group.
    }

    public BindingManager createBindingManager(JComponent component) {
        synchronized (bindings.getRoot()) {
            return new LeafBindingManager(bindings, component);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.feem.koom.ui.UserInterface#requestTerminate()
     */
    @Override
    public void requestTerminate() {
        synchronized (this) {
            if (terminated) {
                return;
            }

            terminated = true;
        }

        manager.releaseInterface(this);
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.feem.koom.ui.UserInterface#attach(net.feem.koom.Session)
     */
    @Override
    public void setSession(Session session) {
        synchronized (this) {
            // Notify children of session change.
            terminal.setSession(session);
        }
    }
}
