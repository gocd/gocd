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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import javax.swing.JFrame;

import com.apple.eawt.Application;
import com.thoughtworks.go.agent.bootstrapper.BootstrapperLoggingHelper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import static java.lang.Integer.parseInt;
import static org.apache.commons.io.IOUtils.closeQuietly;

public class AgentMacWindow extends JFrame {
    private static final File PREFS_FILE = new File(System.getProperty("user.home"),
            "/Library/Preferences/com.thoughtworks.studios.cruise.agent.properties");
    private static final Log LOG = LogFactory.getLog(AgentMacWindow.class);

    private final Properties properties;
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

    private AgentMacWindow() throws IOException {
        firstRun = !PREFS_FILE.exists();

        if (firstRun) {
            saveDefaultPrefs();
        }

        properties = loadPrefs();
        macBootstrapperAppAdapter = initializeApplicationAdapter();
    }

    private static void saveDefaultPrefs() {
        LOG.info("Initializing preferences in " + PREFS_FILE);
        savePrefs(defaultProperties());
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

    private static Properties defaultProperties() {
        Properties props = new Properties();
        props.setProperty("server", "127.0.0.1");
        props.setProperty("port", "8153");
        return props;
    }

    private static Properties loadPrefs() throws IOException {
        FileInputStream stream = null;
        try {
            stream = new FileInputStream(PREFS_FILE);
            Properties myProps = defaultProperties();
            myProps.load(stream);
            LOG.info("Loaded preferences from " + PREFS_FILE);
            return myProps;
        } catch (Exception e) {
            LOG.error("File not found for " + PREFS_FILE, e);
        } finally {
            closeQuietly(stream);
        }

        return new Properties();
    }

    private static void savePrefs(Properties prefs) {
        FileOutputStream stream = null;
        try {
            stream = new FileOutputStream(PREFS_FILE, false);
            prefs.store(stream, null);
        } catch (FileNotFoundException e) {
            LOG.error("File not found for " + PREFS_FILE, e);
        } catch (IOException e) {
            LOG.error("IO exception on " + PREFS_FILE, e);
        } finally {
            closeQuietly(stream);
        }
    }

    void launchBootStrapper() {
        if (bootstrapLauncher == null) {
            bootstrapLauncher = new MacBootstrapperThread(getHost(), getPort());
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

    public String getHost() {
        return properties.getProperty("server");
    }

    public void setHost(String newHost) {
        properties.setProperty("server", newHost);
        savePrefs(properties);
        restartBootstrapper();
    }

    private void restartBootstrapper() {
        stopBootStrapper();
        launchBootStrapper();
    }

    private int getPort() {
        return parseInt(properties.getProperty("port"));
    }

}
