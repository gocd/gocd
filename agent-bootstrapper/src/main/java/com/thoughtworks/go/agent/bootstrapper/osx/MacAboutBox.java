/*
 * Copyright 2017 ThoughtWorks, Inc.
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

package com.thoughtworks.go.agent.bootstrapper.osx;

import com.thoughtworks.go.CurrentGoCDVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

public class MacAboutBox extends JFrame implements ActionListener {
    private static final Logger LOG = LoggerFactory.getLogger(MacAboutBox.class);
    private static final int FRAME_WIDTH = 280;
    private static final int FRAME_HEIGHT = 230;
    private static final int FRAME_TOP = 200;
    private static final int FRAME_LEFT = 350;
    private static final Font TITLE_FONT = new Font("Lucida Grande", Font.BOLD, 14);
    private static final Font BODY_FONT = new Font("Lucida Grande", Font.PLAIN, 10);


    public MacAboutBox() {
        super("");
        setResizable(false);
        addWindowListener(new SymWindow());

        getContentPane().setLayout(new BorderLayout(15, 15));


        List<JLabel> aboutLabels = new ArrayList<>();
        aboutLabels.add(emptyLabel());
        aboutLabels.add(titleLabel("Go Agent"));
        aboutLabels.add(bodyLabel("Bootstrapper Version " + getBootstrapperVersion()));
        aboutLabels.add(emptyLabel());
        aboutLabels.add(bodyLabel("Java Version " + System.getProperty("java.version")));
        aboutLabels.add(bodyLabel("Copyright (C) 2015 ThoughtWorks Inc."));
        aboutLabels.add(emptyLabel());

        Panel textPanel2 = new Panel(new GridLayout(aboutLabels.size(), 1));
        for (JLabel aboutLabel : aboutLabels) {
            textPanel2.add(aboutLabel);
        }

        getContentPane().add(textPanel2, BorderLayout.CENTER);
        pack();
        setLocation(FRAME_LEFT, FRAME_TOP);
        setSize(FRAME_WIDTH, FRAME_HEIGHT);
        setResizable(false);
    }

    private String getBootstrapperVersion() {
        return CurrentGoCDVersion.getInstance().fullVersion();
    }

    private JLabel emptyLabel() {
        return label("", null);
    }

    private JLabel titleLabel(String content) {
        return label(content, TITLE_FONT);
    }

    private JLabel bodyLabel(String content) {
        return label(content, BODY_FONT);
    }

    private JLabel label(String content, Font font) {
        JLabel jLabel = new JLabel(content);
        jLabel.setHorizontalAlignment(JLabel.CENTER);
        if (font != null) {
            jLabel.setFont(font);
        }
        return jLabel;
    }

    class SymWindow extends WindowAdapter {
        public void windowClosing(WindowEvent event) {
            setVisible(false);
        }
    }

    public void actionPerformed(ActionEvent newEvent) {
        setVisible(false);
    }
}
