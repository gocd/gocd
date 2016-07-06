/*
 * Copyright 2016 ThoughtWorks, Inc.
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

package com.thoughtworks.go.config;

import com.thoughtworks.go.config.materials.dependency.DependencyMaterialConfig;
import com.thoughtworks.go.config.materials.git.GitMaterialConfig;
import com.thoughtworks.go.config.remote.ConfigRepoConfig;
import com.thoughtworks.go.config.remote.PartialConfig;
import com.thoughtworks.go.config.remote.RepoConfigOrigin;
import com.thoughtworks.go.helper.PartialConfigMother;
import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.serverhealth.*;
import com.thoughtworks.go.util.GoConfigFileHelper;
import com.thoughtworks.go.util.ListUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:WEB-INF/applicationContext-acegi-security.xml"
})
public class GoPartialConfigIntegrationTest {
    @Autowired
    private ServerHealthService serverHealthService;
    @Autowired
    private CachedGoPartials cachedGoPartials;
    @Autowired
    private CachedGoConfig cachedGoConfig;
    @Autowired
    private GoConfigService configService;
    @Autowired
    private GoPartialConfig goPartialConfig;
    @Autowired
    private GoCache goCache;
    @Autowired
    private GoConfigDao goConfigDao;

    private GoConfigFileHelper configHelper = new GoConfigFileHelper();
    private ConfigRepoConfig repoConfig1;
    private ConfigRepoConfig repoConfig2;

    @Before
    public void setUp() throws Exception {
        goCache.clear();
        configHelper.usingCruiseConfigDao(goConfigDao);
        configHelper.onSetUp();
        repoConfig1 = new ConfigRepoConfig(new GitMaterialConfig("url1"), "plugin");
        repoConfig2 = new ConfigRepoConfig(new GitMaterialConfig("url2"), "plugin");
        configHelper.addConfigRepo(repoConfig1);
        configHelper.addConfigRepo(repoConfig2);

    }

    @After
    public void teardown() throws Exception {
        for (PartialConfig partial : cachedGoPartials.lastValidPartials()) {
            assertThat(ErrorCollector.getAllErrors(partial).isEmpty(), is(true));
        }
        for (PartialConfig partial : cachedGoPartials.lastKnownPartials()) {
            assertThat(ErrorCollector.getAllErrors(partial).isEmpty(), is(true));
        }
        configHelper.onTearDown();
    }

    @Test
    public void shouldSaveConfigWhenANewValidPartialGetsAdded() {
        goPartialConfig.onSuccessPartialConfig(repoConfig1, PartialConfigMother.withPipeline("p1", new RepoConfigOrigin(repoConfig1, "4567")));
        assertThat(goConfigDao.loadConfigHolder().config.getAllPipelineNames().contains(new CaseInsensitiveString("p1")), is(true));
    }

    @Test
    public void shouldNotSaveConfigWhenANewInValidPartialGetsAdded() {
        PartialConfig invalidPartial = PartialConfigMother.invalidPartial("p1");
        invalidPartial.setOrigins(new RepoConfigOrigin(repoConfig1,"sha-2"));
        goPartialConfig.onSuccessPartialConfig(repoConfig1, invalidPartial);
        assertThat(goConfigDao.loadConfigHolder().config.getAllPipelineNames().contains(new CaseInsensitiveString("p1")), is(false));
        List<ServerHealthState> serverHealthStates = serverHealthService.filterByScope(HealthStateScope.forPartialConfigRepo(repoConfig1));
        assertThat(serverHealthStates.isEmpty(), is(false));
        assertThat(serverHealthStates.get(0).getLogLevel(), is(HealthStateLevel.ERROR));
    }

    @Test
    public void shouldTryToValidateMergeAndSaveAllKnownPartialsWhenAPartialChange() {
        cachedGoPartials.addOrUpdate(repoConfig1.getMaterialConfig().getFingerprint(), PartialConfigMother.withPipeline("p1_repo1", new RepoConfigOrigin(repoConfig1, "1234")));
        assertThat(goConfigDao.loadConfigHolder().config.getAllPipelineNames().contains(new CaseInsensitiveString("p1_repo1")), is(false));

        goPartialConfig.onSuccessPartialConfig(repoConfig2, PartialConfigMother.withPipeline("p2_repo2", new RepoConfigOrigin(repoConfig2, "4567")));

        assertThat(goConfigDao.loadConfigHolder().config.getAllPipelineNames().contains(new CaseInsensitiveString("p1_repo1")), is(true));
        assertThat(goConfigDao.loadConfigHolder().config.getAllPipelineNames().contains(new CaseInsensitiveString("p2_repo2")), is(true));

        assertThat(serverHealthService.filterByScope(HealthStateScope.forPartialConfigRepo(repoConfig2)).isEmpty(), is(true));
    }

    @Test
    public void shouldValidateAndMergeJustTheChangedPartialAlongWithAllValidPartialsIfValidationOfAllKnownPartialsFail() {
        goPartialConfig.onSuccessPartialConfig(repoConfig1, PartialConfigMother.withPipeline("p1_repo1", new RepoConfigOrigin(repoConfig1, "1")));
        assertThat(goConfigDao.loadConfigHolder().config.getAllPipelineNames().contains(new CaseInsensitiveString("p1_repo1")), is(true));
        assertThat(serverHealthService.filterByScope(HealthStateScope.forPartialConfigRepo(repoConfig1.getMaterialConfig().getFingerprint())).isEmpty(), is(true));

        final String invalidPipelineInPartial = "p1_repo1_invalid";
        PartialConfig invalidPartial = PartialConfigMother.invalidPartial(invalidPipelineInPartial, new RepoConfigOrigin(repoConfig1, "2"));
        goPartialConfig.onSuccessPartialConfig(repoConfig1, invalidPartial);

        assertThat(findPartial(invalidPipelineInPartial, cachedGoPartials.lastValidPartials()), is(nullValue()));
        assertThat(findPartial(invalidPipelineInPartial, cachedGoPartials.lastKnownPartials()), is(invalidPartial));
        assertThat(goConfigDao.loadConfigHolder().config.getAllPipelineNames().contains(new CaseInsensitiveString(invalidPipelineInPartial)), is(false));
        List<ServerHealthState> serverHealthStatesForRepo1 = serverHealthService.filterByScope(HealthStateScope.forPartialConfigRepo(repoConfig1));
        assertThat(serverHealthStatesForRepo1.isEmpty(), is(false));
        assertThat(serverHealthStatesForRepo1.get(0).getLogLevel(), is(HealthStateLevel.ERROR));

        goPartialConfig.onSuccessPartialConfig(repoConfig2, PartialConfigMother.withPipeline("p2_repo2", new RepoConfigOrigin(repoConfig2, "1")));
        assertThat(findPartial(invalidPipelineInPartial, cachedGoPartials.lastValidPartials()), is(nullValue()));
        assertThat(findPartial(invalidPipelineInPartial, cachedGoPartials.lastKnownPartials()), is(invalidPartial));

        assertThat(goConfigDao.loadConfigHolder().config.getAllPipelineNames().contains(new CaseInsensitiveString("p1_repo1")), is(true));
        assertThat(goConfigDao.loadConfigHolder().config.getAllPipelineNames().contains(new CaseInsensitiveString("p2_repo2")), is(true));
        assertThat(goConfigDao.loadConfigHolder().config.getAllPipelineNames().contains(new CaseInsensitiveString(invalidPipelineInPartial)), is(false));

        serverHealthStatesForRepo1 = serverHealthService.filterByScope(HealthStateScope.forPartialConfigRepo(repoConfig1));
        assertThat(serverHealthStatesForRepo1.isEmpty(), is(false));
        assertThat(serverHealthStatesForRepo1.get(0).getLogLevel(), is(HealthStateLevel.ERROR));
        List<ServerHealthState> serverHealthStatesForRepo2 = serverHealthService.filterByScope(HealthStateScope.forPartialConfigRepo(repoConfig2));
        assertThat(serverHealthStatesForRepo2.isEmpty(), is(true));
    }

    private PartialConfig findPartial(final String invalidPipelineInPartial, List<PartialConfig> partials) {
        return ListUtil.find(partials, new ListUtil.Condition() {
            @Override
            public <T> boolean isMet(T item) {
                PartialConfig partialConfig = (PartialConfig) item;
                return partialConfig.getGroups().first().findBy(new CaseInsensitiveString(invalidPipelineInPartial)) != null;
            }
        });
    }

    @Test
    public void shouldMarkAnInvalidKnownPartialAsValidWhenLoadingAnotherPartialMakesThisOneValid_InterConfigRepoDependency() {
        ConfigRepoConfig repoConfig3 = new ConfigRepoConfig(new GitMaterialConfig("url3"), "plugin");
        configHelper.addConfigRepo(repoConfig3);

        PartialConfig repo1 = PartialConfigMother.withPipeline("p1_repo1", new RepoConfigOrigin(repoConfig1, "1"));
        PartialConfig repo2 = PartialConfigMother.withPipeline("p2_repo2", new RepoConfigOrigin(repoConfig2, "1"));
        PartialConfig repo3 = PartialConfigMother.withPipeline("p3_repo3", new RepoConfigOrigin(repoConfig3, "1"));
        PipelineConfig p1 = repo1.getGroups().first().getPipelines().get(0);
        PipelineConfig p2 = repo2.getGroups().first().getPipelines().get(0);
        PipelineConfig p3 = repo3.getGroups().first().getPipelines().get(0);
        p2.addMaterialConfig(new DependencyMaterialConfig(p1.name(), p1.first().name()));
        p2.addMaterialConfig(new DependencyMaterialConfig(p3.name(), p3.first().name()));
        p1.addMaterialConfig(new DependencyMaterialConfig(p3.name(), p3.first().name()));

        goPartialConfig.onSuccessPartialConfig(repoConfig2, repo2);
        assertThat(goConfigDao.loadConfigHolder().config.getAllPipelineNames().contains(p2.name()), is(false));
        assertThat(serverHealthService.filterByScope(HealthStateScope.forPartialConfigRepo(repoConfig2)).isEmpty(), is(false));
        ServerHealthState healthStateForInvalidConfigMerge = serverHealthService.filterByScope(HealthStateScope.forPartialConfigRepo(repoConfig2)).get(0);
        assertThat(healthStateForInvalidConfigMerge.getMessage(), is("Invalid Merged Configuration"));
        assertThat(healthStateForInvalidConfigMerge.getDescription(), is("3+ errors :: Pipeline &quot;p1_repo1&quot; does not exist. It is used from pipeline &quot;p2_repo2&quot;.;; Pipeline with name 'p1_repo1' does not exist, it is defined as a dependency for pipeline 'p2_repo2' (url2 at 1);; Pipeline with name 'p3_repo3' does not exist, it is defined as a dependency for pipeline 'p2_repo2' (url2 at 1);;  -  Config-Repo: url2 at 1"));
        assertThat(healthStateForInvalidConfigMerge.getLogLevel(), is(HealthStateLevel.ERROR));
        assertThat(cachedGoPartials.lastValidPartials().isEmpty(), is(true));
        assertThat(cachedGoPartials.lastKnownPartials().size(), is(1));
        assertThat(cacheContainsPartial(cachedGoPartials.lastKnownPartials(), repo2), is(true));
        assertThat(cacheContainsPartial(cachedGoPartials.lastKnownPartials(), repo1), is(false));

        goPartialConfig.onSuccessPartialConfig(repoConfig3, repo3);
        assertThat(goConfigDao.loadConfigHolder().config.getAllPipelineNames().contains(p3.name()), is(true));
        assertThat(goConfigDao.loadConfigHolder().config.getAllPipelineNames().contains(p2.name()), is(false));
        assertThat(goConfigDao.loadConfigHolder().config.getAllPipelineNames().contains(p1.name()), is(false));
        assertThat(serverHealthService.filterByScope(HealthStateScope.forPartialConfigRepo(repoConfig3)).isEmpty(), is(true));
        assertThat(cachedGoPartials.lastValidPartials().size(), is(1));
        assertThat(cacheContainsPartial(cachedGoPartials.lastValidPartials(), repo2), is(false));
        assertThat(cacheContainsPartial(cachedGoPartials.lastValidPartials(), repo3), is(true));
        assertThat(cachedGoPartials.lastKnownPartials().size(), is(2));
        assertThat(cacheContainsPartial(cachedGoPartials.lastKnownPartials(), repo2), is(true));
        assertThat(cacheContainsPartial(cachedGoPartials.lastKnownPartials(), repo3), is(true));

        goPartialConfig.onSuccessPartialConfig(repoConfig1, repo1);
        assertThat(goConfigDao.loadConfigHolder().config.getAllPipelineNames().contains(p1.name()), is(true));
        assertThat(goConfigDao.loadConfigHolder().config.getAllPipelineNames().contains(p2.name()), is(true));
        assertThat(goConfigDao.loadConfigHolder().config.getAllPipelineNames().contains(p3.name()), is(true));
        assertThat(serverHealthService.filterByScope(HealthStateScope.forPartialConfigRepo(repoConfig1)).isEmpty(), is(true));
        assertThat(cachedGoPartials.lastValidPartials().size(), is(3));
        assertThat(cacheContainsPartial(cachedGoPartials.lastValidPartials(), repo2), is(true));
        assertThat(cacheContainsPartial(cachedGoPartials.lastValidPartials(), repo1), is(true));
        assertThat(cacheContainsPartial(cachedGoPartials.lastValidPartials(), repo3), is(true));
        assertThat(cachedGoPartials.lastKnownPartials().size(), is(3));
        assertThat(cacheContainsPartial(cachedGoPartials.lastKnownPartials(), repo2), is(true));
        assertThat(cacheContainsPartial(cachedGoPartials.lastKnownPartials(), repo1), is(true));
        assertThat(cacheContainsPartial(cachedGoPartials.lastKnownPartials(), repo3), is(true));
    }

    private boolean cacheContainsPartial(List<PartialConfig> partialConfigs, final PartialConfig partialConfig) {
        return ListUtil.find(partialConfigs, new ListUtil.Condition() {
            @Override
            public <T> boolean isMet(T item) {
                PartialConfig config = (PartialConfig) item;
                return partialConfig.getEnvironments().equals(config.getEnvironments()) && partialConfig.getGroups().equals(config.getGroups()) && partialConfig.getOrigin().equals(config.getOrigin());
            }
        }) != null;
    }
}