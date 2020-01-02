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

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:/applicationContext-global.xml",
        "classpath:/applicationContext-dataLocalAccess.xml",
        "classpath:/testPropertyConfigurer.xml",
        "classpath:/spring-all-servlet.xml",
})
public class ResourceRepositoryIntegrationTest {
    private static final String JOB_NAME = "functional";
    private static final String OTHER_JOB_NAME = "unit";
    private static final String STAGE_NAME = "mingle";
    private static final String PIPELINE_NAME = "pipeline";

    @Autowired private JobInstanceSqlMapDao jobInstanceDao;
    @Autowired private ResourceRepository resourceRepository;
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
    public void shouldSaveResource() {
        // Arrange
        JobInstance jobInstance = jobInstanceDao.save(stageId, new JobInstance(JOB_NAME));
        Resource resource = new Resource("something");
        resource.setBuildId(jobInstance.getId());

        // Act
        resourceRepository.save(resource);

        // Assert
        assertThat(resource.getId(), is(not(nullValue())));
    }

    @Test
    public void shouldLoadSavedResource() {
        // Arrange
        JobInstance jobInstance = jobInstanceDao.save(stageId, new JobInstance(JOB_NAME));
        Resource savedResource = new Resource("something");
        savedResource.setBuildId(jobInstance.getId());
        resourceRepository.save(savedResource);

        // Act
        Resources resources = resourceRepository.findByBuildId(jobInstance.getId());

        // Assert
        assertThat(resources.size(), is(1));
        assertThat(resources.get(0), is(savedResource));
    }

    @Test
    public void shouldSaveACopyOfAResource() {
        // Arrange
        JobInstance firstJobInstance = jobInstanceDao.save(stageId, new JobInstance(JOB_NAME + "1"));
        JobInstance secondJobInstance = jobInstanceDao.save(stageId, new JobInstance(JOB_NAME + "2"));
        Resource resource = new Resource("something");

        // Act
        Resource resourceOfFirstJob = resourceRepository.saveCopyOf(firstJobInstance.getId(), resource);

        Resource resourceOfSecondJob = resourceRepository.saveCopyOf(secondJobInstance.getId(), resource);

        // Assert
        Resources firstJobResources = resourceRepository.findByBuildId(firstJobInstance.getId());
        assertThat(firstJobResources.size(), is(1));
        assertThat(firstJobResources.get(0).getId(), equalTo(resourceOfFirstJob.getId()));
        assertThat(firstJobResources, hasItem(resourceOfFirstJob));

        Resources secondJobResources = resourceRepository.findByBuildId(secondJobInstance.getId());
        assertThat(secondJobResources.size(), is(1));
        assertThat(secondJobResources, hasItem(resourceOfSecondJob));

        assertThat(resourceOfFirstJob.getId(), not(equalTo(resourceOfSecondJob.getId())));
    }

    @Test
    public void shouldDeleteResource() throws Exception {
        // Arrange
        JobInstance jobInstance = jobInstanceDao.save(stageId, new JobInstance(JOB_NAME));
        Resource savedResource = new Resource("something");
        savedResource.setBuildId(jobInstance.getId());
        resourceRepository.save(savedResource);

        List<Resource> resourceList = resourceRepository.findByBuildId(jobInstance.getId());
        assertThat(resourceList.size(), is(1));

        // Act
        resourceRepository.deleteAll(Arrays.asList(savedResource));

        // Assert
        resourceList = resourceRepository.findByBuildId(jobInstance.getId());
        assertThat(resourceList.size(), is(0));
    }
}
