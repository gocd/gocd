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
package com.thoughtworks.go.domain.activity;

import com.thoughtworks.go.config.GoConfigDao;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.fixture.PipelineWithTwoStages;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.dao.StageDao;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.messaging.*;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
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
@ContextConfiguration(locations = {
        "classpath:/applicationContext-global.xml",
        "classpath:/applicationContext-dataLocalAccess.xml",
        "classpath:/testPropertyConfigurer.xml",
        "classpath:/spring-all-servlet.xml",
})
public class StageResultCacheTest {
    private final GoConfigFileHelper configHelper = new GoConfigFileHelper();

    @Autowired private StageDao stageDao;
    @Autowired private GoConfigDao goConfigDao;
    @Autowired private StageStatusTopic stageStatusTopic;
    @Autowired private MessagingService<GoMessage> messagingService;
    @Autowired private StageResultCache stageResultCache;
	@Autowired private DatabaseAccessHelper dbHelper;
    @Autowired private MaterialRepository materialRepository;
    @Autowired private TransactionTemplate transactionTemplate;

    private PipelineWithTwoStages pipelineFixture;

    @BeforeEach
    public void setUp(@TempDir Path tempDir) throws Exception {
        configHelper.usingCruiseConfigDao(goConfigDao);

        pipelineFixture = new PipelineWithTwoStages(materialRepository, transactionTemplate, tempDir);
        pipelineFixture.usingConfigHelper(configHelper).usingDbHelper(dbHelper).onSetUp();
    }

    @AfterEach
    public void teardown() throws Exception {
        pipelineFixture.onTearDown();
    }

    @Test
    public void shouldReturnUnknownForStageWithNoHistory() {
        StageConfigIdentifier stage = new StageConfigIdentifier("cruise", "dev");

        assertThat(stageResultCache.previousResult(stage)).isEqualTo(StageResult.Unknown);

        stageResultCache.updateCache(stage, StageResult.Passed);
        assertThat(stageResultCache.previousResult(stage)).isEqualTo(StageResult.Unknown);
    }

    @Test
    public void shouldUpdatePreviousResultWhenNewResultComeIn() {
        StageConfigIdentifier stage = new StageConfigIdentifier("cruise", "dev");

        stageResultCache.updateCache(stage, StageResult.Failed);

        stageResultCache.updateCache(stage, StageResult.Passed);
        assertThat(stageResultCache.previousResult(stage)).isEqualTo(StageResult.Failed);
    }

    @Test
    public void shouldLoadStageResultFromDBWhenNoGivenStageInCache() {
        pipelineFixture.createdPipelineWithAllStagesPassed();
        StageConfigIdentifier stage = new StageConfigIdentifier(pipelineFixture.pipelineName, pipelineFixture.ftStage);
        stageResultCache.updateCache(stage, StageResult.Failed);
        StageResult stageResult = stageResultCache.previousResult(stage);
        assertThat(stageResult).isEqualTo(StageResult.Passed);
    }

    @Test
    public void shouldSendStageResultMessageWhenStageComplete() {
        Pipeline pipeline = pipelineFixture.createdPipelineWithAllStagesPassed();

        StageResultTopicStub stub = new StageResultTopicStub(messagingService);
        StageResultCache cache = new StageResultCache(stageDao, stub, stageStatusTopic);

        StageIdentifier identifier = pipeline.getFirstStage().getIdentifier();

        cache.onMessage(new StageStatusMessage(identifier, StageState.Passed, StageResult.Passed));
        assertThat(stub.message).isEqualTo(new StageResultMessage(identifier, StageEvent.Passes, Username.BLANK));

        cache.onMessage(new StageStatusMessage(identifier, StageState.Failed, StageResult.Failed));
        assertThat(stub.message).isEqualTo(new StageResultMessage(identifier, StageEvent.Breaks, Username.BLANK));
    }


    private static class StageResultTopicStub extends StageResultTopic {
        private StageResultMessage message;

        public StageResultTopicStub(MessagingService<GoMessage> messaging) {
            super(messaging);
        }

        @Override
        public void post(StageResultMessage message) {
            this.message = message;
        }
    }
}
