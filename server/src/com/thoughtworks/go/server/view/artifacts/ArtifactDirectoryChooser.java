/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.server.view.artifacts;

import com.thoughtworks.go.domain.LocatableEntity;
import com.thoughtworks.go.domain.exception.IllegalArtifactLocationException;
import com.thoughtworks.go.util.FileUtil;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;

public class ArtifactDirectoryChooser {
    List<ArtifactLocator> locators = new ArrayList<>();

    public void add(ArtifactLocator artifactLocator) {
        locators.add(artifactLocator);
    }

    public File chooseExistingRoot(LocatableEntity locatableEntity) {
        for (ArtifactLocator locator : locators) {
            if (locator.directoryExists(locatableEntity)) {
                return locator.directoryFor(locatableEntity);
            }
        }
        return null;
    }

    public File preferredRoot(LocatableEntity locatableEntity) {
        return locators.get(0).directoryFor(locatableEntity);
    }

    public File findArtifact(LocatableEntity locatableEntity, String path) throws IllegalArtifactLocationException {
        try {
            File root = chooseExistingRoot(locatableEntity);
            if (root == null) {
                root = preferredRoot(locatableEntity);
            }
            File file = new File(root, path);
            if (!FileUtil.isSubdirectoryOf(root, file)) {
                throw new IllegalArtifactLocationException("Artifact path [" + path + "] is illegal."
                        + " Path must be inside the artifact directory.");
            }
            return file;
        } catch (IOException e) {
            throw new IllegalArtifactLocationException("Artifact path [" + path + "] is illegal."
                    + e.getMessage(), e);
        }
    }

    public File findCachedArtifact(LocatableEntity locatableEntity) {
        for (ArtifactLocator locator : locators) {
            File cachedArtifact = locator.findCachedArtifact(locatableEntity);
            if (cachedArtifact != null && cachedArtifact.exists()) return cachedArtifact;
        }
        return null;
    }

    public File temporaryConsoleFile(LocatableEntity locatableEntity) {
        return new File("data/console", format("%s.log", DigestUtils.md5Hex(locatableEntity.entityLocator())));
    }

}
