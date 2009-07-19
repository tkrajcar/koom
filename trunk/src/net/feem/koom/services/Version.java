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

/**
 * Provides abstract access to Koom version information.
 * 
 * @author cu5
 */
public class Version {
    private static final String version;
    private static final String build;

    static {
        Package koom = Package.getPackage("net.feem.koom");

        version = wrapString(koom.getSpecificationVersion());
        build = wrapString(koom.getImplementationVersion());
    }

    private static String wrapString(String raw) {
        return (raw == null) ? "???" : raw;
    }

    public static String getVersion() {
        return version;
    }

    public static String getBuild() {
        return build;
    }
}
