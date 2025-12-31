/*
 * Copyright Thoughtworks, Inc.
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
import com.thoughtworks.go.logging.LogConfigurator;
import com.thoughtworks.go.util.FileUtil;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.SystemUtil;
import com.thoughtworks.go.util.validators.FileValidator;
import com.thoughtworks.go.util.validators.Validation;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOCase;
import org.apache.commons.io.filefilter.FalseFileFilter;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

public class AgentBootstrapper {

    private static final int DEFAULT_WAIT_TIME_BEFORE_RELAUNCH_IN_MS = 10000;
    public static final String WAIT_TIME_BEFORE_RELAUNCH_IN_MS = "agent.bootstrapper.wait.time.before.relaunch.in.ms";
    public static final String DEFAULT_LOGBACK_CONFIGURATION_FILE = "agent-bootstrapper-logback.xml";
    public static final RegexFileFilter AGENT_LAUNCHER_TMP_FILE_FILTER = new RegexFileFilter("([0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12})agent-launcher.jar", IOCase.INSENSITIVE);

    int waitTimeBeforeRelaunch = SystemUtil.getIntProperty(WAIT_TIME_BEFORE_RELAUNCH_IN_MS, DEFAULT_WAIT_TIME_BEFORE_RELAUNCH_IN_MS);
    private static final Logger LOG = LoggerFactory.getLogger(AgentBootstrapper.class);

    private final AtomicBoolean continueTrying = new AtomicBoolean(true);
    private final boolean jvmExitOnFailure;

    public AgentBootstrapper() {
        jvmExitOnFailure = true;
    }

    public AgentBootstrapper(boolean oneShot) {
        continueTrying.set(!oneShot);
        jvmExitOnFailure = !oneShot;
    }

    public static void main(String[] argv) {
        AgentBootstrapperArgs args = new AgentCLI().parse(argv);
        new LogConfigurator(DEFAULT_LOGBACK_CONFIGURATION_FILE).runWithLogger(() -> new AgentBootstrapper().go(args));
    }

    public void go(AgentBootstrapperArgs bootstrapperArgs) {
        LOG.info("Agent Bootstrapper {} started; cleaning up last bootstrap...", version());
        validate();
        cleanupTempFiles();

        int returnValue = 0;
        DefaultAgentLaunchDescriptorImpl descriptor = new DefaultAgentLaunchDescriptorImpl(bootstrapperArgs, this);

        do {
            ClassLoader tccl = Thread.currentThread().getContextClassLoader();
            try (AgentLauncherCreator agentLauncherCreator = getLauncherCreator()) {
                LOG.info("Creating launcher to download agent binaries...");
                AgentLauncher launcher = agentLauncherCreator.createLauncher();
                LOG.info("Starting launcher...");
                setContextClassLoader(launcher.getClass().getClassLoader());
                returnValue = launcher.launch(descriptor);
                resetContextClassLoader(tccl);
                LOG.info("Launcher returned with {}", returnDesc(returnValue));
                if (returnValue == AgentLauncher.IRRECOVERABLE_ERROR) {
                    break;
                }
            } catch (Exception e) {
                LOG.error("Error starting launcher", e);
            } finally {
                resetContextClassLoader(tccl);
                forceGCToPreventOOM();
            }

            // Immediately restart if launcher isn't up to date.
            if (returnValue != AgentLauncher.NOT_UP_TO_DATE) {
                waitForRelaunchTime();
            }
        } while (continueTrying.get());

        LOG.info("Agent Bootstrapper stopped");

        if (jvmExitOnFailure) {
            jvmExit(returnValue);
        }
    }

    static String returnDesc(int code) {
        return switch (code) {
            case AgentLauncher.IRRECOVERABLE_ERROR -> "IRRECOVERABLE_ERROR (%d / 0x%x)".formatted(code, code);
            case AgentLauncher.NOT_UP_TO_DATE -> "NOT_UP_TO_DATE (%d / 0x%x)".formatted(code, code);
            case -373 -> "AGENT_FATAL_EXCEPTION_OCCURRED (%d / 0x%x)".formatted(code, code);
            case 0 -> "DONE (%d / 0x%x)".formatted(code, code);
            default -> "UNKNOWN (%d / 0x%x)".formatted(code, code);
        };
    }

    private void cleanupTempFiles() {
        FileUtils.deleteQuietly(new File(FileUtil.TMP_PARENT_DIR));
        FileUtils.deleteQuietly(new File("exploded_agent_launcher_dependencies")); // launchers extracted from old versions
        FileUtils.listFiles(new File("."), AGENT_LAUNCHER_TMP_FILE_FILTER, FalseFileFilter.INSTANCE).forEach(FileUtils::deleteQuietly);
        FileUtils.deleteQuietly(new File(new SystemEnvironment().getConfigDir(), "trust.jks"));
    }

    void waitForRelaunchTime() {
        LOG.info("Waiting for {} ms before re-launch....", waitTimeBeforeRelaunch);
        try {
            Thread.sleep(waitTimeBeforeRelaunch);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void forceGCToPreventOOM() {
        System.gc();
    }

    void jvmExit(int returnValue) {
        System.exit(returnValue);
    }

    private void setContextClassLoader(ClassLoader tccl) {
        Thread.currentThread().setContextClassLoader(tccl);
    }

    private void resetContextClassLoader(ClassLoader tccl) {
        Thread.currentThread().setContextClassLoader(tccl);
    }

    public void stopLooping() {
        continueTrying.set(false);
    }

    void validate() {
        Validation validation = new Validation();
        FileValidator.defaultFile(Downloader.AGENT_LAUNCHER, false).validate(validation);
        if (!validation.isSuccessful()) {
            validation.logErrors(LOG);
            throw new RuntimeException("Agent Bootstrapper initialization failed. Could not validate file: " + Downloader.AGENT_LAUNCHER);
        }
    }

    AgentLauncherCreator getLauncherCreator() {
        return new DefaultAgentLauncherCreatorImpl();
    }

    public String version() {
        String version = getClass().getPackage().getImplementationVersion();
        return version == null ? "UNKNOWN" : version;
    }
}
