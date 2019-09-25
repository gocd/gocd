/*
 * Copyright 2019 ThoughtWorks, Inc.
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
package com.thoughtworks.go.server.controller;

import com.thoughtworks.go.domain.Pipeline;
import com.thoughtworks.go.fixture.PipelineWithTwoStages;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.server.service.PipelineService;
import com.thoughtworks.go.server.service.PropertiesService;
import com.thoughtworks.go.server.service.RestfulService;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.util.Csv;
import com.thoughtworks.go.util.GoConfigFileHelper;
import com.thoughtworks.go.util.SystemEnvironment;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.HashMap;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:/applicationContext-global.xml",
        "classpath:/applicationContext-dataLocalAccess.xml",
        "classpath:/testPropertyConfigurer.xml",
        "classpath:/spring-all-servlet.xml",
})
public class PropertiesControllerTest {

    @Autowired private PropertiesService propertiesService;
    @Autowired private RestfulService restfulService;
    @Autowired private DatabaseAccessHelper dbHelper;
    @Autowired private PipelineService pipelineService;
    @Autowired private MaterialRepository materialRepository;
    @Autowired private TransactionTemplate transactionTemplate;
    @Autowired private SystemEnvironment systemEnvironment;
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    private PipelineWithTwoStages fixture;
    private GoConfigFileHelper configHelper;
    private PropertiesController controller;
    public MockHttpServletResponse response;

    @Before
    public void setUp() throws Exception {
        configHelper = new GoConfigFileHelper();
        fixture = new PipelineWithTwoStages(materialRepository, transactionTemplate, temporaryFolder);
        fixture.usingConfigHelper(configHelper).usingDbHelper(dbHelper).onSetUp();
        controller = new PropertiesController(propertiesService, restfulService, pipelineService, systemEnvironment);
        controller.setDisallowPropertiesAccess(false);
        response = new MockHttpServletResponse();
    }

    @After
    public void tearDown() throws Exception {
        fixture.onTearDown();
    }

    @Test
    public void shouldSearchJobPropertiesUptoSpecifiedPipelineCounter() throws Exception {
        Pipeline pipeline1 = createNewPipeline();
        Pipeline pipeline2 = createNewPipeline();
        Pipeline pipeline3 = createNewPipeline();

        controller.jobsSearch(fixture.pipelineName, fixture.devStage,
                PipelineWithTwoStages.JOB_FOR_DEV_STAGE, String.valueOf(pipeline2.getCounter()), null, response);
        Csv csv = Csv.fromString(response.getContentAsString());
        assertThat(csv.containsRow(createCsvRow(pipeline1)), is(true));
        assertThat(csv.containsRow(createCsvRow(pipeline2)), is(true));
        assertThat(csv.containsRow(createCsvRow(pipeline3)), is(false));
    }

    @Test
    public void shouldReturnAllHistoryIfNotLimitPipeline() throws Exception {
        Pipeline pipeline1 = createNewPipeline();
        Pipeline pipeline2 = createNewPipeline();
        Pipeline pipeline3 = createNewPipeline();

        controller.jobsSearch(fixture.pipelineName, fixture.devStage,
                PipelineWithTwoStages.JOB_FOR_DEV_STAGE, null, null, response);
        Csv csv = Csv.fromString(response.getContentAsString());
        assertThat(csv.rowCount(), is(3));
    }

    @Test
    public void shouldReportErrorIfSpecifiedWrongCounter() throws Exception {
        Pipeline pipeline1 = createNewPipeline();
        controller.jobsSearch(fixture.pipelineName, fixture.devStage,
                PipelineWithTwoStages.JOB_FOR_DEV_STAGE, "invalid-label", null, response);
        assertThat(response.getContentAsString(),
                is("Expected a numeric value for query parameter 'limitPipeline', but received [invalid-label]"));
    }

    @Test
    public void shouldReportErrorIfSpecifiedANonExistentPipelineCounter() throws Exception {
        Pipeline pipeline1 = createNewPipeline();
        controller.jobsSearch(fixture.pipelineName, fixture.devStage,
                PipelineWithTwoStages.JOB_FOR_DEV_STAGE, "9999", null, response);
        assertThat(response.getContentAsString(),
                is(String.format("The value [9999] of query parameter 'limitPipeline' is not a valid pipeline counter for pipeline '%s'", pipeline1.getName())));
    }

    //Each row is represented as a map, with column name as key, and column value as value
    private HashMap<String, String> createCsvRow(final Pipeline pipeline) {
        return new HashMap<String, String>() {
            {
                put("cruise_pipeline_label", pipeline.getLabel());
                put("cruise_pipeline_counter", String.valueOf(pipeline.getCounter()));
            }
        };
    }

    private Pipeline createNewPipeline() {
        Pipeline pipeline = fixture.createdPipelineWithAllStagesPassed();
        propertiesService.saveCruiseProperties(
                pipeline.getStages().byName(fixture.devStage).getJobInstances().first());
        return pipeline;
    }
}
