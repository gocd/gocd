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

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import com.thoughtworks.go.config.GoConfigDao;
import com.thoughtworks.go.config.materials.git.GitMaterial;
import com.thoughtworks.go.domain.JobResult;
import com.thoughtworks.go.domain.Pipeline;
import com.thoughtworks.go.domain.ServerSiteUrlConfig;
import com.thoughtworks.go.domain.Stage;
import com.thoughtworks.go.domain.activity.CcTrayStatus;
import com.thoughtworks.go.fixture.PipelineWithTwoStages;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.util.GoConfigFileHelper;
import com.thoughtworks.go.util.XmlUtils;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.xpath.XPath;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static java.lang.String.format;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:WEB-INF/applicationContext-acegi-security.xml"
})
public class CcTrayStatusServiceTest {
    @Autowired private CcTrayStatus ccTrayStatus;
    @Autowired private GoConfigService goConfigService;
    @Autowired private StageService stageService;
    @Autowired private SecurityService securityService;
    @Autowired private ServerConfigService serverConfigService;
    @Autowired private GoConfigDao goConfigDao;
    @Autowired private CcTrayStatusService ccTrayStatusService;
    @Autowired private DatabaseAccessHelper dbHelper;
    @Autowired private MaterialRepository materialRepository;
    @Autowired private TransactionTemplate transactionTemplate;

    private static GoConfigFileHelper configHelper = new GoConfigFileHelper();
    private PipelineWithTwoStages pipelineFixture;

    @Before
    public void setUp() throws Exception {
        pipelineFixture = new PipelineWithTwoStages(materialRepository, transactionTemplate);
        configHelper.onSetUp();
        configHelper.usingCruiseConfigDao(goConfigDao);

        dbHelper.onSetUp();
        pipelineFixture.usingConfigHelper(configHelper).usingDbHelper(dbHelper).onSetUp();
        ccTrayStatus.clear();
        ccTrayStatusService = new CcTrayStatusService(goConfigService,ccTrayStatus,stageService,securityService,serverConfigService);
    }

    @After
    public void teardown() throws Exception {
        dbHelper.onTearDown();
        pipelineFixture.onTearDown();
        configHelper.onTearDown();
    }

    @Test
    public void shouldLoadStageStatusFromDbIfNoSuchStageInCcTrayStatus() throws Exception {
        pipelineFixture.createdPipelineWithAllStagesPassed();
        Document document = ccTrayStatusService.createCctrayXmlDocument("http://localhost:8153");

        List projects = XPath.selectNodes(document, "/Projects/Project");
        assertThat(projects.size(), is(4));

        assertHasProject(document, format("%s :: %s", pipelineFixture.pipelineName, pipelineFixture.devStage));
        assertHasProject(document, format("%s :: %s", pipelineFixture.pipelineName, pipelineFixture.ftStage));
        assertHasProject(document, String.format("%s :: %s :: %s", pipelineFixture.pipelineName,
                pipelineFixture.ftStage, PipelineWithTwoStages.JOB_FOR_FT_STAGE));
        assertHasProject(document, String.format("%s :: %s :: %s", pipelineFixture.pipelineName,
                pipelineFixture.devStage, PipelineWithTwoStages.JOB_FOR_DEV_STAGE));
        assertWebUrlContains(document, "http://localhost:8153");

    }


    @Test
    public void shouldLoadBreakersInformationInCcTrayFeed() throws Exception {
        pipelineFixture.createdPipelineWithAllStagesCompleted(JobResult.Failed);

        Document document = ccTrayStatusService.createCctrayXmlDocument("http://localhost/go");

        List projects = XPath.selectNodes(document, "/Projects/Project");
        assertThat(projects.size(), is(4));

        assertHasNoTags(document, format("/Projects/Project[@name='%s :: %s']/messages", pipelineFixture.pipelineName, pipelineFixture.devStage));//has passed
        assertHasTag(document, format("/Projects/Project[@name='%s :: %s']/messages/message[@text='lgao'][@kind='Breakers']", pipelineFixture.pipelineName, pipelineFixture.ftStage));//has failed
        assertHasNoTags(document, String.format("/Projects/Project[@name='%s :: %s :: %s']/messages", pipelineFixture.pipelineName, pipelineFixture.devStage, PipelineWithTwoStages.JOB_FOR_DEV_STAGE));//has passed
        assertHasTag(document, String.format("/Projects/Project[@name='%s :: %s :: %s']/messages/message[@text='lgao'][@kind='Breakers']", pipelineFixture.pipelineName, pipelineFixture.ftStage,
                PipelineWithTwoStages.JOB_FOR_FT_STAGE));//has failed
    }

    @Test
    @Ignore("this test is exactly the same as previous, we need to fix this to assert something different")
    public void shouldNotLoadStageStatusFromDbIfStageIsAlreadyInCcTrayStatus() throws Exception {
        pipelineFixture.createdPipelineWithAllStagesPassed();
        Document document = ccTrayStatusService.createCctrayXmlDocument("http://localhost/go");

        List projects = XPath.selectNodes(document, "/Projects/Project");
        assertThat(projects.size(), is(4));

        assertHasProject(document, format("%s :: %s", pipelineFixture.pipelineName, pipelineFixture.devStage));
        assertHasProject(document, format("%s :: %s", pipelineFixture.pipelineName, pipelineFixture.ftStage));
        assertHasProject(document, String.format("%s :: %s :: %s", pipelineFixture.pipelineName,
                pipelineFixture.ftStage, PipelineWithTwoStages.JOB_FOR_FT_STAGE));
        assertHasProject(document, String.format("%s :: %s :: %s", pipelineFixture.pipelineName,
                pipelineFixture.devStage, PipelineWithTwoStages.JOB_FOR_DEV_STAGE));
    }

    @Test
    public void shouldNotGenerateCcTrayFeedForStagesThatDoNotHaveHistory() throws Exception {
        pipelineFixture.createdPipelineWithAllStagesPassed();
        Document document = ccTrayStatusService.createCctrayXmlDocument("http://localhost/go");

        assertThat(XPath.selectNodes(document, "/Projects/Project").size(), is(4));

        configHelper.addStageToPipeline(pipelineFixture.pipelineName, "newStage", "newJob");
        document = ccTrayStatusService.createCctrayXmlDocument("http://localhost/go");

        assertThat(XPath.selectNodes(document, "/Projects/Project").size(), is(4));
    }

    @Test
    public void shouldNotGenerateCcTrayFeedForUnauthorizedStages() throws Exception {
        pipelineFixture.createdPipelineWithAllStagesPassed();
        configHelper.addSecurityWithPasswordFile();
        configHelper.addSecurityWithAdminConfig();
        configHelper.addAuthorizedUserForPipelineGroup("jez");

        Document document = ccTrayStatusService.createCctrayXmlDocument("http://localhost/go");

        assertThat(XPath.selectNodes(document, "/Projects/Project").size(), is(0));
    }

    @Test
    public void shouldNotGenerateCcTrayFeedForJobsThatDoNotHaveHistory() throws Exception {
        pipelineFixture.createdPipelineWithAllStagesPassed();
        Document document = ccTrayStatusService.createCctrayXmlDocument("http://localhost/go");

        assertThat(XPath.selectNodes(document, "/Projects/Project").size(), is(4));

        configHelper.addJob(pipelineFixture.pipelineName, pipelineFixture.devStage, "newJob");
        document = ccTrayStatusService.createCctrayXmlDocument("http://localhost/go");

        assertThat(XPath.selectNodes(document, "/Projects/Project").size(), is(4));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldGenerateCcTrayProjectsInOrder() throws Exception {
        Pipeline pipeline = pipelineFixture.createdPipelineWithAllStagesPassed();
        Stage devStage = pipeline.getStages().byName(pipelineFixture.devStage);
        ccTrayStatus.stageStatusChanged(devStage);
        Stage ftStage = pipeline.getStages().byName(pipelineFixture.ftStage);
        ccTrayStatus.stageStatusChanged(ftStage);

        Document document = ccTrayStatusService.createCctrayXmlDocument("");
        List<Element> projects = XPath.selectNodes(document, format("/Projects/Project"));
        assertThat(projects.size(), is(4));

        assertThat(projects.get(0).getAttributeValue("name"), is(devStage.getIdentifier().ccProjectName()));
        assertThat(projects.get(1).getAttributeValue("name"),
                is(devStage.getJobInstances().first().getIdentifier().ccProjectName()));
        assertThat(projects.get(2).getAttributeValue("name"), is(ftStage.getIdentifier().ccProjectName()));
        assertThat(projects.get(3).getAttributeValue("name"),
                is(ftStage.getJobInstances().first().getIdentifier().ccProjectName()));
    }

    @Test
    public void shouldRemoveProjectFromCcTrayStatusIfNoSuchProjectInConfig() throws Exception {
        Pipeline pipeline = pipelineFixture.createdPipelineWithAllStagesPassed();
        ccTrayStatus.stageStatusChanged(pipeline.getFirstStage());
        ccTrayStatusService.createCctrayXmlDocument("");

        configHelper.removeStage(pipelineFixture.pipelineName, pipelineFixture.devStage);
        Document document = ccTrayStatusService.createCctrayXmlDocument("");
        String stageProject = format("%s :: %s", pipelineFixture.pipelineName, pipelineFixture.devStage);
        assertNoProject(document, stageProject);
        String jobProject = String.format("%s :: %s :: %s", pipelineFixture.pipelineName, pipelineFixture.devStage,
                PipelineWithTwoStages.JOB_FOR_DEV_STAGE);
        assertNoProject(document, jobProject);
    }

    @Test
    public void shouldUseSiteUrlForWebUrlWhenPresent() throws JDOMException, URISyntaxException {
        Pipeline pipeline = pipelineFixture.createdPipelineWithAllStagesPassed();
        configHelper.setBaseUrls(new ServerSiteUrlConfig("http://10.99.99.99:8153"),new ServerSiteUrlConfig());
        Stage devStage = pipeline.getStages().byName(pipelineFixture.devStage);
        ccTrayStatus.stageStatusChanged(devStage);
        Stage ftStage = pipeline.getStages().byName(pipelineFixture.ftStage);
        ccTrayStatus.stageStatusChanged(ftStage);
        Document document = ccTrayStatusService.createCctrayXmlDocument("http://localhost:8154/go/cctray.xml");
        assertWebUrlContains(document, "http://10.99.99.99:8153");
    }

    @Test
    public void shouldUseSecureSiteUrlForWebUrlWhenPresent() throws JDOMException, URISyntaxException {
        Pipeline pipeline = pipelineFixture.createdPipelineWithAllStagesPassed();
        configHelper.setBaseUrls(new ServerSiteUrlConfig("http://10.99.99.99:8153"),new ServerSiteUrlConfig("https://10.99.99.99:8154"));
        Stage devStage = pipeline.getStages().byName(pipelineFixture.devStage);
        ccTrayStatus.stageStatusChanged(devStage);
        Stage ftStage = pipeline.getStages().byName(pipelineFixture.ftStage);
        ccTrayStatus.stageStatusChanged(ftStage);
        Document document = ccTrayStatusService.createCctrayXmlDocument("https://localhost:8154/go/cctray.xml");
        assertWebUrlContains(document, "https://10.99.99.99:8154");
    }

    @Test //#6774, #6862
    public void shouldNotFetchBreakersInfoFromDependencyMaterial_whenDownstreamHasOnlyDependencyMaterial() throws URISyntaxException, JDOMException, IOException {
        ScheduleTestUtil u = new ScheduleTestUtil(transactionTemplate, materialRepository, dbHelper, configHelper);

        GitMaterial git = u.wf(new GitMaterial("git"), "f");
        ScheduleTestUtil.AddedPipeline upstream = u.saveConfigWith("upstream", u.m(git));
        ScheduleTestUtil.AddedPipeline downstream = u.saveConfigWith("downstream", u.m(upstream));

        int i = 0;
        u.checkinInOrder(git, u.d(i++), "g0");
        String upstream_1 = u.runAndPass(upstream, "g0");
        u.runAndFail(downstream, upstream_1);
        Document document = ccTrayStatusService.createCctrayXmlDocument("http://localhost/go");
        assertThat(XPath.selectNodes(document, "/Projects/Project").size(), is(4));

        String upstreamPipelineName = upstream.config.name().toString();
        String upstreamStageName = upstream.config.getFirstStageConfig().name().toString();
        String upstreamJobName = upstream.config.getFirstStageConfig().getJobs().get(0).name().toString();
        String downstreamPipelineName = downstream.config.name().toString();
        String downstreamStageName = downstream.config.getFirstStageConfig().name().toString();
        String downstreamJobName = downstream.config.getFirstStageConfig().getJobs().get(0).name().toString();
        assertHasTag(document, format("/Projects/Project[@name='%s :: %s']", upstreamPipelineName, upstreamStageName));
        assertHasTag(document, format("/Projects/Project[@name='%s :: %s :: %s']", upstreamPipelineName, upstreamStageName, upstreamJobName));
        assertHasTag(document, format("/Projects/Project[@name='%s :: %s']", downstreamPipelineName, downstreamStageName));
        assertHasTag(document, format("/Projects/Project[@name='%s :: %s :: %s']", downstreamPipelineName, downstreamStageName, downstreamJobName));
        assertHasNoTags(document, format("/Projects/Project[@name='%s :: %s']/messages", downstreamPipelineName, downstreamStageName));
    }

    @Test //#6774, #6824
    public void shouldNotFetchBreakersInfoFromDependencyMaterial_downstreamHasScmAndPipelineDependency() throws URISyntaxException, JDOMException, IOException {
        ScheduleTestUtil u = new ScheduleTestUtil(transactionTemplate, materialRepository, dbHelper, configHelper);

        GitMaterial git = u.wf(new GitMaterial("git"), "f");
        ScheduleTestUtil.AddedPipeline upstream = u.saveConfigWith("upstream", u.m(git));
        ScheduleTestUtil.AddedPipeline downstream = u.saveConfigWith("downstream", u.m(upstream), u.m(git));

        int i = 0;
        u.checkinInOrder(git, u.d(i++), "g0");
        String upstream_1 = u.runAndPass(upstream, "g0");
        u.runAndFail(downstream, upstream_1, "g0");
        Document document = ccTrayStatusService.createCctrayXmlDocument("http://localhost/go");
        assertThat(XPath.selectNodes(document, "/Projects/Project").size(), is(4));

        String upstreamPipelineName = upstream.config.name().toString();
        String upstreamStageName = upstream.config.getFirstStageConfig().name().toString();
        String upstreamJobName = upstream.config.getFirstStageConfig().getJobs().get(0).name().toString();
        String downstreamPipelineName = downstream.config.name().toString();
        String downstreamStageName = downstream.config.getFirstStageConfig().name().toString();
        String downstreamJobName = downstream.config.getFirstStageConfig().getJobs().get(0).name().toString();
        assertHasTag(document, format("/Projects/Project[@name='%s :: %s']", upstreamPipelineName, upstreamStageName));
        assertHasTag(document, format("/Projects/Project[@name='%s :: %s :: %s']", upstreamPipelineName, upstreamStageName, upstreamJobName));
        assertHasTag(document, format("/Projects/Project[@name='%s :: %s']", downstreamPipelineName, downstreamStageName));
        assertHasTag(document, format("/Projects/Project[@name='%s :: %s :: %s']", downstreamPipelineName, downstreamStageName, downstreamJobName));
        assertHasTag(document, format("/Projects/Project[@name='%s :: %s']/messages/message[@text='loser number 0'][@kind='Breakers']", downstreamPipelineName, downstreamStageName));
    }

    private void assertWebUrlContains(Document document, final String contextPath) throws JDOMException {
        List<Element> projects = XPath.selectNodes(document, format("/Projects/Project"));
        for (Element project : projects) {
            assertThat(project.getAttributeValue("webUrl"), containsString(contextPath));
        }
    }

    public static void assertNoProject(Document document, String projectName) throws Exception {
        assertHasNoTags(document, format("/Projects/Project[@name=\"%s\"]", projectName));
    }

    private static void assertHasNoTags(Document document, final String locator) throws JDOMException, IOException {
        List devStageProjects = XPath.selectNodes(document, locator);
        assertThat(locator + " should be in cctray feed \nActual xml: \n" + XmlUtils.asXml(document), devStageProjects.size(), is(0));
    }

    public static void assertHasProject(Document document, String projectName) throws Exception {
        assertHasTag(document, format("/Projects/Project[@name=\"%s\"]", projectName));
    }

    private static void assertHasTag(Document document, final String locator) throws JDOMException, IOException {
        List devStageProjects = XPath.selectNodes(document, locator);
        assertThat(locator + " should be in cctray feed \nActual xml: \n" + XmlUtils.asXml(document), devStageProjects.size(), is(1));
    }
}



