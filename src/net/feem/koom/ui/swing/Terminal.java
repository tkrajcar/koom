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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

import net.feem.koom.Session;
import net.feem.koom.services.Version;
import net.feem.koom.ui.BindingManager;

/**
 * A Swing-based terminal emulator window.
 * 
 * @author cu5
 */
@SuppressWarnings("serial")
class Terminal extends JFrame {
    private final SwingUserInterface ui;
    private final BindingManager bindings;
    private final JMenuBar menubar;

    Terminal(SwingUserInterface ui) {
        this.ui = ui;

        this.bindings = ui.createBindingManager(this.getRootPane());

        // Configure frame.
        setIconImage(Version.getIcon().getImage());
        setTitle("Koom - disconnected");

        final Console console = new Console();
        add(console);

        addWindowFocusListener(new WindowAdapter() {
            public void windowGainedFocus(WindowEvent evt) {
                console.doFocus();
            }
        });

        // Configure menu bar with basic items.
        this.menubar = new JMenuBar();

        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic((int) 'F');
        fileMenu.add(new JMenuItem("Close", 'C'));
        menubar.add(fileMenu);

        JMenu helpMenu = new JMenu("Help");
        helpMenu.setMnemonic((int) 'H');
        JMenuItem aboutItem = new JMenuItem("About", 'A');
        final AboutBox aboutBox = new AboutBox(this);
        aboutItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                aboutBox.doVisible();
            }
        });
        helpMenu.add(aboutItem);
        menubar.add(helpMenu);

        setJMenuBar(menubar);

        // Register handler to terminate this interface on window close.
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent evt) {
                doClose();
            }
        });

        // Realize GUI.
        pack();

        console.doFocus();
        setVisible(true);
    }

    private void doClose() {
        ui.requestTerminate();
    }

    public void setSession(Session session) {
    }
}
