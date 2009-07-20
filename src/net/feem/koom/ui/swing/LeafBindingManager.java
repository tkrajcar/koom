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
package net.feem.koom.ui.swing;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.KeyStroke;

import net.feem.koom.ui.BindingManager;
import net.feem.koom.ui.KeyStrokeTrigger;
import net.feem.koom.ui.Trigger;

/**
 * @author cu5
 * 
 */
class LeafBindingManager extends BindingManager {
    private final JComponent target;

    LeafBindingManager(BindingManager parent, JComponent target) {
        super(parent);

        this.target = target;

        notifyExisting();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * net.feem.koom.ui.BindingManager#addChild(net.feem.koom.ui.BindingManager)
     */
    @Override
    protected void addChild(BindingManager child) {
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * net.feem.koom.ui.BindingManager#removeChild(net.feem.koom.ui.BindingManager
     * )
     */
    @Override
    protected void removeChild(BindingManager child) {
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * net.feem.koom.ui.BindingManager#notifyChangedCommand(java.lang.String)
     */
    @Override
    protected void processChangedCommand(String command) {
        Action action = getAction(command).createAction(target);
        target.getActionMap().put(command, action);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * net.feem.koom.ui.BindingManager#notifyChangedBinding(net.feem.koom.ui
     * .Trigger)
     */
    @Override
    protected void processChangedBinding(Trigger trigger) {
        if (trigger instanceof KeyStrokeTrigger) {
            KeyStroke key = (KeyStroke) trigger.getValue();
            String command = getCommand(trigger);
            target.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                    .put(key,
                    command);
        }

    }
}
