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

package com.thoughtworks.studios.shine.cruise;

import java.io.File;
import java.util.Date;

import com.thoughtworks.go.config.GoConfigDao;
import com.thoughtworks.go.config.materials.ScmMaterial;
import com.thoughtworks.go.domain.Pipeline;
import com.thoughtworks.go.domain.Stage;
import com.thoughtworks.go.domain.StageFinder;
import com.thoughtworks.go.domain.StageResult;
import com.thoughtworks.go.domain.feed.stage.StageFeedEntry;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.dao.sparql.TestFailureSetup;
import com.thoughtworks.go.server.domain.PipelineTimeline;
import com.thoughtworks.go.server.domain.xml.JobXmlViewModel;
import com.thoughtworks.go.server.domain.xml.PipelineXmlViewModel;
import com.thoughtworks.go.server.domain.xml.StageXmlViewModel;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.server.service.ArtifactsService;
import com.thoughtworks.go.server.service.JobInstanceService;
import com.thoughtworks.go.server.service.PipelineHistoryService;
import com.thoughtworks.go.server.service.PipelineInstanceLoader;
import com.thoughtworks.go.server.service.PropertiesService;
import com.thoughtworks.go.server.service.StageService;
import com.thoughtworks.go.server.service.XmlApiService;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.util.GoConfigFileHelper;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.TempFiles;
import com.thoughtworks.studios.shine.cruise.stage.details.StageResourceImporter;
import com.thoughtworks.studios.shine.cruise.stage.details.StageStorage;
import com.thoughtworks.studios.shine.semweb.Graph;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:WEB-INF/applicationContext-acegi-security.xml"
})
public class BackgroundStageLoaderIntegrationTest {

    private StageStorage stageStorage;
    private TempFiles tempFiles;
    private BackgroundStageLoader loader;
    private StageFinder stageFinder;
    private PipelineInstanceLoader pipelineInstanceLoader;

    private GoConfigFileHelper configHelper = new GoConfigFileHelper();

    @Autowired private StageService stageService;
    @Autowired private PipelineHistoryService pipelineHistoryService;
    @Autowired private MaterialRepository materialRepository;
    @Autowired private GoConfigDao goConfigDao;
    @Autowired private PropertiesService propertiesService;
    @Autowired private ArtifactsService artifactsService;
    @Autowired private JobInstanceService jobInstanceService;
    @Autowired private XmlApiService xmlApiService;
    @Autowired private TransactionTemplate transactionTemplate;
    @Autowired private SystemEnvironment systemEnvironment;

    @Autowired private DatabaseAccessHelper dbHelper;

    @Autowired private PipelineTimeline pipelineTimeline;
    private TestFailureSetup failureSetup;

    @Before
    public void setUp() throws Exception {
        dbHelper.onSetUp();
        tempFiles = new TempFiles();
        File tempFolder = tempFiles.createUniqueFolder("shine");
        stageStorage = new StageStorage(tempFolder.getAbsolutePath());
        stageStorage.clear();
        stageFinder = stageService;
        pipelineInstanceLoader = pipelineHistoryService;
        StageResourceImporter importer = new StageResourceImporter("/artifacts", xmlApiService, stageFinder, pipelineInstanceLoader,systemEnvironment);
        loader = new BackgroundStageLoader(null, importer, stageStorage, pipelineHistoryService, stageService, systemEnvironment );
        configHelper.usingCruiseConfigDao(goConfigDao);
        configHelper.onSetUp();

        failureSetup = new TestFailureSetup(materialRepository, dbHelper, pipelineTimeline, configHelper, transactionTemplate);
    }

    @After
    public void tearDown() throws Exception {
        tempFiles.cleanUp();
        configHelper.onTearDown();
        dbHelper.onTearDown();
    }

    @Test
    public void willNotHandleWhenStageAlreadyImported() {
        TestFailureSetup.SavedStage savedStage = failureSetup.setupPipelineInstanceWithoutTestXmlStubbing(true, null, new Date());

        assertFalse(loader.shouldStopHandling(feedEntry(savedStage)));
        loader.handle(feedEntry(savedStage), pipelineInstanceLoader);
        assertTrue(loader.shouldStopHandling(feedEntry(savedStage)));
    }

    private StageFeedEntry feedEntry(TestFailureSetup.SavedStage stage) {
        return new StageFeedEntry(stage.stage.getId(), stage.pipeline.getId(), stage.stageId, stage.stage.getId(), stage.stage.latestTransitionDate(), StageResult.Failed);
    }


    @Test
    public void willNotHandleWhenStageCompletedBeforeOneWeek() {
        TestFailureSetup.SavedStage savedStage = failureSetup.setupPipelineInstanceWithoutTestXmlStubbing(true, null, new DateTime().minusDays(8).toDate());

        assertTrue(loader.shouldStopHandling(feedEntry(savedStage)));
        assertFalse(stageStorage.isStageStored(savedStage.stageId));
    }

    @Test
    public void handleAStageURLNotYetSeenShouldStoreStageResourceIntoStageStorage() {
        TestFailureSetup.SavedStage savedStage = failureSetup.setupPipelineInstanceWithoutTestXmlStubbing(true, null, new Date());

        loader.handle(feedEntry(savedStage), pipelineInstanceLoader);
        assertTrue(stageStorage.isStageStored(savedStage.stageId));

        Graph storedGraph = stageStorage.load(savedStage.stageId);
        Stage stage = savedStage.stage;
        String baseUrl = "https://localhost:8154/go";
        String jobUrl = new JobXmlViewModel(stage.getJobInstances().first()).httpUrl(baseUrl);

        assertTrue(storedGraph.containsResourceWithURI(jobUrl));
        assertTrue(storedGraph.containsResourceWithURI(new StageXmlViewModel(stage).httpUrl(baseUrl)));
        Pipeline pipeline = savedStage.pipeline;
        assertTrue(storedGraph.containsResourceWithURI(PipelineXmlViewModel.httpUrlForPipeline(baseUrl, pipeline.getId(), pipeline.getName())));
        Modification latestModification = pipeline.getMaterialRevisions().getRevisions().get(0).getLatestModification();
        assertTrue(storedGraph.containsResourceWithURI(ScmMaterial.changesetUrl(latestModification, baseUrl, latestModification.getMaterialInstance().getId())));
    }

    @Test
    public void stageWithInvalidPipelineFeedShouldNotSaveStageToStageStorage() {
        TestFailureSetup.SavedStage stage = failureSetup.setupPipelineInstanceWithoutTestXmlStubbing(true, null, new Date());
        pipelineInstanceLoader = mock(PipelineInstanceLoader.class);
        when(pipelineInstanceLoader.loadPipelineForShine(anyLong())).thenThrow(new RuntimeException("ouch, that hurt!"));

        StageResourceImporter importer = new StageResourceImporter("/artifacts", xmlApiService, stageFinder, pipelineInstanceLoader,systemEnvironment);

        loader = new BackgroundStageLoader(null, importer, stageStorage, pipelineHistoryService, stageService, systemEnvironment);

        try {
            loader.handle(feedEntry(stage), pipelineInstanceLoader);
            fail("should have failed as pipeline load bombed");
        } catch (Exception e) {
            //expected
        }
        assertFalse(stageStorage.isStageStored(stage.stageId));
    }

    @Test
    public void stageWithInvalidJobPropertiesShouldNotSaveStageToStageStorage() {
        PropertiesService propertiesService = mock(PropertiesService.class);
        when(propertiesService.getPropertiesForJob(anyLong())).thenThrow(new RuntimeException("something bad happened"));

        xmlApiService = new XmlApiService(propertiesService, artifactsService, jobInstanceService, stageService);
        StageResourceImporter importer = new StageResourceImporter("/artifacts", xmlApiService, stageFinder, pipelineInstanceLoader,systemEnvironment);
        loader = new BackgroundStageLoader(null, importer, stageStorage, pipelineHistoryService, stageService, systemEnvironment);
        TestFailureSetup.SavedStage stage = failureSetup.setupPipelineInstanceWithoutTestXmlStubbing(true, null, new Date());

        try {
            loader.handle(feedEntry(stage), pipelineInstanceLoader);
            fail("should have failed as properties api bombed");
        } catch(Exception e) {
            //expected
        }
        assertFalse(stageStorage.isStageStored(stage.stageId));
    }

}
