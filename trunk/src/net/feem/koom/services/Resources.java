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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URL;

import javax.swing.ImageIcon;

/**
 * @author cu5
 * 
 */
public class Resources {
    public static URL getResource(String name) {
        return Resources.class.getResource(name);
    }

    public static InputStream getResourceAsStream(String name) {
        return Resources.class.getResourceAsStream(name);
    }

    /**
     * Gets the contents of a resource as an icon.
     * 
     * @param name
     *            resource name
     * @return @{link ImageIcon} of the resource
     */
    public static ImageIcon getResourceAsIcon(String name) {
        URL url = getResource(name);
        if (url == null) {
            return null;
        }

        return new ImageIcon(url);
    }

    /**
     * Gets the contents of a resource as a UTF-8 string.
     * 
     * @param name
     *            resource name
     * @return {@link String} containing resource contents, or <code>null</code>
     *         if there was a problem reading the resource
     */
    public static String getResourceAsText(String name) {
        // Construct UTF-8 reader for the resource.
        Reader reader;

        InputStream stream = getResourceAsStream(name);
        if (stream == null) {
            return null;
        }

        stream = new BufferedInputStream(getResourceAsStream(name));

        try {
            reader = new InputStreamReader(stream, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            // All JVMs are required to support UTF-8.
            Utility.close(stream);
            return null;
        }

        // Return string from resource contents.
        StringBuilder text = new StringBuilder();
        char[] buffer = new char[8192];
        int count;

        try {
            while ((count = reader.read(buffer)) != -1) {
                text.append(buffer, 0, count);
            }
        } catch (IOException ex) {
            return null;
        } finally {
            Utility.close(reader);
        }

        return text.toString();
    }
}
