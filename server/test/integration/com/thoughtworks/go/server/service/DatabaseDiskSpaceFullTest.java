/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.GoConfigDao;
import com.thoughtworks.go.domain.Pipeline;
import com.thoughtworks.go.fixture.DatabaseDiskIsFull;
import com.thoughtworks.go.fixture.PipelineWithTwoStages;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.server.service.result.ServerHealthStateOperationResult;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.util.GoConfigFileHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:WEB-INF/applicationContext-acegi-security.xml"
})
public class DatabaseDiskSpaceFullTest {
    @Autowired private ServerHealthService serverHealthService;
    @Autowired private DatabaseAccessHelper databaseAccessHelper;
    @Autowired private SchedulingCheckerService schedulingChecker;
    @Autowired private MaterialRepository materialRepository;
    @Autowired private GoConfigDao goConfigDao;
    @Autowired private TransactionTemplate transactionTemplate;

    private DatabaseDiskIsFull diskIsFull = new DatabaseDiskIsFull();
    private PipelineWithTwoStages fixture;
    private GoConfigFileHelper configHelper = new GoConfigFileHelper();

    @Before
    public void setUp() throws Exception {
        configHelper.usingCruiseConfigDao(goConfigDao);
        configHelper.onSetUp();
        fixture = new PipelineWithTwoStages(materialRepository, transactionTemplate);
        serverHealthService.removeAllLogs();
        fixture.usingConfigHelper(configHelper).usingDbHelper(databaseAccessHelper).onSetUp();
        configHelper.setupMailHost();
        diskIsFull.onSetUp();
    }

    @After
    public void tearDown() throws Exception {
        diskIsFull.onTearDown();
        serverHealthService.removeAllLogs();
        fixture.onTearDown();
        configHelper.onTearDown();
    }

    @Test
    public void shouldNotRerunStageIfDiskspaceIsFull() throws Exception {
        Pipeline pipeline = fixture.createdPipelineWithAllStagesPassed();
        ServerHealthStateOperationResult result = new ServerHealthStateOperationResult();
        assertThat(schedulingChecker.canScheduleStage(pipeline.getIdentifier(), fixture.devStage, "anyone", result), is(false));
        assertThat(schedulingChecker.canScheduleStage(pipeline.getIdentifier(), fixture.devStage, "anyone", new ServerHealthStateOperationResult()), is(false));
    }

    @Test
    public void shouldNotManualTriggerIfDiskspaceIsFull() throws Exception {
        diskIsFull.onSetUp();
        fixture.createdPipelineWithAllStagesPassed();
        assertThat(schedulingChecker.canManuallyTrigger(fixture.pipelineConfig(), "anyone", new ServerHealthStateOperationResult()), is(false));
        assertThat(schedulingChecker.canTriggerManualPipeline(fixture.pipelineConfig(), "anyone", new ServerHealthStateOperationResult()), is(false));
    }

}