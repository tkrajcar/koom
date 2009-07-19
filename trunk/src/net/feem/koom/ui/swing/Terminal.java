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

import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;

import net.feem.koom.Session;

/**
 * A Swing-based terminal emulator window.
 * 
 * @author cu5
 */
class Terminal extends JFrame {
    private final SwingUserInterface ui;

    private final JMenuBar menubar;

    Terminal(SwingUserInterface ui) {
        this.ui = ui;

        // Configure frame.
        setTitle("Koom - disconnected");

        JTextPane text = new JTextPane();
        text.setEditable(false);
        JScrollPane scroll = new JScrollPane(text);
        add(scroll);

        final JTextField entry = new JTextField(80);
        add(entry, BorderLayout.SOUTH);

        addWindowFocusListener(new WindowAdapter() {
            public void windowGainedFocus(WindowEvent evt) {
                entry.requestFocusInWindow();
            }
        });

        // Configure menu bar with basic items.
        menubar = new JMenuBar();

        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic((int) 'F');
        fileMenu.add(new JMenuItem("Close", 'C'));
        fileMenu.addSeparator();
        fileMenu.add(new JMenuItem("Exit", 'x'));
        menubar.add(fileMenu);

        JMenu helpMenu = new JMenu("Help");
        helpMenu.setMnemonic((int) 'H');
        helpMenu.add(new JMenuItem("About", 'A'));
        menubar.add(helpMenu);

        setJMenuBar(menubar);

        // Register handler on window close.
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent evt) {
                doClose();
            }
        });
    }

    private void doClose() {
        // FIXME: This should just terminate the interface. Once we run out of
        // interfaces, then we can terminate the application.
        System.exit(0);
    }

    public void setSession(Session session) {
    }
}
