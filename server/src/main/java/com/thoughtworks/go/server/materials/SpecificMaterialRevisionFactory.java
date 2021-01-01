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
package com.thoughtworks.go.server.materials;

import java.util.Map;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.domain.MaterialRevisions;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.MaterialConfigConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @understands how to create material revisions from an already known revision
 */
@Component
public class SpecificMaterialRevisionFactory {
    private  MaterialChecker materialChecker;
    private GoConfigService goConfigService;
    private MaterialConfigConverter materialConfigConverter;

    private SpecificMaterialRevisionFactory() {        
    }

    @Autowired
    public SpecificMaterialRevisionFactory(MaterialChecker materialChecker, GoConfigService goConfigService, MaterialConfigConverter materialConfigConverter) {
        this.materialChecker = materialChecker;
        this.goConfigService = goConfigService;
        this.materialConfigConverter = materialConfigConverter;
    }

    public MaterialRevisions create(String pipelineName, Map<String, String> revisionInfo) {
        MaterialRevisions materialRevisions = new MaterialRevisions();
        for (String materialFingerprint : revisionInfo.keySet()) {
            MaterialConfig materialConfig = goConfigService.findMaterial(new CaseInsensitiveString(pipelineName), materialFingerprint);
            if (materialConfig == null) { throw new RuntimeException(String.format("Material with fingerprint [%s] for pipeline [%s] does not exist", materialFingerprint, pipelineName)); }
            materialRevisions.addRevision(materialChecker.findSpecificRevision(materialConfigConverter.toMaterial(materialConfig), revisionInfo.get(materialFingerprint)));
        }
        return materialRevisions;
    }
}
