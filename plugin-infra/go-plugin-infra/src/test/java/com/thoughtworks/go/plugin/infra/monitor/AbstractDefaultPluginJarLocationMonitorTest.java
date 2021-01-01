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
package com.thoughtworks.go.plugin.infra.monitor;

import com.thoughtworks.go.plugin.FileHelper;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;

abstract class AbstractDefaultPluginJarLocationMonitorTest {
    private static final int NO_OF_TRIES_TO_CHECK_MONITOR_RUN = 150;
    File pluginWorkDir;
    FileHelper tempFolder;

    @BeforeEach
    void setUp(@TempDir File tempFolder) throws Exception {
        this.tempFolder = new FileHelper(tempFolder);
        pluginWorkDir = this.tempFolder.newFolder("plugin-work-dir");
    }

    void waitAMoment() throws InterruptedException {
        Thread.yield();
        Thread.sleep(2000);
    }

    void waitUntilNextRun(DefaultPluginJarLocationMonitor monitor) throws InterruptedException {
        long previousRun = monitor.getLastRun();
        int numberOfTries = 0;
        while (previousRun >= monitor.getLastRun() && numberOfTries < NO_OF_TRIES_TO_CHECK_MONITOR_RUN) {
            Thread.yield();
            Thread.sleep(100);
            numberOfTries++;
        }
        if (numberOfTries >= NO_OF_TRIES_TO_CHECK_MONITOR_RUN) {
            throw new RuntimeException("Number of tries exceeded, but monitor thread hasn't run yet");
        }
    }

    void copyPluginToThePluginDirectory(File pluginDir,
                                        String destinationFilenameOfPlugin) throws IOException, URISyntaxException {
        URL resource = getClass().getClassLoader().getResource("defaultFiles/descriptor-aware-test-plugin.jar");

        FileUtils.copyURLToFile(resource, new File(pluginDir, destinationFilenameOfPlugin));
    }

    void updateFileContents(File someFile) {
        try (FileOutputStream output = new FileOutputStream(someFile)) {
            IOUtils.write("some rubbish", output, Charset.defaultCharset());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    BundleOrPluginFileDetails pluginFileDetails(File directory, String pluginFile, boolean bundledPlugin) {
        return new BundleOrPluginFileDetails(new File(directory, pluginFile), bundledPlugin, pluginWorkDir);
    }

}
