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
package net.feem.koom.btech;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import net.feem.koom.services.Resources;

/**
 * Access to the mechref database.
 * 
 * <p>
 * Make sure not to ask for an arbitrarily large number of mechrefs--this store
 * caches even non-existent mechref results.
 * </p>
 * 
 * <p>
 * TODO: We may put some sort of configurable limit on null result caching.
 * </p>
 * 
 * @author cu5
 */
public class TemplateStore {
    private final TemplateFileParser parser = new TemplateFileParser();

    // Holds loaded templates forever.
    private final Map<String, Template> cache = new HashMap<String, Template>();

    public Template getTemplate(String mechref) {
        // Check the cache first.
        if (cache.containsKey(mechref)) {
            return cache.get(mechref);
        }

        // Read in the template.
        Template template;
        try {
            template = parseTemplate(mechref);
        } catch (IOException ex) {
            // I/O error, don't cache result so we can try again.
            return null;
        }

        // A null result is still valid; it just means no such entry.
        cache.put(mechref, template);
        return template;
    }

    private Template parseTemplate(String name) throws IOException {
        InputStream stream = Resources.getResourceAsStream("/units/" + name);
        if (stream == null) {
            // No such resource.
            return null;
        }

        return parser.parse(stream);
    }
}
