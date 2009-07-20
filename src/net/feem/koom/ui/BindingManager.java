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
package net.feem.koom.ui;

import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

/**
 * Command binding manager. The session manager is responsible for global
 * bindings, while individual user interfaces are responsible for local
 * bindings.
 * 
 * <p>
 * For example, a binding to quit the application should be in the session
 * manager, but cut and paste bindings should be local to each user interface,
 * or indeed, individual components. This binding manager just provides a place
 * to manage all of this.
 * </p>
 * 
 * <p>
 * Note that for various implementation reasons, sharing bindings can be a bit
 * impractical. Therefore, this binding manager takes the approach of adding and
 * removing bindings at definition time, rather than query time.
 * </p>
 * 
 * @author cu5
 */
public class BindingManager {
    private final BindingManager root;
    private final BindingManager parent;
    private final Set<BindingManager> children = new HashSet<BindingManager>();

    private final Map<String, ActionFactory> commands = new HashMap<String, ActionFactory>();
    private final Map<Trigger, String> bindings = new HashMap<Trigger, String>();

    public BindingManager() {
        this.root = this;
        this.parent = null;
    }

    public BindingManager(BindingManager parent) {
        this.root = parent.getRoot();
        this.parent = parent;
        parent.addChild(this);
    }

    public BindingManager getRoot() {
        return root;
    }

    public void addCommand(String command, ActionFactory factory) {
        commands.put(command, factory);

        for (BindingManager child : children) {
            child.notifyChangedCommand(command);
        }
    }

    public void removeCommand(String command) {
        commands.remove(command);

        for (BindingManager child : children) {
            child.notifyChangedCommand(command);
        }
    }

    public void addBinding(Trigger trigger, String command) {
        bindings.put(trigger, command);

        for (BindingManager child : children) {
            child.notifyChangedBinding(trigger);
        }
    }

    public void removeBinding(Trigger trigger, String command) {
        bindings.remove(trigger);

        for (BindingManager child : children) {
            child.notifyChangedBinding(trigger);
        }
    }

    protected void addChild(BindingManager child) {
        children.add(child);
    }

    protected void removeChild(BindingManager child) {
        children.remove(child);
    }

    protected void notifyExisting() {
        Deque<BindingManager> stack = new LinkedList<BindingManager>();

        BindingManager root = parent;
        while (root != null) {
            stack.addFirst(root);
            root = root.parent;
        }

        for (BindingManager next : stack) {
            for (String command : next.commands.keySet()) {
                processChangedCommand(command);
            }

            for (Trigger trigger : next.bindings.keySet()) {
                processChangedBinding(trigger);
            }
        }
    }

    protected void notifyChangedCommand(String command) {
        if (commands.containsKey(command)) {
            return;
        }

        processChangedCommand(command);

        for (BindingManager child : children) {
            child.notifyChangedCommand(command);
        }
    }

    protected void notifyChangedBinding(Trigger trigger) {
        if (bindings.containsKey(trigger)) {
            return;
        }

        processChangedBinding(trigger);

        for (BindingManager child : children) {
            child.notifyChangedBinding(trigger);
        }
    }

    protected void processChangedCommand(String command) {
    }

    protected void processChangedBinding(Trigger trigger) {
    }

    protected ActionFactory getAction(String command) {
        ActionFactory factory = commands.get(command);
        if (factory == null) {
            if (parent == null) {
                return null;
            }

            return parent.getAction(command);
        }

        return factory;
    }

    protected String getCommand(Trigger trigger) {
        String command = bindings.get(trigger);
        if (command == null) {
            if (parent == null) {
                return null;
            }

            return parent.getCommand(trigger);
        }

        return command;
    }
}
