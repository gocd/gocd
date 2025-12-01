/*
 * Copyright Thoughtworks, Inc.
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@ExtendWith(DatabaseDiskIsFull.class)
@ContextConfiguration(locations = {
        "classpath:/applicationContext-global.xml",
        "classpath:/applicationContext-dataLocalAccess.xml",
        "classpath:/testPropertyConfigurer.xml",
        "classpath:/spring-all-servlet.xml",
})
public class DatabaseDiskSpaceFullTest {
    @Autowired private ServerHealthService serverHealthService;
    @Autowired private DatabaseAccessHelper databaseAccessHelper;
    @Autowired private SchedulingCheckerService schedulingChecker;
    @Autowired private MaterialRepository materialRepository;
    @Autowired private GoConfigDao goConfigDao;
    @Autowired private TransactionTemplate transactionTemplate;

    private PipelineWithTwoStages pipelineFixture;
    private final GoConfigFileHelper configHelper = new GoConfigFileHelper();

    @BeforeEach
    public void setUp(@TempDir Path tempDir) throws Exception {
        configHelper.usingCruiseConfigDao(goConfigDao);
        pipelineFixture = new PipelineWithTwoStages(materialRepository, transactionTemplate, tempDir);
        serverHealthService.removeAllLogs();
        pipelineFixture.usingConfigHelper(configHelper).usingDbHelper(databaseAccessHelper).onSetUp();
        configHelper.setupMailHost();
    }

    @AfterEach
    public void tearDown() throws Exception {
        serverHealthService.removeAllLogs();
        pipelineFixture.onTearDown();
    }

    @Test
    public void shouldNotRerunStageIfDiskspaceIsFull() {
        Pipeline pipeline = pipelineFixture.createdPipelineWithAllStagesPassed();
        ServerHealthStateOperationResult result = new ServerHealthStateOperationResult();
        assertThat(schedulingChecker.canScheduleStage(pipeline.getIdentifier(), pipelineFixture.devStage, "anyone", result)).isFalse();
        assertThat(schedulingChecker.canScheduleStage(pipeline.getIdentifier(), pipelineFixture.devStage, "anyone", new ServerHealthStateOperationResult())).isFalse();
    }

    @Test
    public void shouldNotManualTriggerIfDiskspaceIsFull() {
        pipelineFixture.createdPipelineWithAllStagesPassed();
        assertThat(schedulingChecker.canManuallyTrigger(pipelineFixture.pipelineConfig(), "anyone", new ServerHealthStateOperationResult())).isFalse();
        assertThat(schedulingChecker.canTriggerManualPipeline(pipelineFixture.pipelineConfig(), "anyone", new ServerHealthStateOperationResult())).isFalse();
    }

}
