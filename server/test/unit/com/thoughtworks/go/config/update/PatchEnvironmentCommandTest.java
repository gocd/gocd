/*
 * Copyright 2016 ThoughtWorks, Inc.
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
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.i18n.Localizable;
import com.thoughtworks.go.i18n.LocalizedMessage;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.EntityHashingService;
import com.thoughtworks.go.server.service.EnvironmentConfigService;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.serverhealth.HealthStateType;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.ArrayList;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class PatchEnvironmentCommandTest {
    private Username currentUser;
    private BasicCruiseConfig cruiseConfig;
    private BasicEnvironmentConfig environmentConfig;
    private CaseInsensitiveString environmentName;
    private HttpLocalizedOperationResult result;
    private Localizable.CurryableLocalizable actionFailed;

    private ArrayList<String> pipelinesToAdd;
    private ArrayList<String> pipelinesToRemove;
    private ArrayList<String> agentsToAdd;
    private ArrayList<String> agentsToRemove;

    private PipelineConfig pipelineConfig;
    private AgentConfig agentConfig;

    @Mock
    private EnvironmentConfigService environmentConfigService;

    @Mock
    private GoConfigService goConfigService;

    @Mock
    private EntityHashingService entityHashingService;

    @Before
    public void setup() throws Exception {
        initMocks(this);
        pipelinesToAdd = new ArrayList<>();
        pipelinesToRemove = new ArrayList<>();
        agentsToAdd = new ArrayList<>();
        agentsToRemove = new ArrayList<>();
        result = new HttpLocalizedOperationResult();

        currentUser = new Username(new CaseInsensitiveString("user"));
        cruiseConfig = new GoConfigMother().defaultCruiseConfig();

        environmentName = new CaseInsensitiveString("Dev");
        environmentConfig = new BasicEnvironmentConfig(environmentName);
        cruiseConfig.addEnvironment(environmentConfig);

        pipelineConfig = new PipelineConfig();
        String pipelineName = "pipeline-1";
        pipelineConfig.setName(pipelineName);
        cruiseConfig.addPipeline("First-Group", pipelineConfig);

        agentConfig = new AgentConfig("uuid-1");
        cruiseConfig.agents().add(agentConfig);

        actionFailed = LocalizedMessage.string("ENV_UPDATE_FAILED", environmentConfig.name());
    }

    @Test
    public void shouldAllowAddingAgentsToTheSpecifiedEnvironment() throws Exception {
        agentsToAdd.add(agentConfig.getUuid());
        PatchEnvironmentCommand command = new PatchEnvironmentCommand(goConfigService, environmentConfig, pipelinesToAdd, pipelinesToRemove, agentsToAdd, agentsToRemove, currentUser, actionFailed, result);
        assertFalse(cruiseConfig.getEnvironments().find(environmentName).hasAgent(agentConfig.getUuid()));
        command.update(cruiseConfig);
        assertTrue(cruiseConfig.getEnvironments().find(environmentName).hasAgent(agentConfig.getUuid()));
    }

    @Test
    public void shouldAllowRemovingAgentsFromTheSpecifiedEnvironment() throws Exception {
        environmentConfig.addAgent(agentConfig.getUuid());
        agentsToRemove.add(agentConfig.getUuid());
        PatchEnvironmentCommand command = new PatchEnvironmentCommand(goConfigService, environmentConfig, pipelinesToAdd, pipelinesToRemove, agentsToAdd, agentsToRemove, currentUser, actionFailed, result);
        assertTrue(cruiseConfig.getEnvironments().find(environmentName).hasAgent(agentConfig.getUuid()));
        command.update(cruiseConfig);
        assertFalse(cruiseConfig.getEnvironments().find(environmentName).hasAgent(agentConfig.getUuid()));
    }

    @Test
    public void shouldAllowAddingPipelinesToTheSpecifiedEnvironment() throws Exception {
        pipelinesToAdd.add(pipelineConfig.name().toString());
        PatchEnvironmentCommand command = new PatchEnvironmentCommand(goConfigService, environmentConfig, pipelinesToAdd, pipelinesToRemove, agentsToAdd, agentsToRemove, currentUser, actionFailed, result);
        assertFalse(cruiseConfig.getEnvironments().find(environmentName).contains(pipelineConfig.name().toString()));
        command.update(cruiseConfig);
        assertTrue(cruiseConfig.getEnvironments().find(environmentName).contains(pipelineConfig.name().toString()));
    }

    @Test
    public void shouldAllowremovingPipelinesToTheSpecifiedEnvironment() throws Exception {
        environmentConfig.addPipeline(pipelineConfig.name());
        pipelinesToRemove.add(pipelineConfig.name().toString());
        PatchEnvironmentCommand command = new PatchEnvironmentCommand(goConfigService, environmentConfig, pipelinesToAdd, pipelinesToRemove, agentsToAdd, agentsToRemove, currentUser, actionFailed, result);
        assertTrue(cruiseConfig.getEnvironments().find(environmentName).contains(pipelineConfig.name().toString()));
        command.update(cruiseConfig);
        assertFalse(cruiseConfig.getEnvironments().find(environmentName).contains(pipelineConfig.name().toString()));
    }

    @Test
    public void shouldValidateInvalidAgentUUIDs() throws Exception {
        String uuid = "invalid-agent-uuid";

        agentsToAdd.add(uuid);
        PatchEnvironmentCommand command = new PatchEnvironmentCommand(goConfigService, environmentConfig, pipelinesToAdd, pipelinesToRemove, agentsToAdd, agentsToRemove, currentUser, actionFailed, result);
        assertFalse(cruiseConfig.getEnvironments().find(environmentName).hasAgent(uuid));
        command.update(cruiseConfig);

        assertFalse(cruiseConfig.getEnvironments().find(environmentName).hasAgent(uuid));

        HttpLocalizedOperationResult expectResult = new HttpLocalizedOperationResult();
        expectResult.badRequest(LocalizedMessage.string("AGENTS_WITH_UUIDS_NOT_FOUND", agentsToAdd));

        assertFalse(result.isSuccessful());
        assertThat(result, is(expectResult));
    }

    @Test
    public void shouldValidateInvalidPipelineNames() throws Exception {
        String pipelineName = "invalid-pipeline-name";

        pipelinesToAdd.add(pipelineName);
        PatchEnvironmentCommand command = new PatchEnvironmentCommand(goConfigService, environmentConfig, pipelinesToAdd, pipelinesToRemove, agentsToAdd, agentsToRemove, currentUser, actionFailed, result);
        assertFalse(cruiseConfig.getEnvironments().find(environmentName).hasAgent(pipelineName));
        command.update(cruiseConfig);

        assertFalse(cruiseConfig.getEnvironments().find(environmentName).hasAgent(pipelineName));

        HttpLocalizedOperationResult expectResult = new HttpLocalizedOperationResult();
        expectResult.badRequest(LocalizedMessage.string("PIPELINES_WITH_NAMES_NOT_FOUND", pipelinesToAdd));

        assertFalse(result.isSuccessful());
        assertThat(result, is(expectResult));
    }

    @Test
    public void shouldNotContinueIfTheUserDontHavePermissionsToOperateOnEnvironments() throws Exception {
        PatchEnvironmentCommand command = new PatchEnvironmentCommand(goConfigService, environmentConfig, pipelinesToAdd, pipelinesToRemove, agentsToAdd, agentsToRemove, currentUser, actionFailed, result);
        when(goConfigService.isAdministrator(currentUser.getUsername())).thenReturn(false);
        assertThat(command.canContinue(cruiseConfig), is(false));
        HttpLocalizedOperationResult expectResult = new HttpLocalizedOperationResult();
        Localizable noPermission = LocalizedMessage.string("NO_PERMISSION_TO_UPDATE_ENVIRONMENT", environmentConfig.name().toString(), currentUser.getDisplayName());
        expectResult.unauthorized(noPermission, HealthStateType.unauthorised());

        assertThat(result, is(expectResult));
    }
}
