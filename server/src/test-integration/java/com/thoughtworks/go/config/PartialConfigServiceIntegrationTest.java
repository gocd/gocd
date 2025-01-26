/*
 * Copyright Thoughtworks, Inc.
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;

import static com.thoughtworks.go.helper.MaterialConfigsMother.git;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
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

    @BeforeEach
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

    @AfterEach
    public void teardown() {
        for (PartialConfig partial : cachedGoPartials.lastValidPartials()) {
            assertThat(ErrorCollector.getAllErrors(partial).isEmpty()).isTrue();
        }
        for (PartialConfig partial : cachedGoPartials.lastKnownPartials()) {
            assertThat(ErrorCollector.getAllErrors(partial).isEmpty()).isTrue();
        }
        configHelper.onTearDown();
    }

    @Test
    public void shouldSaveConfigWhenANewValidPartialGetsAdded() {
        partialConfigService.onSuccessPartialConfig(repoConfig1, PartialConfigMother.withPipeline("p1", new RepoConfigOrigin(repoConfig1, "4567")));
        assertThat(goConfigDao.loadConfigHolder().config.getAllPipelineNames().contains(new CaseInsensitiveString("p1"))).isTrue();
    }

    @Test
    public void shouldNotSaveConfigWhenANewInValidPartialGetsAdded() {
        PartialConfig invalidPartial = PartialConfigMother.invalidPartial("p1");
        invalidPartial.setOrigins(new RepoConfigOrigin(repoConfig1, "sha-2"));
        partialConfigService.onSuccessPartialConfig(repoConfig1, invalidPartial);
        assertThat(goConfigDao.loadConfigHolder().config.getAllPipelineNames().contains(new CaseInsensitiveString("p1"))).isFalse();
        List<ServerHealthState> serverHealthStates = serverHealthService.logsSortedForScope(HealthStateScope.forPartialConfigRepo(repoConfig1));
        assertThat(serverHealthStates.isEmpty()).isFalse();
        assertThat(serverHealthStates.get(0).getLogLevel()).isEqualTo(HealthStateLevel.ERROR);
    }

    @Test
    public void shouldTryToValidateMergeAndSaveAllKnownPartialsWhenAPartialChange() {
        cachedGoPartials.cacheAsLastKnown(repoConfig1.getRepo().getFingerprint(), PartialConfigMother.withPipeline("p1_repo1", new RepoConfigOrigin(repoConfig1, "1234")));
        assertThat(goConfigDao.loadConfigHolder().config.getAllPipelineNames().contains(new CaseInsensitiveString("p1_repo1"))).isFalse();

        partialConfigService.onSuccessPartialConfig(repoConfig2, PartialConfigMother.withPipeline("p2_repo2", new RepoConfigOrigin(repoConfig2, "4567")));

        assertThat(goConfigDao.loadConfigHolder().config.getAllPipelineNames().contains(new CaseInsensitiveString("p1_repo1"))).isTrue();
        assertThat(goConfigDao.loadConfigHolder().config.getAllPipelineNames().contains(new CaseInsensitiveString("p2_repo2"))).isTrue();

        assertThat(serverHealthService.logsSortedForScope(HealthStateScope.forPartialConfigRepo(repoConfig2)).isEmpty()).isTrue();
    }

    @Test
    public void shouldValidateAndMergeJustTheChangedPartialAlongWithAllValidPartialsIfValidationOfAllKnownPartialsFail() {
        partialConfigService.onSuccessPartialConfig(repoConfig1, PartialConfigMother.withPipeline("p1_repo1", new RepoConfigOrigin(repoConfig1, "1")));
        assertThat(goConfigDao.loadConfigHolder().config.getAllPipelineNames().contains(new CaseInsensitiveString("p1_repo1"))).isTrue();
        assertThat(serverHealthService.logsSortedForScope(HealthStateScope.forPartialConfigRepo(repoConfig1.getRepo().getFingerprint())).isEmpty()).isTrue();

        final String invalidPipelineInPartial = "p1_repo1_invalid";
        PartialConfig invalidPartial = PartialConfigMother.invalidPartial(invalidPipelineInPartial, new RepoConfigOrigin(repoConfig1, "2"));
        partialConfigService.onSuccessPartialConfig(repoConfig1, invalidPartial);

        assertThat(findPartial(invalidPipelineInPartial, cachedGoPartials.lastValidPartials())).isNull();
        assertThat(findPartial(invalidPipelineInPartial, cachedGoPartials.lastKnownPartials())).isEqualTo(invalidPartial);
        assertThat(goConfigDao.loadConfigHolder().config.getAllPipelineNames().contains(new CaseInsensitiveString(invalidPipelineInPartial))).isFalse();
        List<ServerHealthState> serverHealthStatesForRepo1 = serverHealthService.logsSortedForScope(HealthStateScope.forPartialConfigRepo(repoConfig1));
        assertThat(serverHealthStatesForRepo1.isEmpty()).isFalse();
        assertThat(serverHealthStatesForRepo1.get(0).getLogLevel()).isEqualTo(HealthStateLevel.ERROR);

        partialConfigService.onSuccessPartialConfig(repoConfig2, PartialConfigMother.withPipeline("p2_repo2", new RepoConfigOrigin(repoConfig2, "1")));
        assertThat(findPartial(invalidPipelineInPartial, cachedGoPartials.lastValidPartials())).isNull();
        assertThat(findPartial(invalidPipelineInPartial, cachedGoPartials.lastKnownPartials())).isEqualTo(invalidPartial);

        assertThat(goConfigDao.loadConfigHolder().config.getAllPipelineNames().contains(new CaseInsensitiveString("p1_repo1"))).isTrue();
        assertThat(goConfigDao.loadConfigHolder().config.getAllPipelineNames().contains(new CaseInsensitiveString("p2_repo2"))).isTrue();
        assertThat(goConfigDao.loadConfigHolder().config.getAllPipelineNames().contains(new CaseInsensitiveString(invalidPipelineInPartial))).isFalse();

        serverHealthStatesForRepo1 = serverHealthService.logsSortedForScope(HealthStateScope.forPartialConfigRepo(repoConfig1));
        assertThat(serverHealthStatesForRepo1.isEmpty()).isFalse();
        assertThat(serverHealthStatesForRepo1.get(0).getLogLevel()).isEqualTo(HealthStateLevel.ERROR);
        List<ServerHealthState> serverHealthStatesForRepo2 = serverHealthService.logsSortedForScope(HealthStateScope.forPartialConfigRepo(repoConfig2));
        assertThat(serverHealthStatesForRepo2.isEmpty()).isTrue();
    }

    private PartialConfig findPartial(final String invalidPipelineInPartial, List<PartialConfig> partials) {
        return partials.stream().filter(item -> item.getGroups().first().findBy(new CaseInsensitiveString(invalidPipelineInPartial)) != null).findFirst().orElse(null);
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
        assertThat(goConfigDao.loadConfigHolder().config.getAllPipelineNames().contains(p2.name())).isFalse();
        assertThat(serverHealthService.logsSortedForScope(HealthStateScope.forPartialConfigRepo(repoConfig2)).isEmpty()).isFalse();
        ServerHealthState healthStateForInvalidConfigMerge = serverHealthService.logsSortedForScope(HealthStateScope.forPartialConfigRepo(repoConfig2)).get(0);
        assertThat(healthStateForInvalidConfigMerge.getMessage()).isEqualTo("Invalid Merged Configuration");
        assertThat(healthStateForInvalidConfigMerge.getDescription()).isEqualTo("Number of errors: 3+\n1. Pipeline 'p1_repo1' does not exist. It is used from pipeline 'p2_repo2'.\n2. Pipeline with name 'p1_repo1' does not exist, it is defined as a dependency for pipeline 'p2_repo2' (url2 at revision 1)\n3. Pipeline with name 'p3_repo3' does not exist, it is defined as a dependency for pipeline 'p2_repo2' (url2 at revision 1)\n- For Config Repo: url2 at revision 1");
        assertThat(healthStateForInvalidConfigMerge.getLogLevel()).isEqualTo(HealthStateLevel.ERROR);
        assertThat(cachedGoPartials.lastValidPartials().isEmpty()).isTrue();
        assertThat(cachedGoPartials.lastKnownPartials().size()).isEqualTo(1);
        assertThat(cacheContainsPartial(cachedGoPartials.lastKnownPartials(), repo2)).isTrue();
        assertThat(cacheContainsPartial(cachedGoPartials.lastKnownPartials(), repo1)).isFalse();

        partialConfigService.onSuccessPartialConfig(repoConfig3, repo3);
        assertThat(goConfigDao.loadConfigHolder().config.getAllPipelineNames().contains(p3.name())).isTrue();
        assertThat(goConfigDao.loadConfigHolder().config.getAllPipelineNames().contains(p2.name())).isFalse();
        assertThat(goConfigDao.loadConfigHolder().config.getAllPipelineNames().contains(p1.name())).isFalse();
        assertThat(serverHealthService.logsSortedForScope(HealthStateScope.forPartialConfigRepo(repoConfig3)).isEmpty()).isTrue();
        assertThat(cachedGoPartials.lastValidPartials().size()).isEqualTo(1);
        assertThat(cacheContainsPartial(cachedGoPartials.lastValidPartials(), repo2)).isFalse();
        assertThat(cacheContainsPartial(cachedGoPartials.lastValidPartials(), repo3)).isTrue();
        assertThat(cachedGoPartials.lastKnownPartials().size()).isEqualTo(2);
        assertThat(cacheContainsPartial(cachedGoPartials.lastKnownPartials(), repo2)).isTrue();
        assertThat(cacheContainsPartial(cachedGoPartials.lastKnownPartials(), repo3)).isTrue();

        partialConfigService.onSuccessPartialConfig(repoConfig1, repo1);
        assertThat(goConfigDao.loadConfigHolder().config.getAllPipelineNames().contains(p1.name())).isTrue();
        assertThat(goConfigDao.loadConfigHolder().config.getAllPipelineNames().contains(p2.name())).isTrue();
        assertThat(goConfigDao.loadConfigHolder().config.getAllPipelineNames().contains(p3.name())).isTrue();
        assertThat(serverHealthService.logsSortedForScope(HealthStateScope.forPartialConfigRepo(repoConfig1)).isEmpty()).isTrue();
        assertThat(cachedGoPartials.lastValidPartials().size()).isEqualTo(3);
        assertThat(cacheContainsPartial(cachedGoPartials.lastValidPartials(), repo2)).isTrue();
        assertThat(cacheContainsPartial(cachedGoPartials.lastValidPartials(), repo1)).isTrue();
        assertThat(cacheContainsPartial(cachedGoPartials.lastValidPartials(), repo3)).isTrue();
        assertThat(cachedGoPartials.lastKnownPartials().size()).isEqualTo(3);
        assertThat(cacheContainsPartial(cachedGoPartials.lastKnownPartials(), repo2)).isTrue();
        assertThat(cacheContainsPartial(cachedGoPartials.lastKnownPartials(), repo1)).isTrue();
        assertThat(cacheContainsPartial(cachedGoPartials.lastKnownPartials(), repo3)).isTrue();
    }

    @Test
    public void shouldPerformPreprocessingOfTemplatesOnCRPartials() {
        configHelper.addTemplate("t1", "stage");
        partialConfigService.onSuccessPartialConfig(repoConfig1, PartialConfigMother.withPipelineAssociatedWithTemplate("pipe-with-template", "t1", new RepoConfigOrigin(repoConfig1, "124")));

        assertThat(goConfigDao.loadConfigHolder().config.hasPipelineNamed(new CaseInsensitiveString("pipe-with-template"))).isTrue();
        assertThat(goConfigDao.loadConfigHolder().config.getPipelineConfigByName(new CaseInsensitiveString("pipe-with-template")).hasTemplateApplied()).isTrue();
    }

    @Test
    public void shouldPerformParamResolutionOnCRPipelines() {
        partialConfigService.onSuccessPartialConfig(repoConfig1, PartialConfigMother.withParams("pipe-with-params", "paramName", "paramValue", new RepoConfigOrigin(repoConfig1, "124")));
        assertThat(goConfigDao.loadConfigHolder().config.hasPipelineNamed(new CaseInsensitiveString("pipe-with-params"))).isTrue();
        String resolvedParam = goConfigDao.loadConfigHolder().config.getPipelineConfigByName(new CaseInsensitiveString("pipe-with-params")).getStage("stage").getVariables().get(0).getValue();
        assertThat(resolvedParam).isEqualTo("paramValue");
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
        assertThat(scms.size()).isEqualTo(1);
        assertThat(scms.first()).isEqualTo(expectedSCM);
        assertThat(cacheContainsPartial(cachedGoPartials.lastKnownPartials(), scmPartial)).isTrue();
    }

    @Test
    public void shouldFailToSaveCRPipelineReferencingATemplateWithParams() {
        configHelper.addTemplate("t1", "param1", "stage");
        partialConfigService.onSuccessPartialConfig(repoConfig1, PartialConfigMother.withPipelineAssociatedWithTemplate("pipe-with-template", "t1", new RepoConfigOrigin(repoConfig1, "124")));

        assertThat(goConfigDao.loadConfigHolder().config.hasPipelineNamed(new CaseInsensitiveString("pipe-with-template"))).isFalse();
        List<ServerHealthState> serverHealthStates = serverHealthService.logsSortedForScope(HealthStateScope.forPartialConfigRepo(repoConfig1));
        assertThat(serverHealthStates.isEmpty()).isFalse();
        assertThat(serverHealthStates.get(0).getLogLevel()).isEqualTo(HealthStateLevel.ERROR);
        assertThat(serverHealthStates.get(0).getDescription()).isEqualTo("Parameter 'param1' is not defined. All pipelines using this parameter directly or via a template must define it.- For Config Repo: url1 at revision 124");
    }

    @Test // See Error #1 from https://github.com/gocd/gocd/issues/8368
    public void shouldRemovePipelinesWhenRulesAreUpdatedToSpecifyNoWhitelist() {
        partialConfigService.onSuccessPartialConfig(repoConfig1, PartialConfigMother.withPipeline("p1_repo1", new RepoConfigOrigin(repoConfig1, "1")));
        assertThat(goConfigDao.loadConfigHolder().config.getAllPipelineNames().contains(new CaseInsensitiveString("p1_repo1"))).isTrue();
        assertThat(serverHealthService.logsSortedForScope(HealthStateScope.forPartialConfigRepo(repoConfig1.getRepo().getFingerprint())).isEmpty()).isTrue();

        goConfigRepoConfigDataSource.onConfigRepoConfigChange(repoConfig1);

        ConfigRepoConfig repoConfig1Cloned = createConfigRepoWithDefaultRules(git("url1"), "plugin", "id-1");
        repoConfig1Cloned.setRules(new Rules());

        partialConfigService.onSuccessPartialConfig(repoConfig1Cloned, PartialConfigMother.withPipeline("p1_repo1", new RepoConfigOrigin(repoConfig1Cloned, "1")));
        assertThat(goConfigDao.loadConfigHolder().config.getAllPipelineNames().contains(new CaseInsensitiveString("p1_repo1"))).isFalse();
        assertThat(serverHealthService.logsSortedForScope(HealthStateScope.forPartialConfigRepo(repoConfig1)).isEmpty()).isFalse();
        ServerHealthState healthStateForInvalidConfigMerge = serverHealthService.logsSortedForScope(HealthStateScope.forPartialConfigRepo(repoConfig1)).get(0);
        assertThat(healthStateForInvalidConfigMerge.getMessage()).isEqualTo("Invalid Merged Configuration");
        assertThat(healthStateForInvalidConfigMerge.getDescription()).isEqualTo("""
                Number of errors: 1+
                I. Rule Validation Errors:\s
                \t1. Not allowed to refer to pipeline group 'group'. Check the 'Rules' of this config repository.

                II. Config Validation Errors:\s
                - For Config Repo: url1 at revision 1""");
        assertThat(healthStateForInvalidConfigMerge.getLogLevel()).isEqualTo(HealthStateLevel.ERROR);

        cachedGoPartials.clear();
    }

    @Test // See Error #2 from https://github.com/gocd/gocd/issues/8368
    public void shouldAddPipelinesWhenRulesAreUpdatedToSpecifyAllowAllPipelines() {
        partialConfigService.onSuccessPartialConfig(repoConfig1, PartialConfigMother.withPipeline("p1_repo1", new RepoConfigOrigin(repoConfig1, "1")));
        assertThat(goConfigDao.loadConfigHolder().config.getAllPipelineNames().contains(new CaseInsensitiveString("p1_repo1"))).isTrue();
        assertThat(serverHealthService.logsSortedForScope(HealthStateScope.forPartialConfigRepo(repoConfig1.getRepo().getFingerprint())).isEmpty()).isTrue();

        goConfigRepoConfigDataSource.onConfigRepoConfigChange(repoConfig1);

        ConfigRepoConfig repoConfig1Cloned = createConfigRepoWithDefaultRules(git("url1"), "plugin", "id-1");
        repoConfig1Cloned.setRules(new Rules());

        partialConfigService.onSuccessPartialConfig(repoConfig1Cloned, PartialConfigMother.withPipeline("p1_repo1", new RepoConfigOrigin(repoConfig1Cloned, "1")));
        assertThat(goConfigDao.loadConfigHolder().config.getAllPipelineNames().contains(new CaseInsensitiveString("p1_repo1"))).isFalse();
        assertThat(serverHealthService.logsSortedForScope(HealthStateScope.forPartialConfigRepo(repoConfig1)).isEmpty()).isFalse();
        ServerHealthState healthStateForInvalidConfigMerge = serverHealthService.logsSortedForScope(HealthStateScope.forPartialConfigRepo(repoConfig1)).get(0);
        assertThat(healthStateForInvalidConfigMerge.getMessage()).isEqualTo("Invalid Merged Configuration");
        assertThat(healthStateForInvalidConfigMerge.getDescription()).isEqualTo("""
                Number of errors: 1+
                I. Rule Validation Errors:\s
                \t1. Not allowed to refer to pipeline group 'group'. Check the 'Rules' of this config repository.

                II. Config Validation Errors:\s
                - For Config Repo: url1 at revision 1""");
        assertThat(healthStateForInvalidConfigMerge.getLogLevel()).isEqualTo(HealthStateLevel.ERROR);

        repoConfig1Cloned = createConfigRepoWithDefaultRules(git("url1"), "plugin", "id-1");
        goConfigRepoConfigDataSource.onConfigRepoConfigChange(repoConfig1Cloned);

        partialConfigService.onSuccessPartialConfig(repoConfig1Cloned, PartialConfigMother.withPipeline("p1_repo1", new RepoConfigOrigin(repoConfig1Cloned, "1")));
        assertThat(goConfigDao.loadConfigHolder().config.getAllPipelineNames().contains(new CaseInsensitiveString("p1_repo1"))).isTrue();
        assertThat(serverHealthService.logsSortedForScope(HealthStateScope.forPartialConfigRepo(repoConfig1.getRepo().getFingerprint())).isEmpty()).isTrue();
    }

    @Test
    public void shouldKeepLastValidPartialsWhenLatestPartialsAreInvalid() {
        partialConfigService.onSuccessPartialConfig(repoConfig1, PartialConfigMother.withPipeline("p1_repo1", new RepoConfigOrigin(repoConfig1, "1")));
        assertThat(goConfigDao.loadConfigHolder().config.getAllPipelineNames().contains(new CaseInsensitiveString("p1_repo1"))).isTrue();
        assertThat(serverHealthService.logsSortedForScope(HealthStateScope.forPartialConfigRepo(repoConfig1.getRepo().getFingerprint())).isEmpty()).isTrue();

        final String invalidPipelineInPartial = "p1_repo1_invalid";
        PartialConfig invalidPartial = PartialConfigMother.invalidPartial(invalidPipelineInPartial, new RepoConfigOrigin(repoConfig1, "2"));
        partialConfigService.onSuccessPartialConfig(repoConfig1, invalidPartial);

        assertThat(findPartial(invalidPipelineInPartial, cachedGoPartials.lastValidPartials())).isNull();
        assertThat(findPartial(invalidPipelineInPartial, cachedGoPartials.lastKnownPartials())).isEqualTo(invalidPartial);
        assertThat(goConfigDao.loadConfigHolder().config.getAllPipelineNames().contains(new CaseInsensitiveString(invalidPipelineInPartial))).isFalse();
        List<ServerHealthState> serverHealthStatesForRepo1 = serverHealthService.logsSortedForScope(HealthStateScope.forPartialConfigRepo(repoConfig1));
        assertThat(serverHealthStatesForRepo1.isEmpty()).isFalse();
        assertThat(serverHealthStatesForRepo1.get(0).getLogLevel()).isEqualTo(HealthStateLevel.ERROR);
    }

    @Test // See Error #3 from https://github.com/gocd/gocd/issues/8368
    public void shouldDiscardLastValidPartialsWhenLatestPartialsAreInvalidWRTRules() {
        partialConfigService.onSuccessPartialConfig(repoConfig1, PartialConfigMother.withPipeline("p1_repo1", new RepoConfigOrigin(repoConfig1, "1")));
        assertThat(goConfigDao.loadConfigHolder().config.getAllPipelineNames().contains(new CaseInsensitiveString("p1_repo1"))).isTrue();
        assertThat(serverHealthService.logsSortedForScope(HealthStateScope.forPartialConfigRepo(repoConfig1.getRepo().getFingerprint())).isEmpty()).isTrue();


        goConfigRepoConfigDataSource.onConfigRepoConfigChange(repoConfig1);

        ConfigRepoConfig repoConfig1Cloned = createConfigRepoWithDefaultRules(git("url1"), "plugin", "id-1");
        repoConfig1Cloned.setRules(new Rules());

        final String invalidPipelineInPartial = "p1_repo1_invalid";
        PartialConfig invalidPartial = PartialConfigMother.invalidPartial(invalidPipelineInPartial, new RepoConfigOrigin(repoConfig1Cloned, "2"));

        partialConfigService.onSuccessPartialConfig(repoConfig1Cloned, invalidPartial);

        assertThat(findPartial(invalidPipelineInPartial, cachedGoPartials.lastValidPartials())).isNull();
        assertThat(findPartial(invalidPipelineInPartial, cachedGoPartials.lastKnownPartials())).isEqualTo(invalidPartial);
        assertThat(goConfigDao.loadConfigHolder().config.getAllPipelineNames().contains(new CaseInsensitiveString("p1_repo1"))).isFalse();
        assertThat(goConfigDao.loadConfigHolder().config.getAllPipelineNames().contains(new CaseInsensitiveString(invalidPipelineInPartial))).isFalse();
        List<ServerHealthState> serverHealthStatesForRepo1 = serverHealthService.logsSortedForScope(HealthStateScope.forPartialConfigRepo(repoConfig1));
        assertThat(serverHealthStatesForRepo1.isEmpty()).isFalse();
        assertThat(serverHealthStatesForRepo1.get(0).getLogLevel()).isEqualTo(HealthStateLevel.ERROR);

        cachedGoPartials.clear();
    }

    @Test
    public void onFailedPartialConfig_shouldRemoveLastValidPartialsFromConfigInCaseOfRuleViolations() {
        partialConfigService.onSuccessPartialConfig(repoConfig1, PartialConfigMother.withPipeline("p1_repo1", new RepoConfigOrigin(repoConfig1, "1")));
        assertThat(goConfigDao.loadConfigHolder().config.getAllPipelineNames().contains(new CaseInsensitiveString("p1_repo1"))).isTrue();
        assertThat(serverHealthService.logsSortedForScope(HealthStateScope.forPartialConfigRepo(repoConfig1.getRepo().getFingerprint())).isEmpty()).isTrue();

        goConfigRepoConfigDataSource.onConfigRepoConfigChange(repoConfig1);

        ConfigRepoConfig repoConfig1Cloned = createConfigRepoWithDefaultRules(git("url1"), "plugin", "id-1");
        repoConfig1Cloned.setRules(new Rules());

        partialConfigService.onFailedPartialConfig(repoConfig1Cloned, null);

        assertThat(cachedGoPartials.getValid(repoConfig1Cloned.getRepo().getFingerprint())).isNull();
        assertThat(goConfigDao.loadConfigHolder().config.getAllPipelineNames().contains(new CaseInsensitiveString("p1_repo1"))).isFalse();

        cachedGoPartials.clear();
    }

    private boolean cacheContainsPartial(List<PartialConfig> partialConfigs, final PartialConfig partialConfig) {
        return partialConfigs.stream().anyMatch(item -> partialConfig.getEnvironments().equals(item.getEnvironments()) && partialConfig.getGroups().equals(item.getGroups()) && partialConfig.getOrigin().equals(item.getOrigin()));
    }
}
