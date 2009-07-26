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

import java.io.IOException;

/**
 * Handles TELNET events on behalf of a {@link TELNETProtocol}.
 * 
 * <p>
 * An event handler may throw a {@link StreamStateException} under certain
 * circumstances, as a signal that a state change has occurred that modifies any
 * further processing. After catching such an exception, the caller should make
 * sure it still is configured correctly for the current options.
 * </p>
 * 
 * <p>
 * It is acceptable to block in event handlers for extended periods of time.
 * </p>
 * 
 * <p>
 * Most of the TELNET commands are antiquated in a modern operating environment,
 * but we still wish to behave correctly in case they're issued. Conceptually,
 * we emulate an NVT that has zero latency and infinite speed, making most
 * TELNET commands no-ops.
 * </p>
 * 
 * @author cu5
 */
public interface TELNETEventHandler {
    //
    // Miscellaneous incoming command processing.
    //
    public void processCommand(byte code) throws IOException;

    //
    // Option negotiation.
    //
    public void processWILL(int option) throws IOException;

    public void processWONT(int option) throws IOException;

    public void processDO(int option) throws IOException;

    public void processDONT(int option) throws IOException;

    //
    // Option sub-negotiation.
    //
    public void beginParam(int option) throws IOException;

    public void endParam() throws IOException;

    public void appendParam(byte nextByte) throws IOException;
}
