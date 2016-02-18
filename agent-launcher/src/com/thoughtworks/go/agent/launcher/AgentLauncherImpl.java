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

package com.thoughtworks.go.agent.launcher;

import com.thoughtworks.cruise.agent.common.launcher.AgentLaunchDescriptor;
import com.thoughtworks.cruise.agent.common.launcher.AgentLauncher;
import com.thoughtworks.go.agent.ServerUrlGenerator;
import com.thoughtworks.go.agent.common.AgentBootstrapperBackwardCompatibility;
import com.thoughtworks.go.agent.common.launcher.AgentProcessParent;
import com.thoughtworks.go.agent.common.util.Downloader;
import com.thoughtworks.go.agent.common.util.JarUtil;
import com.thoughtworks.go.agent.common.util.LoggingHelper;
import com.thoughtworks.go.util.SslVerificationMode;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class AgentLauncherImpl implements AgentLauncher {

    public static final int UNKNOWN_EXCEPTION_OCCURRED = -273;

    /* 50-60 for launcher error codes*/
    public static final int LAUNCHER_NOT_UP_TO_DATE = 60;

    public static final String GO_AGENT_BOOTSTRAP_CLASS = "Go-Agent-Bootstrap-Class";
    public static final String AGENT_BOOTSTRAPPER_LOCK_FILE = ".agent-bootstrapper.running";
    private static Lockfile lockFile = new Lockfile(new File(AGENT_BOOTSTRAPPER_LOCK_FILE));

    private static final Log LOG = LogFactory.getLog(AgentLauncherImpl.class);

    private final AgentProcessParentRunner agentProcessParentRunner;

    public AgentLauncherImpl() {
        this(new AgentJarBasedAgentParentRunner());
    }

    public AgentLauncherImpl(AgentProcessParentRunner agentProcessParentCreator) {
        this.agentProcessParentRunner = agentProcessParentCreator;
    }

    public int launch(AgentLaunchDescriptor descriptor) {
        Thread shutdownHook = null;
        try {
            LoggingHelper.configureLoggerIfNoneExists("go-agent-launcher.log", "go-agent-launcher-log4j.properties");
            int returnValue;

            if (!lockFile.tryLock()) {
                return IRRECOVERABLE_ERROR;
            }

            shutdownHook = registerShutdownHook();

            Map context = descriptor.context();

            AgentBootstrapperBackwardCompatibility backwardCompatibility = backwardCompatibility(context);
            ServerUrlGenerator urlGenerator = backwardCompatibility.getUrlGenerator();
            File rootCertFile = backwardCompatibility.rootCertFile();
            SslVerificationMode sslVerificationMode = backwardCompatibility.sslVerificationMode();

            ServerBinaryDownloader launcherDownloader = new ServerBinaryDownloader(urlGenerator, DownloadableFile.LAUNCHER, rootCertFile, sslVerificationMode);
            if (launcherDownloader.downloadIfNecessary()) {
                return LAUNCHER_NOT_UP_TO_DATE;
            }

            ServerBinaryDownloader agentDownloader = new ServerBinaryDownloader(urlGenerator, DownloadableFile.AGENT, rootCertFile, sslVerificationMode);
            agentDownloader.downloadIfNecessary();

            returnValue = agentProcessParentRunner.run(getLauncherVersion(), launcherDownloader.md5(), urlGenerator, System.getenv(), context);

            try {
                // Sleep a bit so that if there are problems we don't spin
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                return returnValue;
            }
            return returnValue;

        } catch (Exception e) {
            LOG.error("Launch encountered an unknown exception", e);
            return UNKNOWN_EXCEPTION_OCCURRED;
        } finally {
            removeShutDownHook(shutdownHook);
            lockFile.delete();
        }
    }

    private AgentBootstrapperBackwardCompatibility backwardCompatibility(Map context) {
        return new AgentBootstrapperBackwardCompatibility(context);
    }

    private void removeShutDownHook(Thread shutdownHook) {
        if (shutdownHook != null) {
            try {
                Runtime.getRuntime().removeShutdownHook(shutdownHook);
            } catch (Exception e) {
            }
        }
    }

    private Thread registerShutdownHook() {
        Thread shutdownHook = new Thread() {
            @Override
            public void run() {
                lockFile.delete();
            }
        };
        Runtime.getRuntime().addShutdownHook(shutdownHook);
        return shutdownHook;
    }

    private String getLauncherVersion() throws IOException {
        return JarUtil.getGoVersion(Downloader.AGENT_LAUNCHER);
    }

    public static interface AgentProcessParentRunner {
        int run(String launcherVersion, String launcherMd5, ServerUrlGenerator urlGenerator, Map<String, String> environmentVariables, Map context);
    }

    private static class AgentJarBasedAgentParentRunner implements AgentProcessParentRunner {
        public int run(String launcherVersion, String launcherMd5, ServerUrlGenerator urlGenerator, Map<String, String> environmentVariables, Map context) {
            AgentProcessParent agentProcessParent = (AgentProcessParent) JarUtil.objectFromJar(Downloader.AGENT_BINARY, GO_AGENT_BOOTSTRAP_CLASS);
            return agentProcessParent.run(launcherVersion, launcherMd5, urlGenerator, environmentVariables, context);
        }
    }

}
