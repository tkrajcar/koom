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
package net.feem.koom.world;

import java.util.Arrays;

/**
 * World record.
 * 
 * @author cu5
 */
public class World {
    private final String name;
    private char[] secret;

    public World(String name) {
        this.name = name;
    }

    public void dispose() {
        Arrays.fill(secret, '\0');
    }

    public void setSecret(char[] secret) {
        this.secret = secret;
    }

    public String getName() {
        return name;
    }

    public boolean checkAuth(CharSequence cs, int start, int end) {
        if (end - start != secret.length) {
            // Not even the same length.
            return false;
        }

        int idx = 0;
        for (int ii = start; ii < end; ii++) {
            if (secret[idx++] != cs.charAt(ii)) {
                // Mismatch.
                return false;
            }
        }

        return true;
    }
}
