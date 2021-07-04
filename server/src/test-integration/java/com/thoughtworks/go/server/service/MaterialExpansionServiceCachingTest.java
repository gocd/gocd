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

import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.config.materials.svn.SvnMaterialConfig;
import com.thoughtworks.go.domain.materials.svn.Subversion;
import com.thoughtworks.go.domain.materials.svn.SvnCommand;
import com.thoughtworks.go.helper.SvnTestRepoWithExternal;
import com.thoughtworks.go.helper.TestRepo;
import com.thoughtworks.go.server.cache.GoCache;
import org.junit.ClassRule;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.migrationsupport.rules.EnableRuleMigrationSupport;
import org.junit.rules.TemporaryFolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;

import static com.thoughtworks.go.helper.MaterialConfigsMother.svnMaterialConfig;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {
        "classpath:/applicationContext-global.xml",
        "classpath:/applicationContext-dataLocalAccess.xml",
        "classpath:/testPropertyConfigurer.xml",
        "classpath:/spring-all-servlet.xml",
})
@EnableRuleMigrationSupport
public class MaterialExpansionServiceCachingTest {
    @ClassRule
    public static final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Autowired
    GoCache goCache;
    @Autowired
    private MaterialConfigConverter materialConfigConverter;
    @Autowired
    SecretParamResolver secretParamResolver;
    private static SvnTestRepoWithExternal svnRepo;
    private static MaterialExpansionService materialExpansionService;

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
    public static void deleteRepository() throws IOException {
        TestRepo.internalTearDown();
    }

    @Test
    public void shouldExpandSvnMaterialWithExternalsIntoMultipleSvnMaterialsWhenExpandingForScheduling() {
        SvnMaterialConfig svnMaterialConfig = svnMaterialConfig(svnRepo.projectRepositoryUrl(), "mainRepo");
        MaterialConfigs materialConfigs = new MaterialConfigs();

        materialExpansionService.expandForScheduling(svnMaterialConfig, materialConfigs);

        assertThat(materialConfigs.size(), is(2));
        assertThat(materialConfigs.get(0), is(svnMaterialConfig));
        assertThat(((SvnMaterialConfig) materialConfigs.get(1)).getUrl(), endsWith("end2end/"));
    }

    @Test
    public void shouldCacheSvnMaterialCheckExternalCommand() {
        SvnMaterialConfig svnMaterialConfig = svnMaterialConfig(svnRepo.projectRepositoryUrl(), "mainRepo");
        MaterialConfigs materialConfigs = new MaterialConfigs();
        String cacheKey = materialExpansionService.cacheKeyForSubversionMaterialCommand(svnMaterialConfig.getFingerprint());

        Subversion svn = (SvnCommand) goCache.get(cacheKey);
        assertNull(svn);

        materialExpansionService.expandForScheduling(svnMaterialConfig, materialConfigs);
        svn = (SvnCommand) goCache.get(cacheKey);

        assertNotNull(svn);
        assertThat(svn.getUrl().originalArgument(), is(svnMaterialConfig.getUrl()));
    }
}
