/*
 * Copyright 2016 ThoughtWorks, Inc.
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
 */

package com.thoughtworks.go.server.service.materials;

import java.io.File;
import java.util.List;

import com.thoughtworks.go.config.materials.SubprocessExecutionContext;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.materials.Revision;

public interface MaterialPoller<T extends Material> {

    List<Modification> latestModification(T material, File baseDir, final SubprocessExecutionContext execCtx);

    List<Modification> modificationsSince(T material, File baseDir, Revision revision, final SubprocessExecutionContext execCtx);

    /**
     * Performs a server-side checkout of this material. Ignores destination directory, always checkouts directly to flyweight folder.
     */
    void checkout(T material, File baseDir, Revision revision, final SubprocessExecutionContext execCtx);
}

