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
package com.thoughtworks.go.server.service;

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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:/applicationContext-global.xml",
        "classpath:/applicationContext-dataLocalAccess.xml",
        "classpath:/testPropertyConfigurer.xml",
        "classpath:/spring-all-servlet.xml",
})
public class ConfigDbStateRepositoryIntegrationTest {
    @Autowired
    private GoConfigDao goConfigDao;
    @Autowired
    ConfigDbStateRepository configDbStateRepository;
    @Autowired
    private DatabaseAccessHelper dbHelper;

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
        assertThat(configDbStateRepository.getHibernateTemplate().getSessionFactory()).isNotNull();
    }

    @Test
    public void shouldPersistConfigStageArtifactPurgeConfiguration() {
        configHelper.addPipeline("pipeline-one", "stage-zero");
        configHelper.addStageToPipeline("pipeline-one", StageConfigMother.custom("stage-one", true, "job-foo"));
        configHelper.addPipeline("pipeline-two", "stage-two-zero");
        configHelper.addStageToPipeline("pipeline-two", StageConfigMother.custom("stage-two-one", true, "job-foo"));
        configHelper.addStageToPipeline("pipeline-two", StageConfigMother.custom("stage-one", "job-bar"));

        configDbStateRepository.flushConfigState();

        List<StageArtifactCleanupProhibited> list = (List<StageArtifactCleanupProhibited>) configDbStateRepository.getHibernateTemplate().find("from StageArtifactCleanupProhibited");

        assertThat(list).hasSize(5);

        assertThat(list).contains(new StageArtifactCleanupProhibited().setPipelineName("pipeline-one").setStageName("stage-zero").setProhibited(false));
        assertThat(list).contains(new StageArtifactCleanupProhibited().setPipelineName("pipeline-one").setStageName("stage-one").setProhibited(true));

        assertThat(list).contains(new StageArtifactCleanupProhibited().setPipelineName("pipeline-two").setStageName("stage-two-zero").setProhibited(false));
        assertThat(list).contains(new StageArtifactCleanupProhibited().setPipelineName("pipeline-two").setStageName("stage-two-one").setProhibited(true));
        assertThat(list).contains(new StageArtifactCleanupProhibited().setPipelineName("pipeline-two").setStageName("stage-one").setProhibited(false));
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

        List<StageArtifactCleanupProhibited> list = (List<StageArtifactCleanupProhibited>) configDbStateRepository.getHibernateTemplate().find("from StageArtifactCleanupProhibited");
        assertThat(list.size()).isEqualTo(2);

        assertThat(list).contains(new StageArtifactCleanupProhibited().setPipelineName("pipeline-one").setStageName("stage-zero").setProhibited(false));
        assertThat(list).contains(new StageArtifactCleanupProhibited().setPipelineName("pipeline-one").setStageName("stage-one").setProhibited(true));
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

        List<StageArtifactCleanupProhibited> list = (List<StageArtifactCleanupProhibited>) configDbStateRepository.getHibernateTemplate().find("from StageArtifactCleanupProhibited");
        assertThat(list.size()).isEqualTo(2);

        assertThat(list).contains(new StageArtifactCleanupProhibited().setPipelineName("pipeline-one").setStageName("stage-zero").setProhibited(false));
        assertThat(list).contains(new StageArtifactCleanupProhibited().setPipelineName("pipeline-one").setStageName("stage-one").setProhibited(false));
    }
}
