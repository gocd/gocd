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

import java.util.ArrayList;
import java.util.List;

import com.thoughtworks.go.config.GoConfigDao;
import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.domain.Pipeline;
import com.thoughtworks.go.domain.Properties;
import com.thoughtworks.go.domain.Property;
import com.thoughtworks.go.fixture.PipelineWithTwoStages;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.dao.PropertyDao;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.util.GoConfigFileHelper;
import com.thoughtworks.go.util.Csv;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:WEB-INF/applicationContext-acegi-security.xml"
})
public class PropertiesServiceTest {
    @Autowired private PropertiesService propertiesService;
    @Autowired private DatabaseAccessHelper dbHelper;
    @Autowired private MaterialRepository materialRepository;
    @Autowired private GoConfigDao goConfigDao;
    @Autowired private TransactionTemplate transactionTemplate;
    private PipelineWithTwoStages fixture;
    private GoConfigFileHelper configHelper;

    @Before
    public void setUp() throws Exception {
        configHelper = new GoConfigFileHelper();
        configHelper.usingCruiseConfigDao(goConfigDao);
        configHelper.onSetUp();
        fixture = new PipelineWithTwoStages(materialRepository, transactionTemplate);
        fixture.usingConfigHelper(configHelper).usingDbHelper(dbHelper).onSetUp();
    }

    @After
    public void tearDown() throws Exception {
        fixture.onTearDown();
        configHelper.onTearDown();
    }

    @Test
    public void shouldReturnEmptyListIfLimitCountIsNotPositive() {
        Pipeline pipeline1 = createNewPipeline();
        Pipeline pipeline2 = createNewPipeline();

        List<Properties> propertiesList = propertiesService.loadHistory(fixture.pipelineName, fixture.devStage,
                PipelineWithTwoStages.JOB_FOR_DEV_STAGE, pipeline2.getId(), -1);
        assertThat(propertiesList.size(), is(0));

        propertiesList = propertiesService.loadHistory(fixture.pipelineName, fixture.devStage,
                PipelineWithTwoStages.JOB_FOR_DEV_STAGE, pipeline2.getId(), 0);
        assertThat(propertiesList.size(), is(0));
    }

    @Test
    public void shouldLoadHistoryUptoSpecifiedPipeline() {
        Pipeline pipeline1 = createNewPipeline();
        Pipeline pipeline2 = createNewPipeline();

        List<Properties> propertiesList = propertiesService.loadHistory(fixture.pipelineName, fixture.devStage,
                PipelineWithTwoStages.JOB_FOR_DEV_STAGE, pipeline2.getId(), 1);
        assertThat(propertiesList.size(), is(1));
        assertThat(propertiesList.get(0).getValue("cruise_pipeline_counter"),
                is(String.valueOf(pipeline2.getCounter())));

    }

    @Test public void shouldGenerateFromHistoryOfProperties() throws Exception {
        List<Properties> history = new ArrayList<Properties>();
        history.add(new Properties(new Property("a", "100"), new Property("b", "200")));
        history.add(new Properties(new Property("a", "300"), new Property("b", "400")));
        Csv csv = PropertiesService.fromAllPropertiesHistory(history);
        assertThat(csv.toString(), is(
                "a,b\n"
                        + "100,200\n"
                        + "300,400\n"
        ));
    }
    @Test public void shouldGenerateFromProperties() throws Exception {
        Properties props = new Properties(new Property("a", "1"), new Property("b", "2"));
        Csv csv = PropertiesService.fromProperties(props);
        assertThat(csv.toString(), is(
                "a,b\n"
                        + "1,2\n"
        ));
    }

    @Test
    public void shouldLoadOriginalJobPropertiesForGivenJobIdentifier() {
        Properties props = new Properties(new Property("a", "1"), new Property("b", "2"));
        PropertyDao propertyDao = mock(PropertyDao.class);
        JobResolverService resolver = mock(JobResolverService.class);
        PropertiesService service = new PropertiesService(propertyDao, null, null, resolver);
        JobIdentifier oldId = new JobIdentifier("pipeline-name", 10, "label-10", "stage-name", "3", "job-name", 9l);
        when(propertyDao.list(6l)).thenReturn(props);
        when(resolver.actualJobIdentifier(oldId)).thenReturn(new JobIdentifier("pipeline-name", 7, "label-7", "stage-name", "1", "job-name", 6l));
        assertThat(service.getPropertiesForOriginalJob(oldId), is(props));
    }

    private Pipeline createNewPipeline() {
        Pipeline pipeline = fixture.createdPipelineWithAllStagesPassed();
        propertiesService.saveCruiseProperties(
                pipeline.getStages().byName(fixture.devStage).getJobInstances().first());
        return pipeline;
    }
}
