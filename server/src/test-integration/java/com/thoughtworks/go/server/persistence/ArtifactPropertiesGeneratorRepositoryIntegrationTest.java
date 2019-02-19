/*
 * Copyright 2017 ThoughtWorks, Inc.
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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Arrays;
import java.util.List;

import static com.thoughtworks.go.helper.ModificationsMother.modifySomeFiles;
import static com.thoughtworks.go.util.GoConstants.DEFAULT_APPROVED_BY;
import static org.junit.Assert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.not;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:testPropertyConfigurer.xml",
        "classpath:WEB-INF/spring-all-servlet.xml",
})
public class ArtifactPropertiesGeneratorRepositoryIntegrationTest {
    private static final String JOB_NAME = "functional";
    private static final String OTHER_JOB_NAME = "unit";
    private static final String STAGE_NAME = "mingle";
    private static final String PIPELINE_NAME = "pipeline";

    @Autowired private JobInstanceSqlMapDao jobInstanceDao;
    @Autowired private ArtifactPropertiesGeneratorRepository artifactPropertiesGeneratorRepository;
    @Autowired private DatabaseAccessHelper dbHelper;
    @Autowired private InstanceFactory instanceFactory;

    private long stageId;


    @Before
    public void setUp() throws Exception {
        dbHelper.onSetUp();

        PipelineConfig pipelineConfig = PipelineMother.withSingleStageWithMaterials(PIPELINE_NAME, STAGE_NAME, BuildPlanMother.withBuildPlans(JOB_NAME, OTHER_JOB_NAME));
        DefaultSchedulingContext schedulingContext = new DefaultSchedulingContext(DEFAULT_APPROVED_BY);
        Pipeline savedPipeline = instanceFactory.createPipelineInstance(pipelineConfig, modifySomeFiles(pipelineConfig), schedulingContext, "md5-test", new TimeProvider());
        dbHelper.savePipelineWithStagesAndMaterials(savedPipeline);
        Stage savedStage = savedPipeline.getFirstStage();
        stageId = savedStage.getId();
    }

    @After
    public void tearDown() throws Exception {
        dbHelper.onTearDown();
    }


    @Test
    public void shouldSaveArtifactPropertiesGenerator() {
        // Arrange
        JobInstance jobInstance = jobInstanceDao.save(stageId, new JobInstance(JOB_NAME));
        ArtifactPropertiesGenerator artifactPropertiesGenerator = new ArtifactPropertiesGenerator("test", "src", "//xpath");
        artifactPropertiesGenerator.setJobId(jobInstance.getId());

        // Act
        artifactPropertiesGeneratorRepository.save(artifactPropertiesGenerator);

        // Assert
        assertThat(artifactPropertiesGenerator.getId(), is(not(nullValue())));
    }

    @Test
    public void shouldLoadSavedArtifactPropertiesGenerator() {
        // Arrange
        JobInstance jobInstance = jobInstanceDao.save(stageId, new JobInstance(JOB_NAME));
        ArtifactPropertiesGenerator savedArtifactPropertiesGenerator = new ArtifactPropertiesGenerator("test", "src", "//xpath");
        savedArtifactPropertiesGenerator.setJobId(jobInstance.getId());
        artifactPropertiesGeneratorRepository.save(savedArtifactPropertiesGenerator);

        // Act
        List<ArtifactPropertiesGenerator> artifactPropertiesGeneratorList = artifactPropertiesGeneratorRepository.findByBuildId(jobInstance.getId());

        // Assert
        assertThat(artifactPropertiesGeneratorList.size(), is(1));
        assertThat(artifactPropertiesGeneratorList.get(0), is(savedArtifactPropertiesGenerator));
    }

    @Test
    public void shouldSaveACopyOfAnArtifactPropertiesGenerator() {
        // Arrange
        JobInstance firstJobInstance = jobInstanceDao.save(stageId, new JobInstance(JOB_NAME + "1"));
        JobInstance secondJobInstance = jobInstanceDao.save(stageId, new JobInstance(JOB_NAME + "2"));
        ArtifactPropertiesGenerator generator = new ArtifactPropertiesGenerator("test", "src", "//xpath");

        // Act
        ArtifactPropertiesGenerator generatorOfFirstJob = artifactPropertiesGeneratorRepository.saveCopyOf(firstJobInstance.getId(), generator);

        ArtifactPropertiesGenerator generatorOfSecondJob = artifactPropertiesGeneratorRepository.saveCopyOf(secondJobInstance.getId(), generator);

        // Assert
        List<ArtifactPropertiesGenerator> firstJobGenerators = artifactPropertiesGeneratorRepository.findByBuildId(firstJobInstance.getId());
        assertThat(firstJobGenerators.size(), is(1));
        assertThat(firstJobGenerators.get(0).getId(), equalTo(generatorOfFirstJob.getId()));
        assertThat(firstJobGenerators, hasItem(generatorOfFirstJob));

        List<ArtifactPropertiesGenerator> secondJobGenerators = artifactPropertiesGeneratorRepository.findByBuildId(secondJobInstance.getId());
        assertThat(secondJobGenerators.size(), is(1));
        assertThat(secondJobGenerators, hasItem(generatorOfSecondJob));

        assertThat(generatorOfFirstJob.getId(), not(equalTo(generatorOfSecondJob.getId())));
    }

    @Test
    public void shouldDeleteArtifactPropertiesGenerator() throws Exception {
        // Arrange
        JobInstance jobInstance = jobInstanceDao.save(stageId, new JobInstance(JOB_NAME));
        ArtifactPropertiesGenerator savedArtifactPropertiesGenerator = new ArtifactPropertiesGenerator("test", "src", "//xpath");
        savedArtifactPropertiesGenerator.setJobId(jobInstance.getId());
        artifactPropertiesGeneratorRepository.save(savedArtifactPropertiesGenerator);

        List<ArtifactPropertiesGenerator> artifactPropertiesGeneratorList = artifactPropertiesGeneratorRepository.findByBuildId(jobInstance.getId());
        assertThat(artifactPropertiesGeneratorList.size(), is(1));

        // Act
        artifactPropertiesGeneratorRepository.deleteAll(Arrays.asList(savedArtifactPropertiesGenerator));

        // Assert
        artifactPropertiesGeneratorList = artifactPropertiesGeneratorRepository.findByBuildId(jobInstance.getId());
        assertThat(artifactPropertiesGeneratorList.size(), is(0));
    }
}
