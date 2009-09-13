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
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;

import net.feem.koom.services.Preferences;
import net.feem.koom.services.Version;

/**
 * @author cu5
 * 
 */
@SuppressWarnings("serial")
class AboutBox extends JDialog {
    private static final Document license = new PlainDocument();

    // Only show one copy of the about box at a time.
    private static boolean showing;

    static {
        try {
            license.insertString(0, Version.getLongLicense(), null);
        } catch (BadLocationException ex) {
            // Not going to happen.
        }
    }

    public AboutBox(Window owner) {
        super(owner);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent evt) {
                showing = false;
            }
        });

        setTitle("About Koom");
        setIconImage(Version.getIcon().getImage());

        JPanel versionArea = new JPanel();
        JLabel versionLabel = new JLabel();
        versionLabel.setIcon(Version.getIcon());
        versionLabel.setText(Version.getCopyright());
        versionArea.add(versionLabel);
        add(versionArea, BorderLayout.NORTH);

        JTextArea licenseArea = new JTextArea(license, null, 25, 0);
        licenseArea.setEditable(false);
        licenseArea.setFont(Preferences.getMonoFont());
        JScrollPane scroll = new JScrollPane(licenseArea);
        add(scroll);

        pack();
    }

    public void doVisible() {
        if (showing) {
            // Already showing an about box.
        } else {
            showing = true;
            setVisible(true);
        }
    }
}
