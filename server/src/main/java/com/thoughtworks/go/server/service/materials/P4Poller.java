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
package com.thoughtworks.go.server.service.materials;

import java.io.File;
import java.util.List;

import com.thoughtworks.go.config.materials.SubprocessExecutionContext;
import com.thoughtworks.go.config.materials.perforce.P4Material;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.materials.Revision;

public class P4Poller implements MaterialPoller<P4Material> {

    @Override
    public List<Modification> latestModification(P4Material material, File baseDir, SubprocessExecutionContext execCtx) {
        return material.latestModification(baseDir, execCtx);
    }

    @Override
    public List<Modification> modificationsSince(P4Material material, File baseDir, Revision revision, SubprocessExecutionContext execCtx) {
        return material.modificationsSince(baseDir, revision, execCtx);
    }

    @Override
    public void checkout(P4Material material, File baseDir, Revision revision, SubprocessExecutionContext execCtx) {
        material.checkout(baseDir, revision, execCtx);
    }
}
