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
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import com.thoughtworks.go.config.GoConfigDao;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.materials.Filter;
import com.thoughtworks.go.config.materials.IgnoredFiles;
import com.thoughtworks.go.config.materials.SubprocessExecutionContext;
import com.thoughtworks.go.config.materials.mercurial.HgMaterial;
import com.thoughtworks.go.domain.MaterialRevisions;
import com.thoughtworks.go.domain.Pipeline;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.materials.svn.Subversion;
import com.thoughtworks.go.helper.*;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.dao.PipelineDao;
import com.thoughtworks.go.server.scheduling.ScheduleHelper;
import com.thoughtworks.go.util.GoConfigFileHelper;
import com.thoughtworks.go.util.FileUtil;
import com.thoughtworks.go.util.TestFileUtil;
import com.thoughtworks.go.util.command.InMemoryStreamConsumer;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static com.thoughtworks.go.util.command.ProcessOutputStreamConsumer.inMemoryConsumer;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:WEB-INF/applicationContext-acegi-security.xml"
})
public class BuildCauseProducerServiceIntegrationHgTest {

    private static final String STAGE_NAME = "dev";

    @Autowired private GoConfigService goConfigService;
    @Autowired private GoConfigDao goConfigDao;
    @Autowired private PipelineDao pipelineDao;
    @Autowired private PipelineScheduleQueue pipelineScheduleQueue;
    @Autowired private ScheduleHelper scheduleHelper;
    @Autowired private SubprocessExecutionContext subprocessExecutionContext;

    @Autowired PipelineService pipelineService;
    @Autowired private DatabaseAccessHelper dbHelper;
    private static GoConfigFileHelper configHelper = new GoConfigFileHelper();
    public Subversion repository;
    public static SvnTestRepo svnRepository;
    private Pipeline latestPipeline;
    private InMemoryStreamConsumer outputStreamConsumer = inMemoryConsumer();
    private HgTestRepo hgTestRepo;
    private HgMaterial hgMaterial;
    private File workingFolder;
    PipelineConfig mingleConfig;

    @Before
    public void setup() throws Exception {
        dbHelper.onSetUp();
        configHelper.onSetUp();
        configHelper.usingCruiseConfigDao(goConfigDao).initializeConfigFile();
        hgTestRepo = new HgTestRepo("hgTestRepo1");
        hgMaterial = MaterialsMother.hgMaterial(hgTestRepo.projectRepositoryUrl());
        hgMaterial.setFilter(new Filter(new IgnoredFiles("helper/**/*.*")));
        workingFolder = TestFileUtil.createTempFolder("workingFolder");
        outputStreamConsumer = inMemoryConsumer();
        mingleConfig = configHelper.addPipeline("cruise", STAGE_NAME, this.hgMaterial.config(), "unit", "functional");
    }

    @After
    public void teardown() throws Exception {
        TestRepo.internalTearDown();
        dbHelper.onTearDown();
        FileUtil.deleteFolder(goConfigService.artifactsDir());
        FileUtil.deleteFolder(workingFolder);
        TestRepo.internalTearDown();
        pipelineScheduleQueue.clear();
    }


    /**
     * How we handle SVN Material and other Material(Hg Git and etc) is different, which caused the bug
     * #2375
     */
    @Test
    public void shouldNotGetModificationWhenCheckInFilesInIgnoredList() throws Exception {
        prepareAPipelineWithHistory();

        checkInFiles("helper/resources/images/cruise/StageActivity.png",
                "helper/topics/upgrading_go.xml",
                "helper/topics/whats_new_in_go.xml");

        Map<String, BuildCause> beforeLoad = pipelineScheduleQueue.toBeScheduled();

        scheduleHelper.autoSchedulePipelinesWithRealMaterials();

        Map<String, BuildCause> afterLoad = pipelineScheduleQueue.toBeScheduled();
        assertThat(afterLoad.size(), is(beforeLoad.size()));

    }

    private void prepareAPipelineWithHistory() throws SQLException {
        MaterialRevisions materialRevisions = new MaterialRevisions();
        List<Modification> modifications = this.hgMaterial.latestModification(workingFolder, subprocessExecutionContext);
        materialRevisions.addRevision(this.hgMaterial, modifications);
        BuildCause buildCause = BuildCause.createWithModifications(materialRevisions, "");

        latestPipeline = PipelineMother.schedule(mingleConfig, buildCause);
        latestPipeline = dbHelper.savePipelineWithStagesAndMaterials(latestPipeline);
        dbHelper.passStage(latestPipeline.getStages().first());
    }

    private void checkInFiles(String... files) throws Exception {
        for (String fileName : files) {
            File file = new File(workingFolder, fileName);
            FileUtils.writeStringToFile(file, "bla");
            hgMaterial.add(workingFolder, outputStreamConsumer, file);
        }
        hgMaterial.commit(workingFolder, outputStreamConsumer, "comment ", "user");
        hgMaterial.push(workingFolder, outputStreamConsumer);
    }
}
