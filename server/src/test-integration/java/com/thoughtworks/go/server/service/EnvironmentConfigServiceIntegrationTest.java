/*
 * Copyright 2018 ThoughtWorks, Inc.
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
import com.thoughtworks.go.domain.ConfigElementForEdit;
import com.thoughtworks.go.helper.PipelineConfigMother;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.util.GoConfigFileHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:testPropertyConfigurer.xml",
        "classpath:WEB-INF/spring-all-servlet.xml",
})
public class EnvironmentConfigServiceIntegrationTest {

    @Autowired
    private GoConfigDao goConfigDao;
    @Autowired
    private GoConfigService goConfigService;
    @Autowired
    private EntityHashingService entityHashingService;
    @Autowired
    private AgentConfigService agentConfigService;
    @Autowired
    private EnvironmentConfigService service;

    private GoConfigFileHelper configHelper = new GoConfigFileHelper();


    @Before
    public void setup() throws Exception {
        configHelper.usingCruiseConfigDao(goConfigDao).initializeConfigFile();
        configHelper.onSetUp();

        goConfigService.forceNotifyListeners();
    }

    @After
    public void tearDown() throws Exception {
        configHelper.onTearDown();
    }

    @Test
    public void shouldReturnTheCorrectLocalizedMessageForNoPermission() throws IOException {
        configHelper.enableSecurity();
        configHelper.addAdmins("super_hero");
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        service.createEnvironment(env("foo-env", new ArrayList<String>(), new ArrayList<Map<String, String>>(), new ArrayList<String>()), new Username(new CaseInsensitiveString("evil_hacker")), result);
        assertThat(result.message(), is("Failed to access environment 'foo-env'. User 'evil_hacker' does not have permission to access environment."));
    }

    @Test
    public void shouldReturnTheCorrectLocalizedMessageForDuplicateEnvironment() {
        configHelper.addEnvironments("foo-env");
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        service.createEnvironment(env("foo-env", new ArrayList<String>(), new ArrayList<Map<String, String>>(), new ArrayList<String>()), new Username(new CaseInsensitiveString("any")), result);
        assertThat(result.message(), is("Failed to add environment. The environment 'foo-env' already exists."));
    }

    @Test
    public void shouldReturnTheCorrectLocalizedMessageForDuplicatePipelinesInAnEnvironment() {
        BasicEnvironmentConfig environmentConfig = environmentConfig("uat");
        goConfigService.addPipeline(PipelineConfigMother.createPipelineConfig("foo", "dev", "job"), "foo-grp");
        environmentConfig.addPipeline(new CaseInsensitiveString("foo"));
        goConfigService.addEnvironment(environmentConfig);

        ArrayList<String> pipelines = new ArrayList<>();
        pipelines.add("foo");
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        service.createEnvironment(env("foo-env", pipelines, new ArrayList<Map<String, String>>(), new ArrayList<String>()), new Username(new CaseInsensitiveString("any")), result);

        result = new HttpLocalizedOperationResult();
        service.createEnvironment(env("env", pipelines, new ArrayList<Map<String, String>>(), new ArrayList<String>()), new Username(new CaseInsensitiveString("any")), result);
        assertThat(result.message(), is("Failed to add environment 'env'. Associating pipeline(s) which is already part of uat environment"));
    }

    @Test
    public void shouldPointOutDuplicatePipelinesInAnEnvironmentOnEnvironmentUpdate() {
        String pipelineName = "pipeline-1";
        goConfigService.addPipeline(PipelineConfigMother.createPipelineConfig(pipelineName, "dev", "job"), "pipeline-1-grp");
        Username user = new Username(new CaseInsensitiveString("any"));
        service.createEnvironment(env("environment-1", Arrays.asList(pipelineName), new ArrayList<>(), new ArrayList<>()), user, new HttpLocalizedOperationResult());
        String environmentBeingUpdated = "environment-2";
        service.createEnvironment(env(environmentBeingUpdated, new ArrayList<>(), new ArrayList<>(), new ArrayList<>()), user, new HttpLocalizedOperationResult());

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        BasicEnvironmentConfig updatedEnvConfig = new BasicEnvironmentConfig(new CaseInsensitiveString(environmentBeingUpdated));
        updatedEnvConfig.addPipeline(new CaseInsensitiveString(pipelineName));
        service.updateEnvironment(environmentBeingUpdated, updatedEnvConfig,
                user, entityHashingService.md5ForEntity(service.getEnvironmentConfig(environmentBeingUpdated)), result);
        assertThat(result.message(), is("Failed to update environment 'environment-2'. Associating pipeline(s) which is already part of environment-1 environment"));
    }

    @Test
    public void shouldPointOutDuplicatePipelinesInAnEnvironmentOnEnvironmentPatch() {
        String pipelineName = "pipeline-1";
        goConfigService.addPipeline(PipelineConfigMother.createPipelineConfig(pipelineName, "dev", "job"), "pipeline-1-grp");
        Username user = new Username(new CaseInsensitiveString("any"));
        service.createEnvironment(env("environment-1", Arrays.asList(pipelineName), new ArrayList<>(), new ArrayList<>()), user, new HttpLocalizedOperationResult());
        String environmentBeingUpdated = "environment-2";
        service.createEnvironment(env(environmentBeingUpdated, new ArrayList<>(), new ArrayList<>(), new ArrayList<>()), user, new HttpLocalizedOperationResult());

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        BasicEnvironmentConfig environmentConfigBeingUpdated = new BasicEnvironmentConfig(new CaseInsensitiveString(environmentBeingUpdated));
        service.patchEnvironment(environmentConfigBeingUpdated, Arrays.asList(pipelineName),new ArrayList<>(),new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(),
                user, result);
        assertThat(result.message(), is("Failed to update environment 'environment-2'. Associating pipeline(s) which is already part of environment-1 environment"));
    }

    @Test
    public void shouldReturnBadRequestForInvalidEnvName() {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        service.createEnvironment(env("foo env", new ArrayList<String>(), new ArrayList<Map<String, String>>(), new ArrayList<String>()), new Username(new CaseInsensitiveString("any")), result);
        assertThat(result.httpCode(), is(HttpServletResponse.SC_BAD_REQUEST));
        assertThat(result.message(), is("Failed to add environment 'foo env'. failed to save : Environment name is invalid. \"foo env\" should conform to the pattern - [a-zA-Z0-9_\\-]{1}[a-zA-Z0-9_\\-.]*"));
    }

    @Test
    public void shouldUpdateExistingEnvironment_ForNewUpdateEnvironmentMethod() throws Exception {
        BasicEnvironmentConfig uat = environmentConfig("uat");
        goConfigService.addPipeline(PipelineConfigMother.createPipelineConfig("foo", "dev", "job"), "foo-grp");
        goConfigService.addPipeline(PipelineConfigMother.createPipelineConfig("bar", "dev", "job"), "foo-grp");
        Username user = Username.ANONYMOUS;
        agentConfigService.addAgent(new AgentConfig("uuid-1", "host-1", "192.168.1.2"), user);
        agentConfigService.addAgent(new AgentConfig("uuid-2", "host-2", "192.168.1.3"), user);
        uat.addPipeline(new CaseInsensitiveString("foo"));
        uat.addAgent("uuid-2");
        uat.addEnvironmentVariable("env-one", "ONE");
        uat.addEnvironmentVariable("env-two", "TWO");
        goConfigService.addEnvironment(new BasicEnvironmentConfig(new CaseInsensitiveString("dev")));
        goConfigService.addEnvironment(new BasicEnvironmentConfig(new CaseInsensitiveString("qa")));
        goConfigService.addEnvironment(uat);
        goConfigService.addEnvironment(new BasicEnvironmentConfig(new CaseInsensitiveString("acceptance")));
        goConfigService.addEnvironment(new BasicEnvironmentConfig(new CaseInsensitiveString("function_testing")));
        EnvironmentConfig newUat = new BasicEnvironmentConfig(new CaseInsensitiveString("prod"));
        newUat.addPipeline(new CaseInsensitiveString("bar"));
        newUat.addAgent("uuid-1");
        newUat.addEnvironmentVariable("env-three", "THREE");
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        String md5 = entityHashingService.md5ForEntity(uat);
        service.updateEnvironment(uat.name().toString(), newUat, new Username(new CaseInsensitiveString("foo")), md5, result);
        EnvironmentConfig updatedEnv = service.named("prod");
        assertThat(updatedEnv.name(), is(new CaseInsensitiveString("prod")));
        assertThat(updatedEnv.getAgents().getUuids(), is(Arrays.asList("uuid-1")));
        assertThat(updatedEnv.getPipelineNames(), is(Arrays.asList(new CaseInsensitiveString("bar"))));
        EnvironmentVariablesConfig updatedVariables = new EnvironmentVariablesConfig();
        updatedVariables.add("env-three", "THREE");
        assertThat(updatedEnv.getVariables(), is(updatedVariables));
        EnvironmentsConfig currentEnvironments = goConfigService.getCurrentConfig().getEnvironments();
        assertThat(currentEnvironments.indexOf(updatedEnv), is(2));
        assertThat(currentEnvironments.size(), is(5));
    }

    @Test
    public void shouldReturnTheCorrectLocalizedMessageWhenUserDoesNotHavePermissionToUpdate_ForNewUpdateEnvironmentMethod() throws Exception {
        configHelper.addEnvironments("foo");
        configHelper.enableSecurity();
        configHelper.addAdmins("super_hero");
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

        String md5 = entityHashingService.md5ForEntity(service.getEnvironmentConfig("foo"));
        service.updateEnvironment("foo", env("foo-env", new ArrayList<String>(), new ArrayList<Map<String, String>>(), new ArrayList<String>()), new Username(new CaseInsensitiveString("evil_hacker")), md5, result);
        assertThat(result.message(), is("Failed to access environment 'foo-env'. User 'evil_hacker' does not have permission to access environment."));
    }

    @Test
    public void shouldReturnTheCorrectLocalizedMessageForUpdateWhenDuplicateEnvironmentExists_ForNewUpdateEnvironmentMethod() {
        configHelper.addEnvironments("foo-env");
        configHelper.addEnvironments("bar-env");
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        String md5 = entityHashingService.md5ForEntity(service.getEnvironmentConfig("bar-env"));
        service.updateEnvironment("bar-env", env("foo-env", new ArrayList<String>(), new ArrayList<Map<String, String>>(), new ArrayList<String>()), new Username(new CaseInsensitiveString("any")), md5, result);
        assertThat(result.message(), anyOf(
                is("Failed to update environment 'bar-env'. failed to save : Duplicate unique value [foo-env] declared for identity constraint of element \"environments\"."),
                is("Failed to update environment 'bar-env'. failed to save : Duplicate unique value [foo-env] declared for identity constraint \"uniqueEnvironmentName\" of element \"environments\".")
        ));
    }

    @Test
    public void shouldReturnTheCorrectLocalizedMessageForUpdateWhenStaleEtagIsProvided() {
        configHelper.addEnvironments("foo-env");
        configHelper.addEnvironments("bar-env");
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        String md5 = "invalid-md5";
        service.updateEnvironment("bar-env", env("foo-env", new ArrayList<String>(), new ArrayList<Map<String, String>>(), new ArrayList<String>()), new Username(new CaseInsensitiveString("any")), md5, result);
        assertThat(result.message(), is("Someone has modified the configuration for Environment 'bar-env'. Please update your copy of the config with the changes."));
    }

    @Test
    public void shouldReturnBadRequestForUpdateWhenUsingInvalidEnvName_ForNewUpdateEnvironmentMethod_ForNewUpdateEnvironmentMethod() {
        configHelper.addEnvironments("foo-env");
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        String md5 = entityHashingService.md5ForEntity(service.getEnvironmentConfig("foo-env"));
        service.updateEnvironment("foo-env", env("foo env", new ArrayList<String>(), new ArrayList<Map<String, String>>(), new ArrayList<String>()), new Username(new CaseInsensitiveString("any")), md5, result);
        assertThat(result.httpCode(), is(HttpServletResponse.SC_BAD_REQUEST));
        assertThat(result.message(), containsString("Failed to update environment 'foo-env'."));
    }

    @Test
    public void shouldDeleteAnEnvironment() throws Exception {
        String environmentName = "dev";
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        goConfigService.addEnvironment(new BasicEnvironmentConfig(new CaseInsensitiveString(environmentName)));

        assertTrue(goConfigService.hasEnvironmentNamed(new CaseInsensitiveString(environmentName)));
        service.deleteEnvironment(service.getEnvironmentConfig(environmentName), new Username(new CaseInsensitiveString("foo")), result);
        assertFalse(goConfigService.hasEnvironmentNamed(new CaseInsensitiveString(environmentName)));
        assertThat(result.message(), containsString("The environment 'dev' was deleted successfully."));
    }

    @Test
    public void shouldReturnTheCorrectLocalizedMessageWhenUserDoesNotHavePermissionToDelete() {
        configHelper.addEnvironments("foo");
        configHelper.enableSecurity();
        configHelper.addAdmins("super_hero");
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        service.deleteEnvironment(service.getEnvironmentConfig("foo"), new Username(new CaseInsensitiveString("evil_hacker")), result);
        assertThat(result.message(), is("Failed to access environment 'foo'. User 'evil_hacker' does not have permission to access environment."));
    }

    @Test
    public void shouldPatchAnEnvironment() throws Exception {
        String environmentName = "env";

        BasicEnvironmentConfig env = environmentConfig(environmentName);
        Username user = Username.ANONYMOUS;
        String uuid = "uuid-1";
        agentConfigService.addAgent(new AgentConfig(uuid, "host-1", "192.168.1.2"), user);
        goConfigService.addEnvironment(env);
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

        List<String> agentsToremove = new ArrayList<>();
        List<String> agentsToAdd = new ArrayList<>();
        agentsToAdd.add(uuid);
        List<String> pipelinesToAdd = new ArrayList<>();
        List<String> pipelinesToRemove = new ArrayList<>();
        List<EnvironmentVariableConfig> envVarsToAdd = new ArrayList<>();
        List<String> envVarsToRemove = new ArrayList<>();

        service.patchEnvironment(service.getEnvironmentConfig(environmentName), pipelinesToAdd, pipelinesToRemove, agentsToAdd, agentsToremove, envVarsToAdd, envVarsToRemove, user, result);
        EnvironmentConfig updatedEnv = service.named(env.name().toString());

        assertThat(updatedEnv.name(), is(new CaseInsensitiveString(environmentName)));
        assertThat(updatedEnv.getAgents().getUuids(), is(Arrays.asList("uuid-1")));
        assertThat(result.message(), containsString("Updated environment 'env'."));
    }

    @Test
    public void shouldReturnTheCorrectLocalizedMessageWhenUserDoesNotHavePermissionToPatch() {
        configHelper.addEnvironments("foo");
        configHelper.enableSecurity();
        configHelper.addAdmins("super_hero");
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        service.patchEnvironment(service.getEnvironmentConfig("foo"), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new Username(new CaseInsensitiveString("evil_hacker")), result);
        assertThat(result.message(), is("Failed to access environment 'foo'. User 'evil_hacker' does not have permission to access environment."));
    }

    @Test
    public void shouldReturnAClonedInstanceOfEnvironmentConfig() {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        configHelper.addEnvironments("foo-env");
        assertThat(service.named("foo-env"), sameInstance(service.named("foo-env")));
        assertThat(service.named("foo-env"), not(sameInstance(service.getMergedEnvironmentforDisplay("foo-env", result).getConfigElement())));
        assertThat(result.isSuccessful(), is(true));
    }

    @Test
    public void shouldPopulateResultWithErrorIfEnvNotFound() {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        ConfigElementForEdit<EnvironmentConfig> edit = service.getMergedEnvironmentforDisplay("foo-env", result);
        assertThat(result.message(), is("Environment 'foo-env' not found."));
        assertThat(edit, is(nullValue()));
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
