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

import com.thoughtworks.go.config.AgentConfig;
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.GoConfigDao;
import com.thoughtworks.go.domain.AgentRuntimeStatus;
import com.thoughtworks.go.domain.GoConfigRevision;
import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.fixture.PipelineWithTwoStages;
import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.service.ConfigRepository;
import com.thoughtworks.go.util.GoConfigFileHelper;
import com.thoughtworks.go.util.LogFixture;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.sql.DataSource;

import static com.thoughtworks.go.util.SystemUtil.currentWorkingDirectory;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertThat;
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:WEB-INF/applicationContext-acegi-security.xml"
})
public class UpdateAgentStatusTest {
    @Autowired private AgentService agentService;
    @Autowired private GoConfigDao goConfigDao;
    @Autowired private DataSource dataSource;
    @Autowired private DatabaseAccessHelper dbHelper;
    @Autowired private ConfigRepository configRepo;
    @Autowired private TransactionTemplate transactionTemplate;

    @Autowired private MaterialRepository materialRepository;
    private PipelineWithTwoStages preCondition;
    private String agentId = "uuid";
    private static GoConfigFileHelper configHelper = new GoConfigFileHelper();

    @Before
    public void setUp() throws Exception {
        dbHelper.onSetUp();
        configHelper.onSetUp();
        configHelper.usingCruiseConfigDao(goConfigDao);
        preCondition = new PipelineWithTwoStages(materialRepository, transactionTemplate);
        preCondition.usingConfigHelper(configHelper).usingDbHelper(dbHelper).onSetUp();
        agentService.clearAll();
        agentService.requestRegistration(new Username("bob"), AgentRuntimeInfo.fromServer(new AgentConfig(agentId, "CCEDev01", "10.81.2.1"), false, "/var/lib", 0L, "linux", false));
        agentService.approve(agentId);
    }

    @After
    public void tearDown() throws Exception {
        preCondition.onTearDown();
    }

    @Test
    public void shouldUpdateAgentIPAddressWhenItChanges_asAgent() throws Exception {
        CruiseConfig oldConfig = goConfigDao.load();
        String oldIp = oldConfig.agents().getAgentByUuid("uuid").getIpAddress();
        assertThat(oldIp, is("10.81.2.1"));

        AgentIdentifier agentIdentifier1 = new AgentIdentifier("localhost", "10.18.3.95", "uuid");
        AgentRuntimeInfo agentRuntimeInfo1 = new AgentRuntimeInfo(agentIdentifier1, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie", null, false);
        agentRuntimeInfo1.busy(new AgentBuildingInfo("building", "buildLocator"));

        agentService.updateRuntimeInfo(agentRuntimeInfo1);

        CruiseConfig newConfig = goConfigDao.load();
        String newIp = newConfig.agents().getAgentByUuid("uuid").getIpAddress();
        assertThat(newIp, is("10.18.3.95"));
        GoConfigRevision rev = configRepo.getRevision(newConfig.getMd5());
        assertThat(rev.getUsername(), is("agent_uuid_10.18.3.95_CCEDev01"));
    }

    @Test
    public void shouldUpdateAgentWorkingDirWhenItChanges() throws Exception {
        AgentIdentifier agentIdentifier1 = new AgentIdentifier("localhost", "10.18.3.95", "uuid");
        AgentRuntimeInfo agentRuntimeInfo1 = new AgentRuntimeInfo(agentIdentifier1, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie", null, false);
        agentRuntimeInfo1.busy(new AgentBuildingInfo("building", "buildLocator"));
        agentRuntimeInfo1.setLocation("/myDirectory");

        agentService.updateRuntimeInfo(agentRuntimeInfo1);

        assertThat(agentService.findAgentAndRefreshStatus("uuid").getLocation(), is("/myDirectory"));
    }


    @Test
    public void shouldLogWarningWhenIPAddressChanges() throws Exception {
        AgentIdentifier agentIdentifier1 = new AgentIdentifier("localhost", "10.18.3.95", "uuid");
        AgentRuntimeInfo agentRuntimeInfo1 = new AgentRuntimeInfo(agentIdentifier1, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie", null, false);
        agentRuntimeInfo1.busy(new AgentBuildingInfo("building", "buildLocator"));
        agentRuntimeInfo1.setLocation("/myDirectory");

        LogFixture logging = LogFixture.startListening();
        agentService.updateRuntimeInfo(agentRuntimeInfo1);
        assertThat(logging.getLog(),
                containsString("Agent with UUID [uuid] changed IP Address from [10.81.2.1] to [10.18.3.95]"));
        logging.stopListening();
    }

    public JobIdentifier jobIdentifier(long id) {
        return new JobIdentifier("", "", "", "1", "", id);
    }
}



