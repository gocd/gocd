/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.agent.bootstrapper.osx;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JTextField;
import javax.swing.JPanel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public class MacPreferencesPane extends JFrame {
    private static final Log LOG = LogFactory.getLog(AgentMacWindow.class);
    private JTextField serverTextField;
    private String originalHost;

    public MacPreferencesPane(final AgentMacWindow agentMacWindow) {
        super();

        getContentPane().setLayout(new BorderLayout(10, 10));
        JLabel prefsText = new JLabel("Go Server Hostname or IP");

        serverTextField = new JTextField("");
        serverTextField.setColumns(15);
        serverTextField.selectAll();

        JPanel textPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        textPanel.add(prefsText);
        textPanel.add(serverTextField);

        getContentPane().add(textPanel, BorderLayout.NORTH);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));

        JButton okButton = new JButton("OK");
        buttonPanel.add(okButton);
        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent newEvent) {
                String newHost = serverTextField.getText();
                if (!originalHost.equals(newHost)) {
                    LOG.info("Server changed to " + newHost);
                    agentMacWindow.setHost(newHost);
                } else {
                    LOG.info("Server is still " + originalHost);
                }

                setVisible(false);
            }
        });
        getContentPane().add(buttonPanel, BorderLayout.SOUTH);

        setSize(getPreferredSize());
        setLocation(20, 40);
        setResizable(false);
    }

    public void ask() {
        this.serverTextField.setText(originalHost);
        serverTextField.selectAll();
        serverTextField.grabFocus();
        setVisible(true);
    }

    public void setOriginalHost(String originalHost) {
        this.originalHost = originalHost;
    }
}
