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
package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.SecretParams;
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.config.materials.Materials;
import com.thoughtworks.go.config.materials.git.GitMaterial;
import com.thoughtworks.go.config.materials.mercurial.HgMaterialConfig;
import com.thoughtworks.go.config.materials.svn.SvnMaterial;
import com.thoughtworks.go.config.materials.svn.SvnMaterialConfig;
import com.thoughtworks.go.helper.*;
import com.thoughtworks.go.server.cache.GoCache;
import org.assertj.core.api.Assertions;
import org.junit.*;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.migrationsupport.rules.EnableRuleMigrationSupport;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.Collections;

import static com.thoughtworks.go.helper.MaterialConfigsMother.svnMaterialConfig;
import static com.thoughtworks.go.helper.MaterialsMother.svnMaterial;
import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@EnableRuleMigrationSupport
public class MaterialExpansionServiceTest {

    @ClassRule
    public static final TemporaryFolder temporaryFolder = new TemporaryFolder();

    private static SvnTestRepoWithExternal svnRepo;
    private MaterialExpansionService materialExpansionService;
    @Mock
    private GoCache goCache;
    @Mock
    private MaterialConfigConverter materialConfigConverter;
    @Mock
    private SecretParamResolver secretParamResolver;

    @BeforeEach
    public void setUp() throws Exception {
        materialExpansionService = new MaterialExpansionService(goCache, materialConfigConverter, secretParamResolver);
    }

    @BeforeAll
    public static void copyRepository() throws IOException {
        temporaryFolder.create();
        svnRepo = new SvnTestRepoWithExternal(temporaryFolder);
    }

    @AfterAll
    public static void deleteRepository() {
        TestRepo.internalTearDown();
    }

    @Test
    public void shouldExpandMaterialConfigsForScheduling() {
        PipelineConfig pipelineConfig = new PipelineConfig();
        HgMaterialConfig hg = MaterialConfigsMother.hgMaterialConfig();
        pipelineConfig.addMaterialConfig(hg);

        MaterialConfigs materialConfigs = materialExpansionService.expandMaterialConfigsForScheduling(pipelineConfig.materialConfigs());

        assertThat(materialConfigs.size(), is(1));
        assertThat(materialConfigs.get(0), is(hg));
    }

    @Test
    public void shouldExpandMaterialForScheduling() {
        HgMaterialConfig hg = MaterialConfigsMother.hgMaterialConfig();

        MaterialConfigs materialConfigs = new MaterialConfigs();
        materialExpansionService.expandForScheduling(hg, materialConfigs);

        assertThat(materialConfigs.size(), is(1));
        assertThat(materialConfigs.get(0), is(hg));
    }

    @Test
    public void shouldExpandSvnMaterialWithExternalsIntoMultipleSvnMaterialsWhenExpandingForScheduling() {
        SvnMaterialConfig svn = svnMaterialConfig(svnRepo.projectRepositoryUrl(), "mainRepo");
        SvnMaterialConfig svnExt = svnMaterialConfig(svnRepo.externalRepositoryUrl(), "mainRepo/end2end", null);

        PipelineConfig pipelineConfig = new PipelineConfig();
        pipelineConfig.addMaterialConfig(svn);

        String cacheKeyForSvn = materialExpansionService.cacheKeyForSubversionMaterialCommand(svn.getFingerprint());
        String cacheKeyForSvnExt = materialExpansionService.cacheKeyForSubversionMaterialCommand(svnExt.getFingerprint());
        when(goCache.get(cacheKeyForSvn)).thenReturn(null);

        MaterialConfigs materialConfigs = materialExpansionService.expandMaterialConfigsForScheduling(pipelineConfig.materialConfigs());

        assertThat(materialConfigs.size(), is(2));
        assertThat(materialConfigs.get(0), is(svn));
        assertThat(materialConfigs.get(1), is(svnExt));
    }

    @Test
    public void shouldExpandSvnMaterialWithFolders() {
        SvnMaterialConfig svn = svnMaterialConfig(svnRepo.projectRepositoryUrl(), null);
        SvnMaterialConfig svnExt = svnMaterialConfig(svnRepo.externalRepositoryUrl(), "end2end");
        svnExt.setName((CaseInsensitiveString) null);
        PipelineConfig pipelineConfig = new PipelineConfig();
        pipelineConfig.addMaterialConfig(svn);
        String cacheKeyForSvn = materialExpansionService.cacheKeyForSubversionMaterialCommand(svn.getFingerprint());
        String cacheKeyForSvnExt = materialExpansionService.cacheKeyForSubversionMaterialCommand(svnExt.getFingerprint());
        when(goCache.get(cacheKeyForSvn)).thenReturn(null);

        MaterialConfigs materialConfigs = materialExpansionService.expandMaterialConfigsForScheduling(pipelineConfig.materialConfigs());

        assertThat(materialConfigs.size(), is(2));
        assertThat(materialConfigs.get(0), is(svn));
        assertThat(materialConfigs.get(1), is(svnExt));
        assertThat(materialConfigs.get(1).filter(), is(svn.filter()));
    }

    @Test
    public void shouldNotExpandSVNExternalsIfCheckExternalsIsFalse() {
        PipelineConfig pipelineConfig = new PipelineConfig();

        SvnMaterialConfig svn = svnMaterialConfig(svnRepo.projectRepositoryUrl(), null);
        svn.setConfigAttributes(Collections.singletonMap(SvnMaterialConfig.CHECK_EXTERNALS, String.valueOf(false)));
        pipelineConfig.addMaterialConfig(svn);

        String cacheKeyForSvn = materialExpansionService.cacheKeyForSubversionMaterialCommand(svn.getFingerprint());

        MaterialConfigs materialConfigs = materialExpansionService.expandMaterialConfigsForScheduling(pipelineConfig.materialConfigs());

        assertThat(materialConfigs.size(), is(1));
        assertThat(materialConfigs.get(0), is(svn));
    }

    @Test
    public void shouldNotExpandGitSubmodulesIntoMultipleMaterialsWhenExpandingGitMaterialForScheduling() throws Exception {
        GitRepoContainingSubmodule submoduleRepos = new GitRepoContainingSubmodule(temporaryFolder);
        submoduleRepos.addSubmodule("submodule-1", "sub1");
        GitMaterial gitMaterial = new GitMaterial(submoduleRepos.mainRepo().getUrl());
        when(materialConfigConverter.toMaterials(new MaterialConfigs(gitMaterial.config()))).thenReturn(new Materials(gitMaterial));

        Materials materials = new Materials();
        materialExpansionService.expandForScheduling(gitMaterial, materials);

        assertThat(materials.size(), is(1));
        assertThat(materials.get(0), is(gitMaterial));
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
        assertThat(materials.get(0), is(svn));
        assertThat(((SvnMaterial) materials.get(1)).getUrl(), endsWith("end2end/"));
    }

    @Test
    public void shouldResolveSvnMaterialIfPasswordContainsSecretParams() {
        SvnMaterial svn = svnMaterial(svnRepo.projectRepositoryUrl(), "mainRepo", "user1", "{{SECRET:[key][pass]}}", true, "*.doc");
        SvnMaterial expectedExternalSvnMaterial = new SvnMaterial(svnRepo.externalRepositoryUrl(), "user1", "pass1", true, "mainRepo/" + svnRepo.externalMaterial().getFolder());

        expectedExternalSvnMaterial.setFilter(FilterMother.filterFor("*.doc"));
        when(materialConfigConverter.toMaterials(new MaterialConfigs(svn.config(), expectedExternalSvnMaterial.config()))).thenReturn(new Materials(svn, expectedExternalSvnMaterial));

        doAnswer(invocation -> {
            SvnMaterial svnMaterial = invocation.getArgument(0);
            SecretParams secretParams = svnMaterial.getSecretParams();
            secretParams.get(0).setValue("pass1");
            return null;
        }).when(secretParamResolver).resolve(svn);

        materialExpansionService.expandForScheduling(svn, new Materials());

        verify(secretParamResolver).resolve(svn);
    }

    @Test
    public void shouldGenerateCacheKeyForMaterialFingerprint() {
        Assertions.assertThat(materialExpansionService.cacheKeyForSubversionMaterialCommand("1223423423"))
                .isEqualTo("com.thoughtworks.go.server.service.MaterialExpansionService.$cacheKeyForSvnMaterialCheckExternalCommand.$1223423423");

    }
}
