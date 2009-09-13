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
package net.feem.koom;

import static javax.swing.Action.ACCELERATOR_KEY;
import static javax.swing.Action.ACTION_COMMAND_KEY;

import java.awt.event.ActionEvent;
import java.util.HashSet;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.KeyStroke;
import javax.swing.UIManager;

import net.feem.koom.ui.BindingManager;
import net.feem.koom.ui.KeyStrokeTrigger;
import net.feem.koom.ui.StaticActionFactory;
import net.feem.koom.ui.UserInterface;
import net.feem.koom.ui.swing.SwingUserInterface;

/**
 * A session manager controls all the sessions and user interfaces for a given
 * execution of Koom.
 * 
 * @author cu5
 */
public class SessionManager implements Runnable {
    private final String LOOK_AND_FEEL = "com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel";

    private final BindingManager bindings = new BindingManager();
    private final Set<UserInterface> activeUIs = new HashSet<UserInterface>();

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Runnable#run()
     */
    @SuppressWarnings("serial")
    @Override
    public void run() {
        // Set the look and feel.
        try {
            UIManager.setLookAndFeel(LOOK_AND_FEEL);
        } catch (Exception ex) {
            // The default look and feel is fine, too.
        }

        // TODO: Remove this debugging code.
        Action quitAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.exit(2);
            }
        };

        String quitCommand = "QUIT";
        quitAction.putValue(ACTION_COMMAND_KEY, quitCommand);

        KeyStroke quitKey = KeyStroke.getKeyStroke("ctrl pressed Q");
        quitAction.putValue(ACCELERATOR_KEY, quitKey);

        bindings.addCommand(quitCommand, new StaticActionFactory(quitAction));
        bindings.addBinding(new KeyStrokeTrigger(quitKey), quitCommand);

        // There's only one user interface for now, but possibly several later.
        UserInterface mainUI = new SwingUserInterface(this);
        activeUIs.add(mainUI);
        new Thread(mainUI, "MainUI").start();

        // Each session goes in its own thread group.
        Session mainSession = new Session();
        ThreadGroup mainGroup = new ThreadGroup("MainSession");
        new Thread(mainGroup, mainSession, "MainSession").start();

        // We tell the mainUI to use the main session.
        mainUI.setSession(mainSession);

        // Wait around forever to manage sessions.
        synchronized (activeUIs) {
            while (!activeUIs.isEmpty()) {
                try {
                    activeUIs.wait();
                } catch (InterruptedException ex) {
                    // Harmless, we just check the condition again.
                }
            }
        }

        System.exit(0);
    }

    /**
     * Removes a user interface from the active set. This should be called by a
     * user interface once it's finished terminating. When all user interfaces
     * have been released, the application will safely terminate.
     * 
     * @param ui
     *            terminating user interface
     */
    public void releaseInterface(UserInterface ui) {
        synchronized (activeUIs) {
            activeUIs.remove(ui);

            if (activeUIs.isEmpty()) {
                activeUIs.notify();
            }
        }
    }

    /**
     * Gets the manager for global key bindings.
     * 
     * @return global bindings
     */
    public BindingManager getBindings() {
        return bindings;
    }
}
