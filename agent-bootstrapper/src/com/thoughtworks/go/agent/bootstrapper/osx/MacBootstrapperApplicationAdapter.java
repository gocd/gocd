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

import com.apple.eawt.ApplicationEvent;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


class MacBootstrapperApplicationAdapter extends com.apple.eawt.ApplicationAdapter {
    private static final Log LOG = LogFactory.getLog(MacBootstrapperApplicationAdapter.class);
    private MacAboutBox aboutBox;
    private MacPreferencesPane prefPane;
    private AgentMacWindow agentMacWindow;

    public MacBootstrapperApplicationAdapter(AgentMacWindow agentMacWindow) {
        this.agentMacWindow = agentMacWindow;
    }

    public void handleAbout(ApplicationEvent e) {
        if (aboutBox == null) {
            aboutBox = new MacAboutBox();
        }
        LOG.info("Got About event");
        aboutBox.setVisible(true);

        e.setHandled(true);
    }

    public void handlePreferences(ApplicationEvent e) {
        if (prefPane == null) {
            prefPane = new MacPreferencesPane(agentMacWindow);
        }
        LOG.info("Got Preferences event");

        prefPane.ask();

        if (e != null) {
            e.setHandled(true);
        }
    }

    public void handleQuit(ApplicationEvent event) {
        LOG.info("Got the boot...");
        super.handleQuit(event);
        System.exit(0);
    }

    public void requestInitialPreferences() {
        handlePreferences(null);

        while (prefPane.isVisible()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // Don't care
            }
        }
    }
}
