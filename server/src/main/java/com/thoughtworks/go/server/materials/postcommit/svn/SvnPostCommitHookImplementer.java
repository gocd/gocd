/*
 * Copyright 2019 ThoughtWorks, Inc.
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
package com.thoughtworks.go.server.materials.postcommit.svn;

import com.thoughtworks.go.config.materials.svn.SvnMaterial;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.domain.materials.svn.SvnCommand;
import com.thoughtworks.go.server.materials.postcommit.PostCommitHookImplementer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SvnPostCommitHookImplementer implements PostCommitHookImplementer {

    static final String UUID = "uuid";

    @Override
    public Set<Material> prune(Set<Material> materials, Map params) {
        final HashSet<Material> prunedMaterials = new HashSet<>();
        if (params.containsKey(UUID)) {
            final String targetUUID = (String) params.get(UUID);

            final HashMap<String, String> urlToRemoteUUIDMap = createUrlToRemoteUUIDMap(materials);
            for (Material material : materials) {
                if (material instanceof SvnMaterial && isQualified(targetUUID, (SvnMaterial) material, urlToRemoteUUIDMap)) {
                    prunedMaterials.add(material);
                }
            }
        }
        return prunedMaterials;
    }

    boolean isQualified(String incomingUUID, SvnMaterial material, HashMap urlToUUIDMap) {
        if (urlToUUIDMap.containsKey(material.urlForCommandLine())) {
            final String remoteUUID = (String) urlToUUIDMap.get(material.urlForCommandLine());
            return incomingUUID.equals(remoteUUID);
        }
        return false;
    }

    HashMap<String, String> createUrlToRemoteUUIDMap(Set<Material> materials) {
        final HashSet<SvnMaterial> setOfSvnMaterials = new HashSet<>();
        for (Material material : materials) {
            if (material instanceof SvnMaterial) {
                setOfSvnMaterials.add((SvnMaterial) material);
            }
        }
        return getEmptySvnCommand().createUrlToRemoteUUIDMap(setOfSvnMaterials);
    }

    SvnCommand getEmptySvnCommand() {
        return new SvnCommand(null, ".");
    }
}
