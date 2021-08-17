/*
 * Copyright 2021 ThoughtWorks, Inc.
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

import ch.qos.logback.classic.Level;
import com.thoughtworks.go.config.Agent;
import com.thoughtworks.go.config.GoConfigDao;
import com.thoughtworks.go.domain.AgentRuntimeStatus;
import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.fixture.PipelineWithTwoStages;
import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.service.ConfigRepository;
import com.thoughtworks.go.util.GoConfigFileHelper;
import com.thoughtworks.go.util.LogFixture;
import org.junit.Rule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.migrationsupport.rules.EnableRuleMigrationSupport;
import org.junit.rules.TemporaryFolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static com.thoughtworks.go.util.LogFixture.logFixtureFor;
import static com.thoughtworks.go.util.SystemUtil.currentWorkingDirectory;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {
        "classpath:/applicationContext-global.xml",
        "classpath:/applicationContext-dataLocalAccess.xml",
        "classpath:/testPropertyConfigurer.xml",
        "classpath:/spring-all-servlet.xml",
})
@EnableRuleMigrationSupport
public class UpdateAgentStatusTest {
    @Autowired
    private AgentService agentService;
    @Autowired
    private GoConfigDao goConfigDao;
    @Autowired
    private DatabaseAccessHelper dbHelper;
    @Autowired
    private ConfigRepository configRepo;
    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private MaterialRepository materialRepository;

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    private PipelineWithTwoStages preCondition;
    private String agentId = "uuid";
    private static GoConfigFileHelper configHelper = new GoConfigFileHelper();

    @BeforeEach
    public void setUp() throws Exception {
        dbHelper.onSetUp();
        configHelper.onSetUp();
        configHelper.usingCruiseConfigDao(goConfigDao);
        preCondition = new PipelineWithTwoStages(materialRepository, transactionTemplate, temporaryFolder);
        preCondition.usingConfigHelper(configHelper).usingDbHelper(dbHelper).onSetUp();
        agentService.clearAll();
        agentService.saveOrUpdate(new Agent(agentId, "CCEDev01", "10.81.2.1", "cookie"));
    }

    @AfterEach
    public void tearDown() throws Exception {
        preCondition.onTearDown();
    }

    @Test
    public void shouldUpdateAgentIPAddressWhenItChanges_asAgent() throws Exception {
        String oldIp = agentService.getAgentByUUID("uuid").getIpaddress();
        assertThat(oldIp, is("10.81.2.1"));

        AgentIdentifier agentIdentifier1 = new AgentIdentifier("localhost", "10.18.3.95", "uuid");
        AgentRuntimeInfo agentRuntimeInfo1 = new AgentRuntimeInfo(agentIdentifier1, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie");
        agentRuntimeInfo1.busy(new AgentBuildingInfo("building", "buildLocator"));

        agentService.updateRuntimeInfo(agentRuntimeInfo1);

        String newIp = agentService.getAgentByUUID("uuid").getIpaddress();
        assertThat(newIp, is("10.18.3.95"));
    }

    @Test
    public void shouldUpdateAgentWorkingDirWhenItChanges() throws Exception {
        AgentIdentifier agentIdentifier1 = new AgentIdentifier("localhost", "10.18.3.95", "uuid");
        AgentRuntimeInfo agentRuntimeInfo1 = new AgentRuntimeInfo(agentIdentifier1, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie");
        agentRuntimeInfo1.busy(new AgentBuildingInfo("building", "buildLocator"));
        agentRuntimeInfo1.setLocation("/myDirectory");

        agentService.updateRuntimeInfo(agentRuntimeInfo1);

        assertThat(agentService.findAgentAndRefreshStatus("uuid").getLocation(), is("/myDirectory"));
    }


    @Test
    public void shouldLogWarningWhenIPAddressChanges() throws Exception {
        AgentIdentifier agentIdentifier1 = new AgentIdentifier("localhost", "10.18.3.95", "uuid");
        AgentRuntimeInfo agentRuntimeInfo1 = new AgentRuntimeInfo(agentIdentifier1, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie");
        agentRuntimeInfo1.busy(new AgentBuildingInfo("building", "buildLocator"));
        agentRuntimeInfo1.setLocation("/myDirectory");

        try (LogFixture logging = logFixtureFor(AgentService.class, Level.DEBUG)) {
            agentService.updateRuntimeInfo(agentRuntimeInfo1);
            assertThat(logging.getLog(),
                    containsString("Agent with UUID [uuid] changed IP Address from [10.81.2.1] to [10.18.3.95]"));
        }
    }

    public JobIdentifier jobIdentifier(long id) {
        return new JobIdentifier("", "", "", "1", "", id);
    }
}



