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

package com.thoughtworks.go.agent;

import com.thoughtworks.go.CurrentGoCDVersion;
import com.thoughtworks.go.agent.common.AgentBootstrapperBackwardCompatibility;
import com.thoughtworks.go.agent.common.launcher.AgentProcessParent;
import com.thoughtworks.go.agent.common.util.Downloader;
import com.thoughtworks.go.agent.launcher.DownloadableFile;
import com.thoughtworks.go.agent.launcher.ServerBinaryDownloader;
import com.thoughtworks.go.util.GoConstants;
import com.thoughtworks.go.util.SslVerificationMode;
import com.thoughtworks.go.util.SystemEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.apache.commons.lang.StringUtils.join;

public class AgentProcessParentImpl implements AgentProcessParent {

    /* 40-50 for launcher error codes*/
    private static final Logger LOG = LoggerFactory.getLogger(AgentProcessParentImpl.class);
    private static final int EXCEPTION_OCCURRED = -373;
    static final String AGENT_STARTUP_ARGS = "AGENT_STARTUP_ARGS";
    static final String GO_AGENT_STDERR_LOG = "go-agent-stderr.log";
    static final String GO_AGENT_STDOUT_LOG = "go-agent-stdout.log";

    public int run(String launcherVersion, String launcherMd5, ServerUrlGenerator urlGenerator, Map<String, String> env, Map context) {
        int exitValue = 0;
        LOG.info("Agent is version: {}", CurrentGoCDVersion.getInstance().fullVersion());
        String command[] = new String[]{};

        try {
            AgentBootstrapperBackwardCompatibility backwardCompatibility = backwardCompatibility(context);
            File rootCertFile = backwardCompatibility.rootCertFile();
            SslVerificationMode sslVerificationMode = backwardCompatibility.sslVerificationMode();

            ServerBinaryDownloader agentDownloader = new ServerBinaryDownloader(urlGenerator, rootCertFile, sslVerificationMode);
            agentDownloader.downloadIfNecessary(DownloadableFile.AGENT);

            ServerBinaryDownloader pluginZipDownloader = new ServerBinaryDownloader(urlGenerator, rootCertFile, sslVerificationMode);
            pluginZipDownloader.downloadIfNecessary(DownloadableFile.AGENT_PLUGINS);

            ServerBinaryDownloader tfsImplDownloader = new ServerBinaryDownloader(urlGenerator, rootCertFile, sslVerificationMode);
            tfsImplDownloader.downloadIfNecessary(DownloadableFile.TFS_IMPL);

            command = agentInvocationCommand(agentDownloader.getMd5(), launcherMd5, pluginZipDownloader.getMd5(), tfsImplDownloader.getMd5(), env, context, agentDownloader.getSslPort());
            LOG.info("Launching Agent with command: {}", join(command, " "));

            Process agent = invoke(command);

            // The next lines prevent the child process from blocking on Windows

            AgentOutputAppender agentOutputAppenderForStdErr = new AgentOutputAppender(GO_AGENT_STDERR_LOG);
            AgentOutputAppender agentOutputAppenderForStdOut = new AgentOutputAppender(GO_AGENT_STDOUT_LOG);

            if (new SystemEnvironment().consoleOutToStdout()) {
                agentOutputAppenderForStdErr.writeTo(AgentOutputAppender.Outstream.STDERR);
                agentOutputAppenderForStdOut.writeTo(AgentOutputAppender.Outstream.STDOUT);
            }

            agent.getOutputStream().close();
            AgentConsoleLogThread stdErrThd = new AgentConsoleLogThread(agent.getErrorStream(), agentOutputAppenderForStdErr);
            stdErrThd.start();
            AgentConsoleLogThread stdOutThd = new AgentConsoleLogThread(agent.getInputStream(), agentOutputAppenderForStdOut);
            stdOutThd.start();

            Shutdown shutdownHook = new Shutdown(agent);
            Runtime.getRuntime().addShutdownHook(shutdownHook);
            try {
                exitValue = agent.waitFor();
            } catch (InterruptedException ie) {
                LOG.error("Agent was interrupted. Terminating agent and respawning. {}", ie.toString());
                agent.destroy();
            } finally {
                removeShutdownHook(shutdownHook);
                stdErrThd.stopAndJoin();
                stdOutThd.stopAndJoin();
            }
        } catch (Exception e) {
            LOG.error("Exception while executing command: {} - {}", join(command, " "), e.toString());
            exitValue = EXCEPTION_OCCURRED;
        }
        return exitValue;
    }

    private AgentBootstrapperBackwardCompatibility backwardCompatibility(Map context) {
        return new AgentBootstrapperBackwardCompatibility(context);
    }

    private void removeShutdownHook(Shutdown shutdownHook) {
        try {
            Runtime.getRuntime().removeShutdownHook(shutdownHook);
        } catch (Exception e) {
        }
    }

    private String[] agentInvocationCommand(String agentMD5, String launcherMd5, String agentPluginsZipMd5, String tfsImplMd5, Map<String, String> env, Map context,
                                            @Deprecated String sslPort // the port is kept for backward compatibility to ensure that old bootstrappers are able to launch new agents
    ) {
        AgentBootstrapperBackwardCompatibility backwardCompatibility = backwardCompatibility(context);

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
        commandSnippets.add(property(GoConstants.AGENT_PLUGINS_MD5, agentPluginsZipMd5));
        commandSnippets.add(property(GoConstants.AGENT_JAR_MD5, agentMD5));
        commandSnippets.add(property(GoConstants.GIVEN_AGENT_LAUNCHER_JAR_MD5, launcherMd5));
        commandSnippets.add(property(GoConstants.TFS_IMPL_MD5, tfsImplMd5));
        commandSnippets.add("-jar");

        commandSnippets.add(Downloader.AGENT_BINARY);

        commandSnippets.add("-serverUrl");
        commandSnippets.add(backwardCompatibility.sslServerUrl(sslPort));

        if (backwardCompatibility.sslVerificationMode() != null) {
            commandSnippets.add("-sslVerificationMode");
            commandSnippets.add(backwardCompatibility.sslVerificationMode().toString());
        }

        if (backwardCompatibility.rootCertFileAsString() != null) {
            commandSnippets.add("-rootCertFile");
            commandSnippets.add(backwardCompatibility.rootCertFileAsString());
        }

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
        private static final Logger LOG = LoggerFactory.getLogger(Shutdown.class);
        private final Process agent;

        public Shutdown(Process agent) {
            setName("Shutdown" + getName());
            this.agent = agent;
        }

        public void run() {
            LOG.info("Shutdown hook invoked. Shutting down {}", agent);
            agent.destroy();
        }
    }
}
