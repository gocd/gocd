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

package com.thoughtworks.go.agent.bootstrapper;

import com.thoughtworks.cruise.agent.common.launcher.AgentLaunchDescriptor;
import com.thoughtworks.cruise.agent.common.launcher.AgentLauncher;
import com.thoughtworks.go.agent.common.util.Downloader;
import com.thoughtworks.go.agent.common.util.JarUtil;
import com.thoughtworks.go.util.FileUtil;
import com.thoughtworks.go.util.SystemUtil;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.function.Predicate;
import java.util.jar.JarEntry;

public class DefaultAgentLauncherCreatorImpl implements AgentLauncherCreator {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultAgentLauncherCreatorImpl.class);

    private static final String GO_AGENT_LAUNCHER_CLASS = "Go-Agent-Launcher-Class";
    private static final String GO_AGENT_LAUNCHER_LIB_DIR = "Go-Agent-Launcher-Lib-Dir";

    private static final int DEFAULT_MAX_RETRY_FOR_CLEANUP = 5;
    private static final String MAX_RETRY_FOR_LAUNCHER_TEMP_CLEANUP_PROPERTY = "MaxRetryCount.ForLauncher.TempFiles.cleanup";

    private final int maxRetryAttempts = SystemUtil.getIntProperty(MAX_RETRY_FOR_LAUNCHER_TEMP_CLEANUP_PROPERTY, DEFAULT_MAX_RETRY_FOR_CLEANUP);
    private final File inUseLauncher = new File(FileUtil.TMP_PARENT_DIR, new BigInteger(64, new SecureRandom()).toString(16) + "-" + Downloader.AGENT_LAUNCHER);

    private URLClassLoader urlClassLoader;

    public AgentLauncher createLauncher() {
        createTempLauncherJar();
        try {
            String libDir = JarUtil.getManifestKey(inUseLauncher, GO_AGENT_LAUNCHER_LIB_DIR);
            String classNameToLoad = JarUtil.getManifestKey(inUseLauncher, GO_AGENT_LAUNCHER_CLASS);
            return (AgentLauncher) loadClass(inUseLauncher, GO_AGENT_LAUNCHER_CLASS, libDir, classNameToLoad).newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Class<?> loadClass(File sourceJar, String classNameManifestKey, String libDir, String classNameToLoad) throws ClassNotFoundException {
        Predicate<JarEntry> filter = jarEntry -> jarEntry.getName().startsWith(libDir + "/") && jarEntry.getName().endsWith(".jar");
        this.urlClassLoader = JarUtil.getClassLoaderFromJar(sourceJar, filter, getDepsDir(), DefaultAgentLauncherCreatorImpl.class.getClassLoader(), AgentLauncher.class, AgentLaunchDescriptor.class);
        LOG.info("Attempting to load {} as specified by manifest key {}", classNameToLoad, classNameManifestKey);
        return this.urlClassLoader.loadClass(classNameToLoad);
    }

    private void createTempLauncherJar() {
        try {
            inUseLauncher.getParentFile().mkdirs();
            Files.copy(new File(Downloader.AGENT_LAUNCHER).toPath(), inUseLauncher.toPath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        LauncherTempFileHandler.startTempFileReaper();
    }

    private void attemptToCleanupMaxRetryTimes() {
        boolean shouldRetry;
        int retryCount = 0;
        do {
            forceGCToReleaseAnyReferences();
            sleepForAMoment();
            LOG.info("Attempt No: {} to cleanup launcher temp files", retryCount + 1);

            FileUtils.deleteQuietly(inUseLauncher);
            FileUtils.deleteQuietly(getDepsDir());

            ++retryCount;

            shouldRetry = tempFilesExist() && retryCount < maxRetryAttempts;

        } while (shouldRetry);
    }

    private File getDepsDir() {
        return new File(FileUtil.TMP_PARENT_DIR, "deps-" + inUseLauncher.getName());
    }

    private void recordCleanup() {
        LauncherTempFileHandler.writeToFile(Collections.singletonList(inUseLauncher.getName()), true);
    }

    private boolean tempFilesExist() {
        return inUseLauncher.exists() || getDepsDir().exists();
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

    @Override
    public void close() throws Exception {
        urlClassLoader.close();
        urlClassLoader = null;
        recordCleanup();
        attemptToCleanupMaxRetryTimes();
    }
}
