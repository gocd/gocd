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

package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.ServerConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BackupRuntimeValidatorTest {

    @Mock
    ArtifactsDirHolder artifactsDirHolder;

    @InjectMocks
    BackupRuntimeValidator validator;

    @BeforeEach
    public void setUp() {
        lenient().when(artifactsDirHolder.getBackupsDir()).thenReturn(new File(ServerConfig.SERVER_BACKUPS));
    }

    @Test
    void shouldValidatePostBackupScriptWithAbsoluteArtifacts() {
        when(artifactsDirHolder.getArtifactsDir()).thenReturn(new File("/godata/artifacts"));
        validator.validatePostBackupScript("/usr/local/bin/post-backup.sh");
    }

    @Test
    void shouldValidatePostBackupScriptWithRelativeArtifacts() {
        when(artifactsDirHolder.getArtifactsDir()).thenReturn(new File("artifacts"));
        validator.validatePostBackupScript("/usr/local/bin/post-backup.sh");
    }

    @Test
    void shouldValidatePostBackupScriptWithMatchingPrefixesButDifferentDirectories() {
        when(artifactsDirHolder.getArtifactsDir()).thenReturn(new File("artifacts"));
        validator.validatePostBackupScript("artifacts-scripts/post-backup.sh");
    }

    @Test
    void shouldValidatePostBackupScriptInsideBackupsDirectory() {
        when(artifactsDirHolder.getArtifactsDir()).thenReturn(new File("artifacts"));
        when(artifactsDirHolder.getBackupsDir()).thenReturn(new File("artifacts/serverBackups"));
        validator.validatePostBackupScript("artifacts/serverBackups/post-backup.sh");
    }

    @Test
    void shouldFailIfPostBackupScriptIsInsideArtifacts() {
        shouldFailBackupScriptLocationFor("artifacts/post-backup.sh", "artifacts");
        shouldFailBackupScriptLocationFor("/usr/local/bin/artifacts/post-backup.sh", "/usr/local/bin/artifacts");
    }

    @Test
    void shouldFailIfPostBackupScriptIsInsideWorkingDir() {
        shouldFailBackupScriptLocationFor("pipelines/post-backup.sh", "artifacts");
        shouldFailBackupScriptLocationFor(Paths.get(System.getProperty("user.dir"), "artifacts").toString(), "artifacts");
    }

    @Test
    void shouldFailIfPostBackupScriptIsInsideArtifactsAfterCanonicalization() {
        shouldFailBackupScriptLocationFor(Paths.get(System.getProperty("user.dir"), "artifacts/post-backup.sh").toString(), "artifacts");
        shouldFailBackupScriptLocationFor("artifacts/post-backup.sh", Paths.get(System.getProperty("user.dir"), "artifacts").toString());
    }

    private void shouldFailBackupScriptLocationFor(String backupScriptLocation, String artifactsDir) {
        when(artifactsDirHolder.getArtifactsDir()).thenReturn(new File(artifactsDir));
        assertThatThrownBy(() -> validator.validatePostBackupScript(backupScriptLocation))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Post backup script cannot be executed when located within pipelines or artifact storage.");
    }
}