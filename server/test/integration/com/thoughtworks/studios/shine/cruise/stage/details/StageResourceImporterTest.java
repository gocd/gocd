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

package com.thoughtworks.studios.shine.cruise.stage.details;

import java.util.Date;

import com.thoughtworks.go.config.GoConfigDao;
import com.thoughtworks.go.config.materials.ScmMaterial;
import com.thoughtworks.go.domain.Pipeline;
import com.thoughtworks.go.domain.Stage;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.dao.sparql.TestFailureSetup;
import com.thoughtworks.go.server.domain.PipelineTimeline;
import com.thoughtworks.go.server.domain.xml.JobXmlViewModel;
import com.thoughtworks.go.server.domain.xml.PipelineXmlViewModel;
import com.thoughtworks.go.server.domain.xml.StageXmlViewModel;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.server.service.PipelineHistoryService;
import com.thoughtworks.go.server.service.StageService;
import com.thoughtworks.go.server.service.XmlApiService;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.util.GoConfigFileHelper;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.studios.shine.cruise.GoOntology;
import com.thoughtworks.studios.shine.semweb.Graph;
import com.thoughtworks.studios.shine.semweb.grddl.XSLTTransformerRegistry;
import com.thoughtworks.studios.shine.semweb.sesame.InMemoryTempGraphFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static com.thoughtworks.studios.shine.AssertUtils.assertAskIsFalse;
import static com.thoughtworks.studios.shine.AssertUtils.assertAskIsTrue;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:WEB-INF/applicationContext-acegi-security.xml"
})
public class StageResourceImporterTest {
    private StageResourceImporter importer;
    private InMemoryTempGraphFactory graphFactory;

    @Autowired private StageService stageService;
    @Autowired private PipelineHistoryService pipelineHistoryService;
    @Autowired private MaterialRepository materialRepository;
    @Autowired private GoConfigDao goConfigDao;
    @Autowired private XmlApiService xmlApiService;
    @Autowired private TransactionTemplate transactionTemplate;

    @Autowired private DatabaseAccessHelper dbHelper;

    @Autowired private PipelineTimeline pipelineTimeline;
    @Autowired private SystemEnvironment systemEnvironment;


    private GoConfigFileHelper configHelper = new GoConfigFileHelper();
    private TestFailureSetup failureSetup;
    private String baseUrl = "https://localhost:8154/go";

    @Before
    public void setup() throws Exception {
        dbHelper.onSetUp();
        configHelper.usingCruiseConfigDao(goConfigDao);
        configHelper.onSetUp();
        graphFactory = new InMemoryTempGraphFactory();
        importer = new StageResourceImporter("test/data/cruise/artifacts", xmlApiService, stageService, pipelineHistoryService,systemEnvironment);
        failureSetup = new TestFailureSetup(materialRepository, dbHelper, pipelineTimeline, configHelper, transactionTemplate);
    }

    @After
    public void tearDown() throws Exception {
        configHelper.onTearDown();
        dbHelper.onTearDown();
    }

    @Test(expected = CanNotImportABuildingStageException.class)
    public void shouldNotImportStageNotCompleted() throws Exception {
        TestFailureSetup.SavedStage savedStage = failureSetup.setupPipelineInstanceWithoutTestXmlStubbing(false, null, new Date());
        importer.load(savedStage.stageId, graphFactory, new XSLTTransformerRegistry());
    }

    @Test
    public void canLoadAllStageInfoAfterImportIt() throws Exception {
        TestFailureSetup.SavedStage savedStage = failureSetup.setupPipelineInstanceWithoutTestXmlStubbing(true, null, new Date());
        Pipeline pipeline = savedStage.pipeline;
        Graph graph = importer.load(savedStage.stageId, graphFactory, new XSLTTransformerRegistry());

        String pipelineUrl = PipelineXmlViewModel.httpUrlForPipeline(baseUrl, pipeline.getId(), pipeline.getName());
        Stage stage = savedStage.stage;
        String stageUrl = new StageXmlViewModel(stage).httpUrl(baseUrl);
        String jobUrl = new JobXmlViewModel(stage.getJobInstances().get(0)).httpUrl(baseUrl);
        String ask = "" +
                "PREFIX cruise: <" + GoOntology.URI + "> " +
                "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#> " +
                "ASK WHERE {" +
                "  <" + pipelineUrl + "> a cruise:Pipeline ." +
                "  <" + pipelineUrl + "> cruise:hasStage <" + stageUrl + "> ." +
                "  <" + stageUrl + "> a cruise:Stage ." +
                "  <" + stageUrl + "> cruise:hasJob <" + jobUrl + "> ." +
                "  <" + jobUrl + "> a cruise:Job . " +
                "}";

        assertAskIsTrue(graph, ask);
    }

    @Test
    public void shouldNotImportPreviousNextPipelinePointer() throws Exception {
        TestFailureSetup.SavedStage savedStageFirst = failureSetup.setupPipelineInstanceWithoutTestXmlStubbing(true, null, new Date());
        TestFailureSetup.SavedStage savedStageSecond = failureSetup.setupPipelineInstanceWithoutTestXmlStubbing(true, null, new Date());
        TestFailureSetup.SavedStage savedStageThird = failureSetup.setupPipelineInstanceWithoutTestXmlStubbing(true, null, new Date());

        Graph graph = importer.load(savedStageSecond.stageId, graphFactory, new XSLTTransformerRegistry());

        Pipeline pipelineSecond = savedStageSecond.pipeline;
        assertTrue(graph.containsResourceWithURI(PipelineXmlViewModel.httpUrlForPipeline(baseUrl, pipelineSecond.getId(), pipelineSecond.getName())));
        Pipeline pipelineFirst = savedStageFirst.pipeline;
        assertFalse(graph.containsResourceWithURI(PipelineXmlViewModel.httpUrlForPipeline(baseUrl, pipelineFirst.getId(), pipelineFirst.getName())));
        Pipeline pipelineThird = savedStageThird.pipeline;
        assertFalse(graph.containsResourceWithURI(PipelineXmlViewModel.httpUrlForPipeline(baseUrl, pipelineThird.getId(), pipelineThird.getName())));


        String askForPreviousPointer = "" +
                "PREFIX cruise: <" + GoOntology.URI + "> " +
                "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#> " +
                "ASK WHERE {" +
                "  [] cruise:previousPipeline [] . " +
                "}";

        assertAskIsFalse(graph, askForPreviousPointer);

        String askForNextPointer = "" +
                "PREFIX cruise: <" + GoOntology.URI + "> " +
                "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#> " +
                "ASK WHERE {" +
                "  [] cruise:nextPipeline [] . " +
                "}";

        assertAskIsFalse(graph, askForNextPointer);
    }

    @Test
    public void shouldImportPipelineBuildTriggers() throws Exception {
        TestFailureSetup.SavedStage savedStage = failureSetup.setupPipelineInstanceWithoutTestXmlStubbing(true, null, new Date());
        Graph graph = importer.load(savedStage.stageId, graphFactory, new XSLTTransformerRegistry());

        for (Modification modification : savedStage.pipeline.getMaterialRevisions().getRevisions().get(0).getModifications()) {
            String changeSetUri = ScmMaterial.changesetUrl(modification, baseUrl, modification.getMaterialInstance().getId());
            assertTrue(graph.containsResourceWithURI(changeSetUri));

            String ask = "" +
                    "PREFIX cruise: <" + GoOntology.URI + "> " +
                    "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#> " +
                    "ASK WHERE {" +
                    "  <" + PipelineXmlViewModel.httpUrlForPipeline(baseUrl, savedStage.pipeline.getId(), savedStage.pipeline.getName()) + "> cruise:pipelineTrigger <" + changeSetUri + "> . " +
                    "}";

            assertAskIsTrue(graph, ask);
        }
    }
}
