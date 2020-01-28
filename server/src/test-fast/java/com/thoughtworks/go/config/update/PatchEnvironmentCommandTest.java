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
package com.thoughtworks.go.config.update;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.merge.MergeEnvironmentConfig;
import com.thoughtworks.go.config.remote.ConfigRepoConfig;
import com.thoughtworks.go.config.remote.FileConfigOrigin;
import com.thoughtworks.go.config.remote.RepoConfigOrigin;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.ArrayList;

import static com.thoughtworks.go.helper.MaterialConfigsMother.git;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class PatchEnvironmentCommandTest {
    private Username currentUser;
    private BasicCruiseConfig cruiseConfig;
    private BasicEnvironmentConfig environmentConfig;
    private CaseInsensitiveString environmentName;
    private HttpLocalizedOperationResult result;
    private String actionFailed;

    private ArrayList<String> pipelinesToAdd;
    private ArrayList<String> pipelinesToRemove;
    private ArrayList<EnvironmentVariableConfig> envVarsToAdd;
    private ArrayList<String> envVarsToRemove;

    private PipelineConfig pipelineConfig;


    @Mock
    private GoConfigService goConfigService;

    @Before
    public void setup() throws Exception {
        initMocks(this);
        pipelinesToAdd = new ArrayList<>();
        pipelinesToRemove = new ArrayList<>();
        envVarsToAdd = new ArrayList<>();
        envVarsToRemove = new ArrayList<>();

        result = new HttpLocalizedOperationResult();

        currentUser = new Username(new CaseInsensitiveString("user"));
        cruiseConfig = GoConfigMother.defaultCruiseConfig();

        environmentName = new CaseInsensitiveString("Dev");
        environmentConfig = new BasicEnvironmentConfig(environmentName);
        cruiseConfig.addEnvironment(environmentConfig);

        pipelineConfig = new PipelineConfig();
        String pipelineName = "pipeline-1";
        pipelineConfig.setName(pipelineName);
        cruiseConfig.addPipeline("First-Group", pipelineConfig);

        actionFailed = "Failed to update environment '" + environmentConfig.name() + "'.";
    }

    @Test
    public void shouldAllowAddingPipelinesToTheSpecifiedEnvironment() throws Exception {
        pipelinesToAdd.add(pipelineConfig.name().toString());
        PatchEnvironmentCommand command = new PatchEnvironmentCommand(goConfigService, environmentConfig, pipelinesToAdd, pipelinesToRemove, envVarsToAdd, envVarsToRemove, currentUser, actionFailed, result);
        assertFalse(cruiseConfig.getEnvironments().find(environmentName).contains(pipelineConfig.name().toString()));
        command.update(cruiseConfig);
        assertTrue(cruiseConfig.getEnvironments().find(environmentName).contains(pipelineConfig.name().toString()));
    }

    @Test
    public void shouldAllowRemovingPipelinesFromTheSpecifiedEnvironment() throws Exception {
        environmentConfig.addPipeline(pipelineConfig.name());
        pipelinesToRemove.add(pipelineConfig.name().toString());
        PatchEnvironmentCommand command = new PatchEnvironmentCommand(goConfigService, environmentConfig, pipelinesToAdd, pipelinesToRemove, envVarsToAdd, envVarsToRemove, currentUser, actionFailed, result);
        assertTrue(cruiseConfig.getEnvironments().find(environmentName).contains(pipelineConfig.name().toString()));
        command.update(cruiseConfig);
        assertFalse(cruiseConfig.getEnvironments().find(environmentName).contains(pipelineConfig.name().toString()));
    }

    @Test
    public void shouldAllowAddingEnvironmentVariablesToTheSpecifiedEnvironment() throws Exception {
        String variableName = "foo";
        envVarsToAdd.add(new EnvironmentVariableConfig(variableName, "bar"));
        PatchEnvironmentCommand command = new PatchEnvironmentCommand(goConfigService, environmentConfig, pipelinesToAdd, pipelinesToRemove, envVarsToAdd, envVarsToRemove, currentUser, actionFailed, result);
        assertFalse(cruiseConfig.getEnvironments().find(environmentName).getVariables().hasVariable(variableName));
        command.update(cruiseConfig);
        assertTrue(cruiseConfig.getEnvironments().find(environmentName).getVariables().hasVariable(variableName));
    }

    @Test
    public void shouldAllowRemovingEnvironmentVariablesFromTheSpecifiedEnvironment() throws Exception {
        String variableName = "foo";
        environmentConfig.addEnvironmentVariable(variableName, "bar");
        envVarsToRemove.add(variableName);
        PatchEnvironmentCommand command = new PatchEnvironmentCommand(goConfigService, environmentConfig, pipelinesToAdd, pipelinesToRemove, envVarsToAdd, envVarsToRemove, currentUser, actionFailed, result);
        assertTrue(cruiseConfig.getEnvironments().find(environmentName).getVariables().hasVariable(variableName));
        command.update(cruiseConfig);
        assertFalse(cruiseConfig.getEnvironments().find(environmentName).getVariables().hasVariable(variableName));
    }

    @Test
    public void shouldValidateInvalidPipelineNames() throws Exception {
        String pipelineName = "invalid-pipeline-name";

        pipelinesToAdd.add(pipelineName);
        PatchEnvironmentCommand command = new PatchEnvironmentCommand(goConfigService, environmentConfig, pipelinesToAdd, pipelinesToRemove, envVarsToAdd, envVarsToRemove, currentUser, actionFailed, result);
        assertFalse(cruiseConfig.getEnvironments().find(environmentName).hasAgent(pipelineName));
        command.update(cruiseConfig);

        assertFalse(cruiseConfig.getEnvironments().find(environmentName).hasAgent(pipelineName));

        boolean isValid = command.isValid(cruiseConfig);
        assertFalse(isValid);

        HttpLocalizedOperationResult expectedResult = new HttpLocalizedOperationResult();
        expectedResult.unprocessableEntity(actionFailed + " Environment 'Dev' refers to an unknown pipeline 'invalid-pipeline-name'.");

        assertThat(result, is(expectedResult));
    }

    @Test
    public void shouldValidateInvalidPipelineRemoval() throws Exception {
        String pipelineName = "invalid-pipeline-to-remove";

        pipelinesToRemove.add(pipelineName);
        PatchEnvironmentCommand command = new PatchEnvironmentCommand(goConfigService, environmentConfig, pipelinesToAdd, pipelinesToRemove, envVarsToAdd, envVarsToRemove, currentUser, actionFailed, result);
        assertFalse(cruiseConfig.getEnvironments().find(environmentName).containsPipeline(new CaseInsensitiveString(pipelineName)));
        command.update(cruiseConfig);

        boolean isValid = command.isValid(cruiseConfig);
        assertFalse(isValid);

        HttpLocalizedOperationResult expectedResult = new HttpLocalizedOperationResult();
        expectedResult.unprocessableEntity(actionFailed + " Pipeline 'invalid-pipeline-to-remove' does not exist in environment 'Dev'");

        assertThat(result, is(expectedResult));
    }

    @Test
    public void shouldValidateInvalidEnvironmentVariableRemoval() throws Exception {
        String variableName = "invalid-env-var-to-remove";

        envVarsToRemove.add(variableName);
        PatchEnvironmentCommand command = new PatchEnvironmentCommand(goConfigService, environmentConfig, pipelinesToAdd, pipelinesToRemove, envVarsToAdd, envVarsToRemove, currentUser, actionFailed, result);
        assertFalse(cruiseConfig.getEnvironments().find(environmentName).getVariables().hasVariable(variableName));
        command.update(cruiseConfig);

        boolean isValid = command.isValid(cruiseConfig);
        assertFalse(isValid);

        HttpLocalizedOperationResult expectedResult = new HttpLocalizedOperationResult();
        expectedResult.unprocessableEntity(actionFailed + " Environment variable with name 'invalid-env-var-to-remove' does not exist in environment 'Dev'");

        assertThat(result, is(expectedResult));
    }

    @Test
    public void shouldNotAllowRemovingRemotePipeline() throws Exception {
        CaseInsensitiveString pipelineName = new CaseInsensitiveString("remote-pipeline-to-remove");

        BasicEnvironmentConfig local = new BasicEnvironmentConfig(environmentName);
        local.setOrigins(new FileConfigOrigin());
        BasicEnvironmentConfig remote = new BasicEnvironmentConfig(environmentName);
        remote.addPipeline(pipelineName);
        ConfigRepoConfig configRepo = ConfigRepoConfig.createConfigRepoConfig(git("foo/bar.git", "master"), "myPlugin");
        remote.setOrigins(new RepoConfigOrigin(configRepo, "latest"));

        MergeEnvironmentConfig mergedConfig = new MergeEnvironmentConfig(local, remote);

        pipelinesToRemove.add(pipelineName.toString());
        PatchEnvironmentCommand command = new PatchEnvironmentCommand(goConfigService, environmentConfig, pipelinesToAdd, pipelinesToRemove, envVarsToAdd, envVarsToRemove, currentUser, actionFailed, result);
        assertFalse(cruiseConfig.getEnvironments().find(environmentName).containsPipeline(new CaseInsensitiveString(pipelineName.toString())));
        command.update(cruiseConfig);

        cruiseConfig.getEnvironments().replace(cruiseConfig.getEnvironments().find(environmentName), mergedConfig); //preprocess

        boolean isValid = command.isValid(cruiseConfig);
        assertFalse(isValid);

        HttpLocalizedOperationResult expectedResult = new HttpLocalizedOperationResult();
        String message = actionFailed + " Pipeline 'remote-pipeline-to-remove' cannot be removed from environment 'Dev' as the association has been defined remotely in [foo/bar.git at latest]";
        expectedResult.unprocessableEntity(message);

        assertThat(result, is(expectedResult));
    }

    @Test
    public void shouldNotAllowRemovingRemoteEnvironmentVariables() throws Exception {
        String variableName = "remote-env-var-to-remove";

        BasicEnvironmentConfig local = new BasicEnvironmentConfig(environmentName);
        local.setOrigins(new FileConfigOrigin());
        BasicEnvironmentConfig remote = new BasicEnvironmentConfig(environmentName);
        remote.addEnvironmentVariable(variableName, "bar");
        ConfigRepoConfig configRepo = ConfigRepoConfig.createConfigRepoConfig(git("foo/bar.git", "master"), "myPlugin");
        remote.setOrigins(new RepoConfigOrigin(configRepo, "latest"));

        MergeEnvironmentConfig mergedConfig = new MergeEnvironmentConfig(local, remote);

        envVarsToRemove.add(variableName);
        PatchEnvironmentCommand command = new PatchEnvironmentCommand(goConfigService, environmentConfig, pipelinesToAdd, pipelinesToRemove, envVarsToAdd, envVarsToRemove, currentUser, actionFailed, result);
        assertFalse(cruiseConfig.getEnvironments().find(environmentName).getVariables().hasVariable(variableName));
        command.update(cruiseConfig);

        cruiseConfig.getEnvironments().replace(cruiseConfig.getEnvironments().find(environmentName), mergedConfig); //preprocess

        boolean isValid = command.isValid(cruiseConfig);
        assertFalse(isValid);

        HttpLocalizedOperationResult expectedResult = new HttpLocalizedOperationResult();
        String message = actionFailed + " Environment variable with name 'remote-env-var-to-remove' cannot be removed from environment 'Dev' as the association has been defined remotely in [foo/bar.git at latest]";
        expectedResult.unprocessableEntity(message);

        assertThat(result, is(expectedResult));
    }
}
