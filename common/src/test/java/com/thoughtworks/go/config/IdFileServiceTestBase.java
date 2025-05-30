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
package com.thoughtworks.go.config;

import com.thoughtworks.go.util.SystemEnvironment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;
import uk.org.webcompere.systemstubs.properties.SystemProperties;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SystemStubsExtension.class)
public abstract class IdFileServiceTestBase {
    protected IdFileService idFileService;
    protected String DATA = "data";

    @SystemStub
    private SystemProperties restoreSystemProperties;

    @BeforeEach
    public abstract void setUp();

    @AfterEach
    public void tearDown() {
        idFileService.delete();
        new SystemEnvironment().clearProperty(SystemEnvironment.CONFIG_DIR_PROPERTY);
    }

    @Test
    public void shouldLoadDataFromFile() {
        assertThat(idFileService.load()).isEqualTo(DATA);
    }

    @Test
    public void shouldStoreDataToFile() {
        idFileService.store("some-id");

        assertThat(idFileService.load()).isEqualTo("some-id");
    }

    @Test
    public void shouldCheckIfDataPresent() {
        assertTrue(idFileService.dataPresent());

        idFileService.delete();
        assertFalse(idFileService.dataPresent());


        idFileService.store("");
        assertFalse(idFileService.dataPresent());
    }

    @Test
    public void shouldDeleteFile() {
        assertThat(idFileService.file).exists();

        idFileService.delete();

        assertThat(idFileService.file).doesNotExist();
    }

    /**
     * Validates that we can create files (and ensure directories are created) inside a symlinked config directory.
     * Note that use of Files.createDirectories() inside the IdFileService provider will fail on Linux if the target directory
     * although was finally addressed in 17.0.14 via https://bugs.openjdk.org/browse/JDK-8294193 (and always correct on 21)
     */
    @Test
    public void shouldStoreFileInsideSymlinkedConfigDirs(@TempDir Path tempDir) throws Exception {
        Path targetConfig = tempDir.resolve("target-config");
        Path symlinkedConfig = tempDir.resolve("symlink-config-dir");
        Files.createDirectory(targetConfig);
        Files.createSymbolicLink(symlinkedConfig, targetConfig);

        new SystemEnvironment().setProperty(SystemEnvironment.CONFIG_DIR_PROPERTY, symlinkedConfig.toString());
        this.setUp();

        assertThat(idFileService.file).exists().isFile().hasContent(DATA).satisfies( f -> assertThat(f.getPath()).contains("symlink-config-dir"));
        assertThat(idFileService.file.toPath().toRealPath().toString()).contains("target-config");
        assertThat(idFileService.load()).isEqualTo(DATA);
    }
}