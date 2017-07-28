/*
 * Copyright 2017 ThoughtWorks, Inc.
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

package com.thoughtworks.go.domain.materials.tfs;

import com.thoughtworks.go.config.materials.tfs.TfsMaterial;
import com.thoughtworks.go.domain.BuildCommand;
import com.thoughtworks.go.domain.materials.Revision;
import com.thoughtworks.go.domain.materials.RevisionContext;
import com.thoughtworks.go.util.MapBuilder;

import java.io.File;
import java.util.Map;

import static com.thoughtworks.go.domain.BuildCommand.*;
import static com.thoughtworks.go.util.MapBuilder.map;

public class TfsMaterialUpdater {

    private TfsMaterial material;

    public TfsMaterialUpdater(TfsMaterial material) {
        this.material = material;
    }

    public BuildCommand updateTo(String baseDir, RevisionContext revisionContext) {
        Revision revision = revisionContext.getLatestRevision();
        String workingDir = material.workingdir(new File(baseDir)).getPath();
        return compose(
                secret(material.getPassword()),
                execTfsCheckout(material, revision, workingDir)
        );
    }

    private BuildCommand execTfsCheckout(TfsMaterial material, Revision revision, String workingDir) {
        Map<String, String> properties = map(
                "type", "tfs",
                "username", material.getUserName(),
                "password", material.getPassword(),
                "domain", material.getDomain(),
                "projectPath",  material.getProjectPath(),
                "url", material.getUrl(),
                "revision", revision.getRevision());
        return plugin(properties).setWorkingDirectory(workingDir);
    }
}
