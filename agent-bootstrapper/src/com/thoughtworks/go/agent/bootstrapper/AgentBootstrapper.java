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

package com.thoughtworks.go.agent.bootstrapper;

import com.thoughtworks.cruise.agent.common.launcher.AgentLauncher;
import com.thoughtworks.go.agent.common.AgentBootstrapperArgs;
import com.thoughtworks.go.agent.common.AgentCLI;
import com.thoughtworks.go.agent.common.util.Downloader;
import com.thoughtworks.go.util.SystemUtil;
import com.thoughtworks.go.util.validators.FileValidator;
import com.thoughtworks.go.util.validators.Validation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.NDC;

public class AgentBootstrapper {

    private static final int DEFAULT_WAIT_TIME_BEFORE_RELAUNCH_IN_MS = 10000;
    public static final String WAIT_TIME_BEFORE_RELAUNCH_IN_MS = "agent.bootstrapper.wait.time.before.relaunch.in.ms";

    int waitTimeBeforeRelaunch = SystemUtil.getIntProperty(WAIT_TIME_BEFORE_RELAUNCH_IN_MS, DEFAULT_WAIT_TIME_BEFORE_RELAUNCH_IN_MS);
    private static final Log LOG = LogFactory.getLog(AgentBootstrapper.class);

    private boolean loop;

    private AgentLauncherCreator launcherCreator;
    private Thread launcherThread;
    private Thread launcherCreatorShutdownHook;

    public AgentBootstrapper() {
    }

    public AgentBootstrapper(AgentLauncherCreator launcherCreator) {
        this.launcherCreator = launcherCreator;
    }

    public static void main(String[] argv) {
        BootstrapperLoggingHelper.initLog4j();
        AgentBootstrapperArgs args = new AgentCLI().parse(argv);
        new AgentBootstrapper().go(true, args);
    }

    public void go(boolean shouldLoop, AgentBootstrapperArgs bootstrapperArgs) {
        loop = shouldLoop;
        launcherThread = Thread.currentThread();

        validate();

        int returnValue = 0;
        DefaultAgentLaunchDescriptorImpl descriptor = new DefaultAgentLaunchDescriptorImpl(bootstrapperArgs, this);

        do {
            ClassLoader tccl = launcherThread.getContextClassLoader();
            try {
                AgentLauncher launcher = getLauncher();
                LOG.info("Attempting create and start launcher...");
                setContextClassLoader(launcher.getClass().getClassLoader());
                returnValue = launcher.launch(descriptor);
                resetContextClassLoader(tccl);
                LOG.info("Launcher returned with code " + returnValue + "(0x" + Integer.toHexString(returnValue).toUpperCase() + ")");
                if (returnValue == AgentLauncher.IRRECOVERABLE_ERROR) {
                    loop = false;
                }
                launcher = null;
            } catch (Exception e) {
                LOG.error("Error starting launcher", e);
            } finally {
                resetContextClassLoader(tccl);
                forceGCToPreventOOM();
                destoryLauncherCreator();
            }
            waitForRelaunchTime();
        } while (loop);

        destoryLauncherCreator();

        LOG.info("Agent Bootstrapper stopped");

        jvmExit(returnValue);
    }

    void waitForRelaunchTime() {
        LOG.info(String.format("Waiting for %s ms before re-launch....", waitTimeBeforeRelaunch));
        try {
            Thread.sleep(waitTimeBeforeRelaunch);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void forceGCToPreventOOM() {
        System.gc();
    }

    void jvmExit(int returnValue) {
        System.exit(returnValue);
    }

    private void destoryLauncherCreator() {
        LOG.info("Destroying launcher creator");
        try {
            launcherCreator.destroy();
        } catch (Exception e) {
        }
        launcherCreator = null;
        removeLauncherCreatorShutdownHook();
    }

    private void removeLauncherCreatorShutdownHook() {
        try {
            Runtime.getRuntime().removeShutdownHook(launcherCreatorShutdownHook);
        } catch (Exception e) {
        }
    }

    private void setContextClassLoader(ClassLoader tccl) {
        Thread.currentThread().setContextClassLoader(tccl);
    }

    private void resetContextClassLoader(ClassLoader tccl) {
        Thread.currentThread().setContextClassLoader(tccl);
    }

    public void stopLooping() {
        loop = false;
    }

    void validate() {
        Validation validation = new Validation();
        FileValidator.defaultFile(Downloader.AGENT_LAUNCHER, false).validate(validation);
        if (!validation.isSuccessful()) {
            validation.logErrors(LOG);
            throw new RuntimeException("Agent Bootstrapper initialization failed. Could not validate file: " + Downloader.AGENT_LAUNCHER);
        }
    }

    private AgentLauncher getLauncher() {//do not use across 2 invocations -jj
        try {
            initLauncherCreator();
            return launcherCreator.createLauncher();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create an instance of agent launcher", e);
        }
    }

    private void initLauncherCreator() {
        launcherCreator = getLauncherCreator();
        launcherCreatorShutdownHook = new Thread() {
            @Override
            public void run() {
                NDC.push("Agent-BootStrapper-ShutdownHook");
                LOG.info("Interrupting Launcher and initiating shutdown...");
                loop = false;
                launcherThread.interrupt();
                destoryLauncherCreator();
                NDC.pop();
            }
        };
        Runtime.getRuntime().addShutdownHook(launcherCreatorShutdownHook);
    }

    AgentLauncherCreator getLauncherCreator() {
        if (launcherCreator == null) {
            return new DefaultAgentLauncherCreatorImpl();
        }
        return launcherCreator;
    }
}
