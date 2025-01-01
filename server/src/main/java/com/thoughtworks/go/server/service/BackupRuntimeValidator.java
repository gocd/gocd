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

import com.thoughtworks.go.config.CruiseConfig;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static org.apache.commons.lang3.StringUtils.isBlank;

public class BackupRuntimeValidator {
    private final ArtifactsDirHolder artifactsDirHolder;

    public BackupRuntimeValidator(ArtifactsDirHolder artifactsDirHolder) {
        this.artifactsDirHolder = artifactsDirHolder;
    }

    void validatePostBackupScript(String postBackupScriptFile) {
        if (isBlank(postBackupScriptFile)) {
            return;
        }

        if (locatedInSensitiveDirectory(postBackupScriptFile, artifactsDirHolder)) {
            throw new IllegalArgumentException("Post backup script cannot be executed when located within pipelines or artifact storage.");
        }
    }

    private static boolean locatedInSensitiveDirectory(String postBackupScriptFile, ArtifactsDirHolder artifactsDirHolder) {
        try {
            Path canonicalScriptLocation = canonicalPath(postBackupScriptFile);
            return insideNonBackupsArtifactDirectory(artifactsDirHolder, canonicalScriptLocation) || insidePipelinesStorage(canonicalScriptLocation);
        } catch (IOException ignore) {
            // Issue accessing canonical path, assume either script file or targetsit does not exist, so by definition not in a sensitive location
            return false;
        }
    }

    private static boolean insidePipelinesStorage(Path canonicalScriptLocation) throws IOException {
        return canonicalScriptLocation.startsWith(canonicalPath(CruiseConfig.WORKING_BASE_DIR));
    }

    private static boolean insideNonBackupsArtifactDirectory(ArtifactsDirHolder artifactsDirHolder, Path canonicalScriptLocation) throws IOException {
        return canonicalScriptLocation.startsWith(canonicalPath(artifactsDirHolder.getArtifactsDir())) &&
                !canonicalScriptLocation.startsWith(canonicalPath(artifactsDirHolder.getBackupsDir()));
    }

    private static Path canonicalPath(File path) throws IOException {
        return path.getCanonicalFile().toPath();
    }

    private static Path canonicalPath(String path) throws IOException {
        return canonicalPath(new File(path));
    }
}
