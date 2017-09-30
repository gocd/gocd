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

import java.util.List;

import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.config.materials.Materials;
import com.thoughtworks.go.config.materials.svn.SvnMaterialConfig;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.domain.materials.svn.Subversion;
import com.thoughtworks.go.domain.materials.svn.SvnCommand;
import com.thoughtworks.go.domain.materials.svn.SvnExternal;
import com.thoughtworks.go.server.cache.GoCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MaterialExpansionService {

    private GoCache goCache;
    private MaterialConfigConverter materialConfigConverter;

    @Autowired
    public MaterialExpansionService(GoCache goCache, MaterialConfigConverter materialConfigConverter) {
        this.goCache = goCache;
        this.materialConfigConverter = materialConfigConverter;
    }

    public void expandForHistory(Material material, Materials addTheExpandedMaterialsHere) {
        expandForScheduling(material, addTheExpandedMaterialsHere);
    }

    public void expandForScheduling(Material material, Materials addTheExpandedMaterialsHere) {
        MaterialConfigs materialConfigs = new MaterialConfigs();
        expandForScheduling(material.config(), materialConfigs);
        addTheExpandedMaterialsHere.addAll(materialConfigConverter.toMaterials(materialConfigs));
    }

    public MaterialConfigs expandMaterialConfigsForScheduling(MaterialConfigs materialConfigsToBeExpanded)  {
        MaterialConfigs knownMaterialConfigs = new MaterialConfigs();
        materialConfigsToBeExpanded.forEach(materialConfig -> {
expandForScheduling(materialConfig, knownMaterialConfigs);
});
        return knownMaterialConfigs;
    }public void expandForScheduling(MaterialConfig configuredMaterial, MaterialConfigs addTheExpandedMaterialConfigsHere) {
        addTheExpandedMaterialConfigsHere.add(configuredMaterial);
        if(configuredMaterial instanceof SvnMaterialConfig) {
            expandForSchedulingForSvnMaterial(configuredMaterial, addTheExpandedMaterialConfigsHere);
        }
    }

    private void expandForSchedulingForSvnMaterial(MaterialConfig configuredMaterial, MaterialConfigs expandedMaterials) {
        expandExternals(configuredMaterial, expandedMaterials);
    }

    private void expandExternals(MaterialConfig configuredMaterial, MaterialConfigs expandedMaterials)  {
        SvnMaterialConfig svnMaterialConfig = (SvnMaterialConfig) configuredMaterial;
        if (!svnMaterialConfig.isCheckExternals()) {
            return;
        }

        List<SvnExternal> urLs = svn(svnMaterialConfig).getAllExternalURLs();
        urLs.stream().map(svnMaterial -> new SvnMaterialConfig(externalUrl.getURL(), svnMaterialConfig.getUserName(), svnMaterialConfig.getPassword(), true, svnMaterialConfig.folderFor(externalUrl.getFolder()))).map(svnMaterial -> {
svnMaterial.setFilter(svnMaterialConfig.filter());
return svnMaterial;
}).forEach(svnMaterial -> {
expandedMaterials.add(svnMaterial);
});
    }private Subversion svn(SvnMaterialConfig svnMaterialConfig) {
        String cacheKey = cacheKeyForSubversionMaterialCommand(svnMaterialConfig.getFingerprint());
        Subversion svnLazyLoaded = (SvnCommand) goCache.get(cacheKey);
        if (svnLazyLoaded == null || !svnLazyLoaded.getUrl().forCommandline().equals(svnMaterialConfig.getUrl())) {
            synchronized (cacheKey) {
                svnLazyLoaded = (SvnCommand) goCache.get(cacheKey);
                if (svnLazyLoaded == null || !svnLazyLoaded.getUrl().forCommandline().equals(svnMaterialConfig.getUrl())) {
                    svnLazyLoaded = new SvnCommand(svnMaterialConfig.getFingerprint(), svnMaterialConfig.getUrl(), svnMaterialConfig.getUserName(), svnMaterialConfig.getPassword(), svnMaterialConfig.isCheckExternals());
                    goCache.put(cacheKeyForSubversionMaterialCommand(svnMaterialConfig.getFingerprint()), svnLazyLoaded);
                }
            }
        }
        return svnLazyLoaded;
    }

    private String cacheKeyForSubversionMaterialCommand(String svnMaterialConfigFingerprint) {
        return (MaterialExpansionService.class + "_cacheKeyForSvnMaterialCheckExternalCommand_" + svnMaterialConfigFingerprint).intern();
    }
}
