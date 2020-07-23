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
package com.thoughtworks.go.config;

import com.thoughtworks.go.config.materials.dependency.DependencyMaterialConfig;
import com.thoughtworks.go.config.materials.git.GitMaterialConfig;
import com.thoughtworks.go.config.remote.ConfigRepoConfig;
import com.thoughtworks.go.config.remote.PartialConfig;
import com.thoughtworks.go.config.remote.RepoConfigOrigin;
import com.thoughtworks.go.config.rules.Allow;
import com.thoughtworks.go.config.rules.Rules;
import com.thoughtworks.go.domain.config.Configuration;
import com.thoughtworks.go.domain.config.PluginConfiguration;
import com.thoughtworks.go.domain.scm.SCM;
import com.thoughtworks.go.domain.scm.SCMs;
import com.thoughtworks.go.helper.PartialConfigMother;
import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.serverhealth.HealthStateLevel;
import com.thoughtworks.go.serverhealth.HealthStateScope;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.serverhealth.ServerHealthState;
import com.thoughtworks.go.util.GoConfigFileHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;
import java.util.function.Predicate;

import static com.thoughtworks.go.helper.MaterialConfigsMother.git;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:/applicationContext-global.xml",
        "classpath:/applicationContext-dataLocalAccess.xml",
        "classpath:/testPropertyConfigurer.xml",
        "classpath:/spring-all-servlet.xml",
})
public class PartialConfigServiceIntegrationTest {
    @Autowired
    private ServerHealthService serverHealthService;
    @Autowired
    private CachedGoPartials cachedGoPartials;
    @Autowired
    private PartialConfigService partialConfigService;
    @Autowired
    private GoCache goCache;
    @Autowired
    private GoConfigDao goConfigDao;
    @Autowired
    private GoConfigRepoConfigDataSource goConfigRepoConfigDataSource;

    private final GoConfigFileHelper configHelper = new GoConfigFileHelper();
    private ConfigRepoConfig repoConfig1;
    private ConfigRepoConfig repoConfig2;

    @Before
    public void setUp() throws Exception {
        goCache.clear();
        configHelper.usingCruiseConfigDao(goConfigDao);
        configHelper.onSetUp();
        repoConfig1 = createConfigRepoWithDefaultRules(git("url1"), "plugin", "id-1");
        repoConfig2 = createConfigRepoWithDefaultRules(git("url2"), "plugin", "id-2");
        configHelper.addConfigRepo(repoConfig1);
        configHelper.addConfigRepo(repoConfig2);

    }

    private ConfigRepoConfig createConfigRepoWithDefaultRules(GitMaterialConfig materialConfig, String plugin, String id) {
        ConfigRepoConfig config = ConfigRepoConfig.createConfigRepoConfig(materialConfig, plugin, id);
        config.getRules().add(new Allow("refer", "*", "*"));
        return config;
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

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();


    @Test
    public void shouldSaveConfigWhenANewValidPartialGetsAdded() {
        partialConfigService.onSuccessPartialConfig(repoConfig1, PartialConfigMother.withPipeline("p1", new RepoConfigOrigin(repoConfig1, "4567")));
        assertThat(goConfigDao.loadConfigHolder().config.getAllPipelineNames().contains(new CaseInsensitiveString("p1")), is(true));
    }

    @Test
    public void shouldNotSaveConfigWhenANewInValidPartialGetsAdded() {
        PartialConfig invalidPartial = PartialConfigMother.invalidPartial("p1");
        invalidPartial.setOrigins(new RepoConfigOrigin(repoConfig1, "sha-2"));
        partialConfigService.onSuccessPartialConfig(repoConfig1, invalidPartial);
        assertThat(goConfigDao.loadConfigHolder().config.getAllPipelineNames().contains(new CaseInsensitiveString("p1")), is(false));
        List<ServerHealthState> serverHealthStates = serverHealthService.filterByScope(HealthStateScope.forPartialConfigRepo(repoConfig1));
        assertThat(serverHealthStates.isEmpty(), is(false));
        assertThat(serverHealthStates.get(0).getLogLevel(), is(HealthStateLevel.ERROR));
    }

    @Test
    public void shouldTryToValidateMergeAndSaveAllKnownPartialsWhenAPartialChange() {
        cachedGoPartials.cacheAsLastKnown(repoConfig1.getRepo().getFingerprint(), PartialConfigMother.withPipeline("p1_repo1", new RepoConfigOrigin(repoConfig1, "1234")));
        assertThat(goConfigDao.loadConfigHolder().config.getAllPipelineNames().contains(new CaseInsensitiveString("p1_repo1")), is(false));

        partialConfigService.onSuccessPartialConfig(repoConfig2, PartialConfigMother.withPipeline("p2_repo2", new RepoConfigOrigin(repoConfig2, "4567")));

        assertThat(goConfigDao.loadConfigHolder().config.getAllPipelineNames().contains(new CaseInsensitiveString("p1_repo1")), is(true));
        assertThat(goConfigDao.loadConfigHolder().config.getAllPipelineNames().contains(new CaseInsensitiveString("p2_repo2")), is(true));

        assertThat(serverHealthService.filterByScope(HealthStateScope.forPartialConfigRepo(repoConfig2)).isEmpty(), is(true));
    }

    @Test
    public void shouldValidateAndMergeJustTheChangedPartialAlongWithAllValidPartialsIfValidationOfAllKnownPartialsFail() {
        partialConfigService.onSuccessPartialConfig(repoConfig1, PartialConfigMother.withPipeline("p1_repo1", new RepoConfigOrigin(repoConfig1, "1")));
        assertThat(goConfigDao.loadConfigHolder().config.getAllPipelineNames().contains(new CaseInsensitiveString("p1_repo1")), is(true));
        assertThat(serverHealthService.filterByScope(HealthStateScope.forPartialConfigRepo(repoConfig1.getRepo().getFingerprint())).isEmpty(), is(true));

        final String invalidPipelineInPartial = "p1_repo1_invalid";
        PartialConfig invalidPartial = PartialConfigMother.invalidPartial(invalidPipelineInPartial, new RepoConfigOrigin(repoConfig1, "2"));
        partialConfigService.onSuccessPartialConfig(repoConfig1, invalidPartial);

        assertThat(findPartial(invalidPipelineInPartial, cachedGoPartials.lastValidPartials()), is(nullValue()));
        assertThat(findPartial(invalidPipelineInPartial, cachedGoPartials.lastKnownPartials()), is(invalidPartial));
        assertThat(goConfigDao.loadConfigHolder().config.getAllPipelineNames().contains(new CaseInsensitiveString(invalidPipelineInPartial)), is(false));
        List<ServerHealthState> serverHealthStatesForRepo1 = serverHealthService.filterByScope(HealthStateScope.forPartialConfigRepo(repoConfig1));
        assertThat(serverHealthStatesForRepo1.isEmpty(), is(false));
        assertThat(serverHealthStatesForRepo1.get(0).getLogLevel(), is(HealthStateLevel.ERROR));

        partialConfigService.onSuccessPartialConfig(repoConfig2, PartialConfigMother.withPipeline("p2_repo2", new RepoConfigOrigin(repoConfig2, "1")));
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
        return partials.stream().filter(new Predicate<PartialConfig>() {
            @Override
            public boolean test(PartialConfig item) {
                return item.getGroups().first().findBy(new CaseInsensitiveString(invalidPipelineInPartial)) != null;
            }
        }).findFirst().orElse(null);
    }

    @Test
    public void shouldMarkAnInvalidKnownPartialAsValidWhenLoadingAnotherPartialMakesThisOneValid_InterConfigRepoDependency() {
        ConfigRepoConfig repoConfig3 = createConfigRepoWithDefaultRules(git("url3"), "plugin", "id-3");
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

        partialConfigService.onSuccessPartialConfig(repoConfig2, repo2);
        assertThat(goConfigDao.loadConfigHolder().config.getAllPipelineNames().contains(p2.name()), is(false));
        assertThat(serverHealthService.filterByScope(HealthStateScope.forPartialConfigRepo(repoConfig2)).isEmpty(), is(false));
        ServerHealthState healthStateForInvalidConfigMerge = serverHealthService.filterByScope(HealthStateScope.forPartialConfigRepo(repoConfig2)).get(0);
        assertThat(healthStateForInvalidConfigMerge.getMessage(), is("Invalid Merged Configuration"));
        assertThat(healthStateForInvalidConfigMerge.getDescription(), is("Number of errors: 3+\n1. Pipeline 'p1_repo1' does not exist. It is used from pipeline 'p2_repo2'.;; \n2. Pipeline with name 'p1_repo1' does not exist, it is defined as a dependency for pipeline 'p2_repo2' (url2 at 1);; \n3. Pipeline with name 'p3_repo3' does not exist, it is defined as a dependency for pipeline 'p2_repo2' (url2 at 1);; \n- For Config Repo: url2 at 1"));
        assertThat(healthStateForInvalidConfigMerge.getLogLevel(), is(HealthStateLevel.ERROR));
        assertThat(cachedGoPartials.lastValidPartials().isEmpty(), is(true));
        assertThat(cachedGoPartials.lastKnownPartials().size(), is(1));
        assertThat(cacheContainsPartial(cachedGoPartials.lastKnownPartials(), repo2), is(true));
        assertThat(cacheContainsPartial(cachedGoPartials.lastKnownPartials(), repo1), is(false));

        partialConfigService.onSuccessPartialConfig(repoConfig3, repo3);
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

        partialConfigService.onSuccessPartialConfig(repoConfig1, repo1);
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

    @Test
    public void shouldPerformPreprocessingOfTemplatesOnCRPartials() throws Exception {
        configHelper.addTemplate("t1", "stage");
        partialConfigService.onSuccessPartialConfig(repoConfig1, PartialConfigMother.withPipelineAssociatedWithTemplate("pipe-with-template", "t1", new RepoConfigOrigin(repoConfig1, "124")));

        assertThat(goConfigDao.loadConfigHolder().config.hasPipelineNamed(new CaseInsensitiveString("pipe-with-template")), is(true));
        assertThat(goConfigDao.loadConfigHolder().config.getPipelineConfigByName(new CaseInsensitiveString("pipe-with-template")).hasTemplateApplied(), is(true));
    }

    @Test
    public void shouldPerformParamResolutionOnCRPipelines() throws Exception {
        partialConfigService.onSuccessPartialConfig(repoConfig1, PartialConfigMother.withParams("pipe-with-params", "paramName", "paramValue", new RepoConfigOrigin(repoConfig1, "124")));
        assertThat(goConfigDao.loadConfigHolder().config.hasPipelineNamed(new CaseInsensitiveString("pipe-with-params")), is(true));
        String resolvedParam = goConfigDao.loadConfigHolder().config.getPipelineConfigByName(new CaseInsensitiveString("pipe-with-params")).getStage("stage").getVariables().get(0).getValue();
        assertThat(resolvedParam, is("paramValue"));
    }

    @Test
    public void shouldSaveSCMs() {
        Configuration config = new Configuration();
        config.addNewConfigurationWithValue("url", "url", false);
        PluginConfiguration pluginConfig = new PluginConfiguration("plugin.id", "1.0");
        RepoConfigOrigin origin = new RepoConfigOrigin(repoConfig1, "124");
        PartialConfig scmPartial = PartialConfigMother.withSCM("scm_id", "name", pluginConfig, config, origin);
        partialConfigService.onSuccessPartialConfig(repoConfig1, scmPartial);
        SCMs scms = goConfigDao.loadConfigHolder().config.getSCMs();
        SCM expectedSCM = new SCM("scm_id", pluginConfig, config);
        expectedSCM.setOrigins(origin);
        expectedSCM.setName("name");
        assertThat(scms.size(), is(1));
        assertThat(scms.first(), is(expectedSCM));
        assertThat(cacheContainsPartial(cachedGoPartials.lastKnownPartials(), scmPartial), is(true));
    }

    @Test
    public void shouldFailToSaveCRPipelineReferencingATemplateWithParams() {
        configHelper.addTemplate("t1", "param1", "stage");
        partialConfigService.onSuccessPartialConfig(repoConfig1, PartialConfigMother.withPipelineAssociatedWithTemplate("pipe-with-template", "t1", new RepoConfigOrigin(repoConfig1, "124")));

        assertThat(goConfigDao.loadConfigHolder().config.hasPipelineNamed(new CaseInsensitiveString("pipe-with-template")), is(false));
        List<ServerHealthState> serverHealthStates = serverHealthService.filterByScope(HealthStateScope.forPartialConfigRepo(repoConfig1));
        assertThat(serverHealthStates.isEmpty(), is(false));
        assertThat(serverHealthStates.get(0).getLogLevel(), is(HealthStateLevel.ERROR));
        assertThat(serverHealthStates.get(0).getDescription(), is("Parameter 'param1' is not defined. All pipelines using this parameter directly or via a template must define it.- For Config Repo: url1 at 124"));
    }

    @Test // See Error #1 from https://github.com/gocd/gocd/issues/8368
    public void shouldRemovePipelinesWhenRulesAreUpdatedToSpecifyNoWhitelist() {
        partialConfigService.onSuccessPartialConfig(repoConfig1, PartialConfigMother.withPipeline("p1_repo1", new RepoConfigOrigin(repoConfig1, "1")));
        assertThat(goConfigDao.loadConfigHolder().config.getAllPipelineNames().contains(new CaseInsensitiveString("p1_repo1")), is(true));
        assertThat(serverHealthService.filterByScope(HealthStateScope.forPartialConfigRepo(repoConfig1.getRepo().getFingerprint())).isEmpty(), is(true));

        goConfigRepoConfigDataSource.onConfigRepoConfigChange(repoConfig1);

        ConfigRepoConfig repoConfig1Cloned = createConfigRepoWithDefaultRules(git("url1"), "plugin", "id-1");
        repoConfig1Cloned.setRules(new Rules());

        partialConfigService.onSuccessPartialConfig(repoConfig1Cloned, PartialConfigMother.withPipeline("p1_repo1", new RepoConfigOrigin(repoConfig1Cloned, "1")));
        assertThat(goConfigDao.loadConfigHolder().config.getAllPipelineNames().contains(new CaseInsensitiveString("p1_repo1")), is(false));
        assertThat(serverHealthService.filterByScope(HealthStateScope.forPartialConfigRepo(repoConfig1)).isEmpty(), is(false));
        ServerHealthState healthStateForInvalidConfigMerge = serverHealthService.filterByScope(HealthStateScope.forPartialConfigRepo(repoConfig1)).get(0);
        assertThat(healthStateForInvalidConfigMerge.getMessage(), is("Invalid Merged Configuration"));
        assertThat(healthStateForInvalidConfigMerge.getDescription(), is("Number of errors: 1+\n" +
                "I. Rule Validation Errors: \n" +
                "\t1. Not allowed to refer to pipeline group 'group'. Check the 'Rules' of this config repository.;; \n" +
                "\n" +
                "II. Config Validation Errors: \n" +
                "- For Config Repo: url1 at 1"));
        assertThat(healthStateForInvalidConfigMerge.getLogLevel(), is(HealthStateLevel.ERROR));

        cachedGoPartials.clear();
    }

    @Test // See Error #2 from https://github.com/gocd/gocd/issues/8368
    public void shouldAddPipelinesWhenRulesAreUpdatedToSpecifyAllowAllPipelines() {
        partialConfigService.onSuccessPartialConfig(repoConfig1, PartialConfigMother.withPipeline("p1_repo1", new RepoConfigOrigin(repoConfig1, "1")));
        assertThat(goConfigDao.loadConfigHolder().config.getAllPipelineNames().contains(new CaseInsensitiveString("p1_repo1")), is(true));
        assertThat(serverHealthService.filterByScope(HealthStateScope.forPartialConfigRepo(repoConfig1.getRepo().getFingerprint())).isEmpty(), is(true));

        goConfigRepoConfigDataSource.onConfigRepoConfigChange(repoConfig1);

        ConfigRepoConfig repoConfig1Cloned = createConfigRepoWithDefaultRules(git("url1"), "plugin", "id-1");
        repoConfig1Cloned.setRules(new Rules());

        partialConfigService.onSuccessPartialConfig(repoConfig1Cloned, PartialConfigMother.withPipeline("p1_repo1", new RepoConfigOrigin(repoConfig1Cloned, "1")));
        assertThat(goConfigDao.loadConfigHolder().config.getAllPipelineNames().contains(new CaseInsensitiveString("p1_repo1")), is(false));
        assertThat(serverHealthService.filterByScope(HealthStateScope.forPartialConfigRepo(repoConfig1)).isEmpty(), is(false));
        ServerHealthState healthStateForInvalidConfigMerge = serverHealthService.filterByScope(HealthStateScope.forPartialConfigRepo(repoConfig1)).get(0);
        assertThat(healthStateForInvalidConfigMerge.getMessage(), is("Invalid Merged Configuration"));
        assertThat(healthStateForInvalidConfigMerge.getDescription(), is("Number of errors: 1+\n" +
                "I. Rule Validation Errors: \n" +
                "\t1. Not allowed to refer to pipeline group 'group'. Check the 'Rules' of this config repository.;; \n" +
                "\n" +
                "II. Config Validation Errors: \n" +
                "- For Config Repo: url1 at 1"));
        assertThat(healthStateForInvalidConfigMerge.getLogLevel(), is(HealthStateLevel.ERROR));

        repoConfig1Cloned = createConfigRepoWithDefaultRules(git("url1"), "plugin", "id-1");
        goConfigRepoConfigDataSource.onConfigRepoConfigChange(repoConfig1Cloned);

        partialConfigService.onSuccessPartialConfig(repoConfig1Cloned, PartialConfigMother.withPipeline("p1_repo1", new RepoConfigOrigin(repoConfig1Cloned, "1")));
        assertThat(goConfigDao.loadConfigHolder().config.getAllPipelineNames().contains(new CaseInsensitiveString("p1_repo1")), is(true));
        assertThat(serverHealthService.filterByScope(HealthStateScope.forPartialConfigRepo(repoConfig1.getRepo().getFingerprint())).isEmpty(), is(true));
    }

    @Test
    public void shouldKeepLastValidPartialsWhenLatestPartialsAreInvalid() {
        partialConfigService.onSuccessPartialConfig(repoConfig1, PartialConfigMother.withPipeline("p1_repo1", new RepoConfigOrigin(repoConfig1, "1")));
        assertThat(goConfigDao.loadConfigHolder().config.getAllPipelineNames().contains(new CaseInsensitiveString("p1_repo1")), is(true));
        assertThat(serverHealthService.filterByScope(HealthStateScope.forPartialConfigRepo(repoConfig1.getRepo().getFingerprint())).isEmpty(), is(true));

        final String invalidPipelineInPartial = "p1_repo1_invalid";
        PartialConfig invalidPartial = PartialConfigMother.invalidPartial(invalidPipelineInPartial, new RepoConfigOrigin(repoConfig1, "2"));
        partialConfigService.onSuccessPartialConfig(repoConfig1, invalidPartial);

        assertThat(findPartial(invalidPipelineInPartial, cachedGoPartials.lastValidPartials()), is(nullValue()));
        assertThat(findPartial(invalidPipelineInPartial, cachedGoPartials.lastKnownPartials()), is(invalidPartial));
        assertThat(goConfigDao.loadConfigHolder().config.getAllPipelineNames().contains(new CaseInsensitiveString(invalidPipelineInPartial)), is(false));
        List<ServerHealthState> serverHealthStatesForRepo1 = serverHealthService.filterByScope(HealthStateScope.forPartialConfigRepo(repoConfig1));
        assertThat(serverHealthStatesForRepo1.isEmpty(), is(false));
        assertThat(serverHealthStatesForRepo1.get(0).getLogLevel(), is(HealthStateLevel.ERROR));
    }

    @Test // See Error #3 from https://github.com/gocd/gocd/issues/8368
    public void shouldDiscardLastValidPartialsWhenLatestPartialsAreInvalidWRTRules() {
        partialConfigService.onSuccessPartialConfig(repoConfig1, PartialConfigMother.withPipeline("p1_repo1", new RepoConfigOrigin(repoConfig1, "1")));
        assertThat(goConfigDao.loadConfigHolder().config.getAllPipelineNames().contains(new CaseInsensitiveString("p1_repo1")), is(true));
        assertThat(serverHealthService.filterByScope(HealthStateScope.forPartialConfigRepo(repoConfig1.getRepo().getFingerprint())).isEmpty(), is(true));


        goConfigRepoConfigDataSource.onConfigRepoConfigChange(repoConfig1);

        ConfigRepoConfig repoConfig1Cloned = createConfigRepoWithDefaultRules(git("url1"), "plugin", "id-1");
        repoConfig1Cloned.setRules(new Rules());

        final String invalidPipelineInPartial = "p1_repo1_invalid";
        PartialConfig invalidPartial = PartialConfigMother.invalidPartial(invalidPipelineInPartial, new RepoConfigOrigin(repoConfig1Cloned, "2"));

        partialConfigService.onSuccessPartialConfig(repoConfig1Cloned, invalidPartial);

        assertThat(findPartial(invalidPipelineInPartial, cachedGoPartials.lastValidPartials()), is(nullValue()));
        assertThat(findPartial(invalidPipelineInPartial, cachedGoPartials.lastKnownPartials()), is(invalidPartial));
        assertThat(goConfigDao.loadConfigHolder().config.getAllPipelineNames().contains(new CaseInsensitiveString("p1_repo1")), is(false));
        assertThat(goConfigDao.loadConfigHolder().config.getAllPipelineNames().contains(new CaseInsensitiveString(invalidPipelineInPartial)), is(false));
        List<ServerHealthState> serverHealthStatesForRepo1 = serverHealthService.filterByScope(HealthStateScope.forPartialConfigRepo(repoConfig1));
        assertThat(serverHealthStatesForRepo1.isEmpty(), is(false));
        assertThat(serverHealthStatesForRepo1.get(0).getLogLevel(), is(HealthStateLevel.ERROR));

        cachedGoPartials.clear();
    }

    @Test
    public void onFailedPartialConfig_shouldRemoveLastValidPartialsFromConfigInCaseOfRuleViolations() {
        partialConfigService.onSuccessPartialConfig(repoConfig1, PartialConfigMother.withPipeline("p1_repo1", new RepoConfigOrigin(repoConfig1, "1")));
        assertThat(goConfigDao.loadConfigHolder().config.getAllPipelineNames().contains(new CaseInsensitiveString("p1_repo1")), is(true));
        assertThat(serverHealthService.filterByScope(HealthStateScope.forPartialConfigRepo(repoConfig1.getRepo().getFingerprint())).isEmpty(), is(true));

        goConfigRepoConfigDataSource.onConfigRepoConfigChange(repoConfig1);

        ConfigRepoConfig repoConfig1Cloned = createConfigRepoWithDefaultRules(git("url1"), "plugin", "id-1");
        repoConfig1Cloned.setRules(new Rules());

        partialConfigService.onFailedPartialConfig(repoConfig1Cloned, null);

        assertThat(cachedGoPartials.getValid(repoConfig1Cloned.getRepo().getFingerprint()), is(nullValue()));
        assertThat(goConfigDao.loadConfigHolder().config.getAllPipelineNames().contains(new CaseInsensitiveString("p1_repo1")), is(false));

        cachedGoPartials.clear();
    }

    private boolean cacheContainsPartial(List<PartialConfig> partialConfigs, final PartialConfig partialConfig) {
        return partialConfigs.stream().filter(new Predicate<PartialConfig>() {
            @Override
            public boolean test(PartialConfig item) {
                return partialConfig.getEnvironments().equals(item.getEnvironments()) && partialConfig.getGroups().equals(item.getGroups()) && partialConfig.getOrigin().equals(item.getOrigin());
            }
        }).findFirst().isPresent();
    }
}
