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
package com.thoughtworks.go.apiv1.internalmaterials.representers.materials;

import com.thoughtworks.go.api.base.OutputListWriter;
import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.api.representers.ErrorGetter;
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
import java.util.function.Consumer;

import static java.util.Arrays.stream;

public class MaterialsRepresenter {
    enum Materials {
        GIT(GitMaterialConfig.class, new GitMaterialRepresenter()),
        HG(HgMaterialConfig.class, new HgMaterialRepresenter()),
        SVN(SvnMaterialConfig.class, new SvnMaterialRepresenter()),
        P4(P4MaterialConfig.class, new PerforceMaterialRepresenter()),
        TFS(TfsMaterialConfig.class, new TfsMaterialRepresenter()),
        PACKAGE(PackageMaterialConfig.class, new PackageMaterialRepresenter()),
        PLUGIN(PluggableSCMMaterialConfig.class, new PluggableScmMaterialRepresenter());

        private final Class<? extends MaterialConfig> type;
        private final MaterialRepresenter representer;

        Materials(Class<? extends MaterialConfig> type, MaterialRepresenter representer) {
            this.type = type;
            this.representer = representer;
        }
    }

    public static Consumer<OutputWriter> toJSON(MaterialConfig materialConfig) {
        return materialWriter -> {
            if (!materialConfig.errors().isEmpty()) {
                materialWriter.addChild("errors", errorWriter -> {
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

            stream(Materials.values())
                    .filter(material -> material.type == materialConfig.getClass())
                    .findFirst()
                    .ifPresent(material -> {
                        materialWriter.add("type", material.name().toLowerCase());
                        materialWriter.add("fingerprint", materialConfig.getFingerprint());
                        materialWriter.addChild("attributes", material.representer.toJSON(materialConfig));
                    });

        };
    }

}
