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
package com.thoughtworks.go.domain.materials.dependency;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterial;
import com.thoughtworks.go.domain.MaterialInstance;
import com.thoughtworks.go.domain.materials.Material;

public class DependencyMaterialInstance extends MaterialInstance {

    protected DependencyMaterialInstance() {
    }

    public DependencyMaterialInstance(String pipelineName, String stageName, String flyweightName) {
        super(null, null, pipelineName, stageName, null, null, null, null, flyweightName, null, null, null, null);
    }

    @Override public Material toOldMaterial(String name, String folder, String password) {
        DependencyMaterial dep = new DependencyMaterial(new CaseInsensitiveString(pipelineName), new CaseInsensitiveString(stageName));
        setName(name, dep);
        dep.setId(id);
        return dep;
    }
}
