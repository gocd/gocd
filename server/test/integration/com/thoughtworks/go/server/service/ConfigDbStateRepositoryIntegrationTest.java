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

import java.util.List;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.domain.StageArtifactCleanupProhibited;
import com.thoughtworks.go.helper.StageConfigMother;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.util.GoConfigFileHelper;
import com.thoughtworks.go.util.ReflectionUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:WEB-INF/applicationContext-acegi-security.xml"
})
public class ConfigDbStateRepositoryIntegrationTest {
    @Autowired private GoConfigDao goConfigDao;
    @Autowired ConfigDbStateRepository configDbStateRepository;
    @Autowired private DatabaseAccessHelper dbHelper;

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
    public void shouldPopulateSessionFactory() {
        assertThat(configDbStateRepository.getHibernateTemplate().getSessionFactory(), is(not(nullValue())));
    }

    @Test
    public void shouldPersistConfigStageArtifactPurgeConfiguration() {
        configHelper.addPipeline("pipeline-one", "stage-zero");
        configHelper.addStageToPipeline("pipeline-one", StageConfigMother.custom("stage-one", true, "job-foo"));
        configHelper.addPipeline("pipeline-two", "stage-two-zero");
        configHelper.addStageToPipeline("pipeline-two", StageConfigMother.custom("stage-two-one", true, "job-foo"));
        configHelper.addStageToPipeline("pipeline-two", StageConfigMother.custom("stage-one", "job-bar"));

        configDbStateRepository.flushConfigState();

        List<StageArtifactCleanupProhibited> list = configDbStateRepository.getHibernateTemplate().find("from StageArtifactCleanupProhibited");
        assertThat(list.size(), is(5));

        assertThat(list, hasItem(new StageArtifactCleanupProhibited("pipeline-one", "stage-zero", false)));
        assertThat(list, hasItem(new StageArtifactCleanupProhibited("pipeline-one", "stage-one", true)));

        assertThat(list, hasItem(new StageArtifactCleanupProhibited("pipeline-two", "stage-two-zero", false)));
        assertThat(list, hasItem(new StageArtifactCleanupProhibited("pipeline-two", "stage-two-one", true)));
        assertThat(list, hasItem(new StageArtifactCleanupProhibited("pipeline-two", "stage-one", false)));
    }

    @Test
    public void shouldUpdateExistingArtifactPurgeConfigurationOnFlush() {
        configHelper.addPipeline("pipeline-one", "stage-zero");
        configHelper.addStageToPipeline("pipeline-one", StageConfigMother.custom("stage-one", "job-foo"));

        configDbStateRepository.flushConfigState();

        CruiseConfig cruiseConfig = configHelper.currentConfig();
        StageConfig stageConfig = cruiseConfig.stageConfigByName(new CaseInsensitiveString("pipeline-one"), new CaseInsensitiveString("stage-one"));
        ReflectionUtil.setField(stageConfig, "artifactCleanupProhibited", true);
        configHelper.writeConfigFile(cruiseConfig);

        configDbStateRepository.flushConfigState();

        List<StageArtifactCleanupProhibited> list = configDbStateRepository.getHibernateTemplate().find("from StageArtifactCleanupProhibited");
        assertThat(list.size(), is(2));

        assertThat(list, hasItem(new StageArtifactCleanupProhibited("pipeline-one", "stage-zero", false)));
        assertThat(list, hasItem(new StageArtifactCleanupProhibited("pipeline-one", "stage-one", true)));
    }

    @Test
    public void shouldUpdateNonExistentConfigurationsToPurge() {
        configHelper.addPipeline("pipeline-one", "stage-zero");
        configHelper.addStageToPipeline("pipeline-one", StageConfigMother.custom("stage-one", true, "job-foo"));

        configDbStateRepository.flushConfigState();

        CruiseConfig cruiseConfig = configHelper.currentConfig();
        PipelineConfig pipelineConfig = cruiseConfig.pipelineConfigByName(new CaseInsensitiveString("pipeline-one"));
        pipelineConfig.remove(1);

        configHelper.writeConfigFile(cruiseConfig);

        configDbStateRepository.flushConfigState();

        List<StageArtifactCleanupProhibited> list = configDbStateRepository.getHibernateTemplate().find("from StageArtifactCleanupProhibited");
        assertThat(list.size(), is(2));

        assertThat(list, hasItem(new StageArtifactCleanupProhibited("pipeline-one", "stage-zero", false)));
        assertThat(list, hasItem(new StageArtifactCleanupProhibited("pipeline-one", "stage-one", false)));
    }
}
