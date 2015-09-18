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

package com.thoughtworks.go.domain.activity;

import java.sql.SQLException;

import com.thoughtworks.go.config.GoConfigDao;
import com.thoughtworks.go.domain.Pipeline;
import com.thoughtworks.go.domain.Stage;
import com.thoughtworks.go.domain.StageConfigIdentifier;
import com.thoughtworks.go.domain.StageIdentifier;
import com.thoughtworks.go.domain.StageState;
import com.thoughtworks.go.fixture.PipelineWithTwoStages;
import com.thoughtworks.go.helper.StageMother;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.dao.StageDao;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.util.GoConfigFileHelper;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:WEB-INF/applicationContext-acegi-security.xml"
})
public class StageStatusCacheTest {
    @Autowired private GoConfigDao goConfigDao;
    @Autowired private StageStatusCache stageStatusCache;
	@Autowired private DatabaseAccessHelper dbHelper;
    @Autowired private MaterialRepository materialRepository;
    @Autowired private TransactionTemplate transactionTemplate;

    private PipelineWithTwoStages pipelineFixture;
    private static GoConfigFileHelper configFileHelper = new GoConfigFileHelper();

    @Before
    public void setUp() throws Exception {
        dbHelper.onSetUp();
        configFileHelper.onSetUp();
        configFileHelper.usingEmptyConfigFileWithLicenseAllowsUnlimitedAgents();
        configFileHelper.usingCruiseConfigDao(goConfigDao);

        pipelineFixture = new PipelineWithTwoStages(materialRepository, transactionTemplate);
        pipelineFixture.usingConfigHelper(configFileHelper).usingDbHelper(dbHelper).onSetUp();
    }

    @After
    public void teardown() throws Exception {
        dbHelper.onTearDown();
        pipelineFixture.onTearDown();
    }

    @Test
    public void shouldLoadMostRecentInstanceFromDBForTheFirstTime() throws SQLException {
        pipelineFixture.createdPipelineWithAllStagesPassed();

        Pipeline pipeline = pipelineFixture.createPipelineWithFirstStageScheduled();

        Stage expect = pipeline.getStages().byName(pipelineFixture.devStage);
        assertThat(stageStatusCache.currentStage(new StageConfigIdentifier(pipelineFixture.pipelineName,
                pipelineFixture.devStage)), is(expect));
    }

    @Test
    public void shouldLoadMostRecentInstanceFromDBOnlyOnce() throws SQLException {
        Mockery mockery = new Mockery();
        final StageDao mock = mockery.mock(StageDao.class);
        final StageConfigIdentifier identifier = new StageConfigIdentifier("cruise", "dev");
        final Stage instance = StageMother.failingStage("dev");

        mockery.checking(new Expectations() {
            {
                one(mock).mostRecentStage(identifier);
                will(returnValue(instance));
            }
        });

        StageStatusCache cache = new StageStatusCache(mock);
        assertThat(cache.currentStage(identifier).getName(), is(instance.getName()));

        //call currentStage for the second time, should not call stageDao now
        assertThat(cache.currentStage(identifier).getName(), is(instance.getName()));
    }

    @Test
    public void shouldQueryTheDbOnlyOnceForStagesThatHaveNeverBeenBuilt() throws SQLException {
        final StageDao stageDao = Mockito.mock(StageDao.class);
        final StageConfigIdentifier identifier = new StageConfigIdentifier("cruise", "dev");

        StageStatusCache cache = new StageStatusCache(stageDao);
        when(stageDao.mostRecentStage(identifier)).thenReturn(null);
        assertThat(cache.currentStage(identifier), is(nullValue()));

        assertThat(cache.currentStage(identifier), is(nullValue()));

        verify(stageDao, times(1)).mostRecentStage(identifier);
    }

    @Test
    public void shouldRemoveNeverBeenBuiltWhenTheStageIsBuiltForTheFirstTime() throws SQLException {
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
