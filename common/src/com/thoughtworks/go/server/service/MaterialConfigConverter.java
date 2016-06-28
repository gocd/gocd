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

package com.thoughtworks.go.server.service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.config.materials.Materials;
import com.thoughtworks.go.config.materials.PackageMaterialConfig;
import com.thoughtworks.go.config.materials.PluggableSCMMaterialConfig;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterialConfig;
import com.thoughtworks.go.config.materials.git.GitMaterialConfig;
import com.thoughtworks.go.config.materials.mercurial.HgMaterialConfig;
import com.thoughtworks.go.config.materials.perforce.P4MaterialConfig;
import com.thoughtworks.go.config.materials.svn.SvnMaterialConfig;
import com.thoughtworks.go.config.materials.tfs.TfsMaterialConfig;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.domain.materials.dependency.DependencyMaterialInstance;
import com.thoughtworks.go.domain.materials.git.GitMaterialInstance;
import com.thoughtworks.go.domain.materials.mercurial.HgMaterialInstance;
import com.thoughtworks.go.domain.materials.packagematerial.PackageMaterialInstance;
import com.thoughtworks.go.domain.materials.perforce.P4MaterialInstance;
import com.thoughtworks.go.domain.materials.scm.PluggableSCMMaterialInstance;
import com.thoughtworks.go.domain.materials.svn.SvnMaterialInstance;
import com.thoughtworks.go.domain.materials.tfs.TfsMaterialInstance;
import org.springframework.stereotype.Component;

/* To convert a MaterialConfig into a Material. */

@Component
public class MaterialConfigConverter {
    private static HashMap<Class, Class> map = new HashMap<>();

    static {
        map.put(SvnMaterialConfig.class, SvnMaterialInstance.class);
        map.put(GitMaterialConfig.class, GitMaterialInstance.class);
        map.put(HgMaterialConfig.class, HgMaterialInstance.class);
        map.put(P4MaterialConfig.class, P4MaterialInstance.class);
        map.put(TfsMaterialConfig.class, TfsMaterialInstance.class);
        map.put(DependencyMaterialConfig.class, DependencyMaterialInstance.class);
        map.put(PackageMaterialConfig.class, PackageMaterialInstance.class);
        map.put(PluggableSCMMaterialConfig.class, PluggableSCMMaterialInstance.class);
    }

    public Material toMaterial(MaterialConfig materialConfig) {
        return new Materials(new MaterialConfigs(materialConfig)).first();
    }

    public Class getInstanceType(MaterialConfig materialConfig) {
        if (!map.containsKey(materialConfig.getClass())) {
            throw new RuntimeException("Unexpected type: " + materialConfig.getClass().getSimpleName());
        }
        return map.get(materialConfig.getClass());
    }

    public Materials toMaterials(MaterialConfigs materialConfigs) {
        return new Materials(materialConfigs);
    }

    public Set<Material> toMaterials(Set<MaterialConfig> materialConfigs) {
        HashSet<Material> materials = new HashSet<>();
        for (MaterialConfig materialConfig : materialConfigs) {
            materials.add(toMaterial(materialConfig));
        }
        return materials;
    }
}
