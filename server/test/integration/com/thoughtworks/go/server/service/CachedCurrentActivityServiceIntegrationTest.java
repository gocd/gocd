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
import com.thoughtworks.go.fixture.PipelineWithMultipleStages;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
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
public class CachedCurrentActivityServiceIntegrationTest {

    @Autowired private CachedCurrentActivityService cachedCurrentActivityService;
    @Autowired private MaterialRepository materialRepository;
    @Autowired private GoConfigDao goConfigDao;
    @Autowired private DatabaseAccessHelper dbHelper;
    @Autowired private TransactionTemplate transactionTemplate;

    private PipelineWithMultipleStages fixture;
    private GoConfigFileHelper configFileHelper = new GoConfigFileHelper();

    @Before
    public void setup() throws Exception {
        configFileHelper.usingCruiseConfigDao(goConfigDao);
        configFileHelper.onSetUp();

        fixture = new PipelineWithMultipleStages(3, materialRepository, transactionTemplate);
        fixture.usingConfigHelper(configFileHelper).usingDbHelper(dbHelper).onSetUp();
    }

    @After
    public void tearDown() throws Exception {
        fixture.onTearDown();
        configFileHelper.onTearDown();
        dbHelper.onTearDown();
    }

    @Test
    public void testShouldReturnTrueIfAStageOfAPipelineIsActive() {
        Pipeline pipeline = fixture.createPipelineWithFirstStagePassedAndSecondStageRunning();
        assertThat(cachedCurrentActivityService.isAnyStageActive(pipeline.getIdentifier()), is(true));
    }

    @Test
    public void testShouldReturnFalseIfAStageOfAPipelineHasNotStarted() {
        Pipeline pipeline = fixture.createPipelineWithFirstStagePassedAndSecondStageHasNotStarted();
        assertThat(cachedCurrentActivityService.isAnyStageActive(pipeline.getIdentifier()), is(false));
    }

    @Test
    public void testShouldReturnTrueIfAStageOfAPipelineHasBeenScheduked() {
        Pipeline pipeline = fixture.createPipelineWithFirstStageScheduled();
        assertThat(cachedCurrentActivityService.isAnyStageActive(pipeline.getIdentifier()), is(true));
    }

    @Test
    public void testShouldReturnTrueIfAStageIsActive() {
        Pipeline pipeline = fixture.createPipelineWithFirstStagePassedAndSecondStageRunning();
        assertThat(cachedCurrentActivityService.isStageActive(pipeline.getName(), "ft"), is(true));
    }

}
