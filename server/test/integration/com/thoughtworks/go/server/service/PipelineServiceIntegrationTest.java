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

import java.io.File;
import java.util.Date;

import com.thoughtworks.go.config.GoConfigDao;
import com.thoughtworks.go.config.materials.Materials;
import com.thoughtworks.go.config.materials.mercurial.HgMaterial;
import com.thoughtworks.go.domain.MaterialRevisions;
import com.thoughtworks.go.domain.Pipeline;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.domain.materials.ModifiedAction;
import com.thoughtworks.go.domain.materials.dependency.DependencyMaterialRevision;
import com.thoughtworks.go.helper.PipelineMother;
import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.dao.PipelineSqlMapDao;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.util.GoConfigFileHelper;
import com.thoughtworks.go.util.ReflectionUtil;
import org.apache.commons.lang.time.DateUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static com.thoughtworks.go.util.DataStructureUtils.a;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:WEB-INF/applicationContext-acegi-security.xml"
})
public class PipelineServiceIntegrationTest {
    @Autowired private DatabaseAccessHelper dbHelper;
    @Autowired private GoCache goCache;
    @Autowired private GoConfigDao goConfigDao;
    @Autowired private PipelineService pipelineService;
    @Autowired private MaterialRepository materialRepository;
    @Autowired private PipelineSqlMapDao pipelineSqlMapDao;
    @Autowired private TransactionTemplate transactionTemplate;

    private GoConfigFileHelper configHelper = new GoConfigFileHelper();
    private ScheduleTestUtil u;

    @Before
    public void setUp() throws Exception {
        goCache.clear();
        configHelper.usingCruiseConfigDao(goConfigDao);
        configHelper.onSetUp();

        dbHelper.onSetUp();
        u = new ScheduleTestUtil(transactionTemplate, materialRepository, dbHelper, configHelper);
    }

    @After
    public void teardown() throws Exception {
        dbHelper.onTearDown();
        configHelper.onTearDown();
    }

    @Test
    public void shouldFetchPipelinePointedToByGivenDMR() {
        Pipeline pipeline = PipelineMother.passedPipelineInstance("pipeline", "stage", "job");
        dbHelper.savePipelineWithMaterials(pipeline);

        UpstreamPipelineResolver resolver = pipelineService;
        BuildCause loadedBC = resolver.buildCauseFor(
                DependencyMaterialRevision.create(pipeline.getStages().get(0).getIdentifier().getStageLocator(), pipeline.getLabel()).getPipelineName(),
                DependencyMaterialRevision.create(pipeline.getStages().get(0).getIdentifier().getStageLocator(), pipeline.getLabel()).getPipelineCounter());

        assertEquals(pipeline.getBuildCause(), loadedBC);
    }

    @Test
    public void shouldReturnCorrectNumberOfMaterialRevisionsAndMaterials() throws Exception {
        File file1 = new File("file1");
        File file2 = new File("file2");
        File file3 = new File("file3");
        File file4 = new File("file4");
        Material hg = new HgMaterial("url", "Dest");
        String[] hgRevs = new String[]{"h1"};

        u.checkinFiles(hg, "h1", a(file1, file2, file3, file4), ModifiedAction.added);

        ScheduleTestUtil.AddedPipeline pair01 = u.saveConfigWith("pair01", "stageName", u.m(hg));

        u.runAndPass(pair01, hgRevs);

        ReflectionUtil.invoke(pipelineSqlMapDao, "initDao");
        Pipeline pipeline = pipelineService.mostRecentFullPipelineByName("pair01");
        MaterialRevisions materialRevisions = pipeline.getBuildCause().getMaterialRevisions();
        assertThat(materialRevisions.getMaterials().size(), is(1));
    }

    @Test
    public void shouldReturnModificationsInCorrectOrder() throws Exception {
        File file1 = new File("file1");
        File file2 = new File("file2");
        File file3 = new File("file3");
        File file4 = new File("file4");
        Material hg1 = new HgMaterial("url1", "Dest1");
        String[] hgRevs = new String[]{"hg1_2"};

        Date latestModification = new Date();
        Date older = DateUtils.addDays(latestModification, -1);
        u.checkinFiles(hg1, "hg1_1", a(file1, file2, file3, file4), ModifiedAction.added, older);
        u.checkinFiles(hg1, "hg1_2", a(file1, file2, file3, file4), ModifiedAction.modified, latestModification);


        ScheduleTestUtil.AddedPipeline pair01 = u.saveConfigWith("pair01", "stageName", u.m(hg1));

        u.runAndPass(pair01, hgRevs);

        ReflectionUtil.invoke(pipelineSqlMapDao, "initDao");
        Pipeline pipeline = pipelineService.mostRecentFullPipelineByName("pair01");
        MaterialRevisions materialRevisions = pipeline.getBuildCause().getMaterialRevisions();
        assertThat(materialRevisions.getMaterials().size(), is(1));
        assertThat(materialRevisions.getDateOfLatestModification().getTime(), is(latestModification.getTime()));
    }

    @Test
    public void shouldReturnPMRsInCorrectOrder() throws Exception {
        File file1 = new File("file1");
        File file2 = new File("file2");
        File file3 = new File("file3");
        File file4 = new File("file4");
        Material hg1 = new HgMaterial("url1", "Dest1");
        Material hg2 = new HgMaterial("url2", "Dest2");
        String[] hgRevs = new String[]{"h1","h2"};

        Date latestModification = new Date();
        u.checkinFiles(hg2, "h2", a(file1, file2, file3, file4), ModifiedAction.added,  org.apache.commons.lang.time.DateUtils.addDays(latestModification, -1));
        u.checkinFiles(hg1, "h1", a(file1, file2, file3, file4), ModifiedAction.added, latestModification);

        ScheduleTestUtil.AddedPipeline pair01 = u.saveConfigWith("pair01", "stageName", u.m(hg1),u.m(hg2));
        u.runAndPass(pair01, hgRevs);

        ReflectionUtil.invoke(pipelineSqlMapDao, "initDao");
        Pipeline pipeline = pipelineService.mostRecentFullPipelineByName("pair01");
        MaterialRevisions materialRevisions = pipeline.getBuildCause().getMaterialRevisions();
        Materials materials = materialRevisions.getMaterials();
        assertThat(materials.size(), is(2));
        assertThat(materials.get(0),is(hg1));
        assertThat(materials.get(1),is(hg2));
    }
}
