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
package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.exceptions.EntityType;
import com.thoughtworks.go.config.remote.ConfigRepoConfig;
import com.thoughtworks.go.config.remote.PartialConfig;
import com.thoughtworks.go.config.remote.RepoConfigOrigin;
import com.thoughtworks.go.domain.ConfigElementForEdit;
import com.thoughtworks.go.helper.AgentMother;
import com.thoughtworks.go.helper.PartialConfigMother;
import com.thoughtworks.go.helper.PipelineConfigMother;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.util.GoConfigFileHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {
        "classpath:/applicationContext-global.xml",
        "classpath:/applicationContext-dataLocalAccess.xml",
        "classpath:/testPropertyConfigurer.xml",
        "classpath:/spring-all-servlet.xml",
})
public class EnvironmentConfigServiceIntegrationTest {

    @Autowired
    private GoConfigDao goConfigDao;
    @Autowired
    private GoConfigService goConfigService;
    @Autowired
    private EntityHashingService entityHashingService;
    @Autowired
    private AgentService agentService;
    @Autowired
    private EnvironmentConfigService environmentConfigService;
    @Autowired
    private ConfigRepoService configRepoService;
    @Autowired
    private DatabaseAccessHelper dbHelper;

    private final GoConfigFileHelper configHelper = new GoConfigFileHelper();


    @BeforeEach
    public void setup() throws Exception {
        configHelper.usingCruiseConfigDao(goConfigDao);
        configHelper.onSetUp();

        goConfigService.forceNotifyListeners();

        dbHelper.onSetUp();
    }

    @AfterEach
    public void tearDown() throws Exception {
        configHelper.onTearDown();
        dbHelper.onTearDown();
    }

    @Test
    public void shouldReturnTheCorrectLocalizedMessageForDuplicateEnvironment() {
        configHelper.addEnvironments("foo-env");
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        environmentConfigService.createEnvironment(env("foo-env", new ArrayList<>(), new ArrayList<>(), new ArrayList<>()), new Username(new CaseInsensitiveString("any")), result);
        assertThat(result.message()).isEqualTo(EntityType.Environment.alreadyExists("foo-env"));
    }

    @Test
    public void shouldReturnTheCorrectLocalizedMessageForDuplicatePipelinesInAnEnvironment() {
        BasicEnvironmentConfig environmentConfig = environmentConfig("uat");
        goConfigService.addPipeline(PipelineConfigMother.createPipelineConfig("foo", "dev", "job"), "foo-grp");
        environmentConfig.addPipeline(new CaseInsensitiveString("foo"));
        goConfigService.addEnvironment(environmentConfig);

        List<String> pipelines = new ArrayList<>();
        pipelines.add("foo");
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        environmentConfigService.createEnvironment(env("foo-env", pipelines, new ArrayList<>(), new ArrayList<>()), new Username(new CaseInsensitiveString("any")), result);

        result = new HttpLocalizedOperationResult();
        environmentConfigService.createEnvironment(env("env", pipelines, new ArrayList<>(), new ArrayList<>()), new Username(new CaseInsensitiveString("any")), result);
        assertThat(result.message()).isEqualTo("Failed to add environment 'env'. Associating pipeline(s) which is already part of uat environment");
    }

    @Test
    public void shouldPointOutDuplicatePipelinesInAnEnvironmentOnEnvironmentUpdate() {
        String pipelineName = "pipeline-1";
        goConfigService.addPipeline(PipelineConfigMother.createPipelineConfig(pipelineName, "dev", "job"), "pipeline-1-grp");
        Username user = new Username(new CaseInsensitiveString("any"));
        environmentConfigService.createEnvironment(env("environment-1", List.of(pipelineName), new ArrayList<>(), new ArrayList<>()), user, new HttpLocalizedOperationResult());
        String environmentBeingUpdated = "environment-2";
        environmentConfigService.createEnvironment(env(environmentBeingUpdated, new ArrayList<>(), new ArrayList<>(), new ArrayList<>()), user, new HttpLocalizedOperationResult());

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        BasicEnvironmentConfig updatedEnvConfig = new BasicEnvironmentConfig(new CaseInsensitiveString(environmentBeingUpdated));
        updatedEnvConfig.addPipeline(new CaseInsensitiveString(pipelineName));
        environmentConfigService.updateEnvironment(environmentBeingUpdated, updatedEnvConfig,
                user, entityHashingService.hashForEntity(environmentConfigService.getEnvironmentConfig(environmentBeingUpdated)), result);
        assertThat(result.message()).isEqualTo("Failed to update environment 'environment-2'. Associating pipeline(s) which is already part of environment-1 environment");
    }

    @Test
    public void shouldPointOutDuplicatePipelinesInAnEnvironmentOnEnvironmentPatch() {
        String pipelineName = "pipeline-1";
        goConfigService.addPipeline(PipelineConfigMother.createPipelineConfig(pipelineName, "dev", "job"), "pipeline-1-grp");
        Username user = new Username(new CaseInsensitiveString("any"));
        environmentConfigService.createEnvironment(env("environment-1", List.of(pipelineName), new ArrayList<>(), new ArrayList<>()), user, new HttpLocalizedOperationResult());
        String environmentBeingUpdated = "environment-2";
        environmentConfigService.createEnvironment(env(environmentBeingUpdated, new ArrayList<>(), new ArrayList<>(), new ArrayList<>()), user, new HttpLocalizedOperationResult());

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        BasicEnvironmentConfig environmentConfigBeingUpdated = new BasicEnvironmentConfig(new CaseInsensitiveString(environmentBeingUpdated));
        environmentConfigService.patchEnvironment(environmentConfigBeingUpdated, List.of(pipelineName), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(),
                user, result);
        assertThat(result.message()).isEqualTo("Failed to update environment 'environment-2'. Associating pipeline(s) which is already part of environment-1 environment");
    }

    @Test
    public void shouldReturnBadRequestForInvalidEnvName() {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        environmentConfigService.createEnvironment(env("foo env", new ArrayList<>(), new ArrayList<>(), new ArrayList<>()), new Username(new CaseInsensitiveString("any")), result);
        assertThat(result.httpCode()).isEqualTo(HttpServletResponse.SC_BAD_REQUEST);
        assertThat(result.message()).isEqualTo("Failed to add environment 'foo env'. failed to save : Environment name is invalid. \"foo env\" should conform to the pattern - [a-zA-Z0-9_\\-]{1}[a-zA-Z0-9_\\-.]*");
    }

    @Test
    public void shouldUpdateExistingEnvironment_ForNewUpdateEnvironmentMethod() {
        BasicEnvironmentConfig uat = environmentConfig("uat");
        goConfigService.addPipeline(PipelineConfigMother.createPipelineConfig("foo", "dev", "job"), "foo-grp");
        goConfigService.addPipeline(PipelineConfigMother.createPipelineConfig("bar", "dev", "job"), "foo-grp");
        uat.addPipeline(new CaseInsensitiveString("foo"));
        uat.addEnvironmentVariable("env-one", "ONE");
        uat.addEnvironmentVariable("env-two", "TWO");
        goConfigService.addEnvironment(new BasicEnvironmentConfig(new CaseInsensitiveString("dev")));
        goConfigService.addEnvironment(new BasicEnvironmentConfig(new CaseInsensitiveString("qa")));
        goConfigService.addEnvironment(uat);
        goConfigService.addEnvironment(new BasicEnvironmentConfig(new CaseInsensitiveString("acceptance")));
        goConfigService.addEnvironment(new BasicEnvironmentConfig(new CaseInsensitiveString("function_testing")));
        EnvironmentConfig newUat = new BasicEnvironmentConfig(new CaseInsensitiveString("prod"));
        newUat.addPipeline(new CaseInsensitiveString("bar"));
        newUat.addEnvironmentVariable("env-three", "THREE");
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        String digest = entityHashingService.hashForEntity(uat);
        environmentConfigService.updateEnvironment(uat.name().toString(), newUat, new Username(new CaseInsensitiveString("foo")), digest, result);
        EnvironmentConfig updatedEnv = environmentConfigService.getEnvironmentConfig("prod");
        assertThat(updatedEnv.name()).isEqualTo(new CaseInsensitiveString("prod"));
        assertThat(updatedEnv.getPipelineNames()).isEqualTo(List.of(new CaseInsensitiveString("bar")));
        EnvironmentVariablesConfig updatedVariables = new EnvironmentVariablesConfig();
        updatedVariables.add("env-three", "THREE");
        assertThat(updatedEnv.getVariables()).isEqualTo(updatedVariables);
        EnvironmentsConfig currentEnvironments = goConfigService.getCurrentConfig().getEnvironments();
        assertThat(currentEnvironments.indexOf(updatedEnv)).isEqualTo(2);
        assertThat(currentEnvironments.size()).isEqualTo(5);
    }

    @Test
    public void shouldReturnTheCorrectLocalizedMessageForUpdateWhenDuplicateEnvironmentExists_ForNewUpdateEnvironmentMethod() {
        configHelper.addEnvironments("foo-env");
        configHelper.addEnvironments("bar-env");
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        String digest = entityHashingService.hashForEntity(environmentConfigService.getEnvironmentConfig("bar-env"));
        environmentConfigService.updateEnvironment("bar-env", env("foo-env", new ArrayList<>(), new ArrayList<>(), new ArrayList<>()), new Username(new CaseInsensitiveString("any")), digest, result);
        assertThat(result.message()).containsAnyOf("Failed to update environment 'bar-env'. failed to save : Duplicate unique value [foo-env] declared for identity constraint of element \"environments\".",
            "Failed to update environment 'bar-env'. failed to save : Duplicate unique value [foo-env] declared for identity constraint \"uniqueEnvironmentName\" of element \"environments\"");
    }

    @Test
    public void shouldReturnTheCorrectLocalizedMessageForUpdateWhenStaleEtagIsProvided() {
        configHelper.addEnvironments("foo-env");
        configHelper.addEnvironments("bar-env");
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        String digest = "invalid-digest";
        environmentConfigService.updateEnvironment("bar-env", env("foo-env", new ArrayList<>(), new ArrayList<>(), new ArrayList<>()), new Username(new CaseInsensitiveString("any")), digest, result);
        assertThat(result.message()).isEqualTo(EntityType.Environment.staleConfig("bar-env"));
    }

    @Test
    public void shouldReturnBadRequestForUpdateWhenUsingInvalidEnvName_ForNewUpdateEnvironmentMethod_ForNewUpdateEnvironmentMethod() {
        configHelper.addEnvironments("foo-env");
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        String digest = entityHashingService.hashForEntity(environmentConfigService.getEnvironmentConfig("foo-env"));
        environmentConfigService.updateEnvironment("foo-env", env("foo env", new ArrayList<>(), new ArrayList<>(), new ArrayList<>()), new Username(new CaseInsensitiveString("any")), digest, result);
        assertThat(result.httpCode()).isEqualTo(HttpServletResponse.SC_BAD_REQUEST);
        assertThat(result.message()).contains("Failed to update environment 'foo-env'.");
    }

    @Test
    public void shouldDeleteAnEnvironment() {
        String environmentName = "dev";
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        goConfigService.addEnvironment(new BasicEnvironmentConfig(new CaseInsensitiveString(environmentName)));

        assertTrue(goConfigService.hasEnvironmentNamed(new CaseInsensitiveString(environmentName)));
        environmentConfigService.deleteEnvironment(environmentConfigService.getEnvironmentConfig(environmentName), new Username(new CaseInsensitiveString("foo")), result);
        assertFalse(goConfigService.hasEnvironmentNamed(new CaseInsensitiveString(environmentName)));
        assertThat(result.message()).isEqualTo(EntityType.Environment.deleteSuccessful(environmentName));
    }

    @Test
    public void shouldNotDeleteAnEnvironmentDefinedInConfigRepository() {
        String uuid = "uuid-1";
        String envName = "env";
        Username user = Username.ANONYMOUS;
        String configRepoId = createMergeEnvironment(envName, uuid);

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        environmentConfigService.deleteEnvironment(environmentConfigService.getEnvironmentConfig(envName), user, result);

        assertTrue(goConfigService.hasEnvironmentNamed(new CaseInsensitiveString(envName)));
        assertThat(result.message()).isEqualTo(String.format("Failed to delete environment 'env'. Environment is partially defined in [%s] config repositories", configRepoId));
    }

    @Test
    public void shouldDeleteAnEnvWhichContainsAgents() {
        String environmentName = "dev";
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        CaseInsensitiveString envName = new CaseInsensitiveString(environmentName);
        goConfigService.addEnvironment(new BasicEnvironmentConfig(envName));

        Agent agent = AgentMother.approvedAgent();
        agent.addEnvironment(environmentName);
        agentService.register(agent);

        // required to force update the cache from the DB
        environmentConfigService.syncEnvironments(goConfigService.getEnvironments());

        assertTrue(goConfigService.hasEnvironmentNamed(envName));
        assertTrue(environmentConfigService.getEnvironmentConfig(environmentName).hasAgent("uuid"));

        environmentConfigService.deleteEnvironment(environmentConfigService.getEnvironmentConfig(environmentName), new Username(new CaseInsensitiveString("foo")), result);

        assertFalse(goConfigService.hasEnvironmentNamed(envName));
        assertThat(result.message()).isEqualTo(EntityType.Environment.deleteSuccessful(environmentName));
    }

    @Test
    public void shouldPatchAnEnvironment() {
        String environmentName = "env";

        BasicEnvironmentConfig env = environmentConfig(environmentName);
        Username user = Username.ANONYMOUS;

        goConfigService.addEnvironment(env);
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

        List<String> pipelinesToAdd = new ArrayList<>();
        List<String> pipelinesToRemove = new ArrayList<>();
        List<EnvironmentVariableConfig> envVarsToAdd = new ArrayList<>();
        envVarsToAdd.add(new EnvironmentVariableConfig("name", "val"));
        List<String> envVarsToRemove = new ArrayList<>();

        environmentConfigService.patchEnvironment(environmentConfigService.getEnvironmentConfig(environmentName), pipelinesToAdd, pipelinesToRemove, envVarsToAdd, envVarsToRemove, user, result);
        EnvironmentConfig updatedEnv = environmentConfigService.getEnvironmentConfig(env.name().toString());

        assertThat(updatedEnv.name()).isEqualTo(new CaseInsensitiveString(environmentName));
        assertThat(updatedEnv.getVariables().hasVariable("name")).isTrue();
        assertThat(result.message()).contains("Updated environment 'env'.");
    }

    @Test
    public void shouldReturnAClonedInstanceOfEnvironmentConfig() {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        configHelper.addEnvironments("foo-env");
        assertThat(environmentConfigService.getEnvironmentConfig("foo-env")).isSameAs(environmentConfigService.getEnvironmentConfig("foo-env"));
        assertThat(environmentConfigService.getEnvironmentConfig("foo-env")).isNotSameAs(environmentConfigService.getMergedEnvironmentforDisplay("foo-env", result).getConfigElement());
        assertThat(result.isSuccessful()).isTrue();
    }

    @Test
    public void shouldPopulateResultWithErrorIfEnvNotFound() {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        ConfigElementForEdit<EnvironmentConfig> edit = environmentConfigService.getMergedEnvironmentforDisplay("foo-env", result);
        assertThat(result.message()).isEqualTo(EntityType.Environment.notFoundMessage("foo-env"));
        assertThat(edit).isNull();
    }

    @Test
    public void shouldSyncEnvironmentsIfAConfigRepoIsRemoved() {
        String uuid = "uuid-1";
        String envName = "env";
        Username user = Username.ANONYMOUS;
        agentService.register(new Agent(uuid, "host-1", "192.168.1.2"));
        String configRepoId = createMergeEnvironment(envName, uuid);

        EnvironmentConfig envConfig = environmentConfigService.getEnvironmentConfig(envName);

        assertThat(envConfig.getAgents().size()).isEqualTo(1);
        assertThat(envConfig.getAgents().getUuids()).contains(uuid);

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        configRepoService.deleteConfigRepo(configRepoId, user, result);

        EnvironmentConfig envConfigPostDelete = environmentConfigService.getEnvironmentConfig(envName);

        assertThat(envConfigPostDelete.getAgents().size()).isEqualTo(0);
    }

    private String createMergeEnvironment(String envName, String agentUuid) {
        RepoConfigOrigin repoConfigOrigin = PartialConfigMother.createRepoOrigin();
        ConfigRepoConfig configRepo = repoConfigOrigin.getConfigRepo();
        PartialConfig partialConfig = new PartialConfig();
        BasicEnvironmentConfig envConfig = new BasicEnvironmentConfig(new CaseInsensitiveString(envName));
        envConfig.addAgent(agentUuid);
        partialConfig.getEnvironments().add(envConfig);
        partialConfig.setOrigins(repoConfigOrigin);
        goConfigService.updateConfig(cruiseConfig -> {
            cruiseConfig.getConfigRepos().add(configRepo);
            cruiseConfig.getPartials().add(partialConfig);
            cruiseConfig.addEnvironment(envName);
            return cruiseConfig;
        });
        goConfigService.forceNotifyListeners();
        return configRepo.getId();
    }

    private BasicEnvironmentConfig environmentConfig(String name) {
        return new BasicEnvironmentConfig(new CaseInsensitiveString(name));
    }

    public static BasicEnvironmentConfig env(String name, List<String> selectedPipelines, List<Map<String, String>> environmentVariables, List<String> selectedAgents) {
        BasicEnvironmentConfig config = new BasicEnvironmentConfig(new CaseInsensitiveString(name));
        for (String selectedPipeline : selectedPipelines) {
            config.addPipeline(new CaseInsensitiveString(selectedPipeline));
        }
        for (String selectedAgent : selectedAgents) {
            config.addAgent(selectedAgent);
        }
        for (Map<String, String> environmentVariable : environmentVariables) {
            config.getVariables().add(environmentVariable.get("name"), environmentVariable.get("value"));
        }
        return config;
    }

}
