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
package com.thoughtworks.go.server.controller;

import com.thoughtworks.go.ClearSingleton;
import com.thoughtworks.go.config.Agent;
import com.thoughtworks.go.config.GoConfigDao;
import com.thoughtworks.go.config.elastic.ClusterProfile;
import com.thoughtworks.go.config.elastic.ElasticProfile;
import com.thoughtworks.go.domain.JobAgentMetadata;
import com.thoughtworks.go.domain.JobInstance;
import com.thoughtworks.go.domain.Pipeline;
import com.thoughtworks.go.domain.Stage;
import com.thoughtworks.go.fixture.PipelineWithTwoStages;
import com.thoughtworks.go.plugin.access.elastic.ElasticAgentMetadataStore;
import com.thoughtworks.go.plugin.domain.elastic.Capabilities;
import com.thoughtworks.go.plugin.domain.elastic.ElasticAgentPluginInfo;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.dao.JobAgentMetadataDao;
import com.thoughtworks.go.server.dao.JobInstanceDao;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.server.presentation.models.JobDetailPresentationModel;
import com.thoughtworks.go.server.service.*;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.server.util.UuidGenerator;
import com.thoughtworks.go.util.GoConfigFileHelper;
import com.thoughtworks.go.util.GoConstants;
import com.thoughtworks.go.util.SystemEnvironment;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.servlet.ModelAndView;

import java.util.Collections;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:/applicationContext-global.xml",
        "classpath:/applicationContext-dataLocalAccess.xml",
        "classpath:/testPropertyConfigurer.xml",
        "classpath:/spring-all-servlet.xml",
})
public class JobControllerIntegrationTest {

    private JobController controller;
    private MockHttpServletResponse response;
    private MockHttpServletRequest request;
    @Autowired
    private JobInstanceService jobInstanceService;
    @Autowired
    private JobInstanceDao jobInstanceDao;
    @Autowired
    private GoConfigService goConfigService;
    @Autowired
    private AgentService agentService;
    @Autowired
    private PipelineService pipelineService;
    @Autowired
    private RestfulService restfulService;
    @Autowired
    private ArtifactsService artifactService;
    @Autowired
    private DatabaseAccessHelper dbHelper;
    @Autowired
    private GoConfigDao goConfigDao;
    @Autowired
    private MaterialRepository materialRepository;
    @Autowired
    private StageService stageService;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private JobAgentMetadataDao jobAgentMetadataDao;
    @Autowired
    private SystemEnvironment systemEnvironment;
    @Autowired
    private UuidGenerator uuidGenerator;
    @Autowired
    private SecurityService securityService;

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    private GoConfigFileHelper configHelper;
    private PipelineWithTwoStages fixture;
    @Rule
    public final ClearSingleton clearSingleton = new ClearSingleton();

    @Before
    public void setUp() throws Exception {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        configHelper = new GoConfigFileHelper();
        configHelper.usingCruiseConfigDao(goConfigDao);
        fixture = new PipelineWithTwoStages(materialRepository, transactionTemplate, temporaryFolder);
        fixture.usingConfigHelper(configHelper).usingDbHelper(dbHelper).onSetUp();
        controller = new JobController(jobInstanceService, agentService, jobInstanceDao,
                goConfigService, pipelineService, restfulService, artifactService, stageService, jobAgentMetadataDao, systemEnvironment, securityService);
    }

    @After
    public void tearDown() throws Exception {
        fixture.onTearDown();
    }

    @Test
    public void shouldSupportPipelineCounter() throws Exception {
        Pipeline pipeline = fixture.createdPipelineWithAllStagesPassed();
        Stage stage = pipeline.getFirstStage();
        JobInstance job = stage.getFirstJob();
        ModelAndView modelAndView = controller.jobDetail(pipeline.getName(), String.valueOf(pipeline.getCounter()),
                stage.getName(), String.valueOf(stage.getCounter()), job.getName());
        assertThat(presenter(modelAndView).getBuildLocator(), is(job.getIdentifier().buildLocator()));
    }

    @Test
    public void shouldReturnErrorMessageWhenFailedToFindJob() throws Exception {
        try {
            controller.jobDetail(fixture.pipelineName, "1", fixture.devStage, "1", "invalid-job");
        } catch (Exception e) {
            ModelAndView modelAndView = controller.handle(request, response, e);
            assertThat((String) modelAndView.getModel().get(GoConstants.ERROR_FOR_PAGE),
                    containsString("invalid-job not found"));
        }
    }

    @Test
    public void shouldFindJobByPipelineCounterEvenMultiplePipelinesHaveSameLabel() throws Exception {
        fixture.configLabelTemplateUsingMaterialRevision();
        Pipeline oldPipeline = fixture.createdPipelineWithAllStagesPassed();
        Stage stage = oldPipeline.getFirstStage();
        JobInstance job = stage.getFirstJob();
        ModelAndView modelAndView = controller.jobDetail(oldPipeline.getName(),
                String.valueOf(oldPipeline.getCounter()),
                stage.getName(), String.valueOf(stage.getCounter()), job.getName());
        assertThat(presenter(modelAndView).getBuildLocator(), is(job.getIdentifier().buildLocator()));
    }

    @Test
    public void shouldCreateJobPresentationModelWithRightStage() throws Exception {
        controller = new JobController(jobInstanceService, agentService, jobInstanceDao,
                goConfigService, pipelineService, restfulService, artifactService, stageService, jobAgentMetadataDao, systemEnvironment, securityService);
        fixture.configLabelTemplateUsingMaterialRevision();
        Pipeline pipeline = fixture.createdPipelineWithAllStagesPassed();
        Stage devStage = pipeline.getStages().byName("dev");
        JobInstance job = devStage.getFirstJob();

        ModelAndView modelAndView = controller.jobDetail(pipeline.getName(), String.valueOf(pipeline.getCounter()), devStage.getName(), String.valueOf(devStage.getCounter()), job.getName());

        assertThat(presenter(modelAndView).getStageLocator(), is(devStage.stageLocator()));
        assertThat(presenter(modelAndView).getStage(), is(devStage));
    }

    @Test
    public void jobDetailModel_shouldHaveTheElasticProfilePluginIdWhenAgentIsNotAssigned() throws Exception {
        Pipeline pipeline = fixture.createPipelineWithFirstStageAssigned();
        Stage stage = pipeline.getFirstStage();
        JobInstance job = stage.getFirstJob();
        GoPluginDescriptor.About about = GoPluginDescriptor.About.builder().name("name").version("0.1").targetGoVersion("17.3.0").description("desc").build();
        GoPluginDescriptor descriptor = GoPluginDescriptor.builder().id("plugin_id").about(about).build();
        ElasticAgentMetadataStore.instance().setPluginInfo(new ElasticAgentPluginInfo(descriptor, null, null, null, null, new Capabilities(false, true)));

        ElasticProfile profile = new ElasticProfile("profile_id", "cluster_profile_id", Collections.EMPTY_LIST);
        ClusterProfile clusterProfile = new ClusterProfile("cluster_profile_id", "plugin_id", Collections.EMPTY_LIST);

        fixture.addJobAgentMetadata(new JobAgentMetadata(job.getId(), profile, clusterProfile));

        ModelAndView modelAndView = controller.jobDetail(pipeline.getName(), String.valueOf(pipeline.getCounter()),
                stage.getName(), String.valueOf(stage.getCounter()), job.getName());

        assertThat(modelAndView.getModel().get("elasticAgentPluginId"), is("plugin_id"));
    }

    @Test
    public void jobDetailModel_shouldHaveTheElasticPluginIdAndElasticAgentIdWhenAgentIsAssigned() throws Exception {
        Pipeline pipeline = fixture.createPipelineWithFirstStageAssigned();
        Stage stage = pipeline.getFirstStage();
        JobInstance job = stage.getFirstJob();

        GoPluginDescriptor.About about = GoPluginDescriptor.About.builder().name("name").version("0.1").targetGoVersion("17.3.0").description("desc").build();
        GoPluginDescriptor descriptor = GoPluginDescriptor.builder().id("plugin_id").about(about).build();
        ElasticAgentMetadataStore.instance().setPluginInfo(new ElasticAgentPluginInfo(descriptor, null, null, null, null, new Capabilities(false, true)));

        ElasticProfile profile = new ElasticProfile("profile_id", "cluster_profile_id", Collections.EMPTY_LIST);
        ClusterProfile clusterProfile = new ClusterProfile("cluster_profile_id", "plugin_id", Collections.EMPTY_LIST);

        fixture.addJobAgentMetadata(new JobAgentMetadata(job.getId(), profile, clusterProfile));

        final Agent agent = new Agent(job.getAgentUuid(), "localhost", "127.0.0.1", uuidGenerator.randomUuid());
        agent.setElasticAgentId("elastic_agent_id");
        agent.setElasticPluginId("plugin_id");
        agentService.saveOrUpdate(agent);

        ModelAndView modelAndView = controller.jobDetail(pipeline.getName(), String.valueOf(pipeline.getCounter()),
                stage.getName(), String.valueOf(stage.getCounter()), job.getName());

        assertThat(modelAndView.getModel().get("elasticAgentPluginId"), is("plugin_id"));
        assertThat(modelAndView.getModel().get("elasticAgentId"), is("elastic_agent_id"));
    }

    @Test
    public void jobDetailModel_shouldNotHaveTheElasticProfilePluginIdAndElasticAgentIdWhenAgentIsNotElasticAgent() throws Exception {
        Pipeline pipeline = fixture.createPipelineWithFirstStageAssigned();
        Stage stage = pipeline.getFirstStage();
        JobInstance job = stage.getFirstJob();

        final Agent agent = new Agent(job.getAgentUuid(), "localhost", "127.0.0.1", uuidGenerator.randomUuid());
        agentService.saveOrUpdate(agent);

        ModelAndView modelAndView = controller.jobDetail(pipeline.getName(), String.valueOf(pipeline.getCounter()),
                stage.getName(), String.valueOf(stage.getCounter()), job.getName());

        assertNull(modelAndView.getModel().get("elasticAgentPluginId"));
        assertNull(modelAndView.getModel().get("elasticAgentId"));
    }

    @Test
    public void jobDetailModel_shouldNotHaveElasticPluginIdAndElasticAgentIdForACompletedJob() throws Exception {
        Pipeline pipeline = fixture.createdPipelineWithAllStagesPassed();
        Stage stage = pipeline.getFirstStage();
        JobInstance job = stage.getFirstJob();

        final Agent agent = new Agent(job.getAgentUuid(), "localhost", "127.0.0.1", uuidGenerator.randomUuid());
        agent.setElasticAgentId("elastic_agent_id");
        agent.setElasticPluginId("plugin_id");
        agentService.saveOrUpdate(agent);

        ModelAndView modelAndView = controller.jobDetail(pipeline.getName(), String.valueOf(pipeline.getCounter()),
                stage.getName(), String.valueOf(stage.getCounter()), job.getName());

        assertNull(modelAndView.getModel().get("elasticAgentPluginId"));
        assertNull(modelAndView.getModel().get("elasticAgentId"));
    }

    private JobDetailPresentationModel presenter(ModelAndView modelAndView) {
        return (JobDetailPresentationModel) modelAndView.getModel().get("presenter");
    }
}
