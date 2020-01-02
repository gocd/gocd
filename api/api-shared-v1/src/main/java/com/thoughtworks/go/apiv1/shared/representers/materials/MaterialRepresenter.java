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
package com.thoughtworks.go.apiv1.shared.representers.materials;

import com.thoughtworks.go.api.base.OutputListWriter;
import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.api.representers.ErrorGetter;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.config.exceptions.UnprocessableEntityException;
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.config.materials.PackageMaterialConfig;
import com.thoughtworks.go.config.materials.PluggableSCMMaterialConfig;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterialConfig;
import com.thoughtworks.go.config.materials.git.GitMaterialConfig;
import com.thoughtworks.go.config.materials.mercurial.HgMaterialConfig;
import com.thoughtworks.go.config.materials.perforce.P4MaterialConfig;
import com.thoughtworks.go.config.materials.svn.SvnMaterialConfig;
import com.thoughtworks.go.config.materials.tfs.TfsMaterialConfig;
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

    public static void toJSONArray(OutputListWriter materialsWriter, MaterialConfigs materialConfigs) {
        materialConfigs.forEach(materialConfig -> {
            materialsWriter.addChild(materialWriter -> toJSON(materialWriter, materialConfig));
        });
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

    public static MaterialConfigs fromJSONArray(JsonReader jsonReader, ConfigHelperOptions options) {
        MaterialConfigs materialConfigs = new MaterialConfigs();
        jsonReader.readArrayIfPresent("materials", materials -> {
            materials.forEach(material -> {
                materialConfigs.add(MaterialRepresenter.fromJSON(new JsonReader(material.getAsJsonObject()), options));
            });
        });
        return materialConfigs;
    }

    public static MaterialConfig fromJSON(JsonReader jsonReader, ConfigHelperOptions options) {
        String type = jsonReader.getString("type");
        JsonReader attributes = jsonReader.readJsonObject("attributes");
        switch (type) {
            case "git":
                return GitMaterialRepresenter.fromJSON(attributes);
            case "hg":
                return HgMaterialRepresenter.fromJSON(attributes);
            case "svn":
                return SvnMaterialRepresenter.fromJSON(attributes, options);
            case "p4":
                return PerforceMaterialRepresenter.fromJSON(attributes, options);
            case "tfs":
                return TfsMaterialRepresenter.fromJSON(attributes, options);
            case "dependency":
                return DependencyMaterialRepresenter.fromJSON(attributes);
            case "plugin":
                return PluggableScmMaterialRepresenter.fromJSON(attributes, options);
            case "package":
                return PackageMaterialRepresenter.fromJSON(attributes, options);
            default:
                throw new UnprocessableEntityException(String.format("Invalid material type %s. It has to be one of 'git, svn, hg, p4, tfs, dependency, package, plugin'.", type));
        }

    }
}
