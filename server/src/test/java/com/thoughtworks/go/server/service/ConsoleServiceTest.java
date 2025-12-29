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
package com.thoughtworks.go.server.service;

import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.helper.JobIdentifierMother;
import com.thoughtworks.go.server.view.artifacts.ArtifactDirectoryChooser;
import org.apache.commons.io.FileExistsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.thoughtworks.go.util.ArtifactUtil.CONSOLE_LOG_FILE_RELATIVE_PATH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ConsoleServiceTest {

    private ArtifactDirectoryChooser chooser;
    private ConsoleService service;

    @BeforeEach
    public void setUp() {
        chooser = mock(ArtifactDirectoryChooser.class);
        service = new ConsoleService(chooser, mock(ArtifactsDirHolder.class));
    }

    @Test
    public void shouldReturnTemporaryArtifactFileIfItExists() throws Exception {
        JobIdentifier jobIdentifier = JobIdentifierMother.anyBuildIdentifier();

        File consoleFile = mock(File.class);
        when(consoleFile.exists()).thenReturn(true);

        File notExist = mock(File.class);
        when(notExist.exists()).thenReturn(false);

        when(chooser.temporaryConsoleFile(jobIdentifier)).thenReturn(consoleFile);
        when(chooser.findArtifact(jobIdentifier, CONSOLE_LOG_FILE_RELATIVE_PATH)).thenReturn(notExist);

        File file = service.consoleLogFile(jobIdentifier);

        assertThat(file).isEqualTo(consoleFile);
    }

    @Test
    public void shouldReturnFinalArtifactFileIfItExists() throws Exception {
        JobIdentifier jobIdentifier = JobIdentifierMother.anyBuildIdentifier();

        File consoleFile = mock(File.class);

        when(chooser.temporaryConsoleFile(jobIdentifier)).thenReturn(consoleFile);
        when(consoleFile.exists()).thenReturn(false);

        File finalConsoleFile = mock(File.class);

        when(chooser.findArtifact(jobIdentifier, CONSOLE_LOG_FILE_RELATIVE_PATH)).thenReturn(finalConsoleFile);
        when(finalConsoleFile.exists()).thenReturn(true);

        File file = service.consoleLogFile(jobIdentifier);

        assertThat(file).isEqualTo(finalConsoleFile);
    }

    @Test
    public void shouldReturnTemporaryFileIfBothTemporaryAndFinalFilesDoNotExist() throws Exception {
        JobIdentifier jobIdentifier = JobIdentifierMother.anyBuildIdentifier();

        File consoleFile = mock(File.class);

        when(chooser.temporaryConsoleFile(jobIdentifier)).thenReturn(consoleFile);
        when(consoleFile.exists()).thenReturn(false);

        File finalConsoleFile = mock(File.class);

        when(chooser.findArtifact(jobIdentifier, CONSOLE_LOG_FILE_RELATIVE_PATH)).thenReturn(finalConsoleFile);
        when(finalConsoleFile.exists()).thenReturn(false);

        File file = service.consoleLogFile(jobIdentifier);

        assertThat(file).isEqualTo(consoleFile);
    }

    @Test
    public void shouldMoveConsoleArtifacts(@TempDir Path testFolder) throws Exception {
        JobIdentifier jobIdentifier = JobIdentifierMother.anyBuildIdentifier();

        File temporaryConsoleLog = Files.createFile(testFolder.resolve("temporary_console.log")).toFile();
        File finalConsoleLog = testFolder.resolve("final_console.log").toFile();

        when(chooser.temporaryConsoleFile(jobIdentifier)).thenReturn(temporaryConsoleLog);
        when(chooser.findArtifact(jobIdentifier, CONSOLE_LOG_FILE_RELATIVE_PATH)).thenReturn(finalConsoleLog);

        service.moveConsoleArtifacts(jobIdentifier);

        assertThat(temporaryConsoleLog.exists()).isFalse();
        assertThat(finalConsoleLog.exists()).isTrue();
    }

    @Test
    public void shouldCreateTemporaryConsoleFileAndMoveIfItDoesNotExist(@TempDir Path testFolder) throws Exception {
        JobIdentifier jobIdentifier = JobIdentifierMother.anyBuildIdentifier();

        File temporaryConsoleLog = testFolder.resolve("temporary_console.log").toFile();
        File finalConsoleLog = testFolder.resolve("final_console.log").toFile();

        when(chooser.temporaryConsoleFile(jobIdentifier)).thenReturn(temporaryConsoleLog);
        when(chooser.findArtifact(jobIdentifier, CONSOLE_LOG_FILE_RELATIVE_PATH)).thenReturn(finalConsoleLog);

        service.moveConsoleArtifacts(jobIdentifier);

        assertThat(temporaryConsoleLog.exists()).isFalse();
        assertThat(finalConsoleLog.exists()).isTrue();
    }

    @Test
    public void shouldReturnUsefulErrorIfMoveConsoleArtifactsFails(@TempDir Path testFolder) throws Exception {
        JobIdentifier jobIdentifier = JobIdentifierMother.anyBuildIdentifier();

        File temporaryConsoleLog = testFolder.resolve("temporary_console.log").toFile();
        File finalConsoleLog = Files.createFile(testFolder.resolve("final_console.log")).toFile();

        when(chooser.temporaryConsoleFile(jobIdentifier)).thenReturn(temporaryConsoleLog);
        when(chooser.findArtifact(jobIdentifier, CONSOLE_LOG_FILE_RELATIVE_PATH)).thenReturn(finalConsoleLog);

        assertThatThrownBy(() -> service.moveConsoleArtifacts(jobIdentifier))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Unexpected error moving console log from temporary location")
            .hasMessageContaining("temporary_console.log")
            .hasMessageContaining("final_console.log")
            .cause()
            .isInstanceOf(FileExistsException.class)
            .hasMessageContaining("File element in parameter 'destFile' already exists");
    }

}
