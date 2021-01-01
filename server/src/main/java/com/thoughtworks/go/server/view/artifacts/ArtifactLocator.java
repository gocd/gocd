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
package com.thoughtworks.go.server.view.artifacts;

import java.io.File;

import com.thoughtworks.go.domain.LocatableEntity;

public interface ArtifactLocator {
    File findArtifact(LocatableEntity locatableEntity, String artifactPath);

    boolean directoryExists(LocatableEntity locatableEntity);

    File directoryFor(LocatableEntity locatableEntity);

    File findCachedArtifact(LocatableEntity locatableEntity);
}
