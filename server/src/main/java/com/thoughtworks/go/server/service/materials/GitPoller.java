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
package com.thoughtworks.go.server.service.materials;

import com.thoughtworks.go.config.materials.SubprocessExecutionContext;
import com.thoughtworks.go.config.materials.git.GitMaterial;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.materials.Revision;

import java.io.File;
import java.util.List;

public class GitPoller implements MaterialPoller<GitMaterial> {

    @Override
    public List<Modification> latestModification(GitMaterial material, File baseDir, SubprocessExecutionContext execCtx) {
        return toggleShallowCloneFeature(material, execCtx).latestModification(baseDir, execCtx);
    }

    @Override
    public List<Modification> modificationsSince(GitMaterial material, File baseDir, Revision revision, SubprocessExecutionContext execCtx) {
        return toggleShallowCloneFeature(material, execCtx).modificationsSince(baseDir, revision, execCtx);
    }

    @Override
    public void checkout(GitMaterial material, File baseDir, Revision revision, SubprocessExecutionContext execCtx) {
        toggleShallowCloneFeature(material, execCtx).checkout(baseDir, revision, execCtx);
    }

    private GitMaterial toggleShallowCloneFeature(GitMaterial material, SubprocessExecutionContext execCtx) {
        return material.withShallowClone(execCtx.isGitShallowClone());
    }
}
