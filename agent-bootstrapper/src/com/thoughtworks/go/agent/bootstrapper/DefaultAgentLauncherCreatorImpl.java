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
import com.thoughtworks.cruise.agent.common.launcher.AgentLaunchDescriptor;
import com.thoughtworks.go.agent.common.util.Downloader;
import com.thoughtworks.go.agent.common.util.JarUtil;
import com.thoughtworks.go.util.SystemUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;

public class DefaultAgentLauncherCreatorImpl implements AgentLauncherCreator {
    private static final Log LOG = LogFactory.getLog(DefaultAgentLauncherCreatorImpl.class);

    public static final String GO_AGENT_LAUNCHER_CLASS = "Go-Agent-Launcher-Class";
    public static final String GO_AGENT_LAUNCHER_LIB_DIR = "Go-Agent-Launcher-Lib-Dir";

    public static final int DEFAULT_MAX_RETRY_FOR_CLEANUP = 5;
    public static final String MAX_RETRY_FOR_LAUNCHER_TEMP_CLEANUP_PROPERTY = "MaxRetryCount.ForLauncher.TempFiles.cleanup";


    private int maxRetryAttempts = SystemUtil.getIntProperty(MAX_RETRY_FOR_LAUNCHER_TEMP_CLEANUP_PROPERTY,
            DEFAULT_MAX_RETRY_FOR_CLEANUP);
    private File inUseLauncher;

    private volatile boolean launcherDestroyed = false;

    public AgentLauncher createLauncher() {
        inUseLauncher = createTempLauncherJar();
        return (AgentLauncher) JarUtil.objectFromJar(inUseLauncher.getName(),
                GO_AGENT_LAUNCHER_CLASS, GO_AGENT_LAUNCHER_LIB_DIR, AgentLauncher.class, AgentLaunchDescriptor.class);
    }

    private File createTempLauncherJar() {
        File inUseLauncher;
        try {
            inUseLauncher = new File(UUID.randomUUID() + Downloader.AGENT_LAUNCHER);
            FileUtils.copyFile(new File(Downloader.AGENT_LAUNCHER), inUseLauncher);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        LauncherTempFileHandler.startTempFileReaper();
        return inUseLauncher;
    }

    @Override
    public void destroy() {
        if (launcherDestroyed) {
            return;
        }

        recordCleanup();

        attemptToCleanupMaxRetryTimes();

        launcherDestroyed = true;
    }

    private void attemptToCleanupMaxRetryTimes() {
        boolean shouldRetry;
        int retryCount = 0;
        do {
            forceGCToReleaseAnyReferences();

            int oneSec = 1000;
            sleepForAMoment();

            LOG.info("Attempt No: " + (retryCount + 1) + " to cleanup launcher temp files");

            FileUtils.deleteQuietly(inUseLauncher);
            JarUtil.cleanup(inUseLauncher.getName());

            ++retryCount;

            shouldRetry = tempFilesExist() && retryCount < maxRetryAttempts;

        } while (shouldRetry);
    }


    private void recordCleanup() {
        LauncherTempFileHandler.writeToFile(Arrays.asList(inUseLauncher.getName()), true);
    }

    private boolean tempFilesExist() {
        return inUseLauncher.exists() || JarUtil.tempFileExist(inUseLauncher.getName());
    }

    private void sleepForAMoment() {
        int oneSec = 1000;
        try {
            Thread.sleep(oneSec);
        } catch (InterruptedException e) {
        }
    }

    private void forceGCToReleaseAnyReferences() {
        System.gc();
    }

}
