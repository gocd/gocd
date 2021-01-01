/*
 * Copyright 2021 ThoughtWorks, Inc.
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
import com.thoughtworks.go.CurrentGoCDVersion;
import com.thoughtworks.go.agent.ServerUrlGenerator;
import com.thoughtworks.go.agent.common.AgentBootstrapperArgs;
import com.thoughtworks.go.agent.common.UrlConstructor;
import com.thoughtworks.go.agent.common.launcher.AgentProcessParent;
import com.thoughtworks.go.agent.common.util.Downloader;
import com.thoughtworks.go.agent.common.util.JarUtil;
import com.thoughtworks.go.logging.LogConfigurator;
import com.thoughtworks.go.util.FileUtil;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URLClassLoader;
import java.security.SecureRandom;
import java.util.Map;
import java.util.function.Predicate;
import java.util.jar.JarEntry;

public class AgentLauncherImpl implements AgentLauncher {

    public static final int UNKNOWN_EXCEPTION_OCCURRED = -273;

    /* 50-60 for launcher error codes*/
    public static final int LAUNCHER_NOT_UP_TO_DATE = 60;

    public static final String GO_AGENT_BOOTSTRAP_CLASS = "Go-Agent-Bootstrap-Class";
    public static final String AGENT_BOOTSTRAPPER_LOCK_FILE = ".agent-bootstrapper.running";
    private final Lockfile lockFile = new Lockfile(new File(AGENT_BOOTSTRAPPER_LOCK_FILE));

    private static final Logger LOG = LoggerFactory.getLogger(AgentLauncherImpl.class);

    private final AgentProcessParentRunner agentProcessParentRunner;

    public AgentLauncherImpl() {
        this(new AgentJarBasedAgentParentRunner());
    }

    public AgentLauncherImpl(AgentProcessParentRunner agentProcessParentCreator) {
        this.agentProcessParentRunner = agentProcessParentCreator;
    }

    @Override
    public int launch(AgentLaunchDescriptor descriptor) {
        LogConfigurator logConfigurator = new LogConfigurator("agent-launcher-logback.xml");
        return logConfigurator.runWithLogger(() -> doLaunch(descriptor));
    }

    private Integer doLaunch(AgentLaunchDescriptor descriptor) {
        Thread shutdownHook = null;
        try {
            int returnValue;

            if (!lockFile.tryLock()) {
                return IRRECOVERABLE_ERROR;
            }

            shutdownHook = registerShutdownHook();

            Map context = descriptor.context();

            AgentBootstrapperArgs bootstrapperArgs = AgentBootstrapperArgs.fromProperties(context);
            ServerUrlGenerator urlGenerator = new UrlConstructor(bootstrapperArgs.getServerUrl().toExternalForm());

            ServerBinaryDownloader launcherDownloader = new ServerBinaryDownloader(urlGenerator, bootstrapperArgs);
            if (launcherDownloader.downloadIfNecessary(DownloadableFile.LAUNCHER)) {
                return LAUNCHER_NOT_UP_TO_DATE;
            }

            ServerBinaryDownloader agentDownloader = new ServerBinaryDownloader(urlGenerator, bootstrapperArgs);
            agentDownloader.downloadIfNecessary(DownloadableFile.AGENT);

            returnValue = agentProcessParentRunner.run(getLauncherVersion(), launcherDownloader.getMd5(), urlGenerator, System.getenv(), context);

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

    private void removeShutDownHook(Thread shutdownHook) {
        if (shutdownHook != null) {
            try {
                Runtime.getRuntime().removeShutdownHook(shutdownHook);
            } catch (Exception e) {
            }
        }
    }

    private Thread registerShutdownHook() {
        Thread shutdownHook = new Thread(() -> lockFile.delete());
        Runtime.getRuntime().addShutdownHook(shutdownHook);
        return shutdownHook;
    }

    private String getLauncherVersion() {
        return CurrentGoCDVersion.getInstance().fullVersion();
    }

    public interface AgentProcessParentRunner {
        int run(String launcherVersion, String launcherMd5, ServerUrlGenerator urlGenerator, Map<String, String> environmentVariables, Map context);
    }

    private static class AgentJarBasedAgentParentRunner implements AgentProcessParentRunner {
        @Override
        public int run(String launcherVersion, String launcherMd5, ServerUrlGenerator urlGenerator, Map<String, String> environmentVariables, Map context) {
            String agentProcessParentClassName = JarUtil.getManifestKey(Downloader.AGENT_BINARY_JAR, GO_AGENT_BOOTSTRAP_CLASS);
            String tempDirSuffix = new BigInteger(64, new SecureRandom()).toString(16) + "-" + Downloader.AGENT_BINARY_JAR;
            File tempDir = new File(FileUtil.TMP_PARENT_DIR, "deps-" + tempDirSuffix);
            try {
                try (URLClassLoader urlClassLoader = JarUtil.getClassLoaderFromJar(Downloader.AGENT_BINARY_JAR, jarEntryFilter(), tempDir, this.getClass().getClassLoader())) {
                    Class<?> aClass = urlClassLoader.loadClass(agentProcessParentClassName);
                    AgentProcessParent agentProcessParent = (AgentProcessParent) aClass.getDeclaredConstructor().newInstance();
                    return agentProcessParent.run(launcherVersion, launcherMd5, urlGenerator, environmentVariables, context);
                } catch (ReflectiveOperationException | IOException e) {
                    throw new RuntimeException(e);
                }
            } finally {
                FileUtils.deleteQuietly(tempDir);
            }
        }

        private Predicate<JarEntry> jarEntryFilter() {
            return jarEntry -> jarEntry.getName().startsWith("lib/") && jarEntry.getName().endsWith(".jar");
        }
    }

}
