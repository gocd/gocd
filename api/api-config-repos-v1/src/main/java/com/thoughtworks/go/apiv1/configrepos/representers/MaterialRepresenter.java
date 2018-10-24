/*
 * Copyright 2018 ThoughtWorks, Inc.
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

package com.thoughtworks.go.apiv1.configrepos.representers;

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.api.representers.ErrorGetter;
import com.thoughtworks.go.config.materials.git.GitMaterialConfig;
import com.thoughtworks.go.config.materials.mercurial.HgMaterialConfig;
import com.thoughtworks.go.config.materials.perforce.P4MaterialConfig;
import com.thoughtworks.go.config.materials.svn.SvnMaterialConfig;
import com.thoughtworks.go.config.materials.tfs.TfsMaterialConfig;
import com.thoughtworks.go.domain.materials.MaterialConfig;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static java.lang.String.format;

public class MaterialRepresenter {
    private static final Map<Class, String> MATERIAL_TYPES = Collections.unmodifiableMap(new HashMap<Class, String>() {
        {
            put(GitMaterialConfig.class, "git");
            put(HgMaterialConfig.class, "hg");
            put(SvnMaterialConfig.class, "svn");
            put(P4MaterialConfig.class, "p4");
            put(TfsMaterialConfig.class, "tfs");

        }
    });

    private static final Map<String, String> ERROR_MAPPING = Collections.unmodifiableMap(new HashMap<String, String>() {
        {
            put("materialName", "name");
            put("folder", "destination");
            put("autoUpdate", "auto_update");
            put("filterAsString", "filter");
            put("checkexternals", "check_externals");
            put("serverAndPort", "port");
            put("useTickets", "use_tickets");
            put("pipelineName", "pipeline");
            put("stageName", "stage");
            put("pipelineStageName", "pipeline");
            put("packageId", "ref");
            put("scmId", "ref");
            put("encryptedPassword", "encrypted_password");
        }
    });

    public static void toJSON(OutputWriter json, MaterialConfig material) {
        validateSupportedMaterials(material);

        if (!material.errors().isEmpty()) {
            json.addChild("errors", errorWriter -> new ErrorGetter(ERROR_MAPPING).toJSON(errorWriter, material));
        }

        json.add("type", MATERIAL_TYPES.get(material.getClass()));

        switch (MATERIAL_TYPES.get(material.getClass())) {
            case "git":
                json.addChild("attributes", attr -> GitMaterialRepresenter.toJSON(attr, (GitMaterialConfig) material));
                break;
            case "hg":
                json.addChild("attributes", attr -> HgMaterialRepresenter.toJSON(attr, (HgMaterialConfig) material));
                break;
            case "svn":
                json.addChild("attributes", attr -> SvnMaterialRepresenter.toJSON(attr, (SvnMaterialConfig) material));
                break;
            case "p4":
                json.addChild("attributes", attr -> P4MaterialRepresenter.toJSON(attr, (P4MaterialConfig) material));
                break;
            case "tfs":
                json.addChild("attributes", attr -> TfsMaterialRepresenter.toJSON(attr, (TfsMaterialConfig) material));
                break;
        }
    }

    private static void validateSupportedMaterials(MaterialConfig material) {
        if (!MATERIAL_TYPES.containsKey(material.getClass())) {
            throw new IllegalArgumentException(format("Cannot serialize unsupported material type: %s", material.getClass().getSimpleName()));
        }
    }
}
