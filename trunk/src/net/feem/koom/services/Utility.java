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
package net.feem.koom.services;

import java.io.Closeable;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * @author cu5
 */
public class Utility {
    /**
     * Closes a closeable object, ignoring <code>null</code> values and
     * {@link IOException}s as a convenience.
     * 
     * @param closeable
     *            the closeable
     */
    public static void close(Closeable closeable) {
        if (closeable == null) {
            return;
        }

        try {
            closeable.close();
        } catch (IOException ex) {
            // Don't care.
        }
    }

    /**
     * Encodes a string as an array of ASCII bytes. Mostly used to pre-encode
     * constants.
     * 
     * @param string
     *            Java string
     * 
     * @return US-ASCII byte array
     */
    public static byte[] getASCII(String string) {
        try {
            return string.getBytes("US-ASCII");
        } catch (UnsupportedEncodingException ex) {
            // All JVMs should support US-ASCII.
            throw new AssertionError(ex);
        }
    }

    /**
     * Encodes a string as an array of UTF-8 bytes. Mostly used to pre-encode
     * constants.
     * 
     * @param string
     *            Java string
     * 
     * @return UTF-8 byte array
     */
    public static byte[] getUTF8(String string) {
        try {
            return string.getBytes("UTF-8");
        } catch (UnsupportedEncodingException ex) {
            // All JVMs should support UTF-8.
            throw new AssertionError(ex);
        }
    }
}
