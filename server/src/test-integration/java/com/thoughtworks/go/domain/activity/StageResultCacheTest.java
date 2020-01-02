/*
 * Copyright 2020 ThoughtWorks, Inc.
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
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:/applicationContext-global.xml",
        "classpath:/applicationContext-dataLocalAccess.xml",
        "classpath:/testPropertyConfigurer.xml",
        "classpath:/spring-all-servlet.xml",
})
public class StageResultCacheTest {
    @Autowired private StageDao stageDao;
    @Autowired private GoConfigDao goConfigDao;
    @Autowired private StageStatusTopic stageStatusTopic;
    @Autowired private MessagingService messagingService;
    @Autowired private StageResultCache stageResultCache;
	@Autowired private DatabaseAccessHelper dbHelper;
    @Autowired private MaterialRepository materialRepository;
    @Autowired private TransactionTemplate transactionTemplate;
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    private PipelineWithTwoStages pipelineFixture;
    private static GoConfigFileHelper configFileHelper = new GoConfigFileHelper();

    @Before
    public void setUp() throws Exception {

        dbHelper.onSetUp();
        configFileHelper.onSetUp();
        configFileHelper.usingEmptyConfigFileWithLicenseAllowsUnlimitedAgents();
        configFileHelper.usingCruiseConfigDao(goConfigDao);

        pipelineFixture = new PipelineWithTwoStages(materialRepository, transactionTemplate, temporaryFolder);
        pipelineFixture.usingConfigHelper(configFileHelper).usingDbHelper(dbHelper).onSetUp();
    }

    @After
    public void teardown() throws Exception {
        dbHelper.onTearDown();
        pipelineFixture.onTearDown();
    }

    @Test
    public void shouldReturnUnknownForStageWithNoHistory() {
        StageConfigIdentifier stage = new StageConfigIdentifier("cruise", "dev");

        assertThat(stageResultCache.previousResult(stage), is(StageResult.Unknown));

        stageResultCache.updateCache(stage, StageResult.Passed);
        assertThat(stageResultCache.previousResult(stage), is(StageResult.Unknown));
    }

    @Test
    public void shouldUpdatePreviousResultWhenNewResultComeIn() {
        StageConfigIdentifier stage = new StageConfigIdentifier("cruise", "dev");

        stageResultCache.updateCache(stage, StageResult.Failed);

        stageResultCache.updateCache(stage, StageResult.Passed);
        assertThat(stageResultCache.previousResult(stage), is(StageResult.Failed));
    }

    @Test
    public void shouldLoadStageResultFromDBWhenNoGivenStageInCache() {
        pipelineFixture.createdPipelineWithAllStagesPassed();
        StageConfigIdentifier stage = new StageConfigIdentifier(pipelineFixture.pipelineName, pipelineFixture.ftStage);
        stageResultCache.updateCache(stage, StageResult.Failed);
        StageResult stageResult = stageResultCache.previousResult(stage);
        assertThat(stageResult, is(StageResult.Passed));
    }

    @Test
    public void shouldSendStageResultMessageWhenStageComplete() {
        Pipeline pipeline = pipelineFixture.createdPipelineWithAllStagesPassed();

        StageResultTopicStub stub = new StageResultTopicStub(messagingService);
        StageResultCache cache = new StageResultCache(stageDao, stub, stageStatusTopic);

        StageIdentifier identifier = pipeline.getFirstStage().getIdentifier();

        cache.onMessage(new StageStatusMessage(identifier, StageState.Passed, StageResult.Passed));
        assertThat(stub.message, is(new StageResultMessage(identifier, StageEvent.Passes, Username.BLANK)));

        cache.onMessage(new StageStatusMessage(identifier, StageState.Failed, StageResult.Failed));
        assertThat(stub.message, is(new StageResultMessage(identifier, StageEvent.Breaks, Username.BLANK)));
    }


    private class StageResultTopicStub extends StageResultTopic {
        private StageResultMessage message;

        public StageResultTopicStub(MessagingService messaging) {
            super(messaging);
        }

        @Override
        public void post(StageResultMessage message) {
            this.message = message;
        }
    }
}
