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

import com.apple.eawt.Application;
import com.apple.eawt.ApplicationEvent;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import static java.util.Arrays.asList;

public class GoMacLauncher extends JFrame {
    private static final Logger LOG = Logger.getLogger(GoMacLauncher.class.getCanonicalName());
    protected MacAboutBox aboutBox;
    private Application application = Application.getApplication();
    private static final int MAX_SLEEP_SECONDS = 2400;
    private static final int SLEEP_MILLIS = 1000;
    private static final String APPLICATION_SUPPORT_PATHNAME =
            MessageFormat.format("{0}/Library/Application Support/{1}/", System.getProperty("user.home"), System.getProperty("go.application.name", "Go Server"));
    private static final String GO_CONFIG_DIRECTORY_PATH = APPLICATION_SUPPORT_PATHNAME + "config/";
    private static final String CRUISE_SERVER_URL = "http://localhost:8153/go/";

    public static void main(String[] args) {
        new File(GO_CONFIG_DIRECTORY_PATH).mkdirs();
        new GoMacLauncher().spawnProcessAndWait();
    }

    public GoMacLauncher() throws HeadlessException {
        application.addApplicationListener(new MyApplicationAdapter());

        application.setEnabledAboutMenu(true);
        application.setEnabledPreferencesMenu(false);

        setVisible(false);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setResizable(false);
        setSize(getPreferredSize());
    }

    private int spawnProcessAndWait() {
        int exitValue = 0;

        String sep = System.getProperty("file.separator");
        String defaultJava = System.getProperty("java.home") + sep + "bin" + sep + "java";
        String java = System.getProperty("go.java.to.use", defaultJava);

        String startMem = System.getProperty("cruise.server.mem", "512");
        String maxMem = System.getProperty("cruise.server.maxmem", "1024");
        String perm = System.getProperty("cruise.server.permgen", "128");
        String maxPerm = System.getProperty("cruise.server.maxpermgen", "256");
        String lang = System.getProperty("cruise.server.lang", "en");
        String country = System.getProperty("cruise.server.country", "US");
        boolean dbDebugMode = System.getProperty("cruise.server.db_debug_mode") != null;

        final List<String> arguments = new ArrayList<>();
        arguments.add(java);
        arguments.add("-Xms" + startMem + "m");
        arguments.add("-Xmx" + maxMem + "m");
        arguments.add("-XX:PermSize=" + perm + "m");
        arguments.add("-XX:MaxPermSize=" + maxPerm + "m");
        if (dbDebugMode) {
            arguments.add("-DDB_DEBUG_MODE=true");
        }
        arguments.add("-Dapple.awt.UIElement=true");
        arguments.add("-Duser.language=" + lang);
        arguments.add("-Duser.country=" + country);
        arguments.add("-Dcruise.config.file=" + GO_CONFIG_DIRECTORY_PATH + "cruise-config.xml");
        arguments.add("-Dcruise.config.dir=" + GO_CONFIG_DIRECTORY_PATH);
        arguments.add("-Dcruise.server.port=" + System.getProperty("cruise.server.port", "8153"));
        arguments.add("-Dcruise.server.ssl.port=" + System.getProperty("cruise.server.ssl.port", "8154"));
        addOtherArguments(arguments);
        arguments.add("-jar");
        arguments.add(new File(System.getProperty("go.server.mac.go.jar.dir", "."), "go.jar").getAbsolutePath());
        LOG.info("Running server as: " + arguments);

        String[] args = arguments.toArray(new String[arguments.size()]);

        try {
            setUpApplicationSupport();

            ProcessBuilder processBuilder = new ProcessBuilder(args);
            processBuilder.directory(new File(APPLICATION_SUPPORT_PATHNAME));
            processBuilder.redirectErrorStream(true);
            processBuilder.redirectOutput(ProcessBuilder.Redirect.appendTo(new File(APPLICATION_SUPPORT_PATHNAME, "go-server.log")));
            final Process server = processBuilder.start();
            server.getOutputStream().close();

            Thread shutdownHook = new Thread(new Shutdown(server));
            Runtime.getRuntime().addShutdownHook(shutdownHook);


            Thread progressThread = new Thread(new Runnable() {
                public void run() {
                    try {
                        displayLaunchingProgress(server);
                    } catch (InterruptedException e) {
                        // Don't care
                    } catch (IOException e) {
                        LOG.severe("Exception while executing command: " + arguments + " - " + e.toString());
                    }
                }
            });

            progressThread.start();

            try {
                exitValue = server.waitFor();
                if (exitValue != 0) {
                    LOG.severe(String.format(
                            "Server was terminated with exit code %d.  Please see %sgo-server.log for more details.",
                            exitValue, APPLICATION_SUPPORT_PATHNAME));
                    System.exit(1);
                }
            } catch (InterruptedException ie) {
                LOG.severe("Server was interrupted. Terminating. " + ie.toString());
                server.destroy();
            }

            Runtime.getRuntime().removeShutdownHook(shutdownHook);
        } catch (IOException e) {
            LOG.severe("Exception while executing command: " + asList(args) + " - " + e.toString());
        }
        return exitValue;
    }

    /* Any property prefixed by GO_, is passed along as a property, after stripping the GO_ from it. */
    private void addOtherArguments(List<String> currentArguments) {
        Properties properties = System.getProperties();
        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            String key = String.valueOf(entry.getKey());
            if (key.startsWith("GO_")) {
                String value = String.valueOf(entry.getValue());
                currentArguments.add("-D" + key.substring(3) + "=" + value);
            }
        }
    }

    private void displayLaunchingProgress(Process server)
            throws InterruptedException, IOException {
        //Create and set up the window.
        final JFrame frame = new JFrame("Go Server");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        //Create and set up the content pane.
        final LaunchProgress progressPane = new LaunchProgress();
        progressPane.setOpaque(true); //content panes must be opaque
        frame.setContentPane(progressPane);

        //Display the window.
        frame.pack();
        frame.setSize(frame.getPreferredSize());
        frame.setVisible(true);

        // Wait for the server to start
        int slept = 0;
        progressPane.progressBar.setIndeterminate(true);
        boolean startedOK = false;

        // When this URL gets a 200 code back we know we're good to go
        URL url = new URL(CRUISE_SERVER_URL);
        while ((slept * SLEEP_MILLIS / 1000) <= MAX_SLEEP_SECONDS) {
            Thread.sleep(SLEEP_MILLIS);
            progressPane.progressBar.setValue(slept);
            slept++;
            try {
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.connect();
                if (urlConnection.getResponseCode() == 200) {
                    LOG.info("Server up, let's go");
                    startedOK = true;
                    break;
                }
            } catch (Exception e) {
                // Don't care
                LOG.info("Server not up yet, sleeping...");
            }
        }

        if (!startedOK) {
            String errormsg = "Server not up within " + MAX_SLEEP_SECONDS + " seconds.";
            LOG.severe(errormsg);
            server.destroy();
            progressPane.progressBar.setVisible(false);
            progressPane.label.setText(errormsg + " Please see /var/log/system.log for more info.");
            frame.pack();
            frame.setSize(frame.getPreferredSize());
            Thread.sleep(15000);
            System.exit(1);
        }

        frame.setVisible(false);
        frame.dispose();

        // Now launch a browser window pointing at it
        Runtime.getRuntime().exec("open " + CRUISE_SERVER_URL);
    }

    private void setUpApplicationSupport() throws IOException {
        File applicationSupport = new File(APPLICATION_SUPPORT_PATHNAME);
        applicationSupport.mkdirs();

        if (!applicationSupport.isDirectory()) {
            throw new IOException(
                    "Could not create folder " + APPLICATION_SUPPORT_PATHNAME +
                            ". Please check the permission settings for folder " + applicationSupport.getParentFile().getAbsolutePath());
        }

    }

    private class LaunchProgress extends JPanel {
        public JProgressBar progressBar;
        public JLabel label;

        private LaunchProgress() {
            super(new BorderLayout());

            progressBar = new JProgressBar();
            progressBar.setIndeterminate(false);
            progressBar.setStringPainted(true);

            JPanel panel = new JPanel();
            label = new JLabel("Starting up");
            label.setHorizontalAlignment(JLabel.CENTER);
            panel.add(label);
            panel.add(progressBar);

            add(panel, BorderLayout.PAGE_START);
            setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        }
    }

    private static class Shutdown implements Runnable {
        private final Process server;

        public Shutdown(Process server) {
            this.server = server;
        }

        public void run() {
            server.destroy();
        }
    }

    private class MyApplicationAdapter extends com.apple.eawt.ApplicationAdapter {
        public void handleAbout(ApplicationEvent e) {
            if (aboutBox == null) {
                aboutBox = new MacAboutBox();
            }
            LOG.info("Got About describeChange");
            aboutBox.setResizable(false);
            aboutBox.setVisible(true);
            e.setHandled(true);
        }

        public void handleQuit(ApplicationEvent event) {
            LOG.info("Got the boot...");
            super.handleQuit(event);
            System.exit(0);
        }
    }
}
