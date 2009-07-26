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
 * Base class to help manage TELNET option state.
 * 
 * @author cu5
 */
public abstract class TELNETOption {
    private final int option;

    private boolean enabled;
    private boolean trying;

    public TELNETOption(int option) {
        this.option = option;
    }

    public int getOption() {
        return option;
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Tries to enable this option.
     * 
     * @throws IOException
     */
    public void tryEnable() throws IOException {
        if (isEnabled()) {
            // Already enabled.
        } else {
            if (trying) {
                // Already trying.
            } else {
                // Request enabling.
                requestEnable();
                trying = true;
            }
        }
    }

    /**
     * Enables this option.
     * 
     * @throws IOException
     */
    public void enable() throws IOException {
        if (isEnabled()) {
            // Already enabled.
        } else {
            if (trying) {
                // Confirmed.
                trying = false;
            } else {
                // Acknowledge.
                requestEnable();
            }

            enabled = true;
            doEnable();
        }
    }

    /**
     * Disables this option.
     * 
     * @throws IOException
     */
    public void disable() throws IOException {
        if (isEnabled()) {
            // Disable.
            requestDisable();

            enabled = false;
            doDisable();
        } else {
            if (trying) {
                // Rejected.
            } else {
                // Already disabled.
            }
        }
    }

    public void beginParam() {
        // Discard by default.
    }

    public void endParam() throws IOException {
        // Discard by default.
    }

    public void appendParam(byte nextByte) {
        // Discard by default.
    }

    protected abstract void requestEnable() throws IOException;

    protected abstract void requestDisable() throws IOException;

    protected void doEnable() throws IOException {
        // Only toggle by default.
    }

    protected void doDisable() throws IOException {
        // Only toggle by default.
    }
}
