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

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextPane;

import net.feem.koom.services.Preferences;
import net.feem.koom.services.Version;

/**
 * @author cu5
 */
@SuppressWarnings("serial")
class Console extends JPanel {
    private final JTextArea entry;

    Console() {
        super(new BorderLayout());

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true);
        split.setResizeWeight(1);

        entry = new JTextArea(5, 80);
        entry.setFont(Preferences.getMonoFont());
        // entry.addActionListener(new ActionListener() {
        // @Override
        // public void actionPerformed(ActionEvent evt) {
        // entry.setText(null);
        // }
        // });
        split.setBottomComponent(entry);

        JTextPane text = new JTextPane();
        text.setEditable(false);
        text.setFont(Preferences.getMonoFont());
        text.setText(Version.getShortLicense());
        JScrollPane scroll = new JScrollPane(text);
        split.setTopComponent(scroll);

        add(split);
    }

    public void doFocus() {
        entry.requestFocusInWindow();
    }
}
