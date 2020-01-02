/*
 * Copyright 2020 ThoughtWorks, Inc.
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
package com.thoughtworks.go.server.view.artifacts;

import java.io.File;

import com.thoughtworks.go.domain.LocatableEntity;

public class BuildIdArtifactLocator implements ArtifactLocator {
    private final File artifactsRoot;

    public BuildIdArtifactLocator(File artifactsRoot) {
        this.artifactsRoot = artifactsRoot;
    }

    @Override
    public File findArtifact(LocatableEntity identifier, String artifactPath) {
        return new File(directoryFor(identifier), artifactPath);
    }

    @Override
    public boolean directoryExists(LocatableEntity locatableEntity) {
        return directoryFor(locatableEntity).exists();
    }

    @Override
    public File directoryFor(LocatableEntity locatableEntity) {
        return new File(artifactsRoot, String.valueOf(locatableEntity.getId()));
    }

    @Override
    public File findCachedArtifact(LocatableEntity locatableEntity) {
        return null;
    }
}
