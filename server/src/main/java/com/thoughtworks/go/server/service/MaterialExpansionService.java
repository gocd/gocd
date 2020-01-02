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
package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.config.materials.Materials;
import com.thoughtworks.go.config.materials.svn.SvnMaterial;
import com.thoughtworks.go.config.materials.svn.SvnMaterialConfig;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.domain.materials.svn.Subversion;
import com.thoughtworks.go.domain.materials.svn.SvnCommand;
import com.thoughtworks.go.domain.materials.svn.SvnExternal;
import com.thoughtworks.go.server.cache.CacheKeyGenerator;
import com.thoughtworks.go.server.cache.GoCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MaterialExpansionService {

    private final CacheKeyGenerator cacheKeyGenerator;
    private GoCache goCache;
    private MaterialConfigConverter materialConfigConverter;
    private SecretParamResolver secretParamResolver;

    @Autowired
    public MaterialExpansionService(GoCache goCache, MaterialConfigConverter materialConfigConverter,
                                    SecretParamResolver secretParamResolver) {
        this.goCache = goCache;
        this.materialConfigConverter = materialConfigConverter;
        this.secretParamResolver = secretParamResolver;
        this.cacheKeyGenerator = new CacheKeyGenerator(getClass());
    }

    public void expandForHistory(Material material, Materials addTheExpandedMaterialsHere) {
        expandForScheduling(material, addTheExpandedMaterialsHere);
    }

    public void expandForScheduling(Material material, Materials addTheExpandedMaterialsHere) {
        MaterialConfigs materialConfigs = new MaterialConfigs();
        expandForScheduling(material.config(), materialConfigs);
        addTheExpandedMaterialsHere.addAll(materialConfigConverter.toMaterials(materialConfigs));
    }

    public MaterialConfigs expandMaterialConfigsForScheduling(MaterialConfigs materialConfigsToBeExpanded) {
        MaterialConfigs knownMaterialConfigs = new MaterialConfigs();
        for (MaterialConfig materialConfig : materialConfigsToBeExpanded) {
            expandForScheduling(materialConfig, knownMaterialConfigs);
        }
        return knownMaterialConfigs;
    }

    public void expandForScheduling(MaterialConfig configuredMaterial,
                                    MaterialConfigs addTheExpandedMaterialConfigsHere) {
        addTheExpandedMaterialConfigsHere.add(configuredMaterial);
        if (configuredMaterial instanceof SvnMaterialConfig) {
            expandForSchedulingForSvnMaterial(configuredMaterial, addTheExpandedMaterialConfigsHere);
        }
    }

    private void expandForSchedulingForSvnMaterial(MaterialConfig configuredMaterial,
                                                   MaterialConfigs expandedMaterials) {
        expandExternals(configuredMaterial, expandedMaterials);
    }

    private void expandExternals(MaterialConfig configuredMaterial, MaterialConfigs expandedMaterials) {
        SvnMaterialConfig svnMaterialConfig = (SvnMaterialConfig) configuredMaterial;
        if (!svnMaterialConfig.isCheckExternals()) {
            return;
        }

        List<SvnExternal> urLs = svn(svnMaterialConfig).getAllExternalURLs();
        for (SvnExternal externalUrl : urLs) {
            SvnMaterialConfig svnMaterial = new SvnMaterialConfig();
            svnMaterial.setUrl(externalUrl.getURL());
            svnMaterial.setUserName(svnMaterialConfig.getUserName());
            svnMaterial.setPassword(svnMaterialConfig.getPassword());
            svnMaterial.setCheckExternals(true);
            svnMaterial.setFolder(svnMaterialConfig.folderFor(externalUrl.getFolder()));
            svnMaterial.setFilter(svnMaterialConfig.filter());
            expandedMaterials.add(svnMaterial);
        }
    }

    private Subversion svn(SvnMaterialConfig materialConfig) {
        String cacheKey = cacheKeyForSubversionMaterialCommand(materialConfig.getFingerprint());
        Subversion svnLazyLoaded = (SvnCommand) goCache.get(cacheKey);
        if (svnLazyLoaded == null || !svnLazyLoaded.getUrl().originalArgument().equals(materialConfig.getUrl())) {
            synchronized (cacheKey) {
                svnLazyLoaded = (SvnCommand) goCache.get(cacheKey);
                if (svnLazyLoaded == null || !svnLazyLoaded.getUrl().originalArgument().equals(materialConfig.getUrl())) {
                    svnLazyLoaded = new SvnCommand(materialConfig.getFingerprint(), materialConfig.getUrl(),
                            materialConfig.getUserName(), getResolvedPassword(materialConfig), materialConfig.isCheckExternals());
                    goCache.put(cacheKeyForSubversionMaterialCommand(materialConfig.getFingerprint()), svnLazyLoaded);
                }
            }
        }
        return svnLazyLoaded;
    }

    private String getResolvedPassword(SvnMaterialConfig materialConfig) {
        SvnMaterial svnMaterial = new SvnMaterial(materialConfig);
        if (svnMaterial.hasSecretParams()) {
            secretParamResolver.resolve(svnMaterial);
        }

        return svnMaterial.passwordForCommandLine();
    }

    String cacheKeyForSubversionMaterialCommand(String svnMaterialConfigFingerprint) {
        return cacheKeyGenerator.generate("cacheKeyForSvnMaterialCheckExternalCommand", svnMaterialConfigFingerprint);
    }
}
