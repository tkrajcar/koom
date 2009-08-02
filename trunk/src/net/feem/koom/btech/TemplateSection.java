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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Basic, generic section.
 * 
 * @author cu5
 */
public class TemplateSection {
    private int armor = -1;
    private int internals = -1;
    private int rear = -1;

    private final ArrayList<String> crits = new ArrayList<String>();

    // Temporary storage for various properties, while we get things in order.
    private final Map<String, String> misc = new HashMap<String, String>();

    public int getArmor() {
        return armor;
    }

    public int getInternals() {
        return internals;
    }

    public int getRear() {
        return rear;
    }

    /**
     * Returns the number of crit slots.
     * 
     * @return crit slot count
     */
    public int getCritCount() {
        return crits.size();
    }

    /**
     * Gets the specified crit slot.
     * 
     * <p>
     * Note that crit slots are indexed starting from 1, not 0.
     * </p>
     * 
     * @param idx
     *            crit slot index
     * 
     * @return crit slot value
     */
    public String getCrit(int idx) {
        return crits.get(idx - 1);
    }

    public void setArmor(int armor) {
        this.armor = armor;
    }

    public void setInternals(int internals) {
        this.internals = internals;
    }

    public void setRear(int rear) {
        this.rear = rear;
    }

    /**
     * Sets the specified crit slot. Any missing intervening slots will
     * automatically be created with <code>null</code> values.
     * 
     * <p>
     * Note that crit slots are indexed starting from 1, not 0.
     * </p>
     * 
     * @param idx
     *            crit slot index
     * @param value
     *            crit slot value
     */
    public void setCrit(int idx, String value) {
        crits.ensureCapacity(idx);

        for (int ii = crits.size(); ii < idx; ii++) {
            crits.add(null);
        }

        crits.set(idx - 1, value);
    }

    public String getProperty(String name) {
        return misc.get(name);
    }

    public void setProperty(String name, String value) {
        misc.put(name, value);
    }
}
