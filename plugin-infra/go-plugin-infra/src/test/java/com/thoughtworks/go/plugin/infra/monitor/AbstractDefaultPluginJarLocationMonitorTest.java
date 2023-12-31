/*
 * Copyright 2024 Thoughtworks, Inc.
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.Objects;

@ExtendWith(MockitoExtension.class)
abstract class AbstractDefaultPluginJarLocationMonitorTest {
    public static final long TEST_MONITOR_INTERVAL_MILLIS = 100L;
    public static final long TEST_TIMEOUT = TEST_MONITOR_INTERVAL_MILLIS * 5;

    File pluginWorkDir;
    FileHelper tempFolder;

    @BeforeEach
    void setUp(@TempDir File tempFolder) throws Exception {
        this.tempFolder = new FileHelper(tempFolder);
        pluginWorkDir = this.tempFolder.newFolder("plugin-work-dir");
    }

    BundleOrPluginFileDetails pluginFileDetails(File directory, String pluginFile, boolean bundledPlugin) {
        return new BundleOrPluginFileDetails(new File(directory, pluginFile), bundledPlugin, pluginWorkDir);
    }

    void addPlugin(BundleOrPluginFileDetails plugin) throws IOException {
        try (InputStream is = Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("defaultFiles/descriptor-aware-test-plugin.jar"))) {
            Path tempFile = Files.createTempFile(tempFolder.getRoot().toPath(), "plugin-", null);
            Files.copy(is, tempFile, StandardCopyOption.REPLACE_EXISTING);
            Files.move(tempFile, plugin.file().toPath(), StandardCopyOption.ATOMIC_MOVE);
        }
    }

    void updatePlugin(BundleOrPluginFileDetails plugin) throws IOException {
        Files.setLastModifiedTime(plugin.file().toPath(), FileTime.from(Instant.now()));
    }

    void deletePlugin(BundleOrPluginFileDetails plugin) throws IOException {
        Files.delete(plugin.file().toPath());
    }

    void renamePlugin(BundleOrPluginFileDetails orgExternalFile, BundleOrPluginFileDetails newExternalFile) throws IOException {
        Files.move(orgExternalFile.file().toPath(), newExternalFile.file().toPath(), StandardCopyOption.ATOMIC_MOVE);
    }
}
