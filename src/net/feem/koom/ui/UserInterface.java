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
package net.feem.koom.ui;

import net.feem.koom.Session;
import net.feem.koom.SessionManager;

/**
 * @author cu5
 * 
 */
public interface UserInterface extends Runnable {
    /**
     * Requests that the user interface terminate. The user interface is
     * responsible for then calling {@link SessionManager#releaseInterface}.
     */
    public void requestTerminate();

    /**
     * Switches the user interface's current session.
     * 
     * @param session
     *            the new session
     */
    public void setSession(Session session);
}
