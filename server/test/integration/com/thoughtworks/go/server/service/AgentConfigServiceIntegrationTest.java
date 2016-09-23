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

package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.domain.AgentInstance;
import com.thoughtworks.go.helper.AgentMother;
import com.thoughtworks.go.i18n.LocalizedMessage;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.util.GoConfigFileHelper;
import com.thoughtworks.go.util.ReflectionUtil;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.TriState;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:WEB-INF/applicationContext-acegi-security.xml"
})
public class AgentConfigServiceIntegrationTest {
    @Autowired private AgentConfigService agentConfigService;
    @Autowired private GoConfigDao goConfigDao;
    @Autowired private GoConfigService goConfigService;
    private GoConfigFileHelper configHelper;

    @Before
    public void setup() throws Exception {
        configHelper = new GoConfigFileHelper();
        configHelper.usingCruiseConfigDao(goConfigDao).initializeConfigFile();
        configHelper.onSetUp();
        goConfigService.forceNotifyListeners();
    }

    @After
    public void tearDown() throws Exception {
        configHelper.onTearDown();
    }

    @Test
    public void shouldAddAgentToConfigFile() throws Exception {
        Resources resources = new Resources("java");
        AgentConfig approvedAgentConfig = new AgentConfig(UUID.randomUUID().toString(), "test1", "192.168.0.1", resources);
        AgentConfig deniedAgentConfig = new AgentConfig(UUID.randomUUID().toString(), "test2", "192.168.0.2", resources);
        deniedAgentConfig.disable();
        agentConfigService.addAgent(approvedAgentConfig, Username.ANONYMOUS);
        agentConfigService.addAgent(deniedAgentConfig, Username.ANONYMOUS);
        CruiseConfig cruiseConfig = goConfigDao.load();
        assertThat(cruiseConfig.agents().contains(approvedAgentConfig), is(true));
        assertThat(cruiseConfig.agents().getAgentByUuid(approvedAgentConfig.getUuid()).getResources(), is(resources));
        assertThat(cruiseConfig.agents().contains(deniedAgentConfig), is(true));
        assertThat(cruiseConfig.agents().getAgentByUuid(deniedAgentConfig.getUuid()).isDisabled(), is(Boolean.TRUE));
        assertThat(cruiseConfig.agents().getAgentByUuid(deniedAgentConfig.getUuid()).getResources(), is(resources));
    }

    @Test
    public void shouldDeleteMultipleAgents() {
        AgentConfig agentConfig1 = new AgentConfig("UUID1", "remote-host1", "50.40.30.21");
        AgentConfig agentConfig2 = new AgentConfig("UUID2", "remote-host2", "50.40.30.22");
        agentConfig1.disable();
        agentConfig2.disable();
        AgentInstance fromConfigFile1 = AgentInstance.createFromConfig(agentConfig1, new SystemEnvironment());
        AgentInstance fromConfigFile2 = AgentInstance.createFromConfig(agentConfig2, new SystemEnvironment());

        GoConfigDao.CompositeConfigCommand command = agentConfigService.commandForDeletingAgents(fromConfigFile1, fromConfigFile2);

        List<UpdateConfigCommand> commands = command.getCommands();
        assertThat(commands.size(), is(2));
        String uuid1 = (String) ReflectionUtil.getField(commands.get(0), "uuid");
        String uuid2 = (String) ReflectionUtil.getField(commands.get(1), "uuid");
        assertThat(uuid1, is("UUID1"));
        assertThat(uuid2, is("UUID2"));
    }

    @Test
    public void shouldDeleteAgentFromConfigFileGivenUUID() throws Exception {
        AgentConfig agentConfig1 = new AgentConfig(UUID.randomUUID().toString(), "test1", "192.168.0.1");
        AgentConfig agentConfig2 = new AgentConfig(UUID.randomUUID().toString(), "test2", "192.168.0.2");
        AgentInstance fromConfigFile1 = AgentInstance.createFromConfig(agentConfig1, new SystemEnvironment());

        agentConfigService.addAgent(agentConfig1, Username.ANONYMOUS);
        agentConfigService.addAgent(agentConfig2, Username.ANONYMOUS);

        agentConfigService.deleteAgents(Username.ANONYMOUS, fromConfigFile1);

        CruiseConfig cruiseConfig = goConfigDao.load();
        assertThat(cruiseConfig.agents().hasAgent(agentConfig1.getUuid()), is(false));
        assertThat(cruiseConfig.agents().hasAgent(agentConfig2.getUuid()), is(true));
    }

    @Test
    public void shouldRemoveAgentFromEnvironmentBeforeDeletingAgent() throws Exception {
        AgentConfig agentConfig1 = new AgentConfig(UUID.randomUUID().toString(), "hostname", "127.0.0.1");
        AgentInstance fromConfigFile1 = AgentInstance.createFromConfig(agentConfig1, new SystemEnvironment());

        agentConfigService.addAgent(agentConfig1, Username.ANONYMOUS);
        AgentConfig agentConfig2 = new AgentConfig(UUID.randomUUID().toString(), "hostname", "127.0.0.1");
        agentConfigService.addAgent(agentConfig2, Username.ANONYMOUS);

        BasicEnvironmentConfig env = new BasicEnvironmentConfig(new CaseInsensitiveString(UUID.randomUUID().toString()));
        env.addAgent(agentConfig1.getUuid());
        env.addAgent(agentConfig2.getUuid());
        goConfigDao.addEnvironment(env);
        CruiseConfig cruiseConfig = goConfigDao.load();

        assertThat(cruiseConfig.getEnvironments().named(env.name()).getAgents().size(), is(2));

        agentConfigService.deleteAgents(Username.ANONYMOUS, fromConfigFile1);

        cruiseConfig = goConfigDao.load();

        assertThat(cruiseConfig.getEnvironments().named(env.name()).getAgents().size(), is(1));
        assertThat(cruiseConfig.getEnvironments().named(env.name()).getAgents().get(0).getUuid(), is(agentConfig2.getUuid()));
    }

    @Test
    public void shouldUpdateAgentResourcesToConfigFile() throws Exception {
        AgentConfig agentConfig = new AgentConfig("uuid", "test", "127.0.0.1", new Resources("java"));
        agentConfigService.addAgent(agentConfig, Username.ANONYMOUS);
        Resources newResources = new Resources("firefox");
        agentConfigService.updateAgentResources(agentConfig.getUuid(), newResources);
        CruiseConfig cruiseConfig = goConfigDao.load();
        assertThat(cruiseConfig.agents().get(0).getResources(), is(newResources));
    }

    @Test
    public void shouldUpdateAgentApprovalStatusByUuidToConfigFile() throws Exception {
        AgentConfig agentConfig = new AgentConfig("uuid", "test", "127.0.0.1", new Resources("java"));
        agentConfigService.addAgent(agentConfig, Username.ANONYMOUS);
        agentConfigService.updateAgentApprovalStatus(agentConfig.getUuid(), Boolean.TRUE, Username.ANONYMOUS);

        CruiseConfig cruiseConfig = goConfigDao.load();
        assertThat(cruiseConfig.agents().get(0).isDisabled(), is(true));
    }

    @Test
    public void shouldRemoveAgentResourcesInConfigFile() throws Exception {
        AgentConfig agentConfig = new AgentConfig(UUID.randomUUID().toString(), "test", "127.0.0.1", new Resources("java, resource1, resource2"));
        agentConfigService.addAgent(agentConfig, Username.ANONYMOUS);
        CruiseConfig cruiseConfig = goConfigDao.load();
        assertThat(cruiseConfig.agents().hasAgent(agentConfig.getUuid()), is(true));
        agentConfigService.updateAgentResources(agentConfig.getUuid(), new Resources("java"));
        cruiseConfig = goConfigDao.load();
        assertThat(cruiseConfig.agents().getAgentByUuid(agentConfig.getUuid()).getResources().size(), is(1));
        assertThat(cruiseConfig.agents().getAgentByUuid(agentConfig.getUuid()).getResources().first(), is(new Resource("java")));
    }

    @Test
    public void shouldEnableMultipleAgents() {
        AgentConfig agentConfig1 = new AgentConfig(UUID.randomUUID().toString(), "remote-host1", "50.40.30.21");
        AgentConfig agentConfig2 = new AgentConfig(UUID.randomUUID().toString(), "remote-host2", "50.40.30.22");
        agentConfig1.disable();
        agentConfig2.disable();
        AgentInstance fromConfigFile1 = AgentInstance.createFromConfig(agentConfig1, new SystemEnvironment());
        AgentInstance fromConfigFile2 = AgentInstance.createFromConfig(agentConfig2, new SystemEnvironment());

        agentConfigService.addAgent(agentConfig1, Username.ANONYMOUS);
        agentConfigService.addAgent(agentConfig2, Username.ANONYMOUS);

        CruiseConfig cruiseConfig = goConfigDao.load();
        assertThat(cruiseConfig.agents().getAgentByUuid(agentConfig1.getUuid()).isDisabled(), is(true));
        assertThat(cruiseConfig.agents().getAgentByUuid(agentConfig1.getUuid()).isDisabled(), is(true));

        agentConfigService.enableAgents(Username.ANONYMOUS, fromConfigFile1, fromConfigFile2);

        cruiseConfig = goConfigDao.load();
        assertThat(cruiseConfig.agents().getAgentByUuid(agentConfig1.getUuid()).isDisabled(), is(false));
        assertThat(cruiseConfig.agents().getAgentByUuid(agentConfig2.getUuid()).isDisabled(), is(false));
    }

    @Test
    public void shouldEnableTheProvidedAgents() throws Exception {
        AgentConfig agentConfig1 = new AgentConfig(UUID.randomUUID().toString(), "remote-host1", "50.40.30.21");
        AgentConfig agentConfig2 = new AgentConfig(UUID.randomUUID().toString(), "remote-host2", "50.40.30.22");
        agentConfig1.disable();
        agentConfig2.disable();
        agentConfigService.addAgent(agentConfig1, Username.ANONYMOUS);
        agentConfigService.addAgent(agentConfig2, Username.ANONYMOUS);
        CruiseConfig cruiseConfig = goConfigDao.load();
        assertThat(cruiseConfig.agents().getAgentByUuid(agentConfig1.getUuid()).isDisabled(), is(true));
        assertThat(cruiseConfig.agents().getAgentByUuid(agentConfig2.getUuid()).isDisabled(), is(true));

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        ArrayList<String> uuids = new ArrayList<>();
        uuids.add(agentConfig1.getUuid());
        uuids.add(agentConfig2.getUuid());

        agentConfigService.bulkUpdateAgentAttributes(Username.ANONYMOUS, result, uuids, new ArrayList<String>(), new ArrayList<String>(), new ArrayList<String>(), new ArrayList<String>(), TriState.TRUE);

        cruiseConfig = goConfigDao.load();
        assertTrue(result.isSuccessful());
        assertTrue(result.toString(), result.toString().contains("BULK_AGENT_UPDATE_SUCESSFUL"));
        assertFalse(cruiseConfig.agents().getAgentByUuid(agentConfig1.getUuid()).isDisabled());
        assertFalse(cruiseConfig.agents().getAgentByUuid(agentConfig2.getUuid()).isDisabled());
    }

    @Test
    public void shouldDisableTheProvidedAgents() throws Exception {
        AgentConfig agentConfig1 = new AgentConfig(UUID.randomUUID().toString(), "remote-host1", "50.40.30.21");
        AgentConfig agentConfig2 = new AgentConfig(UUID.randomUUID().toString(), "remote-host2", "50.40.30.22");
        agentConfigService.addAgent(agentConfig1, Username.ANONYMOUS);
        agentConfigService.addAgent(agentConfig2, Username.ANONYMOUS);
        CruiseConfig cruiseConfig = goConfigDao.load();
        assertThat(cruiseConfig.agents().getAgentByUuid(agentConfig1.getUuid()).isDisabled(), is(false));
        assertThat(cruiseConfig.agents().getAgentByUuid(agentConfig2.getUuid()).isDisabled(), is(false));

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        ArrayList<String> uuids = new ArrayList<>();
        uuids.add(agentConfig1.getUuid());
        uuids.add(agentConfig2.getUuid());

        agentConfigService.bulkUpdateAgentAttributes(Username.ANONYMOUS, result, uuids, new ArrayList<String>(), new ArrayList<String>(), new ArrayList<String>(), new ArrayList<String>(), TriState.FALSE);

        cruiseConfig = goConfigDao.load();
        assertTrue(result.isSuccessful());
        assertTrue(result.toString(), result.toString().contains("BULK_AGENT_UPDATE_SUCESSFUL"));
        assertTrue(cruiseConfig.agents().getAgentByUuid(agentConfig1.getUuid()).isDisabled());
        assertTrue(cruiseConfig.agents().getAgentByUuid(agentConfig2.getUuid()).isDisabled());
    }

    @Test
    public void shouldNotDisableOrEnableIfTheStateOfAgentIsNotChanged() throws Exception {
        AgentConfig agentConfig1 = new AgentConfig(UUID.randomUUID().toString(), "remote-host1", "50.40.30.21");
        AgentConfig agentConfig2 = new AgentConfig(UUID.randomUUID().toString(), "remote-host2", "50.40.30.22");
        agentConfig1.enable();
        agentConfig2.disable();
        agentConfigService.addAgent(agentConfig1, Username.ANONYMOUS);
        agentConfigService.addAgent(agentConfig2, Username.ANONYMOUS);
        CruiseConfig cruiseConfig = goConfigDao.load();
        assertThat(cruiseConfig.agents().getAgentByUuid(agentConfig1.getUuid()).isDisabled(), is(false));
        assertThat(cruiseConfig.agents().getAgentByUuid(agentConfig2.getUuid()).isDisabled(), is(true));

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        ArrayList<String> uuids = new ArrayList<>();
        uuids.add(agentConfig1.getUuid());
        uuids.add(agentConfig2.getUuid());

        agentConfigService.bulkUpdateAgentAttributes(Username.ANONYMOUS, result, uuids, new ArrayList<String>(), new ArrayList<String>(), new ArrayList<String>(), new ArrayList<String>(), TriState.UNSET);

        cruiseConfig = goConfigDao.load();
        assertTrue(result.isSuccessful());
        assertTrue(result.toString(), result.toString().contains("BULK_AGENT_UPDATE_SUCESSFUL"));
        assertFalse(cruiseConfig.agents().getAgentByUuid(agentConfig1.getUuid()).isDisabled());
        assertTrue(cruiseConfig.agents().getAgentByUuid(agentConfig2.getUuid()).isDisabled());
    }

    @Test
    public void shouldNotDisableAgentsWhenInvalidAgentUUIDIsprovided() throws Exception {
        AgentConfig agentConfig1 = new AgentConfig(UUID.randomUUID().toString(), "remote-host1", "50.40.30.21");
        AgentConfig agentConfig2 = new AgentConfig(UUID.randomUUID().toString(), "remote-host2", "50.40.30.22");
        agentConfigService.addAgent(agentConfig1, Username.ANONYMOUS);
        agentConfigService.addAgent(agentConfig2, Username.ANONYMOUS);
        CruiseConfig cruiseConfig = goConfigDao.load();
        assertFalse(cruiseConfig.agents().getAgentByUuid(agentConfig1.getUuid()).isDisabled());
        assertFalse(cruiseConfig.agents().getAgentByUuid(agentConfig1.getUuid()).isDisabled());

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        ArrayList<String> uuids = new ArrayList<>();
        uuids.add(agentConfig1.getUuid());
        uuids.add(agentConfig2.getUuid());
        uuids.add("invalid-uuid");

        agentConfigService.bulkUpdateAgentAttributes(Username.ANONYMOUS, result, uuids, new ArrayList<String>(), new ArrayList<String>(), new ArrayList<String>(), new ArrayList<String>(), TriState.FALSE);

        cruiseConfig = goConfigDao.load();
        assertFalse(cruiseConfig.agents().getAgentByUuid(agentConfig1.getUuid()).isDisabled());
        assertFalse(cruiseConfig.agents().getAgentByUuid(agentConfig2.getUuid()).isDisabled());

        assertFalse(result.isSuccessful());
        assertThat(result.toString(), result.httpCode(), is(400));
        assertTrue(result.toString(), result.toString().contains("AGENTS_WITH_UUIDS_NOT_FOUND"));
        assertTrue(result.toString(), result.toString().contains("invalid-uuid"));
    }

    @Test
    public void shouldNotUpdateResourcesOnElasticAgents() throws Exception {
        AgentConfig elasticAgent = AgentMother.elasticAgent();
        agentConfigService.addAgent(elasticAgent, Username.ANONYMOUS);
        CruiseConfig cruiseConfig = goConfigDao.load();

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        List<String> uuids = Arrays.asList(elasticAgent.getUuid());
        List<String> resourcesToAdd = Arrays.asList("resource");

        assertTrue(cruiseConfig.agents().getAgentByUuid(elasticAgent.getUuid()).getResources().isEmpty());
        agentConfigService.bulkUpdateAgentAttributes(Username.ANONYMOUS, result, uuids, resourcesToAdd, new ArrayList<String>(), new ArrayList<String>(), new ArrayList<String>(), TriState.FALSE);
        cruiseConfig = goConfigDao.load();

        HttpLocalizedOperationResult expectedResult = new HttpLocalizedOperationResult();
        expectedResult.badRequest(LocalizedMessage.string("CAN_NOT_UPDATE_RESOURCES_ON_ELASTIC_AGENT", uuids));

        assertThat(result, is(expectedResult));
        assertTrue(cruiseConfig.agents().getAgentByUuid(elasticAgent.getUuid()).getResources().isEmpty());
    }

    @Test
    public void shouldNotEnableAgentsWhenInvalidAgentUUIDIsprovided() throws Exception {
        AgentConfig agentConfig1 = new AgentConfig(UUID.randomUUID().toString(), "remote-host1", "50.40.30.21");
        AgentConfig agentConfig2 = new AgentConfig(UUID.randomUUID().toString(), "remote-host2", "50.40.30.22");
        agentConfigService.addAgent(agentConfig1, Username.ANONYMOUS);
        agentConfigService.addAgent(agentConfig2, Username.ANONYMOUS);
        CruiseConfig cruiseConfig = goConfigDao.load();
        assertFalse(cruiseConfig.agents().getAgentByUuid(agentConfig1.getUuid()).isDisabled());
        assertFalse(cruiseConfig.agents().getAgentByUuid(agentConfig2.getUuid()).isDisabled());

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        ArrayList<String> uuids = new ArrayList<>();
        uuids.add(agentConfig1.getUuid());
        uuids.add(agentConfig2.getUuid());
        uuids.add("invalid-uuid");

        agentConfigService.bulkUpdateAgentAttributes(Username.ANONYMOUS, result, uuids, new ArrayList<String>(), new ArrayList<String>(), new ArrayList<String>(), new ArrayList<String>(), TriState.TRUE);

        cruiseConfig = goConfigDao.load();
        assertThat(cruiseConfig.agents().getAgentByUuid(agentConfig1.getUuid()).isDisabled(), is(false));
        assertThat(cruiseConfig.agents().getAgentByUuid(agentConfig2.getUuid()).isDisabled(), is(false));

        assertFalse(result.isSuccessful());
        assertThat(result.toString(), result.httpCode(), is(400));
        assertTrue(result.toString(), result.toString().contains("AGENTS_WITH_UUIDS_NOT_FOUND"));
        assertTrue(result.toString(), result.toString().contains("invalid-uuid"));
    }

    @Test
    public void shouldAddResourcestoTheSpecifiedAgents() throws Exception {
        AgentConfig agentConfig1 = new AgentConfig(UUID.randomUUID().toString(), "remote-host1", "50.40.30.21");
        AgentConfig agentConfig2 = new AgentConfig(UUID.randomUUID().toString(), "remote-host2", "50.40.30.22");
        agentConfigService.addAgent(agentConfig1, Username.ANONYMOUS);
        agentConfigService.addAgent(agentConfig2, Username.ANONYMOUS);
        CruiseConfig cruiseConfig = goConfigDao.load();

        assertThat(cruiseConfig.agents().getAgentByUuid(agentConfig1.getUuid()).getResources().size(), is(0));
        assertThat(cruiseConfig.agents().getAgentByUuid(agentConfig2.getUuid()).getResources().size(), is(0));

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        ArrayList<String> uuids = new ArrayList<>();
        uuids.add(agentConfig1.getUuid());
        uuids.add(agentConfig2.getUuid());

        ArrayList<String> resources = new ArrayList<>();
        resources.add("resource1");
        resources.add("resource2");

        agentConfigService.bulkUpdateAgentAttributes(Username.ANONYMOUS, result, uuids, resources, new ArrayList<String>(), new ArrayList<String>(), new ArrayList<String>(), TriState.FALSE);

        cruiseConfig = goConfigDao.load();

        assertTrue(result.isSuccessful());
        assertThat(result.toString(), containsString("BULK_AGENT_UPDATE_SUCESSFUL"));
        assertThat(cruiseConfig.agents().getAgentByUuid(agentConfig1.getUuid()).getResources().size(), is(2));
        assertThat(cruiseConfig.agents().getAgentByUuid(agentConfig1.getUuid()).getResources(), containsInAnyOrder(new Resource("resource1"), new Resource("resource2")));
    }

    @Test
    public void shouldRemoveResourcesFromTheSpecifiedAgents() throws Exception {
        AgentConfig agentConfig1 = new AgentConfig(UUID.randomUUID().toString(), "remote-host1", "50.40.30.21");
        AgentConfig agentConfig2 = new AgentConfig(UUID.randomUUID().toString(), "remote-host2", "50.40.30.22");
        agentConfig1.addResource(new Resource("resource-1"));
        agentConfig1.addResource(new Resource("resource-2"));
        agentConfig2.addResource(new Resource("resource-2"));
        agentConfigService.addAgent(agentConfig1, Username.ANONYMOUS);
        agentConfigService.addAgent(agentConfig2, Username.ANONYMOUS);
        CruiseConfig cruiseConfig = goConfigDao.load();

        assertThat(cruiseConfig.agents().getAgentByUuid(agentConfig1.getUuid()).getResources().size(), is(2));
        assertThat(cruiseConfig.agents().getAgentByUuid(agentConfig2.getUuid()).getResources().size(), is(1));

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        ArrayList<String> uuids = new ArrayList<>();
        uuids.add(agentConfig1.getUuid());
        uuids.add(agentConfig2.getUuid());

        ArrayList<String> resources = new ArrayList<>();
        resources.add("resource-2");

        agentConfigService.bulkUpdateAgentAttributes(Username.ANONYMOUS, result, uuids, new ArrayList<String>(), resources, new ArrayList<String>(), new ArrayList<String>(), TriState.FALSE);

        cruiseConfig = goConfigDao.load();

        assertTrue(result.isSuccessful());
        assertThat(result.toString(), containsString("BULK_AGENT_UPDATE_SUCESSFUL"));
        assertThat(cruiseConfig.agents().getAgentByUuid(agentConfig1.getUuid()).getResources().size(), is(1));
        assertThat(cruiseConfig.agents().getAgentByUuid(agentConfig1.getUuid()).getResources(), contains(new Resource("resource-1")));
        assertThat(cruiseConfig.agents().getAgentByUuid(agentConfig2.getUuid()).getResources().size(), is(0));
    }

    @Test
    public void shouldAddProvidedAgentsToTheSpecifiedEnvironments() throws Exception {
        AgentConfig agentConfig1 = new AgentConfig(UUID.randomUUID().toString(), "remote-host1", "50.40.30.21");
        AgentConfig agentConfig2 = new AgentConfig(UUID.randomUUID().toString(), "remote-host2", "50.40.30.22");
        agentConfigService.addAgent(agentConfig1, Username.ANONYMOUS);
        agentConfigService.addAgent(agentConfig2, Username.ANONYMOUS);

        BasicEnvironmentConfig environment = new BasicEnvironmentConfig(new CaseInsensitiveString("Dev"));
        goConfigDao.addEnvironment(environment);

        assertFalse(environment.hasAgent(agentConfig1.getUuid()));
        assertFalse(environment.hasAgent(agentConfig2.getUuid()));

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        ArrayList<String> uuids = new ArrayList<>();
        uuids.add(agentConfig1.getUuid());
        uuids.add(agentConfig2.getUuid());

        ArrayList<String> environmentsToAdd = new ArrayList<>();
        environmentsToAdd.add("Dev");

        agentConfigService.bulkUpdateAgentAttributes(Username.ANONYMOUS, result, uuids, new ArrayList<String>(), new ArrayList<String>(), environmentsToAdd, new ArrayList<String>(), TriState.TRUE);

        assertTrue(result.isSuccessful());
        assertThat(result.toString(), containsString("BULK_AGENT_UPDATE_SUCESSFUL"));
        assertThat(goConfigDao.load().getEnvironments().find(new CaseInsensitiveString("Dev")).getAgents().getUuids(), containsInAnyOrder(agentConfig1.getUuid(), agentConfig2.getUuid()));
    }

    @Test
    public void shouldRemoveProvidedAgentsFromTheSpecifiedEnvironments() throws Exception {
        AgentConfig agentConfig1 = new AgentConfig(UUID.randomUUID().toString(), "remote-host1", "50.40.30.21");
        AgentConfig agentConfig2 = new AgentConfig(UUID.randomUUID().toString(), "remote-host2", "50.40.30.22");
        agentConfigService.addAgent(agentConfig1, Username.ANONYMOUS);
        agentConfigService.addAgent(agentConfig2, Username.ANONYMOUS);

        BasicEnvironmentConfig devEnvironment = new BasicEnvironmentConfig(new CaseInsensitiveString("Dev"));
        BasicEnvironmentConfig testEnvironment = new BasicEnvironmentConfig(new CaseInsensitiveString("Test"));
        goConfigDao.addEnvironment(devEnvironment);
        goConfigDao.addEnvironment(testEnvironment);

        testEnvironment.addAgent(agentConfig1.getUuid());
        devEnvironment.addAgent(agentConfig1.getUuid());
        devEnvironment.addAgent(agentConfig2.getUuid());

        assertThat(devEnvironment.getAgents().getUuids(), containsInAnyOrder(agentConfig1.getUuid(), agentConfig2.getUuid()));

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        ArrayList<String> uuids = new ArrayList<>();
        uuids.add(agentConfig1.getUuid());
        uuids.add(agentConfig2.getUuid());

        ArrayList<String> environmentsToRemove = new ArrayList<>();
        environmentsToRemove.add("Dev");

        agentConfigService.bulkUpdateAgentAttributes(Username.ANONYMOUS, result, uuids, new ArrayList<String>(), new ArrayList<String>(), new ArrayList<String>(), environmentsToRemove, TriState.TRUE);

        assertTrue(result.isSuccessful());
        assertThat(result.toString(), containsString("BULK_AGENT_UPDATE_SUCESSFUL"));
        assertFalse(goConfigDao.load().getEnvironments().find(new CaseInsensitiveString("Dev")).hasAgent(agentConfig1.getUuid()));
        assertFalse(goConfigDao.load().getEnvironments().find(new CaseInsensitiveString("Dev")).hasAgent(agentConfig2.getUuid()));
    }

    @Test
    public void shouldNotAddAgentToNonExistingEnvironment() throws Exception {
        AgentConfig agentConfig1 = new AgentConfig(UUID.randomUUID().toString(), "remote-host1", "50.40.30.21");
        AgentConfig agentConfig2 = new AgentConfig(UUID.randomUUID().toString(), "remote-host2", "50.40.30.22");
        agentConfigService.addAgent(agentConfig1, Username.ANONYMOUS);
        agentConfigService.addAgent(agentConfig2, Username.ANONYMOUS);


        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        ArrayList<String> uuids = new ArrayList<>();
        uuids.add(agentConfig1.getUuid());
        uuids.add(agentConfig2.getUuid());

        ArrayList<String> environmentsToAdd = new ArrayList<>();
        environmentsToAdd.add("Non-Existing-Environment");

        agentConfigService.bulkUpdateAgentAttributes(Username.ANONYMOUS, result, uuids, new ArrayList<String>(), new ArrayList<String>(), environmentsToAdd, new ArrayList<String>(), TriState.TRUE);

        assertFalse(result.isSuccessful());
        assertThat(result.toString(), result.httpCode(), is(400));
        assertThat(result.toString(), containsString("ENV_NOT_FOUND"));
        assertThat(result.toString(), containsString("Non-Existing-Environment"));
    }

    @Test
    public void shouldNotRemoveAgentFromNonExistingEnvironment() throws Exception {
        AgentConfig agentConfig1 = new AgentConfig(UUID.randomUUID().toString(), "remote-host1", "50.40.30.21");
        AgentConfig agentConfig2 = new AgentConfig(UUID.randomUUID().toString(), "remote-host2", "50.40.30.22");
        agentConfigService.addAgent(agentConfig1, Username.ANONYMOUS);
        agentConfigService.addAgent(agentConfig2, Username.ANONYMOUS);


        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        ArrayList<String> uuids = new ArrayList<>();
        uuids.add(agentConfig1.getUuid());
        uuids.add(agentConfig2.getUuid());

        ArrayList<String> environmentsToRemove = new ArrayList<>();
        environmentsToRemove.add("NonExistingEnvironment");

        agentConfigService.bulkUpdateAgentAttributes(Username.ANONYMOUS, result, uuids, new ArrayList<String>(), new ArrayList<String>(), new ArrayList<String>(), environmentsToRemove, TriState.TRUE);

        assertFalse(result.isSuccessful());
        assertThat(result.toString(), result.httpCode(), is(400));
        assertThat(result.toString(), containsString("ENV_NOT_FOUND"));
        assertThat(result.toString(), containsString("NonExistingEnvironment"));
    }

    @Test
    public void shouldUpdateResourcesEnvironmentsAndAgentStateOfTheProvidedStatesAllTogether() throws Exception {
        AgentConfig agentConfig1 = new AgentConfig(UUID.randomUUID().toString(), "remote-host1", "50.40.30.21");
        AgentConfig agentConfig2 = new AgentConfig(UUID.randomUUID().toString(), "remote-host2", "50.40.30.22");
        agentConfigService.addAgent(agentConfig1, Username.ANONYMOUS);
        agentConfigService.addAgent(agentConfig2, Username.ANONYMOUS);

        CruiseConfig cruiseConfig = goConfigDao.load();
        BasicEnvironmentConfig environment = new BasicEnvironmentConfig(new CaseInsensitiveString("Dev"));
        goConfigDao.addEnvironment(environment);

        assertThat(environment.getAgents().getUuids(), not(containsInAnyOrder(agentConfig1.getUuid(), agentConfig2.getUuid())));
        assertFalse(cruiseConfig.agents().getAgentByUuid(agentConfig1.getUuid()).isDisabled());
        assertFalse(cruiseConfig.agents().getAgentByUuid(agentConfig1.getUuid()).isDisabled());
        assertThat(cruiseConfig.agents().getAgentByUuid(agentConfig1.getUuid()).getResources().size(), is(0));
        assertThat(cruiseConfig.agents().getAgentByUuid(agentConfig2.getUuid()).getResources().size(), is(0));

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        ArrayList<String> uuids = new ArrayList<>();
        uuids.add(agentConfig1.getUuid());
        uuids.add(agentConfig2.getUuid());
        ArrayList<String> resources = new ArrayList<>();
        resources.add("resource1");
        ArrayList<String> environmentsToAdd = new ArrayList<>();
        environmentsToAdd.add("Dev");

        agentConfigService.bulkUpdateAgentAttributes(Username.ANONYMOUS, result, uuids, resources, new ArrayList<String>(), environmentsToAdd, new ArrayList<String>(), TriState.FALSE);

        cruiseConfig = goConfigDao.load();
        assertTrue(result.isSuccessful());
        assertThat(result.toString(), containsString("BULK_AGENT_UPDATE_SUCESSFUL"));
        assertTrue(cruiseConfig.agents().getAgentByUuid(agentConfig1.getUuid()).isDisabled());
        assertTrue(cruiseConfig.agents().getAgentByUuid(agentConfig2.getUuid()).isDisabled());
        assertThat(cruiseConfig.agents().getAgentByUuid(agentConfig1.getUuid()).getResources(), contains(new Resource("resource1")));
        assertThat(cruiseConfig.agents().getAgentByUuid(agentConfig2.getUuid()).getResources(), contains(new Resource("resource1")));
        assertThat(cruiseConfig.getEnvironments().find(new CaseInsensitiveString("Dev")).getAgents().getUuids(), containsInAnyOrder(agentConfig1.getUuid(), agentConfig2.getUuid()));
    }
}
