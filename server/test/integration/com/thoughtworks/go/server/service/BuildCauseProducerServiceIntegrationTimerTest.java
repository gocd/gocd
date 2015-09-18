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

import java.util.Date;

import com.rits.cloning.Cloner;
import com.thoughtworks.go.config.GoConfigDao;
import com.thoughtworks.go.config.materials.git.GitMaterial;
import com.thoughtworks.go.domain.MaterialRevisions;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.domain.materials.ModifiedAction;
import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.domain.PipelineTimeline;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.server.scheduling.BuildCauseProducerService;
import com.thoughtworks.go.server.service.result.ServerHealthStateOperationResult;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.util.GoConfigFileHelper;
import com.thoughtworks.go.util.LogFixture;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.TestFileUtil;
import org.apache.log4j.Level;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:WEB-INF/applicationContext-acegi-security.xml"
})
public class BuildCauseProducerServiceIntegrationTimerTest {
    public static final String STAGE_NAME = "s";
    public static final Cloner CLONER = new Cloner();
    @Autowired private DatabaseAccessHelper dbHelper;
    @Autowired private GoCache goCache;
    @Autowired private GoConfigDao goConfigDao;
    @Autowired private BuildCauseProducerService buildCauseProducerService;
    @Autowired private MaterialRepository materialRepository;
    @Autowired private TransactionTemplate transactionTemplate;
    @Autowired private SystemEnvironment systemEnvironment;
    @Autowired private PipelineTimeline pipelineTimeline;
    @Autowired private PipelineScheduleQueue piplineScheduleQueue;

    private GoConfigFileHelper configHelper = new GoConfigFileHelper();
    private ScheduleTestUtil u;
    private LogFixture logFixture;

    @Before
    public void setUp() throws Exception {
        goCache.clear();
        configHelper.usingCruiseConfigDao(goConfigDao);
        configHelper.onSetUp();

        dbHelper.onSetUp();
        u = new ScheduleTestUtil(transactionTemplate, materialRepository, dbHelper, configHelper);
        logFixture = LogFixture.startListening();
    }

    @After
    public void teardown() throws Exception {
        systemEnvironment.reset(SystemEnvironment.RESOLVE_FANIN_MAX_BACK_TRACK_LIMIT);
        dbHelper.onTearDown();
        configHelper.onTearDown();
        piplineScheduleQueue.clear();
        logFixture.stopListening();
    }

    @Test
    public void pipelineWithTimerShouldRunWithLatestMaterialsWhenItHasBeenForceRunWithOlderMaterials_GivenTimerIsSetToTriggerOnlyForNewMaterials() throws Exception {
        int i = 1;
        String pipelineName = "p1";
        GitMaterial git1 = u.wf(new GitMaterial("git1"), "folder");

        ScheduleTestUtil.AddedPipeline p1 = u.saveConfigWithTimer(pipelineName, u.timer("* * * * * ? 2000", true), u.m(git1));

        u.checkinFile(git1, "g11", TestFileUtil.createTempFile("blah_g11"), ModifiedAction.added);
        u.checkinFile(git1, "g12", TestFileUtil.createTempFile("blah_g12"), ModifiedAction.added);

        Date mduTimeForG11 = u.d(i++);
        u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p1, mduTimeForG11, "g11");
        u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p1, u.d(i++), "g12");
        u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p1, mduTimeForG11, "g11");
        pipelineTimeline.update();

        buildCauseProducerService.timerSchedulePipeline(p1.config, new ServerHealthStateOperationResult());

        assertThat(piplineScheduleQueue.toBeScheduled().size(), is(1));
        assertThat(piplineScheduleQueue.toBeScheduled().get(pipelineName).getMaterialRevisions(), isSameMaterialRevisionsAs(u.mrs(u.mr(git1, false, "g12"))));
    }

    @Test
    public void pipelineWithTimerShouldRerunWhenItHasAlreadyRunWithLatestMaterials_GivenTimerIsNOTSetToTriggerOnlyForNewMaterials() throws Exception {
        int i = 1;
        String pipelineName = "p1";
        GitMaterial git1 = u.wf(new GitMaterial("git1"), "folder");

        ScheduleTestUtil.AddedPipeline up1 = u.saveConfigWith("up1", u.m(git1));
        ScheduleTestUtil.AddedPipeline p1 = u.saveConfigWithTimer(pipelineName, u.timer("* * * * * ? 2000", false), u.m(git1), u.m(up1));

        u.checkinFile(git1, "g11", TestFileUtil.createTempFile("blah_g11"), ModifiedAction.added);
        String up1_1 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(up1, u.d(i++), "g11");
        pipelineTimeline.update();

        // Run once with latest, when pipeline schedules due to timer.
        buildCauseProducerService.timerSchedulePipeline(p1.config, new ServerHealthStateOperationResult());

        assertThat(piplineScheduleQueue.toBeScheduled().size(), is(1));
        assertThat(piplineScheduleQueue.toBeScheduled().get(pipelineName).getMaterialRevisions(), isSameMaterialRevisionsAs(u.mrs(u.mr(git1, true, "g11"), u.mr(up1, true, up1_1))));

        BuildCause buildCause = piplineScheduleQueue.toBeScheduled().get(pipelineName);
        String up1_2 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(up1, u.d(i++), "g11");
        piplineScheduleQueue.finishSchedule(pipelineName, buildCause, buildCause);

        // Timer time comes around again. Will rerun since the new flag (runOnlyOnNewMaterials) is not ON.
        buildCauseProducerService.timerSchedulePipeline(p1.config, new ServerHealthStateOperationResult());

        assertThat(piplineScheduleQueue.toBeScheduled().size(), is(1));
        assertThat(piplineScheduleQueue.toBeScheduled().get(pipelineName).getMaterialRevisions(), is(u.mrs(u.mr(git1, false, "g11"), u.mr(up1, false, up1_2))));
        assertThat(logFixture.contains(Level.INFO, "Skipping scheduling of timer-triggered pipeline 'p1' as it has previously run with the latest material(s)."), is(false));
    }

    @Test
    public void pipelineWithTimerShouldNotRerunWhenItHasAlreadyRunWithLatestMaterials_GivenTimerIsSetToTriggerOnlyForNewMaterials() throws Exception {
        int i = 1;
        String pipelineName = "p1";
        GitMaterial git1 = u.wf(new GitMaterial("git1"), "folder1");
        GitMaterial git2 = u.wf(new GitMaterial("git2"), "folder2");

        ScheduleTestUtil.AddedPipeline p1 = u.saveConfigWithTimer(pipelineName, u.timer("* * * * * ? 2000", true), u.m(git1), u.m(git2));

        u.checkinFile(git1, "g11", TestFileUtil.createTempFile("blah_g11"), ModifiedAction.added);
        u.checkinFile(git2, "g21", TestFileUtil.createTempFile("blah_g21"), ModifiedAction.added);

        // Run once with latest, when pipeline schedules due to timer.
        buildCauseProducerService.timerSchedulePipeline(p1.config, new ServerHealthStateOperationResult());

        assertThat(piplineScheduleQueue.toBeScheduled().size(), is(1));
        assertThat(piplineScheduleQueue.toBeScheduled().get(pipelineName).getMaterialRevisions(), isSameMaterialRevisionsAs(u.mrs(u.mr(git1, true, "g11"), u.mr(git2, true, "g21"))));

        BuildCause buildCause = piplineScheduleQueue.toBeScheduled().get(pipelineName);
        piplineScheduleQueue.finishSchedule(pipelineName, buildCause, buildCause);

        // Timer time comes around again. Will NOT rerun since the new flag (runOnlyOnNewMaterials) is ON.
        buildCauseProducerService.timerSchedulePipeline(p1.config, new ServerHealthStateOperationResult());

        assertThat(piplineScheduleQueue.toBeScheduled().size(), is(0));
        assertThat(logFixture.contains(Level.INFO, "Skipping scheduling of timer-triggered pipeline 'p1' as it has previously run with the latest material(s)."), is(true));
    }

    @Test
    public void pipelineWithTimerShouldRunOnlyWithMaterialsWhichChanged_GivenTimerIsSetToTriggerOnlyForNewMaterials() throws Exception {
        int i = 1;
        String pipelineName = "p1";
        GitMaterial git1 = u.wf(new GitMaterial("git1"), "folder1");
        GitMaterial git2 = u.wf(new GitMaterial("git2"), "folder2");

        ScheduleTestUtil.AddedPipeline p1 = u.saveConfigWith("up1", u.m(git1));
        ScheduleTestUtil.AddedPipeline p2 = u.saveConfigWithTimer(pipelineName, u.timer("* * * * * ? 2000", true), u.m(git1), u.m(git2), u.m(p1));

        u.checkinFile(git1, "g11", TestFileUtil.createTempFile("blah_g11"), ModifiedAction.added);
        u.checkinFile(git2, "g21", TestFileUtil.createTempFile("blah_g21"), ModifiedAction.added);
        Date mduTimeOfG11 = u.d(i++);
        String p1_1 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p1, mduTimeOfG11, "g11");
        pipelineTimeline.update();

        // Run once with latest, when pipeline schedules due to timer.
        buildCauseProducerService.timerSchedulePipeline(p2.config, new ServerHealthStateOperationResult());

        assertThat(piplineScheduleQueue.toBeScheduled().size(), is(1));
        assertThat(piplineScheduleQueue.toBeScheduled().get(pipelineName).getMaterialRevisions(), isSameMaterialRevisionsAs(u.mrs(u.mr(git1, true, "g11"), u.mr(git2, true, "g21"), u.mr(p1, true, p1_1))));

        BuildCause buildCause = piplineScheduleQueue.toBeScheduled().get(pipelineName);
        u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p2, u.d(i++), "g11", "g21", p1_1);
        piplineScheduleQueue.finishSchedule(pipelineName, buildCause, buildCause);

        // Check in to git2 and run pipeline P1 once before the timer time. Then, timer happens. Shows those two materials in "yellow" (changed), on the UI.
        u.checkinFile(git2, "g22", TestFileUtil.createTempFile("blah_g22"), ModifiedAction.added);
        String p1_2 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p1, mduTimeOfG11, "g11");
        pipelineTimeline.update();
        buildCauseProducerService.timerSchedulePipeline(p2.config, new ServerHealthStateOperationResult());

        assertThat(piplineScheduleQueue.toBeScheduled().size(), is(1));
        assertThat(piplineScheduleQueue.toBeScheduled().get(pipelineName).getMaterialRevisions(), isSameMaterialRevisionsAs(u.mrs(u.mr(git1, false, "g11"), u.mr(git2, true, "g22"), u.mr(p1, true, p1_2))));
        assertThat(logFixture.contains(Level.INFO, "Skipping scheduling of timer-triggered pipeline 'p1' as it has previously run with the latest material(s)."), is(false));
    }

    private Matcher<? super MaterialRevisions> isSameMaterialRevisionsAs(final MaterialRevisions expectedRevisions) {
        return new BaseMatcher<Object>() {
            @Override
            public boolean matches(Object o) {
                MaterialRevisions actualRevisions = (MaterialRevisions) o;
                return actualRevisions.equals(expectedRevisions) && allMaterialRevisionChangedFlagsMatch(expectedRevisions, actualRevisions);
            }

            private boolean allMaterialRevisionChangedFlagsMatch(MaterialRevisions expectedRevisions, MaterialRevisions actualRevisions) {
                for (int i = 0; i < expectedRevisions.numberOfRevisions(); i++) {
                    if (expectedRevisions.getMaterialRevision(i).isChanged() != actualRevisions.getMaterialRevision(i).isChanged()) {
                        return false;
                    }
                }

                return true;
            }

            @Override
            public void describeTo(Description description) {
                description.appendValue(expectedRevisions);
            }
        };
    }
}
