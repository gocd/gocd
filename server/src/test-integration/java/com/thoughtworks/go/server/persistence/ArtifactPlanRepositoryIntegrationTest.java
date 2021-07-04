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
package com.thoughtworks.go.server.persistence;

import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.helper.BuildPlanMother;
import com.thoughtworks.go.helper.PipelineMother;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.dao.JobInstanceSqlMapDao;
import com.thoughtworks.go.server.service.InstanceFactory;
import com.thoughtworks.go.util.TimeProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Arrays;
import java.util.List;

import static com.thoughtworks.go.helper.ModificationsMother.modifySomeFiles;
import static com.thoughtworks.go.util.GoConstants.DEFAULT_APPROVED_BY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {
        "classpath:/applicationContext-global.xml",
        "classpath:/applicationContext-dataLocalAccess.xml",
        "classpath:/testPropertyConfigurer.xml",
        "classpath:/spring-all-servlet.xml",
})
public class ArtifactPlanRepositoryIntegrationTest {
    private static final String JOB_NAME = "functional";
    private static final String OTHER_JOB_NAME = "unit";
    private static final String STAGE_NAME = "mingle";
    private static final String PIPELINE_NAME = "pipeline";

    @Autowired
    private JobInstanceSqlMapDao jobInstanceDao;
    @Autowired
    private ArtifactPlanRepository artifactPlanRepository;
    @Autowired
    private DatabaseAccessHelper dbHelper;
    @Autowired
    private InstanceFactory instanceFactory;

    private long stageId;


    @BeforeEach
    public void setUp() throws Exception {
        dbHelper.onSetUp();

        PipelineConfig pipelineConfig = PipelineMother.withSingleStageWithMaterials(PIPELINE_NAME, STAGE_NAME, BuildPlanMother.withBuildPlans(JOB_NAME, OTHER_JOB_NAME));
        DefaultSchedulingContext schedulingContext = new DefaultSchedulingContext(DEFAULT_APPROVED_BY);
        Pipeline savedPipeline = instanceFactory.createPipelineInstance(pipelineConfig, modifySomeFiles(pipelineConfig), schedulingContext, "md5-test", new TimeProvider());
        dbHelper.savePipelineWithStagesAndMaterials(savedPipeline);
        Stage savedStage = savedPipeline.getFirstStage();
        stageId = savedStage.getId();
    }

    @AfterEach
    public void tearDown() throws Exception {
        dbHelper.onTearDown();
    }


    @Test
    public void shouldSaveArtifactPlan() {
        // Arrange
        JobInstance jobInstance = jobInstanceDao.save(stageId, new JobInstance(JOB_NAME));
        ArtifactPlan artifactPlan = new ArtifactPlan(ArtifactPlanType.file, "src", "dest");
        artifactPlan.setBuildId(jobInstance.getId());

        // Act
        artifactPlanRepository.save(artifactPlan);

        // Assert
        assertThat(artifactPlan.getId(), is(not(nullValue())));
    }

    @Test
    public void shouldLoadSavedArtifactPlan() {
        // Arrange
        JobInstance jobInstance = jobInstanceDao.save(stageId, new JobInstance(JOB_NAME));
        ArtifactPlan savedArtifactPlan = new ArtifactPlan(ArtifactPlanType.file, "src", "dest");
        savedArtifactPlan.setBuildId(jobInstance.getId());
        artifactPlanRepository.save(savedArtifactPlan);

        // Act
        List<ArtifactPlan> artifactPlanList = artifactPlanRepository.findByBuildId(jobInstance.getId());

        // Assert
        assertThat(artifactPlanList.size(), is(1));
        assertThat(artifactPlanList.get(0), is(savedArtifactPlan));
    }

    @Test
    public void shouldLoadSavedArtifactPlanWithTypeUnit() {
        // Arrange
        JobInstance jobInstance = jobInstanceDao.save(stageId, new JobInstance(JOB_NAME));
        ArtifactPlan savedArtifactPlan = new ArtifactPlan(ArtifactPlanType.unit, "src", "dest");
        savedArtifactPlan.setBuildId(jobInstance.getId());
        artifactPlanRepository.save(savedArtifactPlan);

        // Act
        List<ArtifactPlan> artifactPlanList = artifactPlanRepository.findByBuildId(jobInstance.getId());

        // Assert
        assertThat(artifactPlanList.size(), is(1));
        assertThat(artifactPlanList.get(0), is(savedArtifactPlan));
    }

    @Test
    public void shouldLoadSavedTestArtifactPlan() {
        // Arrange
        JobInstance jobInstance = jobInstanceDao.save(stageId, new JobInstance(JOB_NAME));
        ArtifactPlan savedArtifactPlan = new ArtifactPlan(ArtifactPlanType.unit, null, null);
        savedArtifactPlan.setBuildId(jobInstance.getId());
        artifactPlanRepository.save(savedArtifactPlan);

        // Act
        List<ArtifactPlan> artifactPlanList = artifactPlanRepository.findByBuildId(jobInstance.getId());

        // Assert
        assertThat(artifactPlanList.size(), is(1));
        ArtifactPlan loadedArtifactPlan = artifactPlanList.get(0);
        assertThat(loadedArtifactPlan, is(savedArtifactPlan));
    }

    @Test
    public void shouldSaveACopyOfAnArtifactPlan() {
        // Arrange
        JobInstance firstJobInstance = jobInstanceDao.save(stageId, new JobInstance(JOB_NAME + "1"));
        JobInstance secondJobInstance = jobInstanceDao.save(stageId, new JobInstance(JOB_NAME + "2"));
        ArtifactPlan artifactPlan = new ArtifactPlan(ArtifactPlanType.file, "src", "dest");

        // Act
        ArtifactPlan artifactPlanOfFirstJob = artifactPlanRepository.saveCopyOf(firstJobInstance.getId(), artifactPlan);

        ArtifactPlan artifactPlanOfSecondJob = artifactPlanRepository.saveCopyOf(secondJobInstance.getId(), artifactPlan);

        // Assert
        List<ArtifactPlan> firstJobArtifactPlans = artifactPlanRepository.findByBuildId(firstJobInstance.getId());
        assertThat(firstJobArtifactPlans.size(), is(1));
        assertThat(firstJobArtifactPlans.get(0).getId(), equalTo(artifactPlanOfFirstJob.getId()));
        assertThat(firstJobArtifactPlans, hasItem(artifactPlanOfFirstJob));
        assertThat(artifactPlan.getId(), is(not(nullValue())));

        List<ArtifactPlan> secondJobArtifactPlans = artifactPlanRepository.findByBuildId(secondJobInstance.getId());
        assertThat(secondJobArtifactPlans.size(), is(1));
        assertThat(secondJobArtifactPlans.get(0).getId(), equalTo(artifactPlanOfSecondJob.getId()));
        assertThat(secondJobArtifactPlans, hasItem(artifactPlanOfSecondJob));
        assertThat(artifactPlan.getId(), is(not(nullValue())));

        assertThat(artifactPlanOfFirstJob.getId(), not(equalTo(artifactPlanOfSecondJob.getId())));
    }

    @Test
    public void shouldDeleteArtifactPlans() throws Exception {
        JobInstance jobInstance = jobInstanceDao.save(stageId, new JobInstance(JOB_NAME));
        ArtifactPlan artifactPlan = new ArtifactPlan(ArtifactPlanType.file, "src", "dest");
        artifactPlan.setBuildId(jobInstance.getId());
        artifactPlanRepository.save(artifactPlan);
        List<ArtifactPlan> artifactPlanList = artifactPlanRepository.findByBuildId(jobInstance.getId());
        assertThat(artifactPlanList.size(), is(1));

        artifactPlanRepository.deleteAll(Arrays.asList(artifactPlan));
        artifactPlanList = artifactPlanRepository.findByBuildId(jobInstance.getId());
        assertThat(artifactPlanList.size(), is(0));
    }
}
