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

package com.thoughtworks.go.agent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.thoughtworks.go.agent.common.launcher.AgentProcessParent;
import com.thoughtworks.go.agent.common.util.Downloader;
import com.thoughtworks.go.agent.common.util.JarUtil;
import com.thoughtworks.go.agent.launcher.DownloadableFile;
import com.thoughtworks.go.agent.launcher.ServerBinaryDownloader;
import com.thoughtworks.go.util.GoConstants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import static com.thoughtworks.go.agent.common.util.LoggingHelper.CONSOLE_NDC.STDERR;
import static com.thoughtworks.go.agent.common.util.LoggingHelper.CONSOLE_NDC.STDOUT;
import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.apache.commons.lang.StringUtils.join;

public class AgentProcessParentImpl implements AgentProcessParent {

    /* 40-50 for launcher error codes*/
    public static final int EXCEPTION_OCCURRED = -373;
    static final String AGENT_STARTUP_ARGS = "AGENT_STARTUP_ARGS";
    static final String GO_AGENT_STDERR_LOG = "go-agent-stderr.log";
    static final String GO_AGENT_STDOUT_LOG = "go-agent-stdout.log";
    private static final Log LOG = LogFactory.getLog(AgentProcessParentImpl.class);

    public int run(String launcherVersion, String launcherMd5, ServerUrlGenerator urlGenerator, Map<String, String> env) {
        int exitValue = 0;
        LOG.info("Agent is version: " + JarUtil.getGoVersion(Downloader.AGENT_BINARY));
        String command[] = new String[]{};

        try {
            ServerBinaryDownloader agentDownloader = new ServerBinaryDownloader(urlGenerator, DownloadableFile.AGENT);
            String serverBaseUrl = agentDownloader.downloadIfNecessary().serverBaseUrl();

            ServerBinaryDownloader pluginZipDownloader = new ServerBinaryDownloader(urlGenerator, DownloadableFile.AGENT_PLUGINS);
            pluginZipDownloader.downloadIfNecessary().serverBaseUrl();

            command = agentInvocationCommand(serverBaseUrl, agentDownloader.md5(), launcherMd5, env, launcherVersion, pluginZipDownloader.md5());
            LOG.info("Launching Agent with command: " + join(command, " "));

            Process agent = invoke(command);

            // The next lines prevent the child process from blocking on Windows
            agent.getOutputStream().close();
            AgentConsoleLogThread stdErrThd = new AgentConsoleLogThread(agent.getErrorStream(), STDERR, GO_AGENT_STDERR_LOG);
            stdErrThd.start();
            AgentConsoleLogThread stdOutThd = new AgentConsoleLogThread(agent.getInputStream(), STDOUT, GO_AGENT_STDOUT_LOG);
            stdOutThd.start();

            Shutdown shutdownHook = new Shutdown(agent);
            Runtime.getRuntime().addShutdownHook(shutdownHook);
            try {
                exitValue = agent.waitFor();
            } catch (InterruptedException ie) {
                LOG.error("Agent was interrupted. Terminating agent and respawning. " + ie.toString());
                agent.destroy();
            } finally {
                removeShutdownHook(shutdownHook);
                stdErrThd.stopAndJoin();
                stdOutThd.stopAndJoin();
            }
        } catch (Exception e) {
            LOG.error("Exception while executing command: " + join(command, " ") + " - " + e.toString());
            exitValue = EXCEPTION_OCCURRED;
        }
        return exitValue;
    }

    private void removeShutdownHook(Shutdown shutdownHook) {
        try {
            Runtime.getRuntime().removeShutdownHook(shutdownHook);
        } catch (Exception e) {
        }
    }

    private String[] agentInvocationCommand(String serverBaseUrl, String md5, String launcherMd5, Map<String, String> env, String launcherVersion, String agentPluginsZipMd5) {
        String startupArgsString = env.get(AGENT_STARTUP_ARGS);

        List<String> commandSnippets = new ArrayList<>();
        commandSnippets.add(javaCmd());
        if (!isEmpty(startupArgsString)) {
            String[] startupArgs = startupArgsString.split(" ");
            for (String startupArg : startupArgs) {
                String decodedStartupArg = startupArg.trim().replace("%20", " ");
                if (!isEmpty(decodedStartupArg)) {
                    commandSnippets.add(decodedStartupArg);
                }
            }
        }
        commandSnippets.add(property(GoConstants.AGENT_LAUNCHER_VERSION, launcherVersion));
        commandSnippets.add(property(GoConstants.AGENT_PLUGINS_MD5, agentPluginsZipMd5));
        commandSnippets.add(property(GoConstants.AGENT_JAR_MD5, md5));
        commandSnippets.add(property(GoConstants.GIVEN_AGENT_LAUNCHER_JAR_MD5, launcherMd5));
        commandSnippets.add("-jar");
        commandSnippets.add(Downloader.AGENT_BINARY);
        commandSnippets.add(serverBaseUrl);

        return commandSnippets.toArray(new String[]{});
    }

    private String property(final String name, String value) {
        return "-D" + name + "=" + value;
    }

    private String javaCmd() {
        String javaHome = System.getProperty("java.home");
        String pathSep = System.getProperty("file.separator");
        return javaHome + pathSep + "bin" + pathSep + "java";
    }

    Process invoke(String[] command) throws IOException {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        return processBuilder.start();
    }

    private static class Shutdown extends Thread {
        private static final Log LOG = LogFactory.getLog(Shutdown.class);
        private final Process agent;

        public Shutdown(Process agent) {
            setName("Shutdown" + getName());
            this.agent = agent;
        }

        public void run() {
            LOG.info("Shutdown hook invoked. Shutting down " + agent);
            agent.destroy();
        }
    }
}
