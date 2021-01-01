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
package com.thoughtworks.go.apiv2.compare.representers;

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.api.representers.ErrorGetter;
import com.thoughtworks.go.apiv2.compare.representers.material.*;
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

import java.util.HashMap;
import java.util.Map;

public class MaterialRepresenter {
    private static Map<Class, String> classToTypeMap = new HashMap() {{
        put(GitMaterialConfig.class, "git");
        put(HgMaterialConfig.class, "hg");
        put(SvnMaterialConfig.class, "svn");
        put(P4MaterialConfig.class, "p4");
        put(TfsMaterialConfig.class, "tfs");
        put(DependencyMaterialConfig.class, "dependency");
        put(PackageMaterialConfig.class, "package");
        put(PluggableSCMMaterialConfig.class, "plugin");
    }};

    public static void toJSON(OutputWriter outputWriter, Material material) {
        outputWriter
                .add("name", material.getDisplayName())
                .add("fingerprint", material.getFingerprint())
                .add("type", material.getTypeForDisplay())
                .add("description", material.getLongDescription());
    }

    public static void toJSON(OutputWriter jsonWriter, MaterialConfig materialConfig) {
        if (!materialConfig.errors().isEmpty()) {
            jsonWriter.addChild("errors", errorWriter -> {
                HashMap<String, String> errorMapping = new HashMap<>();
                errorMapping.put("materialName", "name");
                errorMapping.put("folder", "destination");
                errorMapping.put("autoUpdate", "auto_update");
                errorMapping.put("filterAsString", "filter");
                errorMapping.put("checkexternals", "check_externals");
                errorMapping.put("serverAndPort", "port");
                errorMapping.put("useTickets", "use_tickets");
                errorMapping.put("pipelineName", "pipeline");
                errorMapping.put("stageName", "stage");
                errorMapping.put("pipelineStageName", "pipeline");
                errorMapping.put("packageId", "ref");
                errorMapping.put("scmId", "ref");
                errorMapping.put("encryptedPassword", "encrypted_password");


                new ErrorGetter(errorMapping).toJSON(errorWriter, materialConfig);
            });

        }
        jsonWriter.add("type", classToTypeMap.get(materialConfig.getClass()));
        switch (classToTypeMap.get(materialConfig.getClass())) {
            case "git":
                jsonWriter.addChild("attributes", attributeWriter -> GitMaterialRepresenter.toJSON(attributeWriter, (GitMaterialConfig) materialConfig));
                break;
            case "hg":
                jsonWriter.addChild("attributes", attributeWriter -> HgMaterialRepresenter.toJSON(attributeWriter, (HgMaterialConfig) materialConfig));
                break;
            case "svn":
                jsonWriter.addChild("attributes", attributeWriter -> SvnMaterialRepresenter.toJSON(attributeWriter, (SvnMaterialConfig) materialConfig));
                break;
            case "p4":
                jsonWriter.addChild("attributes", attributeWriter -> PerforceMaterialRepresenter.toJSON(attributeWriter, (P4MaterialConfig) materialConfig));
                break;
            case "tfs":
                jsonWriter.addChild("attributes", attributeWriter -> TfsMaterialRepresenter.toJSON(attributeWriter, (TfsMaterialConfig) materialConfig));
                break;
            case "dependency":
                jsonWriter.addChild("attributes", attributeWriter -> DependencyMaterialRepresenter.toJSON(attributeWriter, (DependencyMaterialConfig) materialConfig));
                break;
            case "package":
                jsonWriter.addChild("attributes", attributeWriter -> PackageMaterialRepresenter.toJSON(attributeWriter, (PackageMaterialConfig) materialConfig));
                break;
            case "plugin":
                jsonWriter.addChild("attributes", attributeWriter -> PluggableScmMaterialRepresenter.toJSON(attributeWriter, (PluggableSCMMaterialConfig) materialConfig));
                break;
        }

    }
}
