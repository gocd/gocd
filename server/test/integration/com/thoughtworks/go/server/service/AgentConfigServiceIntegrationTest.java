package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.domain.AgentInstance;
import com.thoughtworks.go.server.domain.AgentInstances;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.result.HttpOperationResult;
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

import java.util.List;
import java.util.UUID;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.*;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;

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
}