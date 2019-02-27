/*
 * Copyright 2017 ThoughtWorks, Inc.
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
import com.thoughtworks.go.helper.AgentInstanceMother;
import com.thoughtworks.go.helper.AgentMother;
import com.thoughtworks.go.i18n.LocalizedMessage;
import com.thoughtworks.go.listener.AgentStatusChangeListener;
import com.thoughtworks.go.server.domain.AgentInstances;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.EnvironmentConfigService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.util.GoConfigFileHelper;
import com.thoughtworks.go.util.ReflectionUtil;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.TriState;
import org.apache.commons.lang3.StringUtils;
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
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:testPropertyConfigurer.xml",
        "classpath:WEB-INF/spring-all-servlet.xml",
})
public class AgentConfigServiceIntegrationTest {
    @Autowired private AgentConfigService agentConfigService;
    @Autowired private GoConfigDao goConfigDao;
    @Autowired private GoConfigService goConfigService;
    @Autowired private EnvironmentConfigService environmentConfigService;
    private GoConfigFileHelper configHelper;
    private AgentInstances agentInstances;

    @Before
    public void setup() throws Exception {
        agentInstances = new AgentInstances(getAgentStatusChangeListener());

        configHelper = new GoConfigFileHelper();
        configHelper.usingCruiseConfigDao(goConfigDao).initializeConfigFile();
        configHelper.onSetUp();
        goConfigService.forceNotifyListeners();
    }

    private AgentStatusChangeListener getAgentStatusChangeListener() {
        return new AgentStatusChangeListener() {
            @Override
            public void onAgentStatusChange(AgentInstance agentInstance) {

            }
        };
    }

    @After
    public void tearDown() throws Exception {
        configHelper.onTearDown();
    }

    @Test
    public void shouldAddAgentToConfigFile() throws Exception {
        ResourceConfigs resourceConfigs = new ResourceConfigs("java");
        AgentConfig approvedAgentConfig = new AgentConfig(UUID.randomUUID().toString(), "test1", "192.168.0.1", resourceConfigs);
        AgentConfig deniedAgentConfig = new AgentConfig(UUID.randomUUID().toString(), "test2", "192.168.0.2", resourceConfigs);
        deniedAgentConfig.disable();
        agentConfigService.addAgent(approvedAgentConfig, Username.ANONYMOUS);
        agentConfigService.addAgent(deniedAgentConfig, Username.ANONYMOUS);
        CruiseConfig cruiseConfig = goConfigDao.load();
        assertThat(cruiseConfig.agents().contains(approvedAgentConfig), is(true));
        assertThat(cruiseConfig.agents().getAgentByUuid(approvedAgentConfig.getUuid()).getResourceConfigs(), is(resourceConfigs));
        assertThat(cruiseConfig.agents().contains(deniedAgentConfig), is(true));
        assertThat(cruiseConfig.agents().getAgentByUuid(deniedAgentConfig.getUuid()).isDisabled(), is(Boolean.TRUE));
        assertThat(cruiseConfig.agents().getAgentByUuid(deniedAgentConfig.getUuid()).getResourceConfigs(), is(resourceConfigs));
    }

    @Test
    public void shouldDeleteMultipleAgents() {
        AgentConfig agentConfig1 = new AgentConfig("UUID1", "remote-host1", "50.40.30.21");
        AgentConfig agentConfig2 = new AgentConfig("UUID2", "remote-host2", "50.40.30.22");
        agentConfig1.disable();
        agentConfig2.disable();
        AgentInstance fromConfigFile1 = AgentInstance.createFromConfig(agentConfig1, new SystemEnvironment(), getAgentStatusChangeListener());
        AgentInstance fromConfigFile2 = AgentInstance.createFromConfig(agentConfig2, new SystemEnvironment(), getAgentStatusChangeListener());

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
        AgentInstance fromConfigFile1 = AgentInstance.createFromConfig(agentConfig1, new SystemEnvironment(), getAgentStatusChangeListener());

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
        AgentInstance fromConfigFile1 = AgentInstance.createFromConfig(agentConfig1, new SystemEnvironment(), getAgentStatusChangeListener());

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
        AgentConfig agentConfig = new AgentConfig("uuid", "test", "127.0.0.1", new ResourceConfigs("java"));
        agentConfigService.addAgent(agentConfig, Username.ANONYMOUS);
        ResourceConfigs newResourceConfigs = new ResourceConfigs("firefox");
        agentConfigService.updateAgentResources(agentConfig.getUuid(), newResourceConfigs);
        CruiseConfig cruiseConfig = goConfigDao.load();
        assertThat(cruiseConfig.agents().get(0).getResourceConfigs(), is(newResourceConfigs));
    }

    @Test
    public void shouldUpdateAgentApprovalStatusByUuidToConfigFile() throws Exception {
        AgentConfig agentConfig = new AgentConfig("uuid", "test", "127.0.0.1", new ResourceConfigs("java"));
        agentConfigService.addAgent(agentConfig, Username.ANONYMOUS);
        agentConfigService.updateAgentApprovalStatus(agentConfig.getUuid(), Boolean.TRUE, Username.ANONYMOUS);

        CruiseConfig cruiseConfig = goConfigDao.load();
        assertThat(cruiseConfig.agents().get(0).isDisabled(), is(true));
    }

    @Test
    public void shouldRemoveAgentResourcesInConfigFile() throws Exception {
        AgentConfig agentConfig = new AgentConfig(UUID.randomUUID().toString(), "test", "127.0.0.1", new ResourceConfigs("java, resource1, resource2"));
        agentConfigService.addAgent(agentConfig, Username.ANONYMOUS);
        CruiseConfig cruiseConfig = goConfigDao.load();
        assertThat(cruiseConfig.agents().hasAgent(agentConfig.getUuid()), is(true));
        agentConfigService.updateAgentResources(agentConfig.getUuid(), new ResourceConfigs("java"));
        cruiseConfig = goConfigDao.load();
        assertThat(cruiseConfig.agents().getAgentByUuid(agentConfig.getUuid()).getResourceConfigs().size(), is(1));
        assertThat(cruiseConfig.agents().getAgentByUuid(agentConfig.getUuid()).getResourceConfigs().first(), is(new ResourceConfig("java")));
    }

    @Test
    public void shouldEnableMultipleAgents() {
        AgentConfig agentConfig1 = new AgentConfig(UUID.randomUUID().toString(), "remote-host1", "50.40.30.21");
        AgentConfig agentConfig2 = new AgentConfig(UUID.randomUUID().toString(), "remote-host2", "50.40.30.22");
        agentConfig1.disable();
        agentConfig2.disable();
        AgentInstance fromConfigFile1 = AgentInstance.createFromConfig(agentConfig1, new SystemEnvironment(), getAgentStatusChangeListener());
        AgentInstance fromConfigFile2 = AgentInstance.createFromConfig(agentConfig2, new SystemEnvironment(), getAgentStatusChangeListener());

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

        AgentInstance agentInstance1 = AgentInstance.createFromConfig(agentConfig1, new SystemEnvironment(), getAgentStatusChangeListener());
        AgentInstance agentInstance2 = AgentInstance.createFromConfig(agentConfig2, new SystemEnvironment(), getAgentStatusChangeListener());
        agentInstances.add(agentInstance1);
        agentInstances.add(agentInstance2);

        agentConfigService.addAgent(agentConfig1, Username.ANONYMOUS);
        agentConfigService.addAgent(agentConfig2, Username.ANONYMOUS);
        CruiseConfig cruiseConfig = goConfigDao.load();
        assertThat(cruiseConfig.agents().getAgentByUuid(agentConfig1.getUuid()).isDisabled(), is(true));
        assertThat(cruiseConfig.agents().getAgentByUuid(agentConfig2.getUuid()).isDisabled(), is(true));

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        ArrayList<String> uuids = new ArrayList<>();
        uuids.add(agentConfig1.getUuid());
        uuids.add(agentConfig2.getUuid());

        agentConfigService.bulkUpdateAgentAttributes(agentInstances, Username.ANONYMOUS, result, uuids, environmentConfigService, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), TriState.TRUE);

        cruiseConfig = goConfigDao.load();
        assertTrue(result.isSuccessful());
        assertThat(result.message(), is("Updated agent(s) with uuid(s): [" + StringUtils.join(uuids, ", ") + "]."));
        assertFalse(cruiseConfig.agents().getAgentByUuid(agentConfig1.getUuid()).isDisabled());
        assertFalse(cruiseConfig.agents().getAgentByUuid(agentConfig2.getUuid()).isDisabled());
    }

    @Test
    public void shouldEnablePendingAgents() throws Exception {
        AgentInstance pendingAgent = AgentInstanceMother.pending();
        agentInstances.add(pendingAgent);
        assertThat(pendingAgent.isRegistered(), is(false));

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        ArrayList<String> uuids = new ArrayList<>();
        uuids.add(pendingAgent.getUuid());

        agentConfigService.bulkUpdateAgentAttributes(agentInstances, Username.ANONYMOUS, result, uuids, environmentConfigService, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), TriState.TRUE);

        assertTrue(result.isSuccessful());
        assertThat(result.message(), is("Updated agent(s) with uuid(s): [" + StringUtils.join(uuids, ", ") + "]."));
        CruiseConfig cruiseConfig = goConfigDao.load();
        assertThat(cruiseConfig.agents().getAgentByUuid(pendingAgent.getUuid()).isEnabled(), is(true));
    }

    @Test
    public void shouldDisablePendingAgents() throws Exception {
        AgentInstance pendingAgent = AgentInstanceMother.pending();
        agentInstances.add(pendingAgent);
        assertThat(pendingAgent.isRegistered(), is(false));

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        ArrayList<String> uuids = new ArrayList<>();
        uuids.add(pendingAgent.getUuid());

        agentConfigService.bulkUpdateAgentAttributes(agentInstances, Username.ANONYMOUS, result, uuids, environmentConfigService, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), TriState.FALSE);

        assertTrue(result.isSuccessful());
        assertThat(result.message(), is("Updated agent(s) with uuid(s): [" + StringUtils.join(uuids, ", ") + "]."));
        CruiseConfig cruiseConfig = goConfigDao.load();
        assertThat(cruiseConfig.agents().getAgentByUuid(pendingAgent.getUuid()).isDisabled(), is(true));
    }

    @Test
    public void shouldNotAllowAnyUpdateOperationOnPendingAgentsIfConfigStateIsNotProvided() throws Exception {
        AgentInstance pendingAgent = AgentInstanceMother.pending();
        agentInstances.add(pendingAgent);
        assertThat(pendingAgent.isRegistered(), is(false));

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        ArrayList<String> uuids = new ArrayList<>();
        uuids.add(pendingAgent.getUuid());

        ArrayList<String> resourcesToAdd = new ArrayList<>();
        resourcesToAdd.add("Linux");

        ArrayList<String> resourcesToRemove = new ArrayList<>();
        resourcesToRemove.add("Gauge");

        agentConfigService.bulkUpdateAgentAttributes(agentInstances, Username.ANONYMOUS, result, uuids, environmentConfigService, resourcesToAdd, resourcesToRemove, new ArrayList<>(), new ArrayList<>(), TriState.UNSET);

        HttpLocalizedOperationResult expectedResult = new HttpLocalizedOperationResult();
        expectedResult.badRequest("Pending agents [" + pendingAgent.getUuid() + "] must be explicitly enabled or disabled when performing any operations on them.");
        assertThat(result, is(expectedResult));
    }

    @Test
    public void shouldThrowBadRequestIfNoOperationsProvidedOnBulkUpdateAgents() throws Exception {
        AgentInstance pendingAgent = AgentInstanceMother.pending();
        AgentInstance registeredAgent = AgentInstanceMother.disabled();
        agentInstances.add(pendingAgent);
        agentInstances.add(registeredAgent);

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        ArrayList<String> uuids = new ArrayList<>();
        uuids.add(pendingAgent.getUuid());
        uuids.add(registeredAgent.getUuid());

        agentConfigService.bulkUpdateAgentAttributes(agentInstances, Username.ANONYMOUS, result, uuids, environmentConfigService, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), TriState.UNSET);

        HttpLocalizedOperationResult expectedResult = new HttpLocalizedOperationResult();
        expectedResult.badRequest("No Operation performed on agents.");

        assertThat(result, is(expectedResult));
    }

    @Test
    public void shouldAllowEnablingThePendingAndDisabledAgentsTogether() throws Exception {
        AgentInstance pendingAgent = AgentInstanceMother.pending();
        agentInstances.add(pendingAgent);
        assertThat(pendingAgent.isRegistered(), is(false));
        AgentConfig agentConfig = new AgentConfig(UUID.randomUUID().toString(), "remote-host1", "50.40.30.21");
        agentConfig.disable();

        AgentInstance agentInstance = AgentInstance.createFromConfig(agentConfig, new SystemEnvironment(), getAgentStatusChangeListener());
        agentInstances.add(agentInstance);

        agentConfigService.addAgent(agentConfig, Username.ANONYMOUS);
        CruiseConfig cruiseConfig = goConfigDao.load();
        assertThat(cruiseConfig.agents().getAgentByUuid(agentConfig.getUuid()).isDisabled(), is(true));

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        ArrayList<String> uuids = new ArrayList<>();
        uuids.add(pendingAgent.getUuid());
        uuids.add(agentConfig.getUuid());

        agentConfigService.bulkUpdateAgentAttributes(agentInstances, Username.ANONYMOUS, result, uuids, environmentConfigService, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), TriState.TRUE);

        assertTrue(result.isSuccessful());
        assertThat(result.message(), is("Updated agent(s) with uuid(s): [" + StringUtils.join(uuids, ", ") + "]."));
        cruiseConfig = goConfigDao.load();
        assertThat(cruiseConfig.agents().getAgentByUuid(pendingAgent.getUuid()).isEnabled(), is(true));
        assertThat(cruiseConfig.agents().getAgentByUuid(agentConfig.getUuid()).isEnabled(), is(true));
    }

    @Test
    public void shouldDisableTheProvidedAgents() throws Exception {
        AgentConfig agentConfig1 = new AgentConfig(UUID.randomUUID().toString(), "remote-host1", "50.40.30.21");
        AgentConfig agentConfig2 = new AgentConfig(UUID.randomUUID().toString(), "remote-host2", "50.40.30.22");

        AgentInstance agentInstance1 = AgentInstance.createFromConfig(agentConfig1, new SystemEnvironment(), getAgentStatusChangeListener());
        AgentInstance agentInstance2 = AgentInstance.createFromConfig(agentConfig2, new SystemEnvironment(), getAgentStatusChangeListener());
        agentInstances.add(agentInstance1);
        agentInstances.add(agentInstance2);

        agentConfigService.addAgent(agentConfig1, Username.ANONYMOUS);
        agentConfigService.addAgent(agentConfig2, Username.ANONYMOUS);
        CruiseConfig cruiseConfig = goConfigDao.load();
        assertThat(cruiseConfig.agents().getAgentByUuid(agentConfig1.getUuid()).isDisabled(), is(false));
        assertThat(cruiseConfig.agents().getAgentByUuid(agentConfig2.getUuid()).isDisabled(), is(false));

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        ArrayList<String> uuids = new ArrayList<>();
        uuids.add(agentConfig1.getUuid());
        uuids.add(agentConfig2.getUuid());

        agentConfigService.bulkUpdateAgentAttributes(agentInstances, Username.ANONYMOUS, result, uuids, environmentConfigService, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), TriState.FALSE);

        cruiseConfig = goConfigDao.load();
        assertTrue(result.isSuccessful());
        assertThat(result.message(), is("Updated agent(s) with uuid(s): [" + StringUtils.join(uuids, ", ") + "]."));
        assertTrue(cruiseConfig.agents().getAgentByUuid(agentConfig1.getUuid()).isDisabled());
        assertTrue(cruiseConfig.agents().getAgentByUuid(agentConfig2.getUuid()).isDisabled());
    }

    @Test
    public void shouldNotDisableAgentsWhenInvalidAgentUUIDIsprovided() throws Exception {
        AgentConfig agentConfig1 = new AgentConfig(UUID.randomUUID().toString(), "remote-host1", "50.40.30.21");
        AgentConfig agentConfig2 = new AgentConfig(UUID.randomUUID().toString(), "remote-host2", "50.40.30.22");

        AgentInstance agentInstance1 = AgentInstance.createFromConfig(agentConfig1, new SystemEnvironment(), getAgentStatusChangeListener());
        AgentInstance agentInstance2 = AgentInstance.createFromConfig(agentConfig2, new SystemEnvironment(), getAgentStatusChangeListener());
        agentInstances.add(agentInstance1);
        agentInstances.add(agentInstance2);

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

        agentConfigService.bulkUpdateAgentAttributes(agentInstances, Username.ANONYMOUS, result, uuids, environmentConfigService, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), TriState.FALSE);

        cruiseConfig = goConfigDao.load();
        assertFalse(cruiseConfig.agents().getAgentByUuid(agentConfig1.getUuid()).isDisabled());
        assertFalse(cruiseConfig.agents().getAgentByUuid(agentConfig2.getUuid()).isDisabled());

        assertFalse(result.isSuccessful());
        assertThat(result.httpCode(), is(400));
        assertThat(result.message(), is("Agent(s) '[invalid-uuid]' not found."));
    }

    @Test
    public void shouldNotUpdateResourcesOnElasticAgents() throws Exception {
        AgentConfig elasticAgent = AgentMother.elasticAgent();

        AgentInstance agentInstance = AgentInstance.createFromConfig(elasticAgent, new SystemEnvironment(), getAgentStatusChangeListener());
        agentInstances.add(agentInstance);

        agentConfigService.addAgent(elasticAgent, Username.ANONYMOUS);
        CruiseConfig cruiseConfig = goConfigDao.load();

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        List<String> uuids = Arrays.asList(elasticAgent.getUuid());
        List<String> resourcesToAdd = Arrays.asList("resource");

        assertTrue(cruiseConfig.agents().getAgentByUuid(elasticAgent.getUuid()).getResourceConfigs().isEmpty());
        agentConfigService.bulkUpdateAgentAttributes(agentInstances, Username.ANONYMOUS, result, uuids, environmentConfigService, resourcesToAdd, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), TriState.FALSE);
        cruiseConfig = goConfigDao.load();

        HttpLocalizedOperationResult expectedResult = new HttpLocalizedOperationResult();
        expectedResult.badRequest("Resources on elastic agents with uuids [" + StringUtils.join(uuids, ", ") + "] can not be updated.");

        assertThat(result, is(expectedResult));
        assertTrue(cruiseConfig.agents().getAgentByUuid(elasticAgent.getUuid()).getResourceConfigs().isEmpty());
    }

    @Test
    public void shouldNotEnableAgentsWhenInvalidAgentUUIDIsprovided() throws Exception {
        AgentConfig agentConfig1 = new AgentConfig(UUID.randomUUID().toString(), "remote-host1", "50.40.30.21");
        AgentConfig agentConfig2 = new AgentConfig(UUID.randomUUID().toString(), "remote-host2", "50.40.30.22");

        AgentInstance agentInstance1 = AgentInstance.createFromConfig(agentConfig1, new SystemEnvironment(), getAgentStatusChangeListener());
        AgentInstance agentInstance2 = AgentInstance.createFromConfig(agentConfig2, new SystemEnvironment(), getAgentStatusChangeListener());
        agentInstances.add(agentInstance1);
        agentInstances.add(agentInstance2);

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

        agentConfigService.bulkUpdateAgentAttributes(agentInstances, Username.ANONYMOUS, result, uuids, environmentConfigService, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), TriState.TRUE);

        cruiseConfig = goConfigDao.load();
        assertThat(cruiseConfig.agents().getAgentByUuid(agentConfig1.getUuid()).isDisabled(), is(false));
        assertThat(cruiseConfig.agents().getAgentByUuid(agentConfig2.getUuid()).isDisabled(), is(false));

        assertFalse(result.isSuccessful());
        assertThat(result.httpCode(), is(400));
        assertThat(result.message(), is("Agent(s) '[invalid-uuid]' not found."));
    }

    @Test
    public void shouldAddResourcestoTheSpecifiedAgents() throws Exception {
        AgentConfig agentConfig1 = new AgentConfig(UUID.randomUUID().toString(), "remote-host1", "50.40.30.21");
        AgentConfig agentConfig2 = new AgentConfig(UUID.randomUUID().toString(), "remote-host2", "50.40.30.22");

        AgentInstance agentInstance1 = AgentInstance.createFromConfig(agentConfig1, new SystemEnvironment(), getAgentStatusChangeListener());
        AgentInstance agentInstance2 = AgentInstance.createFromConfig(agentConfig2, new SystemEnvironment(), getAgentStatusChangeListener());
        agentInstances.add(agentInstance1);
        agentInstances.add(agentInstance2);

        agentConfigService.addAgent(agentConfig1, Username.ANONYMOUS);
        agentConfigService.addAgent(agentConfig2, Username.ANONYMOUS);
        CruiseConfig cruiseConfig = goConfigDao.load();

        assertThat(cruiseConfig.agents().getAgentByUuid(agentConfig1.getUuid()).getResourceConfigs().size(), is(0));
        assertThat(cruiseConfig.agents().getAgentByUuid(agentConfig2.getUuid()).getResourceConfigs().size(), is(0));

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        ArrayList<String> uuids = new ArrayList<>();
        uuids.add(agentConfig1.getUuid());
        uuids.add(agentConfig2.getUuid());

        ArrayList<String> resources = new ArrayList<>();
        resources.add("resource1");
        resources.add("resource2");

        agentConfigService.bulkUpdateAgentAttributes(agentInstances, Username.ANONYMOUS, result, uuids, environmentConfigService, resources, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), TriState.FALSE);

        cruiseConfig = goConfigDao.load();

        assertTrue(result.isSuccessful());
        assertThat(result.message(), is("Updated agent(s) with uuid(s): [" + StringUtils.join(uuids, ", ") + "]."));
        assertThat(cruiseConfig.agents().getAgentByUuid(agentConfig1.getUuid()).getResourceConfigs().size(), is(2));
        assertThat(cruiseConfig.agents().getAgentByUuid(agentConfig1.getUuid()).getResourceConfigs(), containsInAnyOrder(new ResourceConfig("resource1"), new ResourceConfig("resource2")));
    }

    @Test
    public void shouldRemoveResourcesFromTheSpecifiedAgents() throws Exception {
        AgentConfig agentConfig1 = new AgentConfig(UUID.randomUUID().toString(), "remote-host1", "50.40.30.21");
        AgentConfig agentConfig2 = new AgentConfig(UUID.randomUUID().toString(), "remote-host2", "50.40.30.22");

        AgentInstance agentInstance1 = AgentInstance.createFromConfig(agentConfig1, new SystemEnvironment(), getAgentStatusChangeListener());
        AgentInstance agentInstance2 = AgentInstance.createFromConfig(agentConfig2, new SystemEnvironment(), getAgentStatusChangeListener());
        agentInstances.add(agentInstance1);
        agentInstances.add(agentInstance2);

        agentConfig1.addResourceConfig(new ResourceConfig("resource-1"));
        agentConfig1.addResourceConfig(new ResourceConfig("resource-2"));
        agentConfig2.addResourceConfig(new ResourceConfig("resource-2"));

        agentConfigService.addAgent(agentConfig1, Username.ANONYMOUS);
        agentConfigService.addAgent(agentConfig2, Username.ANONYMOUS);
        CruiseConfig cruiseConfig = goConfigDao.load();

        assertThat(cruiseConfig.agents().getAgentByUuid(agentConfig1.getUuid()).getResourceConfigs().size(), is(2));
        assertThat(cruiseConfig.agents().getAgentByUuid(agentConfig2.getUuid()).getResourceConfigs().size(), is(1));

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        ArrayList<String> uuids = new ArrayList<>();
        uuids.add(agentConfig1.getUuid());
        uuids.add(agentConfig2.getUuid());

        ArrayList<String> resources = new ArrayList<>();
        resources.add("resource-2");

        agentConfigService.bulkUpdateAgentAttributes(agentInstances, Username.ANONYMOUS, result, uuids, environmentConfigService, new ArrayList<>(), resources, new ArrayList<>(), new ArrayList<>(), TriState.FALSE);

        cruiseConfig = goConfigDao.load();

        assertTrue(result.isSuccessful());
        assertThat(result.message(), is("Updated agent(s) with uuid(s): [" + StringUtils.join(uuids, ", ") + "]."));
        assertThat(cruiseConfig.agents().getAgentByUuid(agentConfig1.getUuid()).getResourceConfigs().size(), is(1));
        assertThat(cruiseConfig.agents().getAgentByUuid(agentConfig1.getUuid()).getResourceConfigs(), contains(new ResourceConfig("resource-1")));
        assertThat(cruiseConfig.agents().getAgentByUuid(agentConfig2.getUuid()).getResourceConfigs().size(), is(0));
    }

    @Test
    public void shouldAddProvidedAgentsToTheSpecifiedEnvironments() throws Exception {
        AgentConfig agentConfig1 = new AgentConfig(UUID.randomUUID().toString(), "remote-host1", "50.40.30.21");
        AgentConfig agentConfig2 = new AgentConfig(UUID.randomUUID().toString(), "remote-host2", "50.40.30.22");

        AgentInstance agentInstance1 = AgentInstance.createFromConfig(agentConfig1, new SystemEnvironment(), getAgentStatusChangeListener());
        AgentInstance agentInstance2 = AgentInstance.createFromConfig(agentConfig2, new SystemEnvironment(), getAgentStatusChangeListener());
        agentInstances.add(agentInstance1);
        agentInstances.add(agentInstance2);

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

        agentConfigService.bulkUpdateAgentAttributes(agentInstances, Username.ANONYMOUS, result, uuids, environmentConfigService, new ArrayList<>(), new ArrayList<>(), environmentsToAdd, new ArrayList<>(), TriState.TRUE);

        assertTrue(result.isSuccessful());
        assertThat(result.message(), is("Updated agent(s) with uuid(s): [" + StringUtils.join(uuids, ", ") + "]."));
        assertThat(goConfigDao.load().getEnvironments().find(new CaseInsensitiveString("Dev")).getAgents().getUuids(), containsInAnyOrder(agentConfig1.getUuid(), agentConfig2.getUuid()));
    }

    @Test
    public void shouldRemoveProvidedAgentsFromTheSpecifiedEnvironments() throws Exception {
        AgentConfig agentConfig1 = new AgentConfig(UUID.randomUUID().toString(), "remote-host1", "50.40.30.21");
        AgentConfig agentConfig2 = new AgentConfig(UUID.randomUUID().toString(), "remote-host2", "50.40.30.22");

        AgentInstance agentInstance1 = AgentInstance.createFromConfig(agentConfig1, new SystemEnvironment(), getAgentStatusChangeListener());
        AgentInstance agentInstance2 = AgentInstance.createFromConfig(agentConfig2, new SystemEnvironment(), getAgentStatusChangeListener());
        agentInstances.add(agentInstance1);
        agentInstances.add(agentInstance2);

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

        agentConfigService.bulkUpdateAgentAttributes(agentInstances, Username.ANONYMOUS, result, uuids, environmentConfigService, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), environmentsToRemove, TriState.TRUE);

        assertTrue(result.isSuccessful());
        assertThat(result.message(), is("Updated agent(s) with uuid(s): [" + StringUtils.join(uuids, ", ") + "]."));
        assertFalse(goConfigDao.load().getEnvironments().find(new CaseInsensitiveString("Dev")).hasAgent(agentConfig1.getUuid()));
        assertFalse(goConfigDao.load().getEnvironments().find(new CaseInsensitiveString("Dev")).hasAgent(agentConfig2.getUuid()));
    }

    @Test
    public void shouldNotAddAgentToNonExistingEnvironment() throws Exception {
        AgentConfig agentConfig1 = new AgentConfig(UUID.randomUUID().toString(), "remote-host1", "50.40.30.21");
        AgentConfig agentConfig2 = new AgentConfig(UUID.randomUUID().toString(), "remote-host2", "50.40.30.22");

        AgentInstance agentInstance1 = AgentInstance.createFromConfig(agentConfig1, new SystemEnvironment(), getAgentStatusChangeListener());
        AgentInstance agentInstance2 = AgentInstance.createFromConfig(agentConfig2, new SystemEnvironment(), getAgentStatusChangeListener());
        agentInstances.add(agentInstance1);
        agentInstances.add(agentInstance2);

        agentConfigService.addAgent(agentConfig1, Username.ANONYMOUS);
        agentConfigService.addAgent(agentConfig2, Username.ANONYMOUS);


        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        ArrayList<String> uuids = new ArrayList<>();
        uuids.add(agentConfig1.getUuid());
        uuids.add(agentConfig2.getUuid());

        ArrayList<String> environmentsToAdd = new ArrayList<>();
        environmentsToAdd.add("Non-Existing-Environment");

        agentConfigService.bulkUpdateAgentAttributes(agentInstances, Username.ANONYMOUS, result, uuids, environmentConfigService, new ArrayList<>(), new ArrayList<>(), environmentsToAdd, new ArrayList<>(), TriState.TRUE);

        assertFalse(result.isSuccessful());
        assertThat(result.httpCode(), is(400));
        assertThat(result.message(), containsString("Environment 'Non-Existing-Environment' not found."));
    }

    @Test
    public void shouldNotRemoveAgentFromNonExistingEnvironment() throws Exception {
        AgentConfig agentConfig1 = new AgentConfig(UUID.randomUUID().toString(), "remote-host1", "50.40.30.21");
        AgentConfig agentConfig2 = new AgentConfig(UUID.randomUUID().toString(), "remote-host2", "50.40.30.22");

        AgentInstance agentInstance1 = AgentInstance.createFromConfig(agentConfig1, new SystemEnvironment(), getAgentStatusChangeListener());
        AgentInstance agentInstance2 = AgentInstance.createFromConfig(agentConfig2, new SystemEnvironment(), getAgentStatusChangeListener());
        agentInstances.add(agentInstance1);
        agentInstances.add(agentInstance2);

        agentConfigService.addAgent(agentConfig1, Username.ANONYMOUS);
        agentConfigService.addAgent(agentConfig2, Username.ANONYMOUS);


        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        ArrayList<String> uuids = new ArrayList<>();
        uuids.add(agentConfig1.getUuid());
        uuids.add(agentConfig2.getUuid());

        ArrayList<String> environmentsToRemove = new ArrayList<>();
        environmentsToRemove.add("NonExistingEnvironment");

        agentConfigService.bulkUpdateAgentAttributes(agentInstances, Username.ANONYMOUS, result, uuids, environmentConfigService, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), environmentsToRemove, TriState.TRUE);

        assertFalse(result.isSuccessful());
        assertThat(result.httpCode(), is(400));
        assertThat(result.message(), containsString("Environment 'NonExistingEnvironment' not found."));
    }

    @Test
    public void shouldUpdateResourcesEnvironmentsAndAgentStateOfTheProvidedStatesAllTogether() throws Exception {
        AgentConfig agentConfig1 = new AgentConfig(UUID.randomUUID().toString(), "remote-host1", "50.40.30.21");
        AgentConfig agentConfig2 = new AgentConfig(UUID.randomUUID().toString(), "remote-host2", "50.40.30.22");

        AgentInstance agentInstance1 = AgentInstance.createFromConfig(agentConfig1, new SystemEnvironment(), getAgentStatusChangeListener());
        AgentInstance agentInstance2 = AgentInstance.createFromConfig(agentConfig2, new SystemEnvironment(), getAgentStatusChangeListener());
        agentInstances.add(agentInstance1);
        agentInstances.add(agentInstance2);

        agentConfigService.addAgent(agentConfig1, Username.ANONYMOUS);
        agentConfigService.addAgent(agentConfig2, Username.ANONYMOUS);

        CruiseConfig cruiseConfig = goConfigDao.load();
        BasicEnvironmentConfig environment = new BasicEnvironmentConfig(new CaseInsensitiveString("Dev"));
        goConfigDao.addEnvironment(environment);

        assertThat(environment.getAgents().getUuids(), not(containsInAnyOrder(agentConfig1.getUuid(), agentConfig2.getUuid())));
        assertFalse(cruiseConfig.agents().getAgentByUuid(agentConfig1.getUuid()).isDisabled());
        assertFalse(cruiseConfig.agents().getAgentByUuid(agentConfig1.getUuid()).isDisabled());
        assertThat(cruiseConfig.agents().getAgentByUuid(agentConfig1.getUuid()).getResourceConfigs().size(), is(0));
        assertThat(cruiseConfig.agents().getAgentByUuid(agentConfig2.getUuid()).getResourceConfigs().size(), is(0));

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        ArrayList<String> uuids = new ArrayList<>();
        uuids.add(agentConfig1.getUuid());
        uuids.add(agentConfig2.getUuid());
        ArrayList<String> resources = new ArrayList<>();
        resources.add("resource1");
        ArrayList<String> environmentsToAdd = new ArrayList<>();
        environmentsToAdd.add("Dev");

        agentConfigService.bulkUpdateAgentAttributes(agentInstances, Username.ANONYMOUS, result, uuids, environmentConfigService, resources, new ArrayList<>(), environmentsToAdd, new ArrayList<>(), TriState.FALSE);

        cruiseConfig = goConfigDao.load();
        assertTrue(result.isSuccessful());
        assertThat(result.message(), is("Updated agent(s) with uuid(s): [" + StringUtils.join(uuids, ", ") + "]."));
        assertTrue(cruiseConfig.agents().getAgentByUuid(agentConfig1.getUuid()).isDisabled());
        assertTrue(cruiseConfig.agents().getAgentByUuid(agentConfig2.getUuid()).isDisabled());
        assertThat(cruiseConfig.agents().getAgentByUuid(agentConfig1.getUuid()).getResourceConfigs(), contains(new ResourceConfig("resource1")));
        assertThat(cruiseConfig.agents().getAgentByUuid(agentConfig2.getUuid()).getResourceConfigs(), contains(new ResourceConfig("resource1")));
        assertThat(cruiseConfig.getEnvironments().find(new CaseInsensitiveString("Dev")).getAgents().getUuids(), containsInAnyOrder(agentConfig1.getUuid(), agentConfig2.getUuid()));
    }
}
