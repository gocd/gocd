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
package com.thoughtworks.go.server.materials;

import java.util.List;
import java.io.File;

import com.thoughtworks.go.config.materials.SubprocessExecutionContext;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.domain.MaterialRevision;

/**
 * @understands how to find modifications from a meterial using different strategies
 */
public interface ModificationSource {
    List<Modification> findModificationsSince(File workingFolder, Material material, MaterialRevision revision);

    List<Modification> findLatestModification(File workingFolder, Material material, final SubprocessExecutionContext execCtx);
}
