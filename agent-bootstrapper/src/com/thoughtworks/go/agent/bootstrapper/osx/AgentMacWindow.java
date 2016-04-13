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

import com.apple.eawt.Application;
import com.thoughtworks.go.agent.common.AgentBootstrapperArgs;
import com.thoughtworks.go.agent.bootstrapper.BootstrapperLoggingHelper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.swing.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

public class AgentMacWindow extends JFrame {
    private static final File PREFS_FILE = new File(System.getProperty("user.home"), "/Library/Preferences/com.thoughtworks.go.agent.properties");
    private static final Log LOG = LogFactory.getLog(AgentMacWindow.class);

    private AgentBootstrapperArgs bootstrapperArgs;
    private final boolean firstRun;
    private final MacBootstrapperApplicationAdapter macBootstrapperAppAdapter;
    private MacBootstrapperThread bootstrapLauncher;

    public static void main(String[] args) throws IOException {
        BootstrapperLoggingHelper.initLog4j();
        new AgentMacWindow().go();
    }

    private void go() {
        if (firstRun) {
            macBootstrapperAppAdapter.requestInitialPreferences();
        }
        launchBootStrapper();
    }

    private AgentMacWindow() {
        firstRun = !PREFS_FILE.exists();

        if (firstRun) {
            saveDefaultPrefs();
        }

        bootstrapperArgs = loadPrefs();
        macBootstrapperAppAdapter = initializeApplicationAdapter();
    }

    private void saveDefaultPrefs() {
        LOG.info("Initializing preferences in " + PREFS_FILE);
        savePrefs(defaultArgs());
    }

    private void savePrefs(AgentBootstrapperArgs args) {
        try {
            try (FileOutputStream fos = new FileOutputStream(PREFS_FILE)) {
                args.toProperties().store(fos, null);
            }
        } catch (IOException e) {
            LOG.error("IO error on " + PREFS_FILE, e);
        }
    }

    private MacBootstrapperApplicationAdapter initializeApplicationAdapter() {
        Application application = Application.getApplication();

        application.setEnabledPreferencesMenu(true);
        application.setEnabledAboutMenu(true);

        setVisible(false);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setResizable(false);
        setSize(getPreferredSize());

        MacBootstrapperApplicationAdapter applicationAdapter = new MacBootstrapperApplicationAdapter(this);
        application.addApplicationListener(applicationAdapter);
        return applicationAdapter;
    }

    private AgentBootstrapperArgs defaultArgs() {
        try {
            return new AgentBootstrapperArgs(new URL("https://127.0.0.1:8154/go"), null, AgentBootstrapperArgs.SslMode.NONE);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    private AgentBootstrapperArgs loadPrefs() {
        try (FileInputStream stream = new FileInputStream(PREFS_FILE)) {
            Properties myProps = new Properties();
            myProps.load(stream);
            LOG.info("Loaded preferences from " + PREFS_FILE);
            return AgentBootstrapperArgs.fromProperties(myProps);
        } catch (Exception e) {
            LOG.error("Error for " + PREFS_FILE, e);
        }
        return defaultArgs();
    }

    void launchBootStrapper() {
        if (bootstrapLauncher == null) {
            bootstrapLauncher = new MacBootstrapperThread(bootstrapperArgs);
            bootstrapLauncher.start();
        }
    }

    void stopBootStrapper() {
        if (bootstrapLauncher != null) {
            bootstrapLauncher.stopLooping();
            bootstrapLauncher.interrupt();
            try {
                Thread.sleep(500);
            } catch (Exception ignore) {
            }
            bootstrapLauncher.stop();
            bootstrapLauncher = null;
        }
    }

    public AgentBootstrapperArgs getBootstrapperArgs() {
        return bootstrapperArgs;
    }

    public void setBootstrapperArgs(AgentBootstrapperArgs bootstrapperArgs) {
        this.bootstrapperArgs = bootstrapperArgs;
        savePrefs(this.bootstrapperArgs);
        restartBootstrapper();
    }

    private void restartBootstrapper() {
        stopBootStrapper();
        launchBootStrapper();
    }

}
