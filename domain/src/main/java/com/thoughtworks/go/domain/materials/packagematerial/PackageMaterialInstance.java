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
package com.thoughtworks.go.domain.materials.packagematerial;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.materials.PackageMaterial;
import com.thoughtworks.go.domain.MaterialInstance;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.util.json.JsonHelper;

public class PackageMaterialInstance extends MaterialInstance {

    protected PackageMaterialInstance() {
    }

    public PackageMaterialInstance(String configuration, String flyweight) {
        super(null, null, null, null, null, null, null, null, flyweight, null, null, null, configuration);
    }

    @Override
    public Material toOldMaterial(String name, String folder, String password) {
        PackageMaterial packageMaterial = JsonHelper.fromJson(configuration, PackageMaterial.class);
        packageMaterial.setName(new CaseInsensitiveString(name));
        packageMaterial.setId(id);
        packageMaterial.setFingerprint(getFingerprint());
        return packageMaterial;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PackageMaterialInstance)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }


        PackageMaterialInstance that = (PackageMaterialInstance) o;

        if (configuration != null ? !configuration.equals(that.configuration) : that.configuration != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (configuration != null ? configuration.hashCode() : 0);
        return result;
    }

    public boolean shouldUpgradeTo(PackageMaterialInstance materialInstance) {
        if(configuration == null && materialInstance.configuration == null){
            return false;
        }
        return configuration == null || !configuration.equals(materialInstance.configuration);
    }

    public void upgradeTo(PackageMaterialInstance newMaterialInstance) {
        this.configuration = newMaterialInstance.configuration;
    }
}
