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
import com.thoughtworks.go.domain.AgentConfigStatus;
import com.thoughtworks.go.domain.AgentInstance;
import com.thoughtworks.go.domain.AgentRuntimeStatus;
import com.thoughtworks.go.domain.AgentStatus;
import com.thoughtworks.go.helper.AgentInstanceMother;
import com.thoughtworks.go.presentation.TriStateSelection;
import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.domain.AgentInstances;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.messaging.SendEmailMessage;
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
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.net.InetAddress;
import java.util.*;

import static com.thoughtworks.go.util.SystemUtil.currentWorkingDirectory;
import static java.lang.String.format;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:WEB-INF/applicationContext-acegi-security.xml"
})
public class AgentServiceIntegrationTest {
    @Autowired private GoConfigDao goConfigDao;
    @Autowired private AgentConfigService agentConfigService;
    @Autowired private EnvironmentConfigService environmentConfigService;
    @Autowired private AgentService agentService;
    @Autowired private GoConfigService goConfigService;
    @Autowired private CachedGoConfig cachedGoConfig;
    @Autowired private SecurityService securityService;
    @Autowired private ServerHealthService serverHealthService;
    @Autowired private AgentDao agentDao;
    @Autowired private ConfigRepository configRepository;

    private static final GoConfigFileHelper CONFIG_HELPER = new GoConfigFileHelper();
    private static final String UUID = "uuid";
    private static final String UUID2 = "uuid2";
    private static final String UUID3 = "uuid3";
    private static final Username USERNAME = new Username(new CaseInsensitiveString("admin"));

    @Before
    public void setUp() throws Exception {
        CONFIG_HELPER.usingCruiseConfigDao(goConfigDao);
        CONFIG_HELPER.onSetUp();
        cachedGoConfig.clearListeners();
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
    }

    private AgentService getAgentService(AgentInstances agentInstances) {
        return new AgentService(agentConfigService, new SystemEnvironment(), agentInstances, environmentConfigService, goConfigService, securityService, agentDao, new UuidGenerator(), serverHealthService);
    }

    @Test
    public void shouldAddResourcesToMultipleAgents() {
        createEnabledAgent(UUID);
        createEnabledAgent(UUID2);

        HttpOperationResult operationResult = new HttpOperationResult();
        agentService.modifyResources(USERNAME, operationResult, Arrays.asList(UUID, UUID2), Arrays.asList(new TriStateSelection("old-resource", TriStateSelection.Action.add),
                new TriStateSelection("new-resource", TriStateSelection.Action.add)));

        assertThat(operationResult.httpCode(), is(200));
        assertThat(operationResult.message(), is("Resource(s) modified on 2 agent(s)"));
        assertThat(agentService.findAgentAndRefreshStatus(UUID).agentConfig().getResources(),hasItem(new Resource("old-resource")));
        assertThat(agentService.findAgentAndRefreshStatus(UUID).agentConfig().getResources(),hasItem(new Resource("new-resource")));
        assertThat(agentService.findAgentAndRefreshStatus(UUID2).agentConfig().getResources(),hasItem(new Resource("old-resource")));
        assertThat(agentService.findAgentAndRefreshStatus(UUID2).agentConfig().getResources(),hasItem(new Resource("new-resource")));
    }

    @Test
    public void shouldAddEnvironmentsToMultipleAgents() throws Exception {
        createEnvironment("uat", "prod");

        createEnabledAgent(UUID);
        createEnabledAgent(UUID2);

        HttpOperationResult operationResult = new HttpOperationResult();
        agentService.modifyEnvironments(USERNAME, operationResult, Arrays.asList(UUID, UUID2), Arrays.asList(
                new TriStateSelection("uat", TriStateSelection.Action.add),
                new TriStateSelection("prod", TriStateSelection.Action.add)));

        assertThat(operationResult.httpCode(), is(200));
        assertThat(operationResult.message(), is("Environment(s) modified on 2 agent(s)"));
        assertThat(environmentConfigService.environmentsFor(UUID), containsSet("uat", "prod"));
        assertThat(environmentConfigService.environmentsFor(UUID2), containsSet("uat", "prod"));
    }

    @Test
    public void shouldNotFailTryingToAddAnAgentThatsAlreadyPresentInEnvironment() throws Exception {
        createEnvironment("uat");

        createEnabledAgent(UUID);
        createEnabledAgent(UUID2);

        CONFIG_HELPER.addAgentToEnvironment("uat", UUID);

        HttpOperationResult operationResult = new HttpOperationResult();
        agentService.modifyEnvironments(USERNAME, operationResult, Arrays.asList(UUID, UUID2), Arrays.asList(
                new TriStateSelection("uat", TriStateSelection.Action.add)));

        assertThat(operationResult.httpCode(), is(200));
        assertThat(environmentConfigService.environmentsFor(UUID), containsSet("uat"));
        assertThat(environmentConfigService.environmentsFor(UUID2), containsSet("uat"));
    }

    @Test
    public void shouldRemoveEnvironmentsFromMultipleAgents() throws Exception {
        createEnvironment("uat", "prod");

        createEnabledAgent(UUID);
        createEnabledAgent(UUID2);

        addAgentToEnv("uat", UUID);
        addAgentToEnv("prod", UUID2);

        HttpOperationResult operationResult = new HttpOperationResult();
        agentService.modifyEnvironments(USERNAME, operationResult, Arrays.asList(UUID, UUID2), Arrays.asList(
                new TriStateSelection("uat", TriStateSelection.Action.remove),
                new TriStateSelection("prod", TriStateSelection.Action.nochange)));

        assertThat(operationResult.httpCode(), is(200));
        assertThat(operationResult.message(), is("Environment(s) modified on 2 agent(s)"));
        assertThat(environmentConfigService.environmentsFor(UUID2), not(containsSet("uat")));
        assertThat(environmentConfigService.environmentsFor(UUID2), containsSet("prod"));
    }

    @Test
    public void shouldNotChangeEnvironmentsOtherThanTheOneRemoveIsRequestedFor() throws Exception {
        createEnvironment("uat", "prod");

        createEnabledAgent(UUID);
        addAgentToEnv("uat", UUID);
        addAgentToEnv("prod", UUID);

        HttpOperationResult operationResult = new HttpOperationResult();
        agentService.modifyEnvironments(USERNAME, operationResult, Arrays.asList(UUID), Arrays.asList(
                new TriStateSelection("uat", TriStateSelection.Action.remove)));

        assertThat(operationResult.httpCode(), is(200));
        assertThat(operationResult.message(), is("Environment(s) modified on 1 agent(s)"));
        assertThat(environmentConfigService.environmentsFor(UUID), not(containsSet("uat")));
        assertThat(environmentConfigService.environmentsFor(UUID), containsSet("prod"));
    }

    @Test
    public void shouldRespondToAgentEnvironmentModificationRequestWith406WhenErrors() throws Exception {

        createEnabledAgent(UUID);

        HttpOperationResult operationResult = new HttpOperationResult();
        agentService.modifyEnvironments(USERNAME, operationResult, Arrays.asList(UUID), Arrays.asList(
                new TriStateSelection("unknown_env", TriStateSelection.Action.add)));

        assertThat(operationResult.httpCode(), is(406));
        assertThat(operationResult.message(), containsString("Could not modify environments:"));
    }

    private TypeSafeMatcher<Set<String>> containsSet(final String... items) {
        return new TypeSafeMatcher<Set<String>>() {
            @Override public boolean matchesSafely(Set<String> item) {
                return item.containsAll(Arrays.asList(items));
            }

            public void describeTo(Description description) {
                description.appendText("to contain ").appendValue(items);
            }
        };
    }

    @Test
    public void shouldNotAllowUpdatingEnvironmentsWhenNotAdmin() throws IOException {
        String agentId = "agent-id";
        HttpOperationResult operationResult = new HttpOperationResult();
        CONFIG_HELPER.addSecurityWithPasswordFile();
        CONFIG_HELPER.addAdmins("admin1");
        agentService.modifyEnvironments(new Username(new CaseInsensitiveString("not-admin")), operationResult, Arrays.asList(UUID), Arrays.asList(new TriStateSelection("uat", TriStateSelection.Action.remove)));
        assertThat(operationResult.httpCode(), is(401));
        assertThat(operationResult.message(), is("Unauthorized to operate on agent"));
    }

    @Test
    public void shouldRemoveResourcesFromMultipleAgents() {
        createEnabledAgent(UUID);
        createEnabledAgent(UUID2);

        HttpOperationResult operationResult = new HttpOperationResult();
        agentService.modifyResources(USERNAME, operationResult, Arrays.asList(UUID, UUID2), Arrays.asList(new TriStateSelection("resource-1", TriStateSelection.Action.add),
                new TriStateSelection("resource-2", TriStateSelection.Action.add)));

        agentService.modifyResources(USERNAME, operationResult, Arrays.asList(UUID, UUID2), Arrays.asList(new TriStateSelection("resource-1", TriStateSelection.Action.remove)));

        assertThat(operationResult.httpCode(), is(200));
        assertThat(operationResult.message(), is("Resource(s) modified on 2 agent(s)"));
        assertThat(agentService.findAgentAndRefreshStatus(UUID).agentConfig().getResources(), hasItem(new Resource("resource-2")));
        assertThat(agentService.findAgentAndRefreshStatus(UUID).agentConfig().getResources(), not(hasItem(new Resource("resource-1"))));
        assertThat(agentService.findAgentAndRefreshStatus(UUID2).agentConfig().getResources(),hasItem(new Resource("resource-2")));
        assertThat(agentService.findAgentAndRefreshStatus(UUID2).agentConfig().getResources(),not(hasItem(new Resource("resource-1"))));

    }

    @Test
    public void shouldFindAnAgentForAGivenUUID() {
        createEnabledAgent(UUID);
        createEnabledAgent(UUID2);
        BasicEnvironmentConfig foo = new BasicEnvironmentConfig(new CaseInsensitiveString("foo"));
        foo.addAgent(UUID);
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        environmentConfigService.createEnvironment(foo, Username.ANONYMOUS, result);

        assertThat(result.isSuccessful(), is(true));

        AgentViewModel actual = agentService.findAgentViewModel(UUID);

        assertThat(actual, is(new AgentViewModel(agentService.findAgentAndRefreshStatus(UUID), "foo")));
    }

    @Test
    public void shouldNotChangeResourcesForNoChange() {
        createEnabledAgent(UUID);
        createEnabledAgent(UUID2);

        HttpOperationResult operationResult = new HttpOperationResult();
        agentService.modifyResources(USERNAME, operationResult, Arrays.asList(UUID), Arrays.asList(new TriStateSelection("resource-1", TriStateSelection.Action.add)));

        agentService.modifyResources(USERNAME, operationResult, Arrays.asList(UUID, UUID2), Arrays.asList(new TriStateSelection("resource-1", TriStateSelection.Action.nochange)));

        assertThat(operationResult.httpCode(), is(200));
        assertThat(operationResult.message(), is("Resource(s) modified on 2 agent(s)"));
        assertThat(agentService.findAgentAndRefreshStatus(UUID).agentConfig().getResources(),hasItem(new Resource("resource-1")));
        assertThat(agentService.findAgentAndRefreshStatus(UUID2).agentConfig().getResources(),not(hasItem(new Resource("resource-1"))));
    }

    @Test
    public void shouldListResourceSelectionForMultipleAgents() {
        createEnabledAgent(UUID);
        createEnabledAgent(UUID2);
        createEnabledAgent(UUID3);

        HttpOperationResult operationResult = new HttpOperationResult();
        agentService.modifyResources(USERNAME, operationResult, Arrays.asList(UUID, UUID2), Arrays.asList(new TriStateSelection("all", TriStateSelection.Action.add)));
        agentService.modifyResources(USERNAME, operationResult, Arrays.asList(UUID2), Arrays.asList(new TriStateSelection("few", TriStateSelection.Action.add)));
        agentService.modifyResources(USERNAME, operationResult, Arrays.asList(UUID3), Arrays.asList(new TriStateSelection("none", TriStateSelection.Action.add)));

        List<TriStateSelection> selections = agentService.getResourceSelections(Arrays.asList(UUID, UUID2));

        assertThat(selections.get(0), is(new TriStateSelection("all", TriStateSelection.Action.add)));
        assertThat(selections.get(1), is(new TriStateSelection("few", TriStateSelection.Action.nochange)));
        assertThat(selections.get(2), is(new TriStateSelection("none", TriStateSelection.Action.remove)));
    }

    @Test
    public void shouldListTriStateSelectionOfEnvironmentsForMultipleAgents() throws Exception {

        String uatAgentUuid = "uat-agent";
        String prodAgentUuid = "prod-agent";

        createEnabledAgent(uatAgentUuid);
        createEnabledAgent(prodAgentUuid);

        createEnvironment("uat", "prod");
        addAgentToEnv("uat", uatAgentUuid);
        addAgentToEnv("prod", prodAgentUuid);

        List<TriStateSelection> selections = agentService.getEnvironmentSelections(Arrays.asList(uatAgentUuid));

        assertThat(selections.size(), is(2));
        assertThat(selections.get(0), is(new TriStateSelection("prod", TriStateSelection.Action.remove)));
        assertThat(selections.get(1), is(new TriStateSelection("uat", TriStateSelection.Action.add)));

        selections = agentService.getEnvironmentSelections(Arrays.asList(uatAgentUuid, prodAgentUuid));
        assertThat(selections.get(0), is(new TriStateSelection("prod", TriStateSelection.Action.nochange)));
        assertThat(selections.get(1), is(new TriStateSelection("uat", TriStateSelection.Action.nochange)));
    }

    private void addAgentToEnv(String uat, String uatAgentUuid) throws Exception {
        CONFIG_HELPER.addAgentToEnvironment(uat, uatAgentUuid);
        goConfigService.forceNotifyListeners();
    }

    @Test
    public void shouldUpdateAgentStatus() throws Exception {
        AgentInstance instance = AgentInstanceMother.building();
        AgentService agentService = getAgentService(new AgentInstances(null, new SystemEnvironment(), instance));
        AgentInstances agents = agentService.findRegisteredAgents();

        String uuid = instance.agentConfig().getUuid();
        assertThat(agents.findAgentAndRefreshStatus(uuid).getStatus(), is(AgentStatus.Building));
        AgentIdentifier agentIdentifier = instance.agentConfig().getAgentIdentifier();
        String cookie = agentService.assignCookie(agentIdentifier);
        agentService.updateRuntimeInfo(new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), cookie, null, false));
        agents = agentService.findRegisteredAgents();
        assertThat(agents.findAgentAndRefreshStatus(uuid).getStatus(), is(AgentStatus.Idle));
    }

    @Test
    public void shouldThrowExceptionWhenADuplicateAgentTriesToUpdateStatus() throws Exception {
        AgentInstance instance = AgentInstanceMother.building();
        AgentService agentService = getAgentService(new AgentInstances(null, new SystemEnvironment(), instance));
        AgentInstances agents = agentService.findRegisteredAgents();

        String uuid = instance.agentConfig().getUuid();
        assertThat(agents.findAgentAndRefreshStatus(uuid).getStatus(), is(AgentStatus.Building));
        AgentIdentifier identifier = instance.agentConfig().getAgentIdentifier();
        agentDao.associateCookie(identifier, "new_cookie");
        AgentRuntimeInfo runtimeInfo = new AgentRuntimeInfo(identifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "old_cookie", null, false);
        try {
            agentService.updateRuntimeInfo(runtimeInfo);
            fail("agent with bad cookie should not be able to update runtime info");
        } catch (AgentWithDuplicateUUIDException e) {
            assertThat(e.getMessage(), is(format("Agent [%s] has invalid cookie", runtimeInfo.agentInfoDebugString())));
        }
        agents = agentService.findRegisteredAgents();
        assertThat(agents.findAgentAndRefreshStatus(uuid).getStatus(), is(AgentStatus.Building));AgentIdentifier agentIdentifier = instance.agentConfig().getAgentIdentifier();
        String cookie = agentService.assignCookie(agentIdentifier);
        agentService.updateRuntimeInfo(new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), cookie, null, false));
    }

    @Test
    public void shouldMarkAgentAsLostContactIfAgentDidNotPingForMoreThanTimeout() throws Exception {
        new SystemEnvironment().setProperty("agent.connection.timeout", "-1");
        CONFIG_HELPER.addMailHost(new MailHost("ghost.name", 25, "loser", "boozer", true, false, "go@foo.mail.com", "admin@foo.mail.com"));

        Date date = new Date(70, 1, 1, 1, 1, 1);
        AgentInstance instance = AgentInstanceMother.idle(date, "CCeDev01");
        ((AgentRuntimeInfo)ReflectionUtil.getField(instance, "agentRuntimeInfo")).setOperatingSystem("Minix");
        EmailSender mailSender = mock(EmailSender.class);
        AgentService agentService = new AgentService(agentConfigService, new SystemEnvironment(), environmentConfigService, goConfigService, securityService, agentDao, new UuidGenerator(), serverHealthService, mailSender);
        AgentInstances agentInstances = (AgentInstances) ReflectionUtil.getField(agentService, "agentInstances");
        agentInstances.add(instance);

        AgentInstances agents = agentService.findRegisteredAgents();
        assertThat(agents.size(), is(1));
        AgentInstance agentInstance = agents.findAgentAndRefreshStatus(instance.agentConfig().getUuid());
        assertThat(agentInstance.getStatus(), is(AgentStatus.LostContact));
    }

    @Test
    public void shouldSendLostContactEmailWhenAgentStateIsLostContact_FEATURE_HIDDEN() throws Exception {
        new SystemEnvironment().setProperty("agent.connection.timeout", "-1");
        CONFIG_HELPER.addMailHost(new MailHost("ghost.name", 25, "loser", "boozer", true, false, "go@foo.mail.com", "admin@foo.mail.com"));

        Date date = new Date(70, 1, 1, 1, 1, 1);
        AgentInstance instance = AgentInstanceMother.idle(date, "CCeDev01");
        ((AgentRuntimeInfo)ReflectionUtil.getField(instance, "agentRuntimeInfo")).setOperatingSystem("Minix");
        EmailSender mailSender = mock(EmailSender.class);
        AgentService agentService = new AgentService(agentConfigService, new SystemEnvironment(), environmentConfigService, goConfigService, securityService, agentDao, new UuidGenerator(), serverHealthService, mailSender);
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
    public void shouldNotShootoutMailsForEveryStatusChange() throws Exception {//should, only for lost-contact
        CONFIG_HELPER.addMailHost(new MailHost("ghost.name", 25, "loser", "boozer", true, false, "go@foo.mail.com", "admin@foo.mail.com"));

        AgentInstance instance = AgentInstanceMother.idle(new Date(), "CCeDev01");
        EmailSender mailSender = mock(EmailSender.class);

        agentDao.associateCookie(instance.getAgentIdentifier(), "rotten-cookie");
        AgentService agentService = new AgentService(agentConfigService, new SystemEnvironment(), environmentConfigService, goConfigService, securityService, agentDao, new UuidGenerator(), serverHealthService, mailSender);
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
    public void shouldLoadAllAgents() throws Exception {
        AgentInstance idle = AgentInstanceMother.idle(new Date(), "CCeDev01");
        AgentInstance pending = AgentInstanceMother.pending();
        AgentInstance building = AgentInstanceMother.building();
        AgentInstance denied = AgentInstanceMother.disabled();
        AgentService agentService = getAgentService(new AgentInstances(null, new SystemEnvironment(), idle, pending, building, denied));

        assertThat(agentService.agents().size(), is(4));

        assertThat(agentService.findAgentAndRefreshStatus(idle.agentConfig().getUuid()), is(idle));
        assertThat(agentService.findAgentAndRefreshStatus(pending.agentConfig().getUuid()), is(pending));
        assertThat(agentService.findAgentAndRefreshStatus(building.agentConfig().getUuid()), is(building));
        assertThat(agentService.findAgentAndRefreshStatus(denied.agentConfig().getUuid()), is(denied));
    }

    @Test
    public void shouldApproveAgent() throws Exception {
        AgentInstance pending = AgentInstanceMother.pending();
        agentService.requestRegistration(new Username("bob"), AgentRuntimeInfo.fromServer(pending.agentConfig(), false, "var/lib", 0L, "linux", false));
        agentService.approve(pending.getUuid());

        assertThat(agentService.findRegisteredAgents().size(), is(1));
        assertThat(agentService.findAgentAndRefreshStatus(pending.agentConfig().getUuid()).agentConfig().isDisabled(), is(false));
        CruiseConfig cruiseConfig = goConfigDao.load();
        assertThat(cruiseConfig.agents().get(0).isDisabled(), is(false));
    }

    @Test
    public void shouldAddOrUpdateAgent() throws Exception {
        AgentInstance pending = AgentInstanceMother.pending();
        agentService.requestRegistration(new Username("bob"), AgentRuntimeInfo.fromServer(pending.agentConfig(), false, "var/lib", 0L, "linux", false));

        agentService.approve(pending.getUuid());

        agentService.requestRegistration(new Username("bob"), AgentRuntimeInfo.fromServer(pending.agentConfig(), true, "var/lib", 0L, "linux", false));
        agentService.requestRegistration(new Username("bob"), AgentRuntimeInfo.fromServer(pending.agentConfig(), true, "var/lib", 0L, "linux", false));
        assertThat(agentService.findRegisteredAgents().size(), is(1));
    }

    @Test
    public void shouldDenyAgentFromPendingList() throws Exception {
        AgentInstance pending = AgentInstanceMother.pending();
        agentService.requestRegistration(new Username("bob"), AgentRuntimeInfo.fromServer(pending.agentConfig(), false, "var/lib", 0L, "linux", false));

        String uuid = pending.getUuid();

        HttpOperationResult operationResult = new HttpOperationResult();
        agentService.disableAgents(USERNAME, operationResult, Arrays.asList(uuid));
        assertAgentDisablingSucceeded(operationResult, uuid);
        Agents agents = agentConfigService.agents();
        assertThat(agentService.agents().size(), is(1));
        assertThat(agents.size(), is(1));
        assertThat(agents.get(0).isDisabled(), is(true));
        assertThat(agentService.findAgentAndRefreshStatus(uuid).isDisabled(), is(true));
        assertThat(agentService.findAgentAndRefreshStatus(uuid).getStatus(), is(AgentStatus.Disabled));
    }

    @Test
    public void shouldDenyApprovedAgent() throws Exception {
        CONFIG_HELPER.addAgent(new AgentConfig(UUID, "agentName", "127.0.0.9"));
        CruiseConfig cruiseConfig = goConfigDao.load();
        assertThat(cruiseConfig.agents().get(0).isDisabled(), is(false));

        agentService.initialize();
        HttpOperationResult operationResult = new HttpOperationResult();
        agentService.disableAgents(USERNAME, operationResult, Arrays.asList(UUID));

        CruiseConfig newCruiseConfig = goConfigDao.load();
        assertThat(newCruiseConfig.agents().get(0).isDisabled(), is(true));
        assertAgentDisablingSucceeded(operationResult, UUID);
    }

    private void assertAgentDisablingSucceeded(HttpOperationResult operationResult, String uuid) {
        assertThat(operationResult.httpCode(), is(200));
        assertThat(operationResult.message(), is(String.format("Disabled 1 agent(s)", uuid)));
    }

    @Test
    public void shouldDenyCorrectAgentWhenTwoOnSameBox() throws Exception {
        CONFIG_HELPER.addAgent(new AgentConfig(UUID, "agentName", "127.0.0.9", new Resources("agent1")));
        CONFIG_HELPER.addAgent(new AgentConfig(UUID2, "agentName", "127.0.0.9", new Resources("agent2")));
        CruiseConfig cruiseConfig = goConfigDao.load();
        assertThat(cruiseConfig.agents().getAgentByUuid(UUID).isDisabled(), is(false));
        assertThat(cruiseConfig.agents().getAgentByUuid(UUID2).isDisabled(), is(false));

        agentService.initialize();
        agentService.disableAgents(USERNAME, new HttpOperationResult(), Arrays.asList(UUID2));

        CruiseConfig newCruiseConfig = goConfigDao.load();
        assertThat(newCruiseConfig.agents().getAgentByUuid(UUID).isDisabled(), is(false));
        assertThat(newCruiseConfig.agents().getAgentByUuid(UUID2).isDisabled(), is(true));
    }

    @Test
    public void shouldBeAbleToDenyBuildingAgent() throws Exception {
        String agentName = "agentName";
        String agentId = DatabaseAccessHelper.AGENT_UUID;
        AgentConfig agentConfig = new AgentConfig(agentId, agentName, "127.0.0.9");
        addAgent(agentConfig);
        AgentIdentifier agentIdentifier = agentConfig.getAgentIdentifier();
        String cookie = agentService.assignCookie(agentIdentifier);
        AgentRuntimeInfo agentRuntimeInfo = new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), cookie, null, false);
        agentRuntimeInfo.busy(new AgentBuildingInfo("path", "buildLocator"));

        agentService.updateRuntimeInfo(agentRuntimeInfo);
        HttpOperationResult operationResult = new HttpOperationResult();
        agentService.disableAgents(USERNAME, operationResult, Arrays.asList(agentId));
        assertAgentDisablingSucceeded(operationResult, agentId);
    }

    @Test
    public void shouldReturn200OnTryingToDisableADisabledAgent() {
        String agentName = "agentName";
        String agentId = DatabaseAccessHelper.AGENT_UUID;
        AgentConfig agentConfig = new AgentConfig(agentId, agentName, "127.0.0.9");
        addAgent(agentConfig);
        disable(agentConfig);
        HttpOperationResult operationResult = new HttpOperationResult();
        agentService.disableAgents(USERNAME, operationResult, Arrays.asList(agentId));
        assertThat(operationResult.httpCode(), is(200));
        assertThat(operationResult.message(), is("Disabled 1 agent(s)"));
    }

    @Test
    public void shouldSetErrorState404OnTryingToDisableUnknownAgent() {
        // pending matches this as well
        String agentId = "unknown-agent-id";
        HttpOperationResult operationResult = new HttpOperationResult();
        agentService.disableAgents(USERNAME, operationResult, Arrays.asList(agentId));
        assertThat(operationResult.httpCode(), is(404));
        assertThat(operationResult.message(), is("Agent not found."));
    }

    @Test
    public void shouldNotAllowDisablingAgentWhenNotAdmin() throws IOException {
        String agentId = "agent-id";
        HttpOperationResult operationResult = new HttpOperationResult();
        CONFIG_HELPER.addSecurityWithPasswordFile();
        CONFIG_HELPER.addAdmins("admin1");
        agentService.disableAgents(new Username(new CaseInsensitiveString("not-admin")), operationResult, Arrays.asList(agentId));
        assertThat(operationResult.httpCode(), is(401));
        assertThat(operationResult.message(), is("Unauthorized to operate on agent"));
    }

    @Test
    public void shouldNotAllowAddingResourcesWhenNotAdmin() throws IOException {
        String agentId = "agent-id";
        CONFIG_HELPER.addSecurityWithPasswordFile();
        HttpOperationResult operationResult = new HttpOperationResult();
        CONFIG_HELPER.addAdmins("admin1");
        agentService.modifyResources(new Username(new CaseInsensitiveString("not-admin")), operationResult, Arrays.asList(agentId), Arrays.asList(new TriStateSelection("dont-care", TriStateSelection.Action.add)));
        assertThat(operationResult.httpCode(), is(401));
        assertThat(operationResult.message(), is("Unauthorized to operate on agent"));
    }

    @Test
    public void shouldAllowDisablingAgentWhenPending() {
        String agentName = "agentName";
        String agentId = DatabaseAccessHelper.AGENT_UUID;
        AgentConfig agentConfig = new AgentConfig(agentId, agentName, "50.40.30.9");
        AgentRuntimeInfo agentRuntimeInfo = new AgentRuntimeInfo(agentConfig.getAgentIdentifier(), AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie", null, false);
        agentRuntimeInfo.busy(new AgentBuildingInfo("path", "buildLocator"));
        agentService.requestRegistration(new Username("bob"), agentRuntimeInfo);
        HttpOperationResult operationResult = new HttpOperationResult();
        agentService.disableAgents(USERNAME, operationResult, Arrays.asList(agentId));
        assertThat(operationResult.httpCode(), is(200));
        assertThat(operationResult.message(), is("Disabled 1 agent(s)"));
    }

    @Test
    public void shouldAllowEnableOfPendingAgent() {
        String agentName = "agentName";
        String agentId = DatabaseAccessHelper.AGENT_UUID;
        AgentConfig agentConfig = new AgentConfig(agentId, agentName, "50.40.30.9");
        AgentRuntimeInfo agentRuntimeInfo = new AgentRuntimeInfo(agentConfig.getAgentIdentifier(), AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie", null, false);
        agentRuntimeInfo.busy(new AgentBuildingInfo("path", "buildLocator"));
        agentService.requestRegistration(new Username("bob"), agentRuntimeInfo);
        HttpOperationResult operationResult = new HttpOperationResult();
        agentService.enableAgents(USERNAME, operationResult, Arrays.asList(agentId));
        assertThat(operationResult.httpCode(), is(200));
        assertThat(operationResult.message(), is("Enabled 1 agent(s)"));
    }

    @Test
    public void shouldAllowEnablingOfADisabledAgent() {
        String agentName = "agentName";
        String agentId = DatabaseAccessHelper.AGENT_UUID;
        AgentConfig agentConfig = new AgentConfig(agentId, agentName, "127.0.0.9");
        addAgent(agentConfig);
        disable(agentConfig);
        HttpOperationResult operationResult = new HttpOperationResult();
        agentService.enableAgents(USERNAME, operationResult, Arrays.asList(agentId));
        assertThat(operationResult.httpCode(), is(200));
        assertThat(operationResult.message(), is("Enabled 1 agent(s)"));
        assertThat(isDisabled(agentConfig),is(false));
    }

    @Test
    public void shouldEnableMultipleAgents() {
        AgentConfig agentConfig1 = createDisabledAgent(UUID);
        AgentConfig agentConfig2 = createDisabledAgent(UUID2);
        HttpOperationResult operationResult = new HttpOperationResult();

        agentService.enableAgents(USERNAME, operationResult, Arrays.asList(UUID, UUID2));

        assertThat(operationResult.httpCode(), is(200));
        assertThat(isDisabled(agentConfig1),is(false));
        assertThat(isDisabled(agentConfig2),is(false));
        assertThat(operationResult.message(), containsString("Enabled 2 agent(s)"));
    }

    @Test
    public void shouldDisableMultipleAgents() {
        AgentConfig agentConfig1 = createEnabledAgent(UUID);
        AgentConfig agentConfig2 = createEnabledAgent(UUID2);
        HttpOperationResult operationResult = new HttpOperationResult();

        agentService.disableAgents(USERNAME, operationResult, Arrays.asList(UUID, UUID2));

        assertThat(operationResult.httpCode(), is(200));
        assertThat(isDisabled(agentConfig1), is(true));
        assertThat(isDisabled(agentConfig2),is(true));
        assertThat(operationResult.message(), containsString("Disabled 2 agent(s)"));
    }

    @Test
    public void shouldReturn401WhenAUnauthorizedUserTriesToEnable() throws IOException {
        String agentId = "agent-id";
        CONFIG_HELPER.addSecurityWithPasswordFile();
        HttpOperationResult operationResult = new HttpOperationResult();
        CONFIG_HELPER.addAdmins("admin1");
        agentService.enableAgents(new Username(new CaseInsensitiveString("not-admin")), operationResult, Arrays.asList(agentId));
        assertThat(operationResult.httpCode(), is(401));
        assertThat(operationResult.message(), is("Unauthorized to operate on agent"));
    }

    @Test
    public void shouldReturn404WhenAgentUUIDNotKnown() {
        // pending matches that as well
        String agentId = "unknown-agent-id";
        HttpOperationResult operationResult = new HttpOperationResult();
        agentService.enableAgents(USERNAME, operationResult, Arrays.asList(agentId));
        assertThat(operationResult.httpCode(), is(404));
        assertThat(operationResult.message(), is("Agent not found."));
    }

    @Test
    public void shouldReturn200WhenAnAlreadyEnableAgentIsEnabled() {
        String agentName = "agentName";
        String agentId = DatabaseAccessHelper.AGENT_UUID;
        AgentConfig agentConfig = new AgentConfig(agentId, agentName, "127.0.0.9");
        addAgent(agentConfig);
        HttpOperationResult operationResult = new HttpOperationResult();
        agentService.enableAgents(USERNAME, operationResult, Arrays.asList(agentId));
        assertThat(operationResult.httpCode(), is(200));
        assertThat(operationResult.message(), is("Enabled 1 agent(s)"));
    }

    @Test
    public void shouldRegisterLocalAgentWithNonLoopbackIpAddress() throws Exception {
        String nonLoopbackIp = SystemUtil.getFirstLocalNonLoopbackIpAddress();
        InetAddress inetAddress = InetAddress.getByName(nonLoopbackIp);
        assertThat(SystemUtil.isLocalIpAddress(nonLoopbackIp), is(true));
        AgentConfig agentConfig = new AgentConfig("uuid", inetAddress.getHostName(), nonLoopbackIp);
        AgentRuntimeInfo agentRuntimeInfo = AgentRuntimeInfo.fromServer(agentConfig, false, "/var/lib", 0L, "linux", false);
        agentService.requestRegistration(new Username("bob"), agentRuntimeInfo);

        AgentInstance agentInstance = agentService.findRegisteredAgents().findAgentAndRefreshStatus("uuid");

        assertThat(agentInstance.agentConfig().getIpAddress(), is(nonLoopbackIp));
        assertThat(agentInstance.getStatus(), is(AgentStatus.Idle));
    }

    @Test
    public void shouldLoadAgentsByApprovalStatus() throws Exception {
        AgentConfig deniedAgent1 = new AgentConfig("uuid1", "deniedAgent1", "127.0.0.1");
        deniedAgent1.disable();

        CONFIG_HELPER.addAgent(deniedAgent1);
        AgentConfig deniedAgent2 = new AgentConfig("uuid2", "deniedAgent2", "127.0.0.2");
        deniedAgent2.disable();
        CONFIG_HELPER.addAgent(deniedAgent2);

        CONFIG_HELPER.addAgent(new AgentConfig("uuid3", "approvedAgent1", "127.0.0.3"));
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
    public void shouldDenyAgentWhenAgentChangedToDenyInConfigFile() throws Exception {
        disableAgent();

        AgentInstance instance = agentService.findAgentAndRefreshStatus("uuid1");
        assertThat(instance.getStatus(), is(AgentStatus.Disabled));

    }

    @Test
    public void shouldChangeAgentToIdleWhenAgentIsReApprovedInConfigFile() throws Exception {
        disableAgent();

        agentConfigService.updateAgentApprovalStatus("uuid1", false, Username.ANONYMOUS);

        AgentInstance instance = agentService.findAgentAndRefreshStatus("uuid1");
        assertThat(instance.getStatus(), is(AgentStatus.Idle));

    }

    @Test
    public void shouldLoadAllAgentsViewModel() throws Exception {
        AgentInstance idle = AgentInstanceMother.updateUuid(AgentInstanceMother.idle(new Date(), "CCeDev01"), UUID);
        AgentInstance pending = AgentInstanceMother.pending();
        AgentInstance building = AgentInstanceMother.building();
        AgentInstance denied = AgentInstanceMother.disabled();
        createEnvironment("uat");
        EnvironmentConfig environment = environmentConfigService.named("uat");
        environment.addAgent(UUID);
        AgentInstances instances = new AgentInstances(null, new SystemEnvironment(), idle, pending, building, denied);
        AgentService agentService = getAgentService(instances);
        AgentsViewModel agents = agentService.agents();

        Collections.sort(agents, new Comparator<AgentViewModel>() {
            public int compare(AgentViewModel o1, AgentViewModel o2) {
                return o1.getUuid().compareTo(o2.getUuid());
            }
        });
        assertThat(agents.size(), is(4));
        AgentViewModel idleAgentModel = agents.get(0);
        assertThat(idleAgentModel.getEnvironments().size(), is(1));
        assertThat(idleAgentModel.getEnvironments(), hasItem("uat"));
    }

    @Test
    public void enabledAgents_shouldNotIncludePendingAgents() throws Exception {
        AgentInstance idle = AgentInstanceMother.updateUuid(AgentInstanceMother.idle(new Date(), "CCeDev01"), UUID);
        AgentInstance pending = AgentInstanceMother.pending();
        AgentInstance building = AgentInstanceMother.building();
        AgentInstance denied = AgentInstanceMother.disabled();
        createEnvironment("uat");
        EnvironmentConfig environment = environmentConfigService.named("uat");
        environment.addAgent(UUID);
        AgentInstances instances = new AgentInstances(null, new SystemEnvironment(), idle, pending, building, denied);
        AgentService agentService = getAgentService(instances);

        AgentsViewModel agents = agentService.registeredAgents();
        assertThat(agents.size(), is(3));
        for (AgentViewModel agent : agents) {
            assertThat(agent.getStatus().getConfigStatus(), not(is(AgentConfigStatus.Pending)));
        }
    }

    @Test
    public void shouldReturn401WhenAUnauthorizedUserTriesToDelete() throws IOException {
        CONFIG_HELPER.addSecurityWithPasswordFile();
        HttpOperationResult operationResult = new HttpOperationResult();
        CONFIG_HELPER.addAdmins("admin1");
        agentService.deleteAgents(new Username(new CaseInsensitiveString("not-admin")), operationResult, Arrays.asList(UUID));
        assertThat(operationResult.httpCode(), is(401));
        assertThat(operationResult.message(), is("Unauthorized to operate on agent"));
    }

    @Test
    public void shouldDeleteOnlyDisabledAgentGivenUUID() throws Exception {
        AgentConfig disabledAgent = createDisabledAndIdleAgent(UUID);
        AgentConfig enabledAgent = createEnabledAgent(UUID2);

        goConfigDao.load();
        assertThat(agentService.agents().size(), is(2));

        HttpOperationResult disabledAgentOperationResult = new HttpOperationResult();
        HttpOperationResult enabledAgentOperationResult = new HttpOperationResult();

        agentService.deleteAgents(USERNAME, disabledAgentOperationResult, Arrays.asList(disabledAgent.getUuid()));
        agentService.deleteAgents(USERNAME, enabledAgentOperationResult, Arrays.asList(enabledAgent.getUuid()));

        assertThat(disabledAgentOperationResult.httpCode(), is(200));
        assertThat(disabledAgentOperationResult.message(), is("Deleted 1 agent(s)."));

        assertThat(enabledAgentOperationResult.httpCode(), is(406));
        assertThat(enabledAgentOperationResult.message(), is("Deleted 0 agent(s). Failed to delete 1 agent(s), as agent(s) might not be disabled or are still building."));

        assertThat(agentService.agents().size(), is(1));
        assertThat(agentService.agents().get(0).getUuid(), is(UUID2));
    }

    @Test
    public void shouldNOTDeleteDisabledAgentThatIsBuildingGivenUUID() throws Exception {
        AgentConfig disabledButBuildingAgent = createDisabledAgent(UUID);

        goConfigDao.load();
        assertThat(agentService.agents().size(), is(1));

        HttpOperationResult operationResult = new HttpOperationResult();

        agentService.deleteAgents(USERNAME, operationResult, Arrays.asList(disabledButBuildingAgent.getUuid()));

        assertThat(operationResult.httpCode(), is(406));
        assertThat(operationResult.message(), is("Deleted 0 agent(s). Failed to delete 1 agent(s), as agent(s) might not be disabled or are still building."));

        assertThat(agentService.agents().size(), is(1));
        assertThat(agentService.agents().get(0).getUuid(), is(UUID));
    }

    @Test
    public void shouldReturn401WhenAUnauthorizedUserTriesToDeleteAgents() throws IOException {
        CONFIG_HELPER.addSecurityWithPasswordFile();
        HttpOperationResult operationResult = new HttpOperationResult();
        CONFIG_HELPER.addAdmins("admin1");
        agentService.deleteAgents(new Username(new CaseInsensitiveString("not-admin")), operationResult, Arrays.asList(UUID));
        assertThat(operationResult.httpCode(), is(401));
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
    public void shouldDeleteAgentsGivenListOfUUIDs() throws Exception {
        AgentConfig disabledAgent1 = createDisabledAndIdleAgent(UUID);
        AgentConfig disabledAgent2 = createDisabledAndIdleAgent(UUID2);

        goConfigDao.load();
        assertThat(agentService.agents().size(), is(2));

        HttpOperationResult operationResult = new HttpOperationResult();

        agentService.deleteAgents(USERNAME, operationResult, Arrays.asList(disabledAgent1.getUuid(), disabledAgent2.getUuid()));

        assertThat(operationResult.httpCode(), is(200));
        assertThat(operationResult.message(), is("Deleted 2 agent(s)."));

        assertThat(agentService.agents().size(), is(0));
    }

    @Test
    public void shouldNOTDeleteAgentsThatAreNotDisabledGivenListOfUUIDs() throws Exception {
        AgentConfig disabledAgent1 = createDisabledAndIdleAgent(UUID);
        AgentConfig disabledAgent2 = createDisabledAndIdleAgent(UUID2);
        AgentConfig enabledAgent1 = createEnabledAgent(UUID3);
        AgentConfig enabledAgent2 = createEnabledAgent("uuid4");

        goConfigDao.load();
        assertThat(agentService.agents().size(), is(4));

        HttpOperationResult operationResult = new HttpOperationResult();

        agentService.deleteAgents(USERNAME, operationResult, Arrays.asList(disabledAgent1.getUuid(), disabledAgent2.getUuid(), enabledAgent1.getUuid(), enabledAgent2.getUuid()));

        assertThat(operationResult.httpCode(), is(406));
        assertThat(operationResult.message(), is("Deleted 2 agent(s). Failed to delete 2 agent(s), as agent(s) might not be disabled or are still building."));

        assertThat(agentService.agents().size(), is(2));
        assertThat(agentService.agents().get(0).getUuid(), is(UUID3));
        assertThat(agentService.agents().get(1).getUuid(), is("uuid4"));
    }

    @Test
    public void updateAgentAttributesShouldUpdateAnAgentHostname() throws Exception {
        AgentConfig agent = createDisabledAndIdleAgent(UUID);

        goConfigDao.load();

        assertThat(agentService.agents().size(), is(1));
        assertThat(agentService.agents().get(0).getHostname(), is(not("some-hostname")));

        HttpOperationResult operationResult = new HttpOperationResult();
        agentService.updateAgentAttributes(USERNAME, operationResult, UUID, "some-hostname", null, null, TriState.UNSET);

        assertThat(operationResult.httpCode(), is(200));
        assertThat(operationResult.message(), is("Updated agent with uuid uuid."));

        assertThat(agentService.agents().size(), is(1));
        assertThat(agentService.agents().get(0).getHostname(), is("some-hostname"));
    }

    @Test
    public void updateAgentAttributesShouldUpdateAnAgentResources() throws Exception {
        AgentConfig agent = createDisabledAndIdleAgent(UUID);

        goConfigDao.load();

        assertThat(agentService.agents().size(), is(1));
        assertThat(agentService.agents().get(0).getResources(), is(empty()));

        HttpOperationResult operationResult = new HttpOperationResult();
        agentService.updateAgentAttributes(USERNAME, operationResult, UUID, null, "linux,java", null, TriState.UNSET);

        assertThat(operationResult.httpCode(), is(200));
        assertThat(operationResult.message(), is("Updated agent with uuid uuid."));

        assertThat(agentService.agents().size(), is(1));
        assertThat(agentService.agents().get(0).resources(), is(new Resources("linux,java")));
    }


    @Test
    public void updateAgentAttributesShouldUpdateAnAgentEnvironments() throws Exception {
        createEnvironment("a", "b", "c", "d", "e");
        AgentConfig agent = createEnabledAgent(UUID);

        HttpOperationResult operationResult = new HttpOperationResult();
        agentService.modifyEnvironments(USERNAME, operationResult, Arrays.asList(UUID), Arrays.asList(
                new TriStateSelection("a", TriStateSelection.Action.add),
                new TriStateSelection("b", TriStateSelection.Action.add),
                new TriStateSelection("c", TriStateSelection.Action.add)));
        assertThat(operationResult.httpCode(), is(200));

        goConfigDao.load();

        assertThat(agentService.agents().size(), is(1));
        assertThat(agentService.agents().get(0).getResources(), is(empty()));

        operationResult = new HttpOperationResult();
        agentService.updateAgentAttributes(USERNAME, operationResult, UUID, null, null, "c,d,e", TriState.UNSET);

        assertThat(operationResult.httpCode(), is(200));
        assertThat(operationResult.message(), is("Updated agent with uuid uuid."));

        assertThat(agentService.agents().size(), is(1));
        assertThat(agentService.agents().get(0).getEnvironments().equals(new HashSet<>(Arrays.asList("c", "d", "e"))), is(true));
    }

    @Test
    public void updateAgentAttributesShouldUpdateAnAgentEnableState() throws Exception {
        AgentConfig agent = createDisabledAndIdleAgent(UUID);

        goConfigDao.load();

        assertThat(agentService.agents().size(), is(1));
        assertThat(agentService.agents().get(0).isEnabled(), is(false));

        HttpOperationResult operationResult = new HttpOperationResult();
        agentService.updateAgentAttributes(USERNAME, operationResult, UUID, null, null, null, TriState.TRUE);

        assertThat(operationResult.httpCode(), is(200));
        assertThat(operationResult.message(), is("Updated agent with uuid uuid."));

        assertThat(agentService.agents().size(), is(1));
        assertThat(agentService.agents().get(0).isEnabled(), is(true));
    }

    @Test
    public void updateAgentAttributesShouldUpdateAnAgentDisableState() throws Exception {
        AgentConfig agent = createEnabledAgent(UUID);

        goConfigDao.load();

        assertThat(agentService.agents().size(), is(1));
        assertThat(agentService.agents().get(0).isEnabled(), is(true));

        HttpOperationResult operationResult = new HttpOperationResult();
        agentService.updateAgentAttributes(USERNAME, operationResult, UUID, null, null, null, TriState.FALSE);

        assertThat(operationResult.httpCode(), is(200));
        assertThat(operationResult.message(), is("Updated agent with uuid uuid."));

        assertThat(agentService.agents().size(), is(1));
        assertThat(agentService.agents().get(0).isEnabled(), is(false));
    }

    @Test
    public void updateAgentAttributesShouldNotUpdateAgentEnableStateIfTristateIsNotDefined() throws Exception {
        AgentConfig enabledAgent = createEnabledAgent("enabled");
        AgentConfig disabledAgent = createDisabledAgent("disabled");

        goConfigDao.load();

        assertThat(agentService.agents().size(), is(2));
        assertThat(agentService.findAgentAndRefreshStatus("enabled").agentConfig().isDisabled(), is(false));
        assertThat(agentService.findAgentAndRefreshStatus("disabled").agentConfig().isDisabled(), is(true));

        HttpOperationResult operationResult = new HttpOperationResult();
        agentService.updateAgentAttributes(USERNAME, operationResult, "enabled", "new.enabled.hostname", "linux,java", null, TriState.UNSET);
        agentService.updateAgentAttributes(USERNAME, operationResult, "disabled", "new.disabled.hostname", "linux,java", null, TriState.UNSET);

        assertThat(operationResult.httpCode(), is(200));

        assertThat(agentService.agents().size(), is(2));
        assertThat(agentService.findAgentAndRefreshStatus("enabled").agentConfig().isDisabled(), is(false));
        assertThat(agentService.findAgentAndRefreshStatus("disabled").agentConfig().isDisabled(), is(true));
    }

    @Test
    public void testShouldUpdateAnAgentIfInputsAreValid() throws Exception {
        String headCommitBeforeUpdate = configRepository.getCurrentRevCommit().name();
        AgentConfig agent = createDisabledAndIdleAgent(UUID);
        createEnvironment("a", "b");

        goConfigDao.load();

        assertThat(agentService.agents().size(), is(1));
        assertThat(agentService.agents().get(0).getHostname(), is(not("some-hostname")));
        assertThat(agentService.agents().get(0).isEnabled(), is(false));

        HttpOperationResult operationResult = new HttpOperationResult();
        agentService.updateAgentAttributes(USERNAME, operationResult, UUID, "some-hostname", "linux,java", "a,b", TriState.UNSET);

        assertThat(operationResult.httpCode(), is(200));
        assertThat(operationResult.message(), is("Updated agent with uuid uuid."));

        assertThat(agentService.agents().size(), is(1));
        assertThat(agentService.agents().get(0).getHostname(), is("some-hostname"));
        assertThat(agentService.agents().get(0).resources(), is(new Resources("linux,java")));
        assertThat(agentService.agents().get(0).isEnabled(), is(false));
        assertEquals(agentService.agents().get(0).getEnvironments(), new HashSet<>(Arrays.asList("a", "b")));
        assertThat(configRepository.getCurrentRevCommit().name(), is(not(headCommitBeforeUpdate)));
        assertThat(configRepository.getCurrentRevision().getUsername(), is(USERNAME.getDisplayName()));
    }

    @Test
    public void testShouldNotUpdateHostnameOrResourcesOrEnvironmentsIfNoneAreSpecified() throws Exception {
        createEnvironment("a", "b");
        AgentConfig agent = createDisabledAndIdleAgent(UUID);
        String originalHostname = agent.getHostname();

        HttpOperationResult operationResult = new HttpOperationResult();
        agentService.modifyEnvironments(USERNAME, operationResult, Arrays.asList(UUID), Arrays.asList(
                new TriStateSelection("a", TriStateSelection.Action.add),
                new TriStateSelection("b", TriStateSelection.Action.add)));

        goConfigDao.load();
        agentConfigService.updateAgentResources(agent.getUuid(), new Resources("linux,java"));

        assertThat(agentService.agents().size(), is(1));

        operationResult = new HttpOperationResult();
        agentService.updateAgentAttributes(USERNAME, operationResult, UUID, null, null, null, TriState.UNSET);

        assertThat(operationResult.httpCode(), is(200));
        assertThat(operationResult.message(), is("Updated agent with uuid uuid."));

        assertThat(agentService.agents().size(), is(1));
        assertThat(agentService.agents().get(0).getHostname(), is(originalHostname));
        assertThat(agentService.agents().get(0).resources().resourceNames(), is(new Resources("linux,java").resourceNames()));
        assertEquals(agentService.agents().get(0).getEnvironments(), new HashSet<>(Arrays.asList("a", "b")));
    }

    @Test
    public void testShouldThrowErrorOnUpdatingAgentOnInvalidInputs() throws Exception {
        AgentConfig agent = createDisabledAndIdleAgent(UUID);
        String originalHostname = agent.getHostname();
        List<String> originalResourceNames = agent.getResources().resourceNames();

        goConfigDao.load();

        assertThat(agentService.agents().size(), is(1));
        assertThat(agentService.agents().get(0).getHostname(), is(not("some-hostname")));

        HttpOperationResult operationResult = new HttpOperationResult();
        AgentInstance agentInstance = agentService.updateAgentAttributes(USERNAME, operationResult, UUID, "some-hostname", "lin!ux", null, TriState.UNSET);

        assertThat(operationResult.httpCode(), is(422));
        assertThat(operationResult.message(), is("Updating agent failed:"));
        assertThat(agentInstance.agentConfig().getResources().first().errors().on(JobConfig.RESOURCES), is("Resource name 'lin!ux' is not valid. Valid names much match '^[-\\w\\s|.]*$'"));

        assertThat(agentService.agents().size(), is(1));
        assertThat(agentService.agents().get(0).getHostname(), is(originalHostname));
        assertThat(agentService.agents().get(0).getResources(), is(originalResourceNames));
    }

    @Test
    public void shouldNOTDeleteDisabledAgentsThatIsBuildingGivenListOfUUIDs() throws Exception {
        AgentConfig disabledButBuildingAgent = createDisabledAgent(UUID);
        AgentConfig disabledAgent1 = createDisabledAndIdleAgent(UUID2);

        goConfigDao.load();
        assertThat(agentService.agents().size(), is(2));

        HttpOperationResult operationResult = new HttpOperationResult();

        agentService.deleteAgents(USERNAME, operationResult, Arrays.asList(disabledAgent1.getUuid(), disabledButBuildingAgent.getUuid()));

        assertThat(operationResult.httpCode(), is(406));
        assertThat(operationResult.message(), is("Deleted 1 agent(s). Failed to delete 1 agent(s), as agent(s) might not be disabled or are still building."));

        assertThat(agentService.agents().size(), is(1));
        assertThat(agentService.agents().get(0).getUuid(), is(UUID));
    }

    private void createEnvironment(String... environmentNames) throws Exception {
        CONFIG_HELPER.addEnvironments(environmentNames);
        goConfigService.forceNotifyListeners();
    }

    private AgentConfig createEnabledAgent(String uuid) {
        AgentConfig agentConfig = new AgentConfig(uuid, "agentName", "127.0.0.9");
        addAgent(agentConfig);
        return agentConfig;
    }

    private void disable(AgentConfig agentConfig) {
        AgentIdentifier agentIdentifier = agentConfig.getAgentIdentifier();
        String cookie = agentService.assignCookie(agentIdentifier);
        AgentRuntimeInfo agentRuntimeInfo = new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), cookie, null, false);
        agentRuntimeInfo.busy(new AgentBuildingInfo("path", "buildLocator"));
        agentService.updateRuntimeInfo(agentRuntimeInfo);
        agentService.disableAgents(USERNAME, new HttpOperationResult(), Arrays.asList(agentConfig.getUuid()));
        assertThat(isDisabled(agentConfig),is(true));
    }

    private void disableAgent() {
        AgentConfig pending = new AgentConfig("uuid1", "agent1", "192.168.0.1");
        agentService.requestRegistration(new Username("bob"), AgentRuntimeInfo.fromServer(pending, false, "/var/lib", 0L, "linux", false));
        agentService.approve("uuid1");
        agentConfigService.updateAgentApprovalStatus("uuid1", true, Username.ANONYMOUS);
    }

    private boolean isDisabled(AgentConfig agentConfig) {
        return agentService.findAgentAndRefreshStatus(agentConfig.getUuid()).isDisabled();
    }

    public void addAgent(AgentConfig agentConfig) {
        agentService.requestRegistration(new Username("bob"), AgentRuntimeInfo.fromServer(agentConfig, false, "/var/lib", 0L, "linux", false));
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
        AgentRuntimeInfo agentRuntimeInfo = new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), cookie, null, false);
        agentRuntimeInfo.idle();
        agentService.updateRuntimeInfo(agentRuntimeInfo);
        assertTrue(agentService.findAgentAndRefreshStatus(uuid).isIdle());

        agentService.disableAgents(USERNAME, new HttpOperationResult(), Arrays.asList(agentConfig.getUuid()));
        AgentConfig updatedAgent = goConfigDao.load().agents().getAgentByUuid(agentConfig.getUuid());
        assertThat(isDisabled(updatedAgent),is(true));
        return updatedAgent;
    }
}
