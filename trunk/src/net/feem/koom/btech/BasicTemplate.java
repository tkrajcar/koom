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

import java.util.HashMap;
import java.util.Map;

/**
 * Basic, generic template.
 * 
 * @author cu5
 */
public class BasicTemplate implements Template {
    private final TemplateType type;
    private final TemplateMoveType moveType;

    private String ref;
    private String name;

    private final Map<String, TemplateSection> sections = new HashMap<String, TemplateSection>();

    // Temporary storage for various properties, while we get things in order.
    private final Map<String, String> misc = new HashMap<String, String>();

    public BasicTemplate(TemplateType type, TemplateMoveType moveType) {
        this.type = type;
        this.moveType = moveType;
    }

    @Override
    public TemplateType getType() {
        return type;
    }

    @Override
    public TemplateMoveType getMoveType() {
        return moveType;
    }

    @Override
    public String getRef() {
        return ref;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public TemplateSection getSection(String name) {
        return sections.get(name);
    }

    @Override
    public Iterable<String> getSectionNames() {
        return sections.keySet();
    }

    public void setRef(String ref) {
        this.ref = ref;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProperty(String name) {
        return misc.get(name);
    }

    public void setProperty(String name, String value) {
        misc.put(name, value);
    }

    public TemplateSection addSection(String name) {
        TemplateSection section = new TemplateSection();
        sections.put(name, section);
        return section;
    }
}
