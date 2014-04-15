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

package com.thoughtworks.go.server.util;

import java.awt.BorderLayout;
import java.awt.HeadlessException;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.URL;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import com.apple.eawt.Application;
import com.apple.eawt.ApplicationEvent;
import sun.net.www.protocol.http.HttpURLConnection;

import static java.util.Arrays.asList;

public class GoMacLauncher extends JFrame {
    private static final Logger LOG = Logger.getLogger(GoMacLauncher.class.getCanonicalName());
    protected MacAboutBox aboutBox;
    private Application application = Application.getApplication();
    private static final int MAX_SLEEP_SECONDS = 2400;
    private static final int SLEEP_MILLIS = 1000;
    private static final String APPLICATION_SUPPORT_PATHNAME = System.getProperty("user.home")+"/Library/Application Support/Go Server/";
    private static final String GO_CONFIG_DIRECTORY_PATH = APPLICATION_SUPPORT_PATHNAME + "config/";
    private static final String CRUISE_SERVER_URL = "http://localhost:8153/go/";

    public static void main(String[] args) {
        new File(GO_CONFIG_DIRECTORY_PATH).mkdirs();

        //TODO: remove this call after a few releases
        moveFilesFromGoServerDirToConfigDir();

        new GoMacLauncher().spawnProcessAndWait();
    }

    private static void moveFilesFromGoServerDirToConfigDir() {
        File dir = new File(APPLICATION_SUPPORT_PATHNAME);
        File[] files = null;

        try {
            files = dir.listFiles(new FileFilter() {
                @Override public boolean accept(File file) {
                    if (file.isDirectory()) {
                        return false;
                    }
                    String fileName = file.getName();
                    if (fileName.matches("cruise-config\\.xml.*")) {
                        return true;
                    }
                    if (fileName.matches(".*\\.properties.*")) {
                        return true;
                    }
                    if (fileName.matches(".*\\.jks")) {
                        return true;
                    }
                    if (fileName.matches(".*store$")) {
                        return true;
                    }
                    if (fileName.matches("cruise-config\\.xsd")) {
                        return true;
                    }
                    if (fileName.matches("^go-server$")) {
                        return true;
                    }
                    if (fileName.matches("jetty\\.xml")) {
                        return true;
                    }
                    if (fileName.matches("cipher")) {
                        return true;
                    }
                    return false;
                }
            });

            if (files != null && files.length > 0) {
                for (File currentFile : files) {
                    moveFileToConfigDir(currentFile);
                }
            }
        } catch (Exception e) {
            LOG.severe("Exception while moving files: " + e.toString());
        }
    }

    private static void moveFileToConfigDir(File file) throws IOException {
        LOG.info("Moving file: " + file.getAbsolutePath());
        boolean success = file.renameTo(new File(GO_CONFIG_DIRECTORY_PATH + file.getName()));
        if (!success) {
            throw new IOException("Could not move file: " + file.getAbsolutePath());
        }
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

        String java = System.getProperty("java.home") + System.getProperty("file.separator") +
                "bin" + System.getProperty("file.separator") + "java";

        String startMem = System.getProperty("cruise.server.mem", "512");
        String maxMem = System.getProperty("cruise.server.maxmem", "1024");
        String perm = System.getProperty("cruise.server.permgen", "128");
        String maxPerm = System.getProperty("cruise.server.maxpermgen", "256");
        String lang = System.getProperty("cruise.server.lang", "en");
        String country = System.getProperty("cruise.server.country", "US");
        boolean dbDebugMode = System.getProperty("cruise.server.db_debug_mode") != null;

        final String[] args = new String[]{
                java,
                "-Xms" + startMem + "m",
                "-Xmx" + maxMem + "m",
                "-XX:PermSize=" + perm + "m",
                "-XX:MaxPermSize=" + maxPerm + "m",
                // Stupid exec doesn't support an empty or null argument.
                dbDebugMode ? "-DDB_DEBUG_MODE=true" : "-Dignored=true",
                "-Duser.language=" + lang,
                "-Duser.country=" + country,
                "-Dcruise.config.file=" + GO_CONFIG_DIRECTORY_PATH + "cruise-config.xml",
                "-Dcruise.config.dir=" + GO_CONFIG_DIRECTORY_PATH,
                "-Dcruise.server.port=8153",
                "-Dcruise.server.ssl.port=8154",
                "-jar", new File("go.jar").getAbsolutePath()
        };

        LOG.info("Running server as: " + asList(args));


        try {
            setUpApplicationSupport();

            final Process server = Runtime.getRuntime().exec(args, null,
                    new File(APPLICATION_SUPPORT_PATHNAME));
            // The next three lines prevent the child process from blocking on Windows
            server.getErrorStream().close();
            server.getInputStream().close();
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
                        LOG.severe("Exception while executing command: " + asList(args) + " - " + e.toString());
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
                HttpURLConnection urlConnection = new HttpURLConnection(url, null);
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
