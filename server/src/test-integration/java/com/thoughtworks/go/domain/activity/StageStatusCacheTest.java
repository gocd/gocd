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
package com.thoughtworks.go.domain.activity;

import com.thoughtworks.go.config.GoConfigDao;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.fixture.PipelineWithTwoStages;
import com.thoughtworks.go.helper.StageMother;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.dao.StageDao;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.util.GoConfigFileHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.nio.file.Path;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {
        "classpath:/applicationContext-global.xml",
        "classpath:/applicationContext-dataLocalAccess.xml",
        "classpath:/testPropertyConfigurer.xml",
        "classpath:/spring-all-servlet.xml",
})
public class StageStatusCacheTest {
    @Autowired private GoConfigDao goConfigDao;
    @Autowired private StageStatusCache stageStatusCache;
	@Autowired private DatabaseAccessHelper dbHelper;
    @Autowired private MaterialRepository materialRepository;
    @Autowired private TransactionTemplate transactionTemplate;

    private PipelineWithTwoStages pipelineFixture;
    private static GoConfigFileHelper configFileHelper = new GoConfigFileHelper();

    @BeforeEach
    public void setUp(@TempDir Path tempDir) throws Exception {
        dbHelper.onSetUp();
        configFileHelper.onSetUp();
        configFileHelper.usingEmptyConfigFileWithLicenseAllowsUnlimitedAgents();
        configFileHelper.usingCruiseConfigDao(goConfigDao);

        pipelineFixture = new PipelineWithTwoStages(materialRepository, transactionTemplate, tempDir);
        pipelineFixture.usingConfigHelper(configFileHelper).usingDbHelper(dbHelper).onSetUp();
    }

    @AfterEach
    public void teardown() throws Exception {
        dbHelper.onTearDown();
        pipelineFixture.onTearDown();
    }

    @Test
    public void shouldLoadMostRecentInstanceFromDBForTheFirstTime() {
        pipelineFixture.createdPipelineWithAllStagesPassed();

        Pipeline pipeline = pipelineFixture.createPipelineWithFirstStageScheduled();

        Stage expect = pipeline.getStages().byName(pipelineFixture.devStage);
        assertThat(stageStatusCache.currentStage(new StageConfigIdentifier(pipelineFixture.pipelineName,
                pipelineFixture.devStage)), is(expect));
    }

    @Test
    public void shouldLoadMostRecentInstanceFromDBOnlyOnce() {
        final StageDao mock = mock(StageDao.class);
        final StageConfigIdentifier identifier = new StageConfigIdentifier("cruise", "dev");
        final Stage instance = StageMother.failingStage("dev");

        when(mock.mostRecentStage(identifier)).thenReturn(instance);

        StageStatusCache cache = new StageStatusCache(mock);
        assertThat(cache.currentStage(identifier).getName(), is(instance.getName()));

        //call currentStage for the second time, should not call stageDao now
        assertThat(cache.currentStage(identifier).getName(), is(instance.getName()));
    }

    @Test
    public void shouldQueryTheDbOnlyOnceForStagesThatHaveNeverBeenBuilt() {
        final StageDao stageDao = Mockito.mock(StageDao.class);
        final StageConfigIdentifier identifier = new StageConfigIdentifier("cruise", "dev");

        StageStatusCache cache = new StageStatusCache(stageDao);
        when(stageDao.mostRecentStage(identifier)).thenReturn(null);
        assertThat(cache.currentStage(identifier), is(nullValue()));

        assertThat(cache.currentStage(identifier), is(nullValue()));

        verify(stageDao, times(1)).mostRecentStage(identifier);
    }

    @Test
    public void shouldRemoveNeverBeenBuiltWhenTheStageIsBuiltForTheFirstTime() {
        final StageDao stageDao = Mockito.mock(StageDao.class);
        final StageConfigIdentifier identifier = new StageConfigIdentifier("cruise", "dev");
        final Stage instance = StageMother.failingStage("dev");
        instance.setIdentifier(new StageIdentifier("cruise", 1, "dev", "1"));
        when(stageDao.mostRecentStage(identifier)).thenReturn(null);

        StageStatusCache cache = new StageStatusCache(stageDao);
        assertThat(cache.currentStage(identifier), is(nullValue()));

        cache.stageStatusChanged(instance);
        assertThat(cache.currentStage(identifier), is(instance));

        verify(stageDao, times(1)).mostRecentStage(identifier);
    }

    @Test
    public void shouldRefreshCurrentStageWhenNewStageComes() {
        Stage stage = StageMother.failingStage("dev");
        StageIdentifier identifier = new StageIdentifier("cruise", null, "1", "dev", "1");
        stage.setIdentifier(identifier);

        stageStatusCache.stageStatusChanged(stage);
        assertThat(stageStatusCache.currentStage(identifier.stageConfigIdentifier()).stageState(),
                is(StageState.Failing));

        Stage newStage = StageMother.completedFailedStageInstance("pipeline-name", "dev", "linux-firefox");
        newStage.setIdentifier(identifier);

        stageStatusCache.stageStatusChanged(newStage);
        assertThat(stageStatusCache.currentStage(identifier.stageConfigIdentifier()).stageState(),
                is(StageState.Failed));
    }

}
