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

import java.io.File;
import java.util.List;

import com.thoughtworks.go.config.materials.SubprocessExecutionContext;
import com.thoughtworks.go.domain.MaterialRevision;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.server.service.MaterialService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @understands how to find modifications for materials using the SCM directly
 */
@Component
public class LegacyMaterialChecker implements ModificationSource {
    private final MaterialService materialService;
    private SubprocessExecutionContext execCtx;

    @Autowired
    public LegacyMaterialChecker(MaterialService materialService, final SubprocessExecutionContext execCtx) {
        this.materialService = materialService;
        this.execCtx = execCtx;
    }

    @Override
    public List<Modification> findModificationsSince(File workingFolder, Material material, MaterialRevision revision) {
        return materialService.modificationsSince(material, workingFolder, revision.getRevision(), execCtx);
    }

    @Override
    public List<Modification> findLatestModification(File workingFolder, Material material, final SubprocessExecutionContext execCtx) {
        List<Modification> modifications = materialService.latestModification(material, workingFolder, execCtx);
        if (modifications.isEmpty()) {
            throw new RuntimeException(
                    String.format("Latest modifications check for the material '%s' returned an empty modification list. This might be because the material might be wrongly configured.", material));
        }
        return modifications;
    }
}
