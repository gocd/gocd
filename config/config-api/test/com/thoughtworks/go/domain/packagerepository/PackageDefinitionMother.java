/*
 * Copyright 2015 ThoughtWorks, Inc.
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

package com.thoughtworks.go.domain.packagerepository;

import com.thoughtworks.go.domain.config.Configuration;

import java.io.Serializable;
import java.util.HashMap;

public class PackageDefinitionMother {
    public static PackageDefinition create(String id, PackageRepository repository) {
        return create(id, "package-" + id, new Configuration(), repository);
    }

    public static PackageDefinition create(String id) {
        return create(id, null);
    }

    public static PackageDefinition create(final String id, final String name, final Configuration configuration, PackageRepository packageRepository) {
        PackageDefinition packageDefinition = new PackageDefinition(id, name, configuration);
        packageDefinition.setRepository(packageRepository);
        return packageDefinition;
    }

    public static HashMap<String, Serializable> paramsForPackageMaterialCreation(String repoId, String pkgName) {
        HashMap<String, HashMap> config = new HashMap<String, HashMap>();
        config.put("0", paramsForPackageMaterialConfig("key1", "value1"));
        config.put("1", paramsForPackageMaterialConfig("key2", "value2"));

        HashMap<String, Serializable> packageDef = new HashMap<String, Serializable>();
        packageDef.put("repositoryId", repoId);
        packageDef.put("name", pkgName);
        packageDef.put("configuration", config);

        HashMap<String, Serializable> params = new HashMap<String, Serializable>();
        params.put("package_definition", packageDef);
        return params;
    }

    public static HashMap<String, Serializable> paramsForPackageMaterialAssociation(String repoId, String pkgId) {
        HashMap<String, Serializable> packageDef = new HashMap<String, Serializable>();
        packageDef.put("repositoryId", repoId);

        HashMap<String, Serializable> params = new HashMap<String, Serializable>();
        params.put("package_definition", packageDef);
        params.put("packageId", pkgId);
        return params;
    }

    public static HashMap paramsForPackageMaterialConfig(String key, String value) {
        HashMap property = new HashMap();
        HashMap<String, String> valueMap = new HashMap<String, String>();
        HashMap<String, Serializable> keyMap = new HashMap<String, Serializable>();
        keyMap.put("name", key);
        valueMap.put("value", value);
        property.put("configurationKey", keyMap);
        property.put("configurationValue", valueMap);
        return property;
    }
}
