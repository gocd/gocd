/*
 * Copyright 2015 ThoughtWorks, Inc.
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
 */

package com.thoughtworks.go.server.service;

import java.io.IOException;
import java.util.Collections;

import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.helper.FilterMother;
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.config.materials.Materials;
import com.thoughtworks.go.config.materials.git.GitMaterial;
import com.thoughtworks.go.config.materials.mercurial.HgMaterialConfig;
import com.thoughtworks.go.config.materials.svn.SvnMaterial;
import com.thoughtworks.go.config.materials.svn.SvnMaterialConfig;
import com.thoughtworks.go.helper.GitSubmoduleRepos;
import com.thoughtworks.go.helper.MaterialConfigsMother;
import com.thoughtworks.go.helper.SvnTestRepoWithExternal;
import com.thoughtworks.go.helper.TestRepo;
import com.thoughtworks.go.server.cache.GoCache;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.thoughtworks.go.helper.MaterialConfigsMother.svnMaterialConfig;
import static com.thoughtworks.go.helper.MaterialsMother.svnMaterial;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MaterialExpansionServiceTest {

    private static SvnTestRepoWithExternal svnRepo;
    private MaterialExpansionService materialExpansionService;
    private GoCache goCache;
    private MaterialConfigConverter materialConfigConverter;

    @Before
    public void setUp() throws Exception {
        goCache = mock(GoCache.class);
        materialConfigConverter = mock(MaterialConfigConverter.class);
        materialExpansionService = new MaterialExpansionService(goCache, materialConfigConverter);
    }

    @BeforeClass
    public static void copyRepository() throws IOException {
        svnRepo = new SvnTestRepoWithExternal();
    }

    @AfterClass
    public static void deleteRepository() throws IOException {
        TestRepo.internalTearDown();
    }

    @Test
    public void shouldExpandMaterialConfigsForScheduling() {
        PipelineConfig pipelineConfig = new PipelineConfig();
        HgMaterialConfig hg = MaterialConfigsMother.hgMaterialConfig();
        pipelineConfig.addMaterialConfig(hg);

        MaterialConfigs materialConfigs = materialExpansionService.expandMaterialConfigsForScheduling(pipelineConfig.materialConfigs());

        assertThat(materialConfigs.size(), is(1));
        assertThat((HgMaterialConfig) materialConfigs.get(0), is(hg));
    }

    @Test
    public void shouldExpandMaterialForScheduling() {
        HgMaterialConfig hg = MaterialConfigsMother.hgMaterialConfig();

        MaterialConfigs materialConfigs = new MaterialConfigs();
        materialExpansionService.expandForScheduling(hg, materialConfigs);

        assertThat(materialConfigs.size(), is(1));
        assertThat((HgMaterialConfig) materialConfigs.get(0), is(hg));
    }

    @Test
    public void shouldExpandSvnMaterialWithExternalsIntoMultipleSvnMaterialsWhenExpandingForScheduling() {
        SvnMaterialConfig svn = svnMaterialConfig(svnRepo.projectRepositoryUrl(), "mainRepo");
        SvnMaterialConfig svnExt = svnMaterialConfig(svnRepo.externalRepositoryUrl(), "mainRepo/end2end", null);

        PipelineConfig pipelineConfig = new PipelineConfig();
        pipelineConfig.addMaterialConfig(svn);

        String cacheKeyForSvn = MaterialExpansionService.class + "_cacheKeyForSvnMaterialCheckExternalCommand_" + svn.getFingerprint();
        String cacheKeyForSvnExt = MaterialExpansionService.class + "_cacheKeyForSvnMaterialCheckExternalCommand_" + svnExt.getFingerprint();
        when(goCache.get(cacheKeyForSvn)).thenReturn(null);
        when(goCache.get(cacheKeyForSvnExt)).thenReturn(null);

        MaterialConfigs materialConfigs = materialExpansionService.expandMaterialConfigsForScheduling(pipelineConfig.materialConfigs());

        assertThat(materialConfigs.size(), is(2));
        assertThat((SvnMaterialConfig) materialConfigs.get(0), is(svn));
        assertThat((SvnMaterialConfig) materialConfigs.get(1), is(svnExt));
    }

    @Test
    public void shouldExpandSvnMaterialWithFolders() {
        SvnMaterialConfig svn = svnMaterialConfig(svnRepo.projectRepositoryUrl(), null);
        SvnMaterialConfig svnExt = svnMaterialConfig(svnRepo.externalRepositoryUrl(), "end2end");
        svnExt.setName(null);
        PipelineConfig pipelineConfig = new PipelineConfig();
        pipelineConfig.addMaterialConfig(svn);
        String cacheKeyForSvn = MaterialExpansionService.class + "_cacheKeyForSvnMaterialCheckExternalCommand_" + svn.getFingerprint();
        String cacheKeyForSvnExt = MaterialExpansionService.class + "_cacheKeyForSvnMaterialCheckExternalCommand_" + svnExt.getFingerprint();
        when(goCache.get(cacheKeyForSvn)).thenReturn(null);
        when(goCache.get(cacheKeyForSvnExt)).thenReturn(null);

        MaterialConfigs materialConfigs = materialExpansionService.expandMaterialConfigsForScheduling(pipelineConfig.materialConfigs());

        assertThat(materialConfigs.size(), is(2));
        assertThat((SvnMaterialConfig) materialConfigs.get(0), is(svn));
        assertThat((SvnMaterialConfig) materialConfigs.get(1), is(svnExt));
        assertThat(materialConfigs.get(1).filter(), is(svn.filter()));
    }

    @Test
    public void shouldNotExapandSVNExternalsIfCheckExternalsIsFalse() {
        PipelineConfig pipelineConfig = new PipelineConfig();

        SvnMaterialConfig svn = svnMaterialConfig(svnRepo.projectRepositoryUrl(), null);
        svn.setConfigAttributes(Collections.singletonMap(SvnMaterialConfig.CHECK_EXTERNALS, String.valueOf(false)));
        pipelineConfig.addMaterialConfig(svn);

        String cacheKeyForSvn = MaterialExpansionService.class + "_cacheKeyForSvnMaterialCheckExternalCommand_" + svn.getFingerprint();
        when(goCache.get(cacheKeyForSvn)).thenReturn(null);

        MaterialConfigs materialConfigs = materialExpansionService.expandMaterialConfigsForScheduling(pipelineConfig.materialConfigs());

        assertThat(materialConfigs.size(), is(1));
        assertThat((SvnMaterialConfig) materialConfigs.get(0), is(svn));
    }

    @Test
    public void shouldNotExpandGitSubmodulesIntoMultipleMaterialsWhenExpandingGitMaterialForScheduling() throws Exception {
        GitSubmoduleRepos submoduleRepos = new GitSubmoduleRepos();
        submoduleRepos.addSubmodule("submodule-1", "sub1");
        GitMaterial gitMaterial = new GitMaterial(submoduleRepos.mainRepo().getUrl());
        when(materialConfigConverter.toMaterials(new MaterialConfigs(gitMaterial.config()))).thenReturn(new Materials(gitMaterial));

        Materials materials = new Materials();
        materialExpansionService.expandForScheduling(gitMaterial, materials);

        assertThat(materials.size(), is(1));
        assertThat((GitMaterial) materials.get(0), is(gitMaterial));
    }

    @Test
    public void shouldExpandSvnMaterialWithExternalsIntoMultipleSvnMaterialsWhenExpandingForHistory() {
        SvnMaterial svn = svnMaterial(svnRepo.projectRepositoryUrl(), "mainRepo", "user1", "pass1", true, "*.doc");
        SvnMaterial expectedExternalSvnMaterial = new SvnMaterial(svnRepo.externalRepositoryUrl(), "user1", "pass1", true, "mainRepo/" + svnRepo.externalMaterial().getFolder());

        expectedExternalSvnMaterial.setFilter(FilterMother.filterFor("*.doc"));
        when(materialConfigConverter.toMaterials(new MaterialConfigs(svn.config(), expectedExternalSvnMaterial.config()))).thenReturn(new Materials(svn, expectedExternalSvnMaterial));

        Materials materials = new Materials();
        materialExpansionService.expandForScheduling(svn, materials);

        assertThat(materials.size(), is(2));
        assertThat((SvnMaterial) materials.get(0), is(svn));
        assertThat(((SvnMaterial) materials.get(1)).getUrl(), endsWith("end2end/"));
    }

}
