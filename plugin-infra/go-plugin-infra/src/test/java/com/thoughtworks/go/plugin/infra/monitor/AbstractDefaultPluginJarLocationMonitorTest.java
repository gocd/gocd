/*
 * Copyright 2023 Thoughtworks, Inc.
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
abstract class AbstractDefaultPluginJarLocationMonitorTest {
    public static final long MONITOR_WAIT_MILLIS = TimeUnit.SECONDS.toMillis(2);

    File pluginWorkDir;
    FileHelper tempFolder;

    @Mock
    PluginJarChangeListener changeListener;

    @BeforeEach
    void setUp(@TempDir File tempFolder) throws Exception {
        this.tempFolder = new FileHelper(tempFolder);
        pluginWorkDir = this.tempFolder.newFolder("plugin-work-dir");
    }

    void copyPluginToThePluginDirectory(BundleOrPluginFileDetails plugin) throws IOException {
        try (InputStream is = Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("defaultFiles/descriptor-aware-test-plugin.jar"))) {
            Files.copy(is, plugin.file().toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    void updateFileContents(File someFile) throws IOException {
        Files.writeString(someFile.toPath(), "some rubbish", StandardCharsets.UTF_8, StandardOpenOption.APPEND);
    }

    BundleOrPluginFileDetails pluginFileDetails(File directory, String pluginFile, boolean bundledPlugin) {
        return new BundleOrPluginFileDetails(new File(directory, pluginFile), bundledPlugin, pluginWorkDir);
    }

    void verifyNoMoreInteractionsOtherThanPhantomUpdatesFor(BundleOrPluginFileDetails... plugins) {
        // Sometimes there are phantom update events from the OS. We are not worried about these.
        for (var plugin : Optional.ofNullable(plugins).orElse(new BundleOrPluginFileDetails[0])) {
            verify(changeListener, atMostOnce()).pluginJarUpdated(plugin);
        }
        verifyNoMoreInteractions(changeListener);
    }
}
