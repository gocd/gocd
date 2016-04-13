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

package com.thoughtworks.go.agent.bootstrapper.osx;

import com.beust.jcommander.ParameterException;
import com.thoughtworks.go.agent.common.AgentBootstrapperArgs;
import com.thoughtworks.go.agent.common.CertificateFileValidator;
import com.thoughtworks.go.agent.common.ServerUrlValidator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;


public class MacPreferencesPane extends JFrame {
    private static final Log LOG = LogFactory.getLog(AgentMacWindow.class);
    private JTextField serverTextField;
    private FileBrowser fileBrowser;
    private JButton okButton;
    private AgentMacWindow agentMacWindow;
    private SslModeComponent sslModeComponent;

    public MacPreferencesPane(final AgentMacWindow agentMacWindow) {
        super();
        this.agentMacWindow = agentMacWindow;
        BorderLayout border = new BorderLayout(10, 10);
        getContentPane().setLayout(border);

        createView();


        sslModeComponent.noneModeRadioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                fileBrowser.setEnabled(!sslModeComponent.noneModeRadioButton.isSelected());
            }
        });

        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent newEvent) {
                try {
                    AgentBootstrapperArgs newArgs = new AgentBootstrapperArgs(
                            new URL(serverTextField.getText()),
                            fileBrowser.getFile(), sslModeComponent.getSslMode());

                    try {
                        new ServerUrlValidator().validate("The server url", newArgs.getServerUrl().toExternalForm());
                    } catch (ParameterException e) {
                        JOptionPane.showMessageDialog(getContentPane(), e.getMessage(), "Invalid server url", JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    if (newArgs.getRootCertFile() != null) {
                        try {
                            new CertificateFileValidator().validate("The server root certificate", newArgs.getRootCertFile().getPath());
                        } catch (ParameterException e) {
                            JOptionPane.showMessageDialog(getContentPane(), e.getMessage(), "Invalid server root certificate", JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                    }

                    if (!newArgs.equals(agentMacWindow.getBootstrapperArgs())) {
                        agentMacWindow.setBootstrapperArgs(newArgs);
                        LOG.info("Updating preferences to " + newArgs);
                    } else {
                        LOG.info("Preferences are unchanged " + newArgs);
                    }
                    setVisible(false);
                } catch (MalformedURLException e) {
                    JOptionPane.showMessageDialog(getContentPane(), "The server url must be an HTTPS url and must begin with https://", "Invalid server url", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        setSize(getPreferredSize());
        setLocation(20, 30);
        setResizable(false);
    }

    private void createView() {
        JPanel controlsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        serverTextField = new JTextField("");
        serverTextField.setColumns(15);
        serverTextField.selectAll();

        JPanel textPanel = new JPanel(new GridLayout(4, 2, 0, 10));

        textPanel.add(new JLabel("Go Server Hostname or IP"));
        textPanel.add(serverTextField);

        textPanel.add(new JLabel("SSL Mode"));
        sslModeComponent = new SslModeComponent();
        textPanel.add(sslModeComponent);

        textPanel.add(new JLabel("Server root certificate"));
        fileBrowser = new FileBrowser();
        textPanel.add(fileBrowser);

        controlsPanel.add(textPanel);

        getContentPane().add(controlsPanel, BorderLayout.NORTH);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));

        okButton = new JButton("OK");
        buttonPanel.add(okButton);
        getContentPane().add(buttonPanel, BorderLayout.SOUTH);
    }

    public void ask() {
        AgentBootstrapperArgs bootstrapperArgs = agentMacWindow.getBootstrapperArgs();

        this.serverTextField.setText(bootstrapperArgs.getServerUrl().toString());
        this.fileBrowser.setFile(bootstrapperArgs.getRootCertFile());
        this.sslModeComponent.setSslMode(bootstrapperArgs.getSslMode());
        serverTextField.selectAll();
        serverTextField.grabFocus();
        setVisible(true);
    }

    private class FileBrowser extends JPanel {
        private File file;
        private final JTextField textField;
        private final JButton browse;

        FileBrowser() {
            super();
            setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));

            textField = new JTextField(15);
            textField.setEnabled(false);
            add(textField);

            browse = new JButton("Browse");
            add(browse);

            browse.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    JFileChooser jFileChooser = new JFileChooser(file != null ? file.getParentFile() : null);
                    int returnVal = jFileChooser.showOpenDialog(FileBrowser.this);

                    if (returnVal == JFileChooser.APPROVE_OPTION) {
                        setFile(jFileChooser.getSelectedFile());
                    }
                }
            });
        }

        @Override
        public void setEnabled(boolean enabled) {
            super.setEnabled(enabled);
            browse.setEnabled(enabled);
        }

        public File getFile() {
            return file;
        }

        public void setFile(File file) {
            this.file = file;
            if (file != null) {
                textField.setText(file.toString());
            }
        }
    }

    private class SslModeComponent extends JPanel {

        private AgentBootstrapperArgs.SslMode sslVerificationMode;
        private final JRadioButton fullModeRadioButton;
        private final JRadioButton noneModeRadioButton;
        private final JRadioButton noHostVerifyModeRadioButton;

        SslModeComponent() {
            super();
            setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));

            fullModeRadioButton = new JRadioButton("Full Verification");
            fullModeRadioButton.putClientProperty("SSL_MODE", AgentBootstrapperArgs.SslMode.FULL);
            fullModeRadioButton.setToolTipText("Perform a complete SSL verification before connecting to the agent.");

            noneModeRadioButton = new JRadioButton("No verification");
            noneModeRadioButton.putClientProperty("SSL_MODE", AgentBootstrapperArgs.SslMode.NONE);
            noneModeRadioButton.setToolTipText("Completely disable any SSL verification");

            noHostVerifyModeRadioButton = new JRadioButton("Don't verify host");
            noHostVerifyModeRadioButton.putClientProperty("SSL_MODE", AgentBootstrapperArgs.SslMode.NO_VERIFY_HOST);
            noHostVerifyModeRadioButton.setToolTipText("Verify the server certificate, but not the hostname.");

            ButtonGroup sslModeButtonGroup = new ButtonGroup();

            ActionListener actionListener = new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    JRadioButton b = (JRadioButton) e.getSource();
                    setSslMode((AgentBootstrapperArgs.SslMode) b.getClientProperty("SSL_MODE"));

                }
            };

            for (JRadioButton button : Arrays.asList(fullModeRadioButton, noneModeRadioButton, noHostVerifyModeRadioButton)) {
                sslModeButtonGroup.add(button);
                add(button);
                button.addActionListener(actionListener);
            }
        }

        private void setSslMode(AgentBootstrapperArgs.SslMode sslVerificationMode) {
            switch (sslVerificationMode) {
                case FULL:
                    fullModeRadioButton.setSelected(true);
                    fileBrowser.setEnabled(true);
                    break;
                case NONE:
                    noneModeRadioButton.setSelected(true);
                    fileBrowser.setEnabled(false);
                    break;
                case NO_VERIFY_HOST:
                    noHostVerifyModeRadioButton.setSelected(true);
                    fileBrowser.setEnabled(true);
                    break;
            }
            this.sslVerificationMode = sslVerificationMode;
        }

        public AgentBootstrapperArgs.SslMode getSslMode() {
            return sslVerificationMode;
        }
    }
}
