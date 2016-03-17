/*
 * Copyright 2016 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.thoughtworks.go.server.launcher;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

public class MacAboutBox extends JFrame implements ActionListener {
    protected JLabel titleLabel;
    protected JLabel[] aboutLabel;
    protected int labelCount = 8;
    protected int aboutWidth = 280;
    protected int aboutHeight = 230;
    protected int aboutTop = 200;
    protected int aboutLeft = 350;
    protected Font titleFont, bodyFont;

    public MacAboutBox() {
        super("");
        setResizable(false);
        SymWindow aSymWindow = new SymWindow();
        addWindowListener(aSymWindow);

        // Initialize useful fonts
        titleFont = new Font("Lucida Grande", Font.BOLD, 14);
        if (titleFont == null) {
            titleFont = new Font("SansSerif", Font.BOLD, 14);
        }
        bodyFont = new Font("Lucida Grande", Font.PLAIN, 10);
        if (bodyFont == null) {
            bodyFont = new Font("SansSerif", Font.PLAIN, 10);
        }

        getContentPane().setLayout(new BorderLayout(15, 15));

        aboutLabel = new JLabel[labelCount];
        aboutLabel[0] = new JLabel("");
        aboutLabel[1] = new JLabel("Go Server");
        aboutLabel[1].setFont(titleFont);
        aboutLabel[2] = new JLabel(
                "Server Version " + getCruiseVersion("go.jar"));
        aboutLabel[2].setFont(bodyFont);
        aboutLabel[3] = new JLabel("<html><a href='http://localhost:8153/go'>"
                + "http://localhost:8153/go</a></html>");
        aboutLabel[3].setFont(bodyFont);
        aboutLabel[3].setSize(aboutLabel[3].getPreferredSize());
        aboutLabel[3].addMouseListener(new LinkMouseListener());
        aboutLabel[4] = new JLabel("");
        aboutLabel[5] = new JLabel("Java Version " + System.getProperty("java.version"));
        aboutLabel[5].setFont(bodyFont);
        aboutLabel[6] = new JLabel("Copyright (C) 2015 ThoughtWorks Inc.");
        aboutLabel[6].setFont(bodyFont);
        aboutLabel[7] = new JLabel("");

        Panel textPanel2 = new Panel(new GridLayout(labelCount, 1));
        for (int i = 0; i < labelCount; i++) {
            aboutLabel[i].setHorizontalAlignment(JLabel.CENTER);
            textPanel2.add(aboutLabel[i]);
        }
        getContentPane().add(textPanel2, BorderLayout.CENTER);
        pack();
        setLocation(aboutLeft, aboutTop);
        setSize(aboutWidth, aboutHeight);
    }

    class SymWindow extends java.awt.event.WindowAdapter {
        public void windowClosing(java.awt.event.WindowEvent event) {
            setVisible(false);
        }
    }

    public void actionPerformed(ActionEvent newEvent) {
        setVisible(false);
    }

    public static String getCruiseVersion(String jar) {
        String version = null;
        try {
            JarFile jarFile = new JarFile(jar);
            Manifest manifest = jarFile.getManifest();
            if (manifest != null) {
                Attributes attributes = manifest.getMainAttributes();
                version = attributes.getValue("Go-Version");
            }
        } catch (IOException e) {
        }
        return version;
    }

    private class LinkMouseListener implements MouseListener {
        private final Cursor def = new Cursor(Cursor.DEFAULT_CURSOR);
        private final Cursor link = new Cursor(Cursor.HAND_CURSOR);

        public void mouseClicked(MouseEvent e) {
            // Launch Page
            try {
                Runtime.getRuntime().exec("open http://localhost:8153/go");
            } catch (IOException e1) {
                // Don't care
            }
        }

        public void mousePressed(MouseEvent e) {
            // Don't care
        }

        public void mouseReleased(MouseEvent e) {
            // Don't care
        }

        public void mouseEntered(MouseEvent e) {
            // Change cursor to hand
            setCursor(link);
        }

        public void mouseExited(MouseEvent e) {
            // Change cursor to default
            setCursor(def);
        }
    }
}
