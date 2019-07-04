/*
 * Copyright 2019 ThoughtWorks, Inc.
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
import com.thoughtworks.go.domain.AgentConfigStatus;
import com.thoughtworks.go.domain.AgentInstance;
import com.thoughtworks.go.domain.AgentRuntimeStatus;
import com.thoughtworks.go.domain.AgentStatus;
import com.thoughtworks.go.helper.AgentInstanceMother;
import com.thoughtworks.go.helper.EnvironmentConfigMother;
import com.thoughtworks.go.listener.AgentStatusChangeListener;
import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.domain.AgentInstances;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.messaging.SendEmailMessage;
import com.thoughtworks.go.server.messaging.notifications.AgentStatusChangeNotifier;
import com.thoughtworks.go.server.persistence.AgentDao;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.server.service.result.HttpOperationResult;
import com.thoughtworks.go.server.ui.AgentViewModel;
import com.thoughtworks.go.server.ui.AgentsViewModel;
import com.thoughtworks.go.server.util.UuidGenerator;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.service.ConfigRepository;
import com.thoughtworks.go.util.*;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.ContextConfiguration;

import java.io.IOException;
import java.net.InetAddress;
import java.util.*;

import static com.thoughtworks.go.util.SystemUtil.currentWorkingDirectory;
import static java.lang.String.format;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:testPropertyConfigurer.xml",
        "classpath:WEB-INF/spring-all-servlet.xml",
})

//TODO: Vrushali and Viraj need to fix this
public class AgentServiceIntegrationTest {
    @Autowired
    private GoConfigDao goConfigDao;
    @Autowired
    private EnvironmentConfigService environmentConfigService;
    @Autowired
    private AgentService agentService;
    @Autowired
    private GoConfigService goConfigService;
    @Autowired
    private CachedGoConfig cachedGoConfig;
    @Autowired
    private SecurityService securityService;
    @Autowired
    private ServerHealthService serverHealthService;
    @Autowired
    private AgentDao agentDao;
    @Autowired
    private ConfigRepository configRepository;
    @Autowired
    private DatabaseAccessHelper dbHelper;

    private static final GoConfigFileHelper CONFIG_HELPER = new GoConfigFileHelper();
    private static final String UUID = "uuid";
    private static final String UUID2 = "uuid2";
    private static final Username USERNAME = new Username(new CaseInsensitiveString("admin"));

    @Before
    public void setUp() throws Exception {
        CONFIG_HELPER.usingCruiseConfigDao(goConfigDao);
        CONFIG_HELPER.onSetUp();
        dbHelper.onSetUp();
        cachedGoConfig.clearListeners();
        agentDao.clearListeners();
        agentService.clearAll();
        agentService.initialize();
        environmentConfigService.initialize();
    }

    @After
    public void tearDown() throws Exception {
        new SystemEnvironment().setProperty("agent.connection.timeout", "300");
        CONFIG_HELPER.usingCruiseConfigDao(goConfigDao);
        CONFIG_HELPER.onTearDown();
        cachedGoConfig.clearListeners();
        agentService.clearAll();
        dbHelper.onTearDown();
    }

    private AgentService getAgentService(AgentInstances agentInstances) {
        return new AgentService(new SystemEnvironment(), agentInstances, securityService, agentDao,
                new UuidGenerator(), serverHealthService, agentStatusChangeNotifier(), goConfigService);
    }

    @Test
    public void shouldAddResourcesWhileBulkUpdatingAgents() {
        createEnabledAgent(UUID);
        createEnabledAgent(UUID2);

        List<String> uuids = Arrays.asList(UUID, UUID2);
        List<String> resourcesToAdd = Arrays.asList("resource1", "resource2");
        List<String> resourcesToRemove = Collections.emptyList();
        List<String> emptyEnvs = Collections.emptyList();

        HttpLocalizedOperationResult operationResult = new HttpLocalizedOperationResult();
        agentService.bulkUpdateAgentAttributes(USERNAME, operationResult, uuids, resourcesToAdd, resourcesToRemove, emptyEnvs, emptyEnvs, TriState.TRUE);
        assertThat(operationResult.httpCode(), is(200));
        assertThat(operationResult.message(), is("Updated agent(s) with uuid(s): [uuid, uuid2]."));

        ResourceConfigs uuidResources = agentService.findAgentAndRefreshStatus(UUID).agentConfig().getResourceConfigs();
        assertThat(uuidResources, hasItem(new ResourceConfig("resource1")));
        assertThat(uuidResources, hasItem(new ResourceConfig("resource2")));

        ResourceConfigs uuid2Resources = agentService.findAgentAndRefreshStatus(UUID2).agentConfig().getResourceConfigs();
        assertThat(uuid2Resources, hasItem(new ResourceConfig("resource1")));
        assertThat(uuid2Resources, hasItem(new ResourceConfig("resource2")));
    }

    @Test
    public void shouldAddEnvironmentsWhileBulkUpdatingAgents() {
        createEnvironment("uat", "prod");

        createEnabledAgent(UUID);
        createEnabledAgent(UUID2);

        HttpLocalizedOperationResult operationResult = new HttpLocalizedOperationResult();
        agentService.bulkUpdateAgentAttributes(USERNAME, operationResult, Arrays.asList(UUID, UUID2), Collections.emptyList(), Collections.emptyList(), Arrays.asList("uat", "prod"), Collections.emptyList(), TriState.TRUE);

        assertThat(operationResult.httpCode(), is(200));
        assertThat(operationResult.message(), is("Updated agent(s) with uuid(s): [uuid, uuid2]."));
        assertThat(environmentConfigService.environmentsFor(UUID), containsSet("uat", "prod"));
        assertThat(environmentConfigService.environmentsFor(UUID2), containsSet("uat", "prod"));
    }

    @Test
    public void shouldNotFailWhenAddingAgentEnvironmentAssociationThatAlreadyExists() {
        createEnvironment("uat");

        createEnabledAgent(UUID);
        createEnabledAgent(UUID2);

        HttpLocalizedOperationResult operationResult = new HttpLocalizedOperationResult();
        agentService.bulkUpdateAgentAttributes(USERNAME, operationResult, Arrays.asList(UUID, UUID2), Collections.emptyList(), Collections.emptyList(), Arrays.asList("uat"), Collections.emptyList(), TriState.TRUE);

        operationResult = new HttpLocalizedOperationResult();
        agentService.bulkUpdateAgentAttributes(USERNAME, operationResult, Arrays.asList(UUID, UUID2), Collections.emptyList(), Collections.emptyList(), Arrays.asList("uat"), Collections.emptyList(), TriState.TRUE);

        assertThat(operationResult.httpCode(), is(200));
        assertThat(environmentConfigService.environmentsFor(UUID), containsSet("uat"));
        assertThat(environmentConfigService.environmentsFor(UUID2), containsSet("uat"));
    }

    @Test
    public void shouldRemoveEnvironmentsWhileBulkUpdatingAgents() {
        createEnvironment("uat", "prod");

        AgentConfig enabledAgent1 = createEnabledAgent(UUID);
        enabledAgent1.setEnvironments("uat");
        agentDao.saveOrUpdate(enabledAgent1);

        AgentConfig enabledAgent2 = createEnabledAgent(UUID2);
        enabledAgent2.setEnvironments("uat,prod");
        agentDao.saveOrUpdate(enabledAgent2);

        HttpLocalizedOperationResult operationResult = new HttpLocalizedOperationResult();
        agentService.bulkUpdateAgentAttributes(USERNAME, operationResult, Arrays.asList(UUID, UUID2), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Arrays.asList("uat"), TriState.TRUE);

        assertThat(operationResult.httpCode(), is(200));
        assertThat(operationResult.message(), is("Updated agent(s) with uuid(s): [uuid, uuid2]."));

        assertThat(environmentConfigService.environmentsFor(UUID), not(containsSet("uat")));

        assertThat(environmentConfigService.environmentsFor(UUID2), not(containsSet("uat")));
        assertThat(environmentConfigService.environmentsFor(UUID2), containsSet("prod"));
    }

    @Test
    public void shouldOnlyAddRemoveAgentEnvironmentAssociationThatAreRequested() {
        createEnvironment("uat", "prod", "perf", "test", "dev");

        AgentConfig agent1 = createEnabledAgent(UUID);
        agent1.setEnvironments("uat,prod");
        agentDao.saveOrUpdate(agent1);

        AgentConfig agent2 = createEnabledAgent(UUID2);
        agent2.setEnvironments("prod,uat,perf");
        agentDao.saveOrUpdate(agent2);

        HttpLocalizedOperationResult operationResult = new HttpLocalizedOperationResult();
        agentService.bulkUpdateAgentAttributes(USERNAME, operationResult, Arrays.asList(UUID, UUID2), Collections.emptyList(), Collections.emptyList(), Arrays.asList("dev", "test", "perf"), Arrays.asList("uat", "prod"), TriState.TRUE);

        assertThat(operationResult.httpCode(), is(200));
        assertThat(operationResult.message(), is("Updated agent(s) with uuid(s): [uuid, uuid2]."));

        String[] expectedEnvs = new String[]{"perf", "dev", "test"};
        assertThat(environmentConfigService.environmentsFor(UUID).size(), is(3));
        assertThat(environmentConfigService.environmentsFor(UUID), containsSet(expectedEnvs));

        assertThat(environmentConfigService.environmentsFor(UUID2).size(), is(3));
        assertThat(environmentConfigService.environmentsFor(UUID2), containsSet(expectedEnvs));
    }

    @Test
    public void shouldThrow400ErrorWhenNonExistingEnvironmentIsAssociatedWithAgent() {
        createEnabledAgent(UUID);

        HttpLocalizedOperationResult operationResult = new HttpLocalizedOperationResult();
        agentService.bulkUpdateAgentAttributes(USERNAME, operationResult, Arrays.asList(UUID), Collections.emptyList(), Collections.emptyList(), Arrays.asList("unknown_env"), Collections.emptyList(), TriState.TRUE);

        assertThat(operationResult.httpCode(), is(400));
        assertThat(operationResult.message(), containsString("Environment with name 'unknown_env' was not found!"));
    }

    private TypeSafeMatcher<Set<String>> containsSet(final String... items) {
        return new TypeSafeMatcher<Set<String>>() {
            @Override
            public boolean matchesSafely(Set<String> item) {
                return item.containsAll(Arrays.asList(items));
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("to contain ").appendValue(items);
            }
        };
    }

    @Test
    public void shouldNotAllowUpdatingEnvironmentsWhenNotAdmin() {
        CONFIG_HELPER.enableSecurity();
        CONFIG_HELPER.addAdmins("adminUser");

        HttpLocalizedOperationResult operationResult = new HttpLocalizedOperationResult();
        List<String> emptyList = Collections.emptyList();
        agentService.bulkUpdateAgentAttributes(USERNAME, operationResult, Arrays.asList(UUID), emptyList, emptyList, emptyList, Arrays.asList("uat"), TriState.TRUE);

        assertThat(operationResult.httpCode(), is(403));
        assertThat(operationResult.message(), is("Unauthorized to edit."));
    }

    @Test
    public void shouldAddRemoveResourcesWhileBuikUpdatingAgents() {
        createEnabledAgent(UUID);
        createEnabledAgent(UUID2);

        HttpLocalizedOperationResult operationResult = new HttpLocalizedOperationResult();
        String resource1 = "resource1";
        String resource2 = "resource2";
        agentService.bulkUpdateAgentAttributes(USERNAME, operationResult, Arrays.asList(UUID, UUID2), Arrays.asList(resource1), Arrays.asList(resource2), Collections.emptyList(), Collections.emptyList(), TriState.TRUE);

        assertThat(operationResult.httpCode(), is(200));
        assertThat(operationResult.message(), is("Updated agent(s) with uuid(s): [uuid, uuid2]."));

        assertThat(agentService.findAgentAndRefreshStatus(UUID).agentConfig().getResourceConfigs(), hasItem(new ResourceConfig(resource1)));
        assertThat(agentService.findAgentAndRefreshStatus(UUID).agentConfig().getResourceConfigs(), not(hasItem(new ResourceConfig(resource2))));

        assertThat(agentService.findAgentAndRefreshStatus(UUID2).agentConfig().getResourceConfigs(), hasItem(new ResourceConfig(resource1)));
        assertThat(agentService.findAgentAndRefreshStatus(UUID2).agentConfig().getResourceConfigs(), not(hasItem(new ResourceConfig(resource2))));
    }

    @Test
    public void shouldFindAgentViewModelForAnExistingAgent() {
        AgentConfig agent = createEnabledAgent(UUID);
        String env = "prod";
        agent.setEnvironments(env);
        agentDao.saveOrUpdate(agent);

        AgentViewModel actual = agentService.findAgentViewModel(UUID);
        assertThat(actual.getUuid(), is(UUID));
        HashSet<String> envSet = new HashSet<>();
        envSet.add(env);
        assertThat(actual.getEnvironments(), is(envSet));
    }

    @Test
    public void shouldNotChangeResourcesForNoChange() {
        createEnabledAgent(UUID);
        createEnabledAgent(UUID2);

        HttpLocalizedOperationResult operationResult = new HttpLocalizedOperationResult();
        agentService.bulkUpdateAgentAttributes(USERNAME, operationResult, Arrays.asList(UUID), Arrays.asList("resource-1"), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), TriState.TRUE);

        assertThat(operationResult.httpCode(), is(200));
        assertThat(operationResult.message(), is("Updated agent(s) with uuid(s): [uuid]."));
        assertThat(agentService.findAgentAndRefreshStatus(UUID).agentConfig().getResourceConfigs(), hasItem(new ResourceConfig("resource-1")));
        assertThat(agentService.findAgentAndRefreshStatus(UUID2).agentConfig().getResourceConfigs(), not(hasItem(new ResourceConfig("resource-1"))));
    }

    @Test
    public void shouldUpdateAgentStatus() {
        AgentInstance instance = AgentInstanceMother.building();
        AgentService agentService = getAgentService(new AgentInstances(new SystemEnvironment(), agentStatusChangeListener(), instance));
        AgentInstances agents = agentService.findRegisteredAgents();

        String uuid = instance.agentConfig().getUuid();
        assertThat(agents.findAgentAndRefreshStatus(uuid).getStatus(), is(AgentStatus.Building));
        AgentIdentifier agentIdentifier = instance.agentConfig().getAgentIdentifier();
        String cookie = agentService.assignCookie(agentIdentifier);
        agentService.updateRuntimeInfo(new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), cookie));
        agents = agentService.findRegisteredAgents();
        assertThat(agents.findAgentAndRefreshStatus(uuid).getStatus(), is(AgentStatus.Idle));
    }

    @Test
    public void shouldThrowExceptionWhenADuplicateAgentTriesToUpdateStatus() {
        AgentInstance instance = AgentInstanceMother.building();
        AgentService agentService = getAgentService(new AgentInstances(new SystemEnvironment(), agentStatusChangeListener(), instance));
        AgentInstances agents = agentService.findRegisteredAgents();

        String uuid = instance.agentConfig().getUuid();
        assertThat(agents.findAgentAndRefreshStatus(uuid).getStatus(), is(AgentStatus.Building));
        AgentIdentifier identifier = instance.agentConfig().getAgentIdentifier();
        agentDao.associateCookie(identifier, "new_cookie");
        AgentRuntimeInfo runtimeInfo = new AgentRuntimeInfo(identifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "old_cookie");
        try {
            agentService.updateRuntimeInfo(runtimeInfo);
            fail("agent with bad cookie should not be able to update runtime info");
        } catch (AgentWithDuplicateUUIDException e) {
            assertThat(e.getMessage(), is(format("Agent [%s] has invalid cookie", runtimeInfo.agentInfoDebugString())));
        }
        agents = agentService.findRegisteredAgents();
        assertThat(agents.findAgentAndRefreshStatus(uuid).getStatus(), is(AgentStatus.Building));
        AgentIdentifier agentIdentifier = instance.agentConfig().getAgentIdentifier();
        String cookie = agentService.assignCookie(agentIdentifier);
        agentService.updateRuntimeInfo(new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), cookie));
    }

    @Test
    public void shouldMarkAgentAsLostContactIfAgentDidNotPingForMoreThanTimeout() {
        new SystemEnvironment().setProperty("agent.connection.timeout", "-1");
        CONFIG_HELPER.addMailHost(new MailHost("ghost.name", 25, "loser", "boozer", true, false, "go@foo.mail.com", "admin@foo.mail.com"));

        Date date = new Date(70, 1, 1, 1, 1, 1);
        AgentInstance instance = AgentInstanceMother.idle(date, "CCeDev01");
        ((AgentRuntimeInfo) ReflectionUtil.getField(instance, "agentRuntimeInfo")).setOperatingSystem("Minix");
        EmailSender mailSender = mock(EmailSender.class);
        AgentService agentService = new AgentService(new SystemEnvironment(), securityService, agentDao, new UuidGenerator(), serverHealthService, agentStatusChangeNotifier(), goConfigService);
        AgentInstances agentInstances = (AgentInstances) ReflectionUtil.getField(agentService, "agentInstances");
        agentInstances.add(instance);

        AgentInstances agents = agentService.findRegisteredAgents();
        assertThat(agents.size(), is(1));
        AgentInstance agentInstance = agents.findAgentAndRefreshStatus(instance.agentConfig().getUuid());
        assertThat(agentInstance.getStatus(), is(AgentStatus.LostContact));
    }

    @Test
    public void shouldSendLostContactEmailWhenAgentStateIsLostContact_FEATURE_HIDDEN() {
        new SystemEnvironment().setProperty("agent.connection.timeout", "-1");
        CONFIG_HELPER.addMailHost(new MailHost("ghost.name", 25, "loser", "boozer", true, false, "go@foo.mail.com", "admin@foo.mail.com"));

        Date date = new Date(70, 1, 1, 1, 1, 1);
        AgentInstance instance = AgentInstanceMother.idle(date, "CCeDev01");
        ((AgentRuntimeInfo) ReflectionUtil.getField(instance, "agentRuntimeInfo")).setOperatingSystem("Minix");
        EmailSender mailSender = mock(EmailSender.class);
        AgentService agentService = new AgentService(new SystemEnvironment(), securityService, agentDao, new UuidGenerator(), serverHealthService, agentStatusChangeNotifier(), goConfigService);
        AgentInstances agentInstances = (AgentInstances) ReflectionUtil.getField(agentService, "agentInstances");
        agentInstances.add(instance);

        AgentInstances agents = agentService.findRegisteredAgents();
        assertThat(agents.size(), is(1));
        AgentInstance agentInstance = agents.findAgentAndRefreshStatus(instance.agentConfig().getUuid());
        assertThat(agentInstance.getStatus(), is(AgentStatus.LostContact));
        String body = String.format("The email has been sent out automatically by the Go server at (%s) to Go administrators.\n"
                + "\n"
                + "The Go server has lost contact with agent:\n"
                + "\n"
                + "Agent name: CCeDev01\n"
                + "Free Space: 10.0 KB\n"
                + "Sandbox: /var/lib/foo\n"
                + "IP Address: 10.18.5.1\n"
                + "OS: Minix\n"
                + "Resources: \n"
                + "Environments: \n"
                + "\n"
                + "Lost contact at: %s", SystemUtil.getFirstLocalNonLoopbackIpAddress(), date);
//        verify(mailSender).sendEmail(new SendEmailMessage("[Lost Contact] Go agent host: " + instance.getHostname(), body, "admin@foo.mail.com"));
        verify(mailSender, never()).sendEmail(new SendEmailMessage("[Lost Contact] Go agent host: " + instance.getHostname(), body, "admin@foo.mail.com"));
    }

    @Test
    public void shouldNotShootoutMailsForEveryStatusChange() {//should, only for lost-contact
        CONFIG_HELPER.addMailHost(new MailHost("ghost.name", 25, "loser", "boozer", true, false, "go@foo.mail.com", "admin@foo.mail.com"));

        AgentInstance instance = AgentInstanceMother.idle(new Date(), "CCeDev01");
        EmailSender mailSender = mock(EmailSender.class);

        agentDao.associateCookie(instance.getAgentIdentifier(), "rotten-cookie");
        AgentService agentService = new AgentService(new SystemEnvironment(), securityService, agentDao, new UuidGenerator(), serverHealthService, agentStatusChangeNotifier(), goConfigService);
        AgentInstances agentInstances = (AgentInstances) ReflectionUtil.getField(agentService, "agentInstances");
        agentInstances.add(instance);

        ReflectionUtil.setField(instance, "lastHeardTime", null);

        AgentRuntimeInfo runtimeInfo = (AgentRuntimeInfo) ReflectionUtil.getField(instance, "agentRuntimeInfo");
        runtimeInfo.setCookie("rotten-cookie");
        runtimeInfo.setStatus(AgentStatus.Building);

        agentService.updateRuntimeInfo(runtimeInfo);

        verify(mailSender, never()).sendEmail(any(SendEmailMessage.class));
    }

    @Test
    public void shouldLoadAllAgents() {
        AgentInstance idle = AgentInstanceMother.idle(new Date(), "CCeDev01");
        AgentInstance pending = AgentInstanceMother.pending();
        AgentInstance building = AgentInstanceMother.building();
        AgentInstance denied = AgentInstanceMother.disabled();
        AgentService agentService = getAgentService(new AgentInstances(new SystemEnvironment(), agentStatusChangeListener(), idle, pending, building, denied));

        assertThat(agentService.agentInstances().size(), is(4));

        assertThat(agentService.findAgentAndRefreshStatus(idle.agentConfig().getUuid()), is(idle));
        assertThat(agentService.findAgentAndRefreshStatus(pending.agentConfig().getUuid()), is(pending));
        assertThat(agentService.findAgentAndRefreshStatus(building.agentConfig().getUuid()), is(building));
        assertThat(agentService.findAgentAndRefreshStatus(denied.agentConfig().getUuid()), is(denied));
    }

    private AgentStatusChangeListener agentStatusChangeListener() {
        return agentInstance -> {
        };
    }

    @Test
    public void shouldApproveAgent() {
        AgentInstance pending = AgentInstanceMother.pending();
        agentService.requestRegistration(new Username("bob"), AgentRuntimeInfo.fromServer(pending.agentConfig(), false, "var/lib", 0L, "linux"));
        agentService.approve(pending.getUuid());

        assertThat(agentService.findRegisteredAgents().size(), is(1));
        assertThat(agentService.findAgentAndRefreshStatus(pending.agentConfig().getUuid()).agentConfig().isDisabled(), is(false));
        assertThat(agentDao.agentByUuid(pending.getUuid()).isDisabled(), is(false));
    }

    @Test
    public void shouldAddOrUpdateAgent() {
        AgentInstance pending = AgentInstanceMother.pending();
        agentService.requestRegistration(new Username("bob"), AgentRuntimeInfo.fromServer(pending.agentConfig(), false, "var/lib", 0L, "linux"));

        agentService.approve(pending.getUuid());

        agentService.requestRegistration(new Username("bob"), AgentRuntimeInfo.fromServer(pending.agentConfig(), true, "var/lib", 0L, "linux"));
        agentService.requestRegistration(new Username("bob"), AgentRuntimeInfo.fromServer(pending.agentConfig(), true, "var/lib", 0L, "linux"));
        assertThat(agentService.findRegisteredAgents().size(), is(1));
    }

    @Test
    public void shouldDenyAgentFromPendingList() {
        AgentInstance pending = AgentInstanceMother.pending();
        agentService.requestRegistration(new Username("bob"), AgentRuntimeInfo.fromServer(pending.agentConfig(), false, "var/lib", 0L, "linux", false));

        String uuid = pending.getUuid();

        HttpLocalizedOperationResult operationResult = new HttpLocalizedOperationResult();
        agentService.bulkUpdateAgentAttributes(USERNAME, operationResult, Arrays.asList(uuid), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), TriState.FALSE);

        assertThatAgentIsDisabled(operationResult, uuid);

        AgentInstances agents = agentService.agentInstances();

        assertThat(agents.size(), is(1));
        assertThat(agents.size(), is(1));
        assertThat(agents.findAgent(uuid).isDisabled(), is(true));
        assertThat(agentService.findAgentAndRefreshStatus(uuid).isDisabled(), is(true));
        assertThat(agentService.findAgentAndRefreshStatus(uuid).getStatus(), is(AgentStatus.Disabled));
    }

    @Test
    public void shouldDenyApprovedAgent() {
        AgentConfig agentConfig = new AgentConfig(UUID, "agentName", "127.0.0.9", "cookie");
        agentDao.saveOrUpdate(agentConfig);
        AgentConfig agentFromDB = agentDao.agentByUuid(UUID);
        assertThat(agentFromDB.isDisabled(), is(false));

        HttpLocalizedOperationResult operationResult = new HttpLocalizedOperationResult();
        agentService.bulkUpdateAgentAttributes(USERNAME, operationResult, Arrays.asList(UUID), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), TriState.FALSE);

        assertThatAgentIsDisabled(operationResult, UUID);
        assertThat(agentService.agents().get(0).isDisabled(), is(true));
    }

    private void assertThatAgentIsDisabled(HttpLocalizedOperationResult operationResult, String uuid) {
        assertThat(operationResult.httpCode(), is(200));
        assertThat(operationResult.message(), is(String.format("Updated agent(s) with uuid(s): [%s].", uuid)));
    }

    @Test
    public void shouldDenyCorrectAgentWhenTwoOnSameBox() {
        AgentConfig agent1 = new AgentConfig(UUID, "agentName", "127.0.0.9", "cookie");
        AgentConfig agent2 = new AgentConfig(UUID2, "agentName", "127.0.0.9", "cookie2");

        agentDao.saveOrUpdate(agent1);
        agentDao.saveOrUpdate(agent2);

        assertThat(agentDao.agentByUuid(UUID).isDisabled(), is(false));
        assertThat(agentDao.agentByUuid(UUID2).isDisabled(), is(false));

        HttpLocalizedOperationResult operationResult = new HttpLocalizedOperationResult();
        agentService.bulkUpdateAgentAttributes(USERNAME, operationResult, Arrays.asList(UUID2), Collections.emptyList(),
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
                TriState.FALSE);

        assertThat(agentService.agents().getAgentByUuid(UUID).isDisabled(), is(false));
        assertThat(agentService.agents().getAgentByUuid(UUID2).isDisabled(), is(true));
    }

    @Test
    public void shouldBeAbleToDenyBuildingAgent() {
        String agentName = "agentName";
        String agentId = DatabaseAccessHelper.AGENT_UUID;
        AgentConfig agentConfig = new AgentConfig(agentId, agentName, "127.0.0.9");
        addAgent(agentConfig);
        AgentIdentifier agentIdentifier = agentConfig.getAgentIdentifier();
        String cookie = agentService.assignCookie(agentIdentifier);
        AgentRuntimeInfo agentRuntimeInfo = new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), cookie);
        agentRuntimeInfo.busy(new AgentBuildingInfo("path", "buildLocator"));

        agentService.updateRuntimeInfo(agentRuntimeInfo);

        HttpLocalizedOperationResult operationResult = new HttpLocalizedOperationResult();
        agentService.bulkUpdateAgentAttributes(USERNAME, operationResult, Arrays.asList(agentId), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), TriState.FALSE);

        assertThatAgentIsDisabled(operationResult, agentId);
    }

    @Test
    public void shouldReturn200OnTryingToDisableADisabledAgent() {
        String agentName = "agentName";
        String agentId = DatabaseAccessHelper.AGENT_UUID;
        AgentConfig agentConfig = new AgentConfig(agentId, agentName, "127.0.0.9");
        addAgent(agentConfig);
        disable(agentConfig);

        HttpLocalizedOperationResult operationResult = new HttpLocalizedOperationResult();
        agentService.bulkUpdateAgentAttributes(USERNAME, operationResult, Arrays.asList(agentId), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), TriState.FALSE);

        assertThat(operationResult.httpCode(), is(200));
        assertThat(operationResult.message(), is(String.format("Updated agent(s) with uuid(s): [%s].", agentId)));
    }

    @Test
    public void shouldSetErrorState404OnTryingToDisableUnknownAgent() {
        // pending matches this as well
        String agentId = "unknown-agent-id";

        HttpLocalizedOperationResult operationResult = new HttpLocalizedOperationResult();
        agentService.bulkUpdateAgentAttributes(USERNAME, operationResult, Arrays.asList(agentId), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), TriState.FALSE);

        assertThat(operationResult.httpCode(), is(400));
        assertThat(operationResult.message(), is("Agents with uuids 'unknown-agent-id' were not found!"));
    }

    @Test
    public void shouldNotAllowDisablingAgentWhenNotAdmin() {
        String agentId = "agent-id";
        CONFIG_HELPER.enableSecurity();
        CONFIG_HELPER.addAdmins("admin1");

        HttpLocalizedOperationResult operationResult = new HttpLocalizedOperationResult();
        agentService.bulkUpdateAgentAttributes(new Username(new CaseInsensitiveString("not-admin")), operationResult, Arrays.asList(agentId), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), TriState.FALSE);

        assertThat(operationResult.httpCode(), is(403));
        assertThat(operationResult.message(), is("Unauthorized to edit."));
    }

    @Test
    public void shouldNotAllowAddingResourcesWhenNotAdmin() {
        String agentId = "agent-id";
        CONFIG_HELPER.enableSecurity();
        CONFIG_HELPER.addAdmins("admin1");

        HttpLocalizedOperationResult operationResult = new HttpLocalizedOperationResult();
        agentService.bulkUpdateAgentAttributes(new Username(new CaseInsensitiveString("not-admin")), operationResult, Arrays.asList(agentId), Arrays.asList("dont-care"), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), TriState.UNSET);

        assertThat(operationResult.httpCode(), is(403));
        assertThat(operationResult.message(), is("Unauthorized to edit."));
    }

    @Test
    public void shouldAllowDisablingAgentWhenPending() {
        String agentName = "agentName";
        String agentId = DatabaseAccessHelper.AGENT_UUID;
        AgentConfig agentConfig = new AgentConfig(agentId, agentName, "50.40.30.9");
        AgentRuntimeInfo agentRuntimeInfo = new AgentRuntimeInfo(agentConfig.getAgentIdentifier(), AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie");
        agentRuntimeInfo.busy(new AgentBuildingInfo("path", "buildLocator"));
        agentService.requestRegistration(new Username("bob"), agentRuntimeInfo);

        HttpLocalizedOperationResult operationResult = new HttpLocalizedOperationResult();
        agentService.bulkUpdateAgentAttributes(USERNAME, operationResult, Arrays.asList(agentId), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), TriState.FALSE);

        assertThat(operationResult.httpCode(), is(200));
        assertThat(operationResult.message(), is(String.format("Updated agent(s) with uuid(s): [%s].", agentId)));
    }

    @Test
    public void shouldAllowEnableOfPendingAgent() {
        String agentName = "agentName";
        String agentId = DatabaseAccessHelper.AGENT_UUID;
        AgentConfig agentConfig = new AgentConfig(agentId, agentName, "50.40.30.9");
        AgentRuntimeInfo agentRuntimeInfo = new AgentRuntimeInfo(agentConfig.getAgentIdentifier(), AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie");
        agentRuntimeInfo.busy(new AgentBuildingInfo("path", "buildLocator"));
        agentService.requestRegistration(new Username("bob"), agentRuntimeInfo);

        HttpLocalizedOperationResult operationResult = new HttpLocalizedOperationResult();
        agentService.bulkUpdateAgentAttributes(USERNAME, operationResult, Arrays.asList(agentId), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), TriState.TRUE);

        assertThat(operationResult.httpCode(), is(200));
        assertThat(operationResult.message(), is(String.format("Updated agent(s) with uuid(s): [%s].", agentId)));
    }

    @Test
    public void shouldAllowEnablingOfADisabledAgent() {
        String agentName = "agentName";
        String agentId = DatabaseAccessHelper.AGENT_UUID;
        AgentConfig agentConfig = new AgentConfig(agentId, agentName, "127.0.0.9");
        addAgent(agentConfig);
        disable(agentConfig);

        HttpLocalizedOperationResult operationResult = new HttpLocalizedOperationResult();
        agentService.bulkUpdateAgentAttributes(USERNAME, operationResult, Arrays.asList(agentId), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), TriState.TRUE);

        assertThat(operationResult.httpCode(), is(200));
        assertThat(operationResult.message(), is(String.format("Updated agent(s) with uuid(s): [%s].", agentId)));
        assertThat(isDisabled(agentConfig), is(false));
    }

    @Test
    public void shouldEnableMultipleAgents() {
        AgentConfig agentConfig1 = createDisabledAgent(UUID);
        AgentConfig agentConfig2 = createDisabledAgent(UUID2);

        HttpLocalizedOperationResult operationResult = new HttpLocalizedOperationResult();
        agentService.bulkUpdateAgentAttributes(USERNAME, operationResult, Arrays.asList(UUID, UUID2), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), TriState.TRUE);

        assertThat(operationResult.httpCode(), is(200));
        assertThat(isDisabled(agentConfig1), is(false));
        assertThat(isDisabled(agentConfig2), is(false));
        assertThat(operationResult.message(), containsString("Updated agent(s) with uuid(s): [uuid, uuid2]."));
    }

    @Test
    public void shouldDisableMultipleAgents() {
        AgentConfig agentConfig1 = createEnabledAgent(UUID);
        AgentConfig agentConfig2 = createEnabledAgent(UUID2);

        HttpLocalizedOperationResult operationResult = new HttpLocalizedOperationResult();
        agentService.bulkUpdateAgentAttributes(USERNAME, operationResult, Arrays.asList(UUID, UUID2), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), TriState.FALSE);

        assertThat(operationResult.httpCode(), is(200));
        assertThat(isDisabled(agentConfig1), is(true));
        assertThat(isDisabled(agentConfig2), is(true));
        assertThat(operationResult.message(), containsString("Updated agent(s) with uuid(s): [uuid, uuid2]."));
    }

    @Test
    public void shouldReturn403WhenAUnauthorizedUserTriesToEnable() {
        String agentId = "agent-id";
        CONFIG_HELPER.enableSecurity();
        CONFIG_HELPER.addAdmins("admin1");

        HttpLocalizedOperationResult operationResult = new HttpLocalizedOperationResult();
        agentService.bulkUpdateAgentAttributes(new Username(new CaseInsensitiveString("not-admin")), operationResult, Arrays.asList(agentId), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), TriState.TRUE);

        assertThat(operationResult.httpCode(), is(403));
        assertThat(operationResult.message(), is("Unauthorized to edit."));
    }

    @Test
    public void shouldReturn404WhenAgentUUIDNotKnown() {
        // pending matches that as well
        String agentId = "unknown-agent-id";

        HttpLocalizedOperationResult operationResult = new HttpLocalizedOperationResult();
        agentService.bulkUpdateAgentAttributes(USERNAME, operationResult, Arrays.asList(agentId), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), TriState.TRUE);

        assertThat(operationResult.httpCode(), is(400));
        assertThat(operationResult.message(), is("Agents with uuids 'unknown-agent-id' were not found!"));
    }

    @Test
    public void shouldReturn200WhenAnAlreadyEnableAgentIsEnabled() {
        String agentName = "agentName";
        String agentId = DatabaseAccessHelper.AGENT_UUID;
        AgentConfig agentConfig = new AgentConfig(agentId, agentName, "127.0.0.9");
        addAgent(agentConfig);

        HttpLocalizedOperationResult operationResult = new HttpLocalizedOperationResult();
        agentService.bulkUpdateAgentAttributes(USERNAME, operationResult, Arrays.asList(agentId), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), TriState.TRUE);

        assertThat(operationResult.httpCode(), is(200));
        assertThat(operationResult.message(), is(String.format("Updated agent(s) with uuid(s): [%s].", agentId)));
    }

    @Test
    public void shouldRegisterLocalAgentWithNonLoopbackIpAddress() throws Exception {
        String nonLoopbackIp = SystemUtil.getFirstLocalNonLoopbackIpAddress();
        InetAddress inetAddress = InetAddress.getByName(nonLoopbackIp);
        assertThat(SystemUtil.isLocalIpAddress(nonLoopbackIp), is(true));
        AgentConfig agentConfig = new AgentConfig("uuid", inetAddress.getHostName(), nonLoopbackIp);
        AgentRuntimeInfo agentRuntimeInfo = AgentRuntimeInfo.fromServer(agentConfig, false, "/var/lib", 0L, "linux");
        agentService.requestRegistration(new Username("bob"), agentRuntimeInfo);

        AgentInstance agentInstance = agentService.findRegisteredAgents().findAgentAndRefreshStatus("uuid");

        assertThat(agentInstance.agentConfig().getIpaddress(), is(nonLoopbackIp));
        assertThat(agentInstance.getStatus(), is(AgentStatus.Idle));
    }

    @Test
    public void shouldLoadAgentsByApprovalStatus() {
        AgentConfig deniedAgent1 = new AgentConfig("uuid1", "deniedAgent1", "127.0.0.1", "cookie1");
        deniedAgent1.disable();

        agentDao.saveOrUpdate(deniedAgent1);
        AgentConfig deniedAgent2 = new AgentConfig("uuid2", "deniedAgent2", "127.0.0.2", "cookie1");
        deniedAgent2.disable();
        agentDao.saveOrUpdate(deniedAgent2);

        agentDao.saveOrUpdate(new AgentConfig("uuid3", "approvedAgent1", "127.0.0.3", "cookie1"));
        goConfigDao.load();

        agentService.initialize();

        AgentInstances approvedAgents = agentService.findEnabledAgents();
        assertThat(approvedAgents.size(), is(1));
        assertThat(approvedAgents.findAgentAndRefreshStatus("uuid3").agentConfig().getHostname(), is("approvedAgent1"));

        AgentInstances deniedAgents = agentService.findDisabledAgents();
        assertThat(deniedAgents.size(), is(2));
        assertThat(deniedAgents.findAgentAndRefreshStatus("uuid1").agentConfig().getHostname(), is("deniedAgent1"));
        assertThat(deniedAgents.findAgentAndRefreshStatus("uuid2").agentConfig().getHostname(), is("deniedAgent2"));
    }

    @Test
    // #2651
    public void shouldReturnFalseIfAgentBuildIsNotCancelled() {
        assertThat(agentService.findAgentAndRefreshStatus("a-new-agent-uuid").isCancelled(), is(false));
    }

    @Test
    public void shouldDenyAgentWhenAgentChangedToDenyInConfigFile() {
        disableAgent();

        AgentInstance instance = agentService.findAgentAndRefreshStatus("uuid1");
        assertThat(instance.getStatus(), is(AgentStatus.Disabled));

    }

    @Test
    public void shouldChangeAgentToIdleWhenAgentIsReApprovedInConfigFile() {
        disableAgent();

        agentService.updateAgentApprovalStatus("uuid1", false, Username.ANONYMOUS);

        AgentInstance instance = agentService.findAgentAndRefreshStatus("uuid1");
        assertThat(instance.getStatus(), is(AgentStatus.Idle));

    }

    @Test
    public void enabledAgents_shouldNotIncludePendingAgents() {
        AgentInstance idle = AgentInstanceMother.updateUuid(AgentInstanceMother.idle(new Date(), "CCeDev01"), UUID);
        AgentInstance pending = AgentInstanceMother.pending();
        AgentInstance building = AgentInstanceMother.building();
        AgentInstance denied = AgentInstanceMother.disabled();
        createEnvironment("uat");
        EnvironmentConfig environment = environmentConfigService.named("uat");
        environment.addAgent(UUID);
        AgentInstances instances = new AgentInstances(new SystemEnvironment(), agentStatusChangeListener(), idle, pending, building, denied);
        AgentService agentService = getAgentService(instances);

        AgentsViewModel agents = agentService.registeredAgents();
        assertThat(agents.size(), is(3));
        for (AgentViewModel agent : agents) {
            assertThat(agent.getStatus().getConfigStatus(), not(is(AgentConfigStatus.Pending)));
        }
    }

    @Test
    public void shouldReturn403WhenAUnauthorizedUserTriesToDelete() {
        CONFIG_HELPER.enableSecurity();
        HttpOperationResult operationResult = new HttpOperationResult();
        CONFIG_HELPER.addAdmins("admin1");
        agentService.deleteAgents(new Username(new CaseInsensitiveString("not-admin")), operationResult, Arrays.asList(UUID));
        assertThat(operationResult.httpCode(), is(403));
        assertThat(operationResult.message(), is("Unauthorized to operate on agent"));
    }

    @Test
    public void shouldDeleteOnlyDisabledAgentGivenUUID() {
        AgentConfig disabledAgent = createDisabledAndIdleAgent(UUID);
        AgentConfig enabledAgent = createEnabledAgent(UUID2);

        goConfigDao.load();
        assertThat(agentService.agentInstances().size(), is(2));

        HttpOperationResult disabledAgentOperationResult = new HttpOperationResult();
        HttpOperationResult enabledAgentOperationResult = new HttpOperationResult();

        agentService.deleteAgents(USERNAME, disabledAgentOperationResult, Arrays.asList(disabledAgent.getUuid()));
        agentService.deleteAgents(USERNAME, enabledAgentOperationResult, Arrays.asList(enabledAgent.getUuid()));

        assertThat(disabledAgentOperationResult.httpCode(), is(200));
        assertThat(disabledAgentOperationResult.message(), is("Deleted 1 agent(s)."));

        assertThat(enabledAgentOperationResult.httpCode(), is(406));
        assertThat(enabledAgentOperationResult.message(), is("Failed to delete 1 agent(s), as agent(s) might not be disabled or are still building."));

        assertThat(agentService.agentInstances().size(), is(1));
        assertTrue(agentService.agentInstances().hasAgent(UUID2));
    }

    @Test
    public void shouldNOTDeleteDisabledAgentThatIsBuildingGivenUUID() {
        AgentConfig disabledButBuildingAgent = createDisabledAgent(UUID);

        goConfigDao.load();
        assertThat(agentService.agentInstances().size(), is(1));

        HttpOperationResult operationResult = new HttpOperationResult();

        agentService.deleteAgents(USERNAME, operationResult, Arrays.asList(disabledButBuildingAgent.getUuid()));

        assertThat(operationResult.httpCode(), is(406));
        assertThat(operationResult.message(), is("Failed to delete 1 agent(s), as agent(s) might not be disabled or are still building."));

        assertThat(agentService.agentInstances().size(), is(1));
        assertTrue(agentService.agentInstances().hasAgent(UUID));
    }

    @Test
    public void shouldReturn403WhenAUnauthorizedUserTriesToDeleteAgents() {
        CONFIG_HELPER.enableSecurity();
        HttpOperationResult operationResult = new HttpOperationResult();
        CONFIG_HELPER.addAdmins("admin1");
        agentService.deleteAgents(new Username(new CaseInsensitiveString("not-admin")), operationResult, Arrays.asList(UUID));
        assertThat(operationResult.httpCode(), is(403));
        assertThat(operationResult.message(), is("Unauthorized to operate on agent"));
    }

    @Test
    public void shouldReturn404WhenAgentUUIDNotKnownForDeleteAgents() {
        String agentId = "unknown-agent-id";
        HttpOperationResult operationResult = new HttpOperationResult();
        agentService.deleteAgents(USERNAME, operationResult, Arrays.asList(agentId));
        assertThat(operationResult.httpCode(), is(404));
        assertThat(operationResult.message(), is("Agent not found."));
    }

    @Test
    public void shouldDeleteAgentsGivenListOfUUIDs() {
        AgentConfig disabledAgent1 = createDisabledAndIdleAgent(UUID);
        AgentConfig disabledAgent2 = createDisabledAndIdleAgent(UUID2);

        goConfigDao.load();
        assertThat(agentService.agentInstances().size(), is(2));

        HttpOperationResult operationResult = new HttpOperationResult();

        agentService.deleteAgents(USERNAME, operationResult, Arrays.asList(disabledAgent1.getUuid(), disabledAgent2.getUuid()));

        assertThat(operationResult.httpCode(), is(200));
        assertThat(operationResult.message(), is("Deleted 2 agent(s)."));

        assertThat(agentService.agentInstances().size(), is(0));
    }

    @Test
    public void shouldNOTDeleteAnyAgentIfAtLeastOneOfTheRequestedAgentIsNotDisabled() {
        AgentConfig disabledAgent = createDisabledAndIdleAgent(UUID);
        AgentConfig enabledAgent = createEnabledAgent(UUID2);

        goConfigDao.load();
        assertThat(agentService.agentInstances().size(), is(2));

        HttpOperationResult operationResult = new HttpOperationResult();

        agentService.deleteAgents(USERNAME, operationResult, Arrays.asList(disabledAgent.getUuid(), enabledAgent.getUuid()));

        assertThat(operationResult.httpCode(), is(406));
        assertThat(operationResult.message(), is("Failed to delete 2 agent(s), as agent(s) might not be disabled or are still building."));
        assertThat(agentService.agentInstances().size(), is(2));
    }

    @Test
    public void updateAgentAttributesShouldUpdateAnAgentHostname() {
        createDisabledAndIdleAgent(UUID);

        goConfigDao.load();

        assertThat(agentService.agentInstances().size(), is(1));
        assertThat(getFirstAgent().getHostname(), is(not("some-hostname")));

        HttpOperationResult operationResult = new HttpOperationResult();
        agentService.updateAgentAttributes(USERNAME, operationResult, UUID, "some-hostname", null, null, TriState.UNSET);

        assertThat(operationResult.httpCode(), is(200));
        assertThat(operationResult.message(), is("Updated agent with uuid uuid."));

        assertThat(agentService.agentInstances().size(), is(1));
        assertThat(getFirstAgent().getHostname(), is("some-hostname"));
    }


    @Test
    public void updateAgentAttributesShouldUpdateAnAgentResources() {
        createDisabledAndIdleAgent(UUID);

        goConfigDao.load();

        assertThat(agentService.agentInstances().size(), is(1));
        assertThat(getFirstAgent().getResourceConfigs(), is(empty()));

        HttpOperationResult operationResult = new HttpOperationResult();
        agentService.updateAgentAttributes(USERNAME, operationResult, UUID, null, "linux,java", null, TriState.UNSET);

        assertThat(operationResult.httpCode(), is(200));
        assertThat(operationResult.message(), is("Updated agent with uuid uuid."));

        assertThat(agentService.agentInstances().size(), is(1));
        assertThat(getFirstAgent().getResourceConfigs().resourceNames(), is(Arrays.asList("java", "linux")));
    }


    @Test
    public void updateAgentAttributesShouldUpdateAnAgentEnvironments() {
        createEnvironment("a", "b", "c", "d", "e");
        createEnabledAgent(UUID);

        HttpLocalizedOperationResult operationResult = new HttpLocalizedOperationResult();
        agentService.bulkUpdateAgentAttributes(USERNAME, operationResult, Arrays.asList(UUID), Collections.emptyList(), Collections.emptyList(), Arrays.asList("a", "b", "c"), Collections.emptyList(), TriState.UNSET);

        assertThat(operationResult.httpCode(), is(200));

        goConfigDao.load();

        assertThat(agentService.agentInstances().size(), is(1));
        assertThat(getFirstAgent().getResourceConfigs(), is(empty()));

        HttpOperationResult operationResult1 = new HttpOperationResult();
        agentService.updateAgentAttributes(USERNAME, operationResult1, UUID, null, null, "c,d,e", TriState.UNSET);

        assertThat(operationResult1.httpCode(), is(200));
        assertThat(operationResult1.message(), is("Updated agent with uuid uuid."));

        assertThat(agentService.agentInstances().size(), is(1));
        assertThat(getEnvironments(getFirstAgent().getUuid()).equals(new HashSet<>(Arrays.asList("c", "d", "e"))), is(true));
    }


    @Test
    public void updateAgentAttributesShouldUpdateAnAgentEnableState() {
        createDisabledAndIdleAgent(UUID);

        goConfigDao.load();

        assertThat(agentService.agentInstances().size(), is(1));
        assertThat(getFirstAgent().isDisabled(), is(true));

        HttpOperationResult operationResult = new HttpOperationResult();
        agentService.updateAgentAttributes(USERNAME, operationResult, UUID, null, null, null, TriState.TRUE);

        assertThat(operationResult.httpCode(), is(200));
        assertThat(operationResult.message(), is("Updated agent with uuid uuid."));

        assertThat(agentService.agentInstances().size(), is(1));
        assertThat(getFirstAgent().isDisabled(), is(false));
    }

    @Test
    public void updateAgentAttributesShouldUpdateAnAgentDisableState() {
        createEnabledAgent(UUID);

        goConfigDao.load();

        assertThat(agentService.agentInstances().size(), is(1));
        assertThat(getFirstAgent().isDisabled(), is(false));

        HttpOperationResult operationResult = new HttpOperationResult();
        agentService.updateAgentAttributes(USERNAME, operationResult, UUID, null, null, null, TriState.FALSE);

        assertThat(operationResult.httpCode(), is(200));
        assertThat(operationResult.message(), is("Updated agent with uuid uuid."));

        assertThat(agentService.agentInstances().size(), is(1));
        assertThat(getFirstAgent().isDisabled(), is(true));
    }

    @Test
    public void updateAgentAttributesShouldNotUpdateAgentEnableStateIfTristateIsNotDefined() {
        createEnabledAgent("enabled");
        createDisabledAgent("disabled");

        goConfigDao.load();

        assertThat(agentService.agentInstances().size(), is(2));
        assertThat(agentService.findAgentAndRefreshStatus("enabled").agentConfig().isDisabled(), is(false));
        assertThat(agentService.findAgentAndRefreshStatus("disabled").agentConfig().isDisabled(), is(true));

        HttpOperationResult operationResult = new HttpOperationResult();
        agentService.updateAgentAttributes(USERNAME, operationResult, "enabled", "new.enabled.hostname", "linux,java", null, TriState.UNSET);
        agentService.updateAgentAttributes(USERNAME, operationResult, "disabled", "new.disabled.hostname", "linux,java", null, TriState.UNSET);

        assertThat(operationResult.httpCode(), is(200));

        assertThat(agentService.agentInstances().size(), is(2));
        assertThat(agentService.findAgentAndRefreshStatus("enabled").agentConfig().isDisabled(), is(false));
        assertThat(agentService.findAgentAndRefreshStatus("disabled").agentConfig().isDisabled(), is(true));
    }

    @Test
    public void shouldUpdateAgentAttributesForValidInputs() throws Exception {
        String headCommitBeforeUpdate = configRepository.getCurrentRevCommit().name();
        createDisabledAndIdleAgent(UUID);
        createEnvironment("a", "b");

        goConfigDao.load();

        assertThat(agentService.agentInstances().size(), is(1));
        assertThat(getFirstAgent().getHostname(), is(not("some-hostname")));
        assertThat(getFirstAgent().isDisabled(), is(true));

        HttpOperationResult operationResult = new HttpOperationResult();
        agentService.updateAgentAttributes(USERNAME, operationResult, UUID, "some-hostname", "linux,java", "a,b", TriState.UNSET);

        assertThat(operationResult.httpCode(), is(200));
        assertThat(operationResult.message(), is("Updated agent with uuid uuid."));

        assertThat(agentService.agentInstances().size(), is(1));
        assertThat(getFirstAgent().getHostname(), is("some-hostname"));
        assertThat(getFirstAgent().getResourceConfigs().resourceNames(), equalTo(Arrays.asList("java", "linux")));
        assertThat(getFirstAgent().isDisabled(), is(true));
        assertEquals(getEnvironments(getFirstAgent().getUuid()), new HashSet<>(Arrays.asList("a", "b")));
//        assertThat(configRepository.getCurrentRevCommit().name(), is(not(headCommitBeforeUpdate)));
//        assertThat(configRepository.getCurrentRevision().getUsername(), is(USERNAME.getDisplayName()));
    }

    @Test
    public void shouldNotUpdateAgentAttributesHostnameOrEnvironmentsIfNoneAreSpecified() {
        createEnvironment("a", "b");
        AgentConfig agent = createDisabledAndIdleAgent(UUID);
        String originalHostname = agent.getHostname();

        HttpLocalizedOperationResult operationResult = new HttpLocalizedOperationResult();
        agentService.bulkUpdateAgentAttributes(USERNAME, operationResult, Arrays.asList(UUID), Collections.emptyList(), Collections.emptyList(), Arrays.asList("a", "b"), Collections.emptyList(), TriState.TRUE);

        goConfigDao.load();

        assertThat(agentService.agentInstances().size(), is(1));

        HttpOperationResult operationResult1 = new HttpOperationResult();
        agentService.updateAgentAttributes(USERNAME, operationResult1, UUID, null, null, null, TriState.UNSET);

        assertThat(operationResult1.httpCode(), is(200));
        assertThat(operationResult1.message(), is("Updated agent with uuid uuid."));

        assertThat(agentService.agentInstances().size(), is(1));
        assertThat(getFirstAgent().getHostname(), is(originalHostname));
        assertEquals(getEnvironments(getFirstAgent().getUuid()), new HashSet<>(Arrays.asList("a", "b")));
    }

    @Test
    public void testShouldThrowErrorOnUpdatingAgentOnInvalidInputs() {
        AgentConfig agent = createDisabledAndIdleAgent(UUID);
        String originalHostname = agent.getHostname();
        List<String> originalResourceNames = agent.getResourceConfigs().resourceNames();

        goConfigDao.load();

        assertThat(agentService.agentInstances().size(), is(1));
        assertThat(getFirstAgent().getHostname(), is(not("some-hostname")));

        HttpOperationResult operationResult = new HttpOperationResult();
        AgentInstance agentInstance = agentService.updateAgentAttributes(USERNAME, operationResult, UUID, "some-hostname", "lin!ux,asdh*", null, TriState.UNSET);

        assertThat(operationResult.httpCode(), is(422));
        assertThat(operationResult.message(), is("Updating agent failed:"));
        assertThat(agentInstance.agentConfig().errors().on(JobConfig.RESOURCES), is("Resource name 'lin!ux' is not valid. Valid names much match '^[-\\w\\s|.]*$'"));

        assertThat(agentService.agentInstances().size(), is(1));
        assertThat(getFirstAgent().getHostname(), is(originalHostname));
        assertThat(getFirstAgent().getResourceConfigs().resourceNames(), is(originalResourceNames));
    }

    @Test
    public void shouldNOTDeleteAgentsIfAtLeastOneAgentIsBuildingGivenListOfUUIDs() {
        AgentConfig disabledButBuildingAgent = createDisabledAgent(UUID);
        AgentConfig disabledAgent1 = createDisabledAndIdleAgent(UUID2);

        goConfigDao.load();
        assertThat(agentService.agentInstances().size(), is(2));

        HttpOperationResult operationResult = new HttpOperationResult();

        agentService.deleteAgents(USERNAME, operationResult, Arrays.asList(disabledAgent1.getUuid(), disabledButBuildingAgent.getUuid()));

        assertThat(operationResult.httpCode(), is(406));
        assertThat(operationResult.message(), is("Failed to delete 2 agent(s), as agent(s) might not be disabled or are still building."));

        assertThat(agentService.agentInstances().size(), is(2));
    }

    private void createEnvironment(String... environmentNames) {
        CONFIG_HELPER.addEnvironments(environmentNames);
        goConfigService.forceNotifyListeners();
    }

    private AgentConfig createEnabledAgent(String uuid) {
        AgentConfig agentConfig = new AgentConfig(uuid, "agentName", "127.0.0.9", uuid);
        addAgent(agentConfig);
        return agentConfig;
    }

    private void disable(AgentConfig agentConfig) {
        AgentIdentifier agentIdentifier = agentConfig.getAgentIdentifier();
        String cookie = agentService.assignCookie(agentIdentifier);
        AgentRuntimeInfo agentRuntimeInfo = new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), cookie);
        agentRuntimeInfo.busy(new AgentBuildingInfo("path", "buildLocator"));
        agentService.updateRuntimeInfo(agentRuntimeInfo);

        HttpLocalizedOperationResult operationResult = new HttpLocalizedOperationResult();
        agentService.bulkUpdateAgentAttributes(USERNAME, operationResult, Arrays.asList(agentConfig.getUuid()), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), TriState.FALSE);

        assertThat(isDisabled(agentConfig), is(true));
    }

    private void disableAgent() {
        AgentConfig pending = new AgentConfig("uuid1", "agent1", "192.168.0.1");
        agentService.requestRegistration(new Username("bob"), AgentRuntimeInfo.fromServer(pending, false, "/var/lib", 0L, "linux"));
        agentService.approve("uuid1");
        agentService.updateAgentApprovalStatus("uuid1", true, Username.ANONYMOUS);
    }

    private boolean isDisabled(AgentConfig agentConfig) {
        return agentService.findAgentAndRefreshStatus(agentConfig.getUuid()).isDisabled();
    }

    public void addAgent(AgentConfig agentConfig) {
        agentService.requestRegistration(new Username("bob"), AgentRuntimeInfo.fromServer(agentConfig, false, "/var/lib", 0L, "linux"));
        agentService.approve(agentConfig.getUuid());
    }

    private AgentConfig createDisabledAgent(String uuid) {
        AgentConfig agentConfig = new AgentConfig(uuid, "agentName", "127.0.0.9");
        addAgent(agentConfig);
        disable(agentConfig);
        return agentConfig;
    }

    private AgentConfig createDisabledAndIdleAgent(String uuid) {
        AgentConfig agentConfig = new AgentConfig(uuid, "agentName", "127.0.0.9");
        addAgent(agentConfig);

        AgentIdentifier agentIdentifier = agentConfig.getAgentIdentifier();
        String cookie = agentService.assignCookie(agentIdentifier);
        AgentRuntimeInfo agentRuntimeInfo = new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), cookie);
        agentRuntimeInfo.idle();
        agentService.updateRuntimeInfo(agentRuntimeInfo);
        assertTrue(agentService.findAgentAndRefreshStatus(uuid).isIdle());

        HttpLocalizedOperationResult operationResult = new HttpLocalizedOperationResult();
        agentService.bulkUpdateAgentAttributes(USERNAME, operationResult, Arrays.asList(agentConfig.getUuid()), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), TriState.FALSE);

        AgentConfig updatedAgent = agentService.agentByUuid(agentConfig.getUuid());
        assertThat(isDisabled(updatedAgent), is(true));
        return updatedAgent;
    }

    private AgentInstance getFirstAgent() {
        for (AgentInstance agentInstance : agentService.agentInstances()) {
            return agentInstance;
        }
        return null;
    }

    private Set<String> getEnvironments(String uuid) {
        return environmentConfigService.environmentsFor(uuid);
    }

    private AgentStatusChangeNotifier agentStatusChangeNotifier() {
        return new MockAgentStatusChangeNotifier();
    }

    private class MockAgentStatusChangeNotifier extends AgentStatusChangeNotifier {
        public MockAgentStatusChangeNotifier() {
            super(null, null);
        }

        @Override
        public void onAgentStatusChange(AgentInstance agentInstance) {}
    }
}
