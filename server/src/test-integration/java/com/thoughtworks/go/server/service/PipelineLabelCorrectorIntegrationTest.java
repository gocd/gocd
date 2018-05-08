/*
 * Copyright 2018 ThoughtWorks, Inc.
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
import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.dao.PipelineSqlMapDao;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.util.GoConfigFileHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.sql.SQLException;

import static com.thoughtworks.go.util.IBatisUtil.arguments;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:testPropertyConfigurer.xml"
})
public class PipelineLabelCorrectorIntegrationTest {
    @Autowired private DatabaseAccessHelper dbHelper;
    @Autowired private GoConfigDao goConfigDao;
    @Autowired private PipelineSqlMapDao pipelineSqlMapDao;
    @Autowired private PipelineLabelCorrector pipelineLabelCorrector;

    private GoConfigFileHelper configHelper = new GoConfigFileHelper();

    @Before
    public void setUp() throws Exception {
        configHelper.usingCruiseConfigDao(goConfigDao);
        configHelper.onSetUp();
        dbHelper.onSetUp();
    }

    @After
    public void teardown() throws Exception {
        dbHelper.onTearDown();
        configHelper.onTearDown();
    }

    @Test
    public void shouldRemoveDuplicateEntriesForPipelineCounterFromDbAndKeepTheOneMatchingPipelineNameCaseInConfig() throws SQLException {
        String pipelineName = "Pipeline-Name";
        configHelper.addPipeline(pipelineName, "stage-name");
        pipelineSqlMapDao.getSqlMapClient().insert("insertPipelineLabelCounter", arguments("pipelineName", pipelineName.toLowerCase()).and("count", 10).asMap());
        pipelineSqlMapDao.getSqlMapClient().insert("insertPipelineLabelCounter", arguments("pipelineName", pipelineName.toUpperCase()).and("count", 20).asMap());
        pipelineSqlMapDao.getSqlMapClient().insert("insertPipelineLabelCounter", arguments("pipelineName", pipelineName).and("count", 30).asMap());
        assertThat(pipelineSqlMapDao.getPipelineNamesWithMultipleEntriesForLabelCount().size(), is(1));
        assertThat(pipelineSqlMapDao.getPipelineNamesWithMultipleEntriesForLabelCount().get(0).equalsIgnoreCase(pipelineName), is(true));

        pipelineLabelCorrector.correctPipelineLabelCountEntries();
        assertThat(pipelineSqlMapDao.getPipelineNamesWithMultipleEntriesForLabelCount().isEmpty(), is(true));
        assertThat(pipelineSqlMapDao.getCounterForPipeline(pipelineName), is(30));
        assertThat(pipelineSqlMapDao.getCounterForPipeline(pipelineName.toLowerCase()), is(30));
        assertThat(pipelineSqlMapDao.getCounterForPipeline(pipelineName.toUpperCase()), is(30));
    }
}
