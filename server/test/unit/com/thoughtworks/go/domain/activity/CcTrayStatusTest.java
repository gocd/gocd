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

package com.thoughtworks.go.domain.activity;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.domain.JobInstance;
import com.thoughtworks.go.domain.JobInstances;
import com.thoughtworks.go.domain.JobState;
import com.thoughtworks.go.domain.MaterialRevision;
import com.thoughtworks.go.domain.MaterialRevisions;
import com.thoughtworks.go.domain.NullStage;
import com.thoughtworks.go.domain.Stage;
import com.thoughtworks.go.domain.StageIdentifier;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.helper.JobInstanceMother;
import com.thoughtworks.go.helper.MaterialsMother;
import com.thoughtworks.go.helper.ModificationsMother;
import com.thoughtworks.go.helper.StageConfigMother;
import com.thoughtworks.go.helper.StageMother;
import com.thoughtworks.go.server.dao.StageDao;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.util.TimeProvider;
import org.junit.Before;
import org.junit.Test;

import static com.thoughtworks.go.domain.activity.ProjectStatus.DEFAULT_LAST_BUILD_LABEL;
import static com.thoughtworks.go.domain.activity.ProjectStatus.DEFAULT_LAST_BUILD_STATUS;
import static com.thoughtworks.go.domain.activity.ProjectStatus.DEFAULT_LAST_BUILD_TIME;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CcTrayStatusTest {

    private MaterialRepository materialRepo;
    private StageDao stageDao;

    @Before
    public void setUp() {
        materialRepo = mock(MaterialRepository.class);
        stageDao = mock(StageDao.class);
        when(materialRepo.findMaterialRevisionsForPipeline(anyLong())).thenReturn(new MaterialRevisions());
        Stage stage = new Stage();
        stage.setPipelineId(10l);
        when(stageDao.stageById(anyLong())).thenReturn(stage);
    }

    @Test
    public void shouldUpdateJobStatusWhenItComplete() throws Exception {
        CcTrayStatus status = new CcTrayStatus(materialRepo, stageDao);
        JobInstance job = buildingJob("cruise", "label", "dev", "linux-firefox");
        status.jobStatusChanged(job);

        assertThat(status.projects().size(), is(1));
        assertThat(status.projects(),
                hasItem(new ProjectStatus("cruise :: dev :: linux-firefox", "Building", DEFAULT_LAST_BUILD_STATUS,
                        DEFAULT_LAST_BUILD_LABEL, DEFAULT_LAST_BUILD_TIME, job.getIdentifier().webUrl())));


        job = failedJob("cruise", "label", "dev", "linux-firefox");
        status.jobStatusChanged(job);

        assertThat(status.projects(),
                hasItem(projectStatusForFailedJob(job)));
    }

    @Test
    public void shouldAddBreakersToFailedStageAndJobsOnly() throws Exception {
        CcTrayStatus status = new CcTrayStatus(materialRepo, stageDao);

        JobInstance passedJob = passedJob("go", DEFAULT_LAST_BUILD_LABEL, "dev", "linux-firefox");
        JobInstance failedJob = failedJob("go", DEFAULT_LAST_BUILD_LABEL, "dev", "win-firefox");
        passedJob.setStageId(1L);
        failedJob.setStageId(1L);
        
        Stage failedStage = new Stage("dev", new JobInstances(passedJob, failedJob), "me", "success", false, false, new TimeProvider());
        failedStage.setPipelineId(12l);
        failedStage.calculateResult();

        when(stageDao.stageById(1L)).thenReturn(failedStage);
        Modification losersOtherCommit = ModificationsMother.checkinWithComment("125", "comment 3", "loser", "loser@different-email.com", new Date(), "baz.c");
        MaterialRevision revision = new MaterialRevision(MaterialsMother.gitMaterial("foo.com"), losersOtherCommit);
        revision.markAsChanged();

        when(materialRepo.findMaterialRevisionsForPipeline(12l)).thenReturn(new MaterialRevisions(revision));

        status.jobStatusChanged(passedJob);

        HashSet<String> breakers = new HashSet<String>();

        assertThat(status.projects().size(), is(1));
        assertThat((HashSet<String>) ((ProjectStatus)(status.projects().toArray()[0])).getBreakers(), is(breakers));
    }

    @Test
    public void shouldAddBreakersToJobStatusWhenStageCompletesWithFailure() throws Exception {
        CcTrayStatus status = new CcTrayStatus(materialRepo, stageDao);

        String stageName = "dev";
        String pipelineName = "go";
        JobInstance passedJob = passedJob(pipelineName, DEFAULT_LAST_BUILD_LABEL, stageName, "linux-firefox");
        JobInstance failedJob = failedJob(pipelineName, DEFAULT_LAST_BUILD_LABEL, stageName, "win-firefox");

        Stage failedStage = new Stage(stageName, new JobInstances(passedJob, failedJob), "me", "success", false, false, new TimeProvider());
        failedStage.setIdentifier(new StageIdentifier(pipelineName, 1, "1", stageName, "1"));
        failedStage.setPipelineId(12l);
        failedStage.calculateResult();

        Modification losersOtherCommit = ModificationsMother.checkinWithComment("125", "comment 3", "loser", "loser@different-email.com", new Date(), "baz.c");
        MaterialRevision revision = new MaterialRevision(MaterialsMother.gitMaterial("foo.com"), losersOtherCommit);

        when(materialRepo.findMaterialRevisionsForPipeline(12l)).thenReturn(new MaterialRevisions(revision));
        revision.markAsChanged();

        status.stageStatusChanged(failedStage);

        HashSet<String> breakers = new HashSet<String>();
        breakers.add("loser");

        assertThat(status.projects().size(), is(3));
        ProjectStatus failedJobEntry = status.getProject(failedJob.getIdentifier().ccProjectName());
        ProjectStatus passedJobEntry = status.getProject(passedJob.getIdentifier().ccProjectName());
        ProjectStatus stageEntry = status.getProject(failedStage.getIdentifier().ccProjectName());

        assertThat((HashSet<String>) failedJobEntry.getBreakers(), is(breakers));
        assertThat((HashSet<String>) stageEntry.getBreakers(), is(breakers));
        assertThat((HashSet<String>) passedJobEntry.getBreakers(), is(new HashSet<String>()));
        
        Set<String> breakersSet = CcTrayStatus.computeBreakersIfStageFailed(failedStage, new MaterialRevisions(revision));
        assertThat(breakersSet, hasItem("loser"));
        assertThat(breakersSet.size(), is(1));
    }

    @Test
    public void shouldCaptureUniqueModificationAuthorNamesAsBreakers_inCaseOfFailure() throws Exception {
        CcTrayStatus status = new CcTrayStatus(materialRepo, stageDao);
        String pipelineName = "cruise";
        String stageName = "dev";
        JobInstance job = failedJob(pipelineName, DEFAULT_LAST_BUILD_LABEL, stageName, "linux-firefox");
        Stage stage = new Stage(stageName, new JobInstances(job), "me", "manual", false, false, new TimeProvider());
        stage.setIdentifier(new StageIdentifier(pipelineName, 1, DEFAULT_LAST_BUILD_LABEL, stageName, "1"));
        stage.calculateResult();
        stage.setPipelineId(12l);

        Modification losersCommit = ModificationsMother.checkinWithComment("123", "comment 1", "loser", "loser@boozer.com", new Date(), "foo.c");
        Modification boozersCommit = ModificationsMother.checkinWithComment("124", "comment 2", "boozer", "boozer@loser.com", new Date(), "bar.c");
        Modification losersOtherCommit = ModificationsMother.checkinWithComment("125", "comment 3", "loser", "loser@different-email.com", new Date(), "baz.c");
        MaterialRevision revision = new MaterialRevision(MaterialsMother.gitMaterial("foo.com"), losersCommit, boozersCommit, losersOtherCommit);
        revision.markAsChanged();
        when(materialRepo.findMaterialRevisionsForPipeline(12l)).thenReturn(new MaterialRevisions(revision));

        status.stageStatusChanged(stage);
        assertThat(status.projects().size(), is(2));
        HashSet<String> breakers = new HashSet<String>();
        breakers.add("loser");
        breakers.add("boozer");
        assertThat(status.projects(), hasItem(new ProjectStatus("cruise :: dev :: linux-firefox", "Sleeping", "Failure", DEFAULT_LAST_BUILD_LABEL, job.getCompletedDate(),
                job.getIdentifier().webUrl(), breakers)));
        assertThat(status.projects(), hasItem(new ProjectStatus("cruise :: dev", "Sleeping", "Failure", DEFAULT_LAST_BUILD_LABEL, job.getCompletedDate(),
                        stage.getIdentifier().webUrl(), breakers)));
    }

    @Test
    public void shouldCaptureAuthorNamesOfChangedRevisionsOnlyAsBreakers() throws Exception {
        CcTrayStatus status = new CcTrayStatus(materialRepo, stageDao);
        String stageName = "dev";
        String pipelineName = "cruise";
        JobInstance job = failedJob(pipelineName, DEFAULT_LAST_BUILD_LABEL, stageName, "linux-firefox");

        Stage stage = new Stage(stageName, new JobInstances(job), "me", "manual", false, false, new TimeProvider());
        stage.setIdentifier(new StageIdentifier(pipelineName, 1, DEFAULT_LAST_BUILD_LABEL, stageName, "1"));
        stage.calculateResult();
        stage.setPipelineId(12l);

        Modification losersCommit = ModificationsMother.checkinWithComment("123", "comment 1", "loser", "loser@boozer.com", new Date(), "foo.c");
        Modification boozersCommit = ModificationsMother.checkinWithComment("124", "comment 2", "boozer", "boozer@loser.com", new Date(), "bar.c");
        MaterialRevision changedRevision = new MaterialRevision(MaterialsMother.gitMaterial("foo.com"), losersCommit, boozersCommit);
        changedRevision.markAsChanged();

        Modification barsCommit = ModificationsMother.checkinWithComment("125", "comment 1", "bar", "bar@loser.com", new Date(), "bar.c");
        MaterialRevision unchangedRevision = new MaterialRevision(MaterialsMother.gitMaterial("bar.com"), barsCommit);

        MaterialRevisions revisions = new MaterialRevisions(changedRevision, unchangedRevision);

        when(materialRepo.findMaterialRevisionsForPipeline(12l)).thenReturn(revisions);

        status.stageStatusChanged(stage);
        assertThat(status.projects().size(), is(2));
        HashSet<String> breakers = new HashSet<String>();
        breakers.add("loser");
        breakers.add("boozer");
        assertThat(status.projects(), hasItem(new ProjectStatus("cruise :: dev :: linux-firefox", "Sleeping", "Failure", DEFAULT_LAST_BUILD_LABEL, job.getCompletedDate(),
                job.getIdentifier().webUrl(), breakers)));
        assertThat(status.projects(), hasItem(new ProjectStatus("cruise :: dev", "Sleeping", "Failure", DEFAULT_LAST_BUILD_LABEL, job.getCompletedDate(),
                stage.getIdentifier().webUrl(), breakers)));
    }

    @Test
    public void shouldNotCaptureAuthorNamesForDependencyMaterial() throws Exception {
        CcTrayStatus status = new CcTrayStatus(materialRepo, stageDao);
        String pipelineName = "cruise";
        String stageName = "dev";
        JobInstance job = failedJob(pipelineName, DEFAULT_LAST_BUILD_LABEL, stageName, "linux-firefox");

        Stage stage = new Stage(stageName, new JobInstances(job), "me", "manual", false, false, new TimeProvider());
        stage.setIdentifier(new StageIdentifier(pipelineName, 1, DEFAULT_LAST_BUILD_LABEL, stageName, "1"));
        stage.calculateResult();
        stage.setPipelineId(12l);

        Modification losersCommit = ModificationsMother.checkinWithComment("123", "comment 1", "loser", "loser@boozer.com", new Date(), "foo.c");

        MaterialRevision changedRevision = new MaterialRevision(MaterialsMother.gitMaterial("foo.com"), losersCommit);
        changedRevision.markAsChanged();

        MaterialRevision pipelineRevision = ModificationsMother.dependencyMaterialRevision("dep-pipe", 1, "pipe-1", "dep-stage", 1, new Date());
        pipelineRevision.markAsChanged();

        MaterialRevisions revisions = new MaterialRevisions(changedRevision, pipelineRevision);

        when(materialRepo.findMaterialRevisionsForPipeline(12l)).thenReturn(revisions);

        status.stageStatusChanged(stage);
        assertThat(status.projects().size(), is(2));
        HashSet<String> breakers = new HashSet<String>();
        breakers.add("loser");
        ProjectStatus projectStatus = new ProjectStatus("cruise :: dev :: linux-firefox", "Sleeping", "Failure", DEFAULT_LAST_BUILD_LABEL, job.getCompletedDate(),
                job.getIdentifier().webUrl(), breakers);
        assertThat(status.projects(), hasItem(projectStatus));
        assertThat(status.projects(), hasItem(new ProjectStatus("cruise :: dev", "Sleeping", "Failure", DEFAULT_LAST_BUILD_LABEL, job.getCompletedDate(),
                        stage.getIdentifier().webUrl(), breakers)));
    }

    @Test
    public void shouldUpdateJobStatusWhenItChangesAgain() throws Exception {
        CcTrayStatus status = new CcTrayStatus(materialRepo, stageDao);

        JobInstance job = failedJob("cruise", "label", "dev", "linux-firefox");
        status.jobStatusChanged(job);

        String lastBuildLabel = job.getIdentifier().getStageIdentifier().ccTrayLastBuildLabel();
        Date lastBuildTime = job.getStartedDateFor(JobState.Completed);
        assertThat(status.projects(),
                hasItem(new ProjectStatus("cruise :: dev :: linux-firefox", "Sleeping", "Failure",
                        lastBuildLabel,
                        lastBuildTime, job.getIdentifier().webUrl())));

        job = buildingJob("cruise", "label", "dev", "linux-firefox");
        status.jobStatusChanged(job);

        assertThat(status.projects(),
                hasItem(new ProjectStatus("cruise :: dev :: linux-firefox", "Building", "Failure",
                        lastBuildLabel, lastBuildTime, job.getIdentifier().webUrl())));
    }


    @Test
    public void shouldUpdateStageStatusWhenItComplete() throws Exception {
        CcTrayStatus status = new CcTrayStatus(materialRepo, stageDao);
        Stage stage = scheduleStage("cruise", "label", "dev", "linux-firefox");
        JobInstance job = stage.getJobInstances().first();

        status.stageStatusChanged(stage);
        assertThat(status.projects().size(), is(2));
        assertThat(status.projects(),
                hasItem(new ProjectStatus("cruise :: dev", "Building", DEFAULT_LAST_BUILD_STATUS,
                        DEFAULT_LAST_BUILD_LABEL,
                        DEFAULT_LAST_BUILD_TIME, stage.getIdentifier().webUrl())));

        assertThat(status.projects(),
                hasItem(new ProjectStatus("cruise :: dev :: linux-firefox", "Building", DEFAULT_LAST_BUILD_STATUS,
                        DEFAULT_LAST_BUILD_LABEL,
                        DEFAULT_LAST_BUILD_TIME, job.getIdentifier().webUrl())));

        stage = failedStage("cruise", "label", "dev", "1", "linux-firefox");
        job = stage.getJobInstances().first();
        status.stageStatusChanged(stage);

        assertThat(status.projects().size(), is(2));
        assertThat(status.projects(), hasItem(projectStatusForFailedStage(stage)));

        assertThat(status.projects(), hasItem(projectStatusForFailedJob(job)));
    }

    @Test
    public void shouldUpdateStageActivityWhenItChangesAgain() throws Exception {
        CcTrayStatus status = new CcTrayStatus(materialRepo, stageDao);

        Stage stage = failedStage("cruise", "label", "dev", "1", "linux-firefox");
        JobInstance job = stage.getJobInstances().first();
        status.stageStatusChanged(stage);

        String lastBuildLabelOfStage = stage.getIdentifier().ccTrayLastBuildLabel();
        Date lastBuildTimeOfStage = stage.completedDate();

        assertThat(status.projects().size(), is(2));
        assertThat(status.projects(),
                hasItem(new ProjectStatus("cruise :: dev", "Sleeping", "Failure", lastBuildLabelOfStage,
                        lastBuildTimeOfStage, stage.getIdentifier().webUrl())));

        String lastBuildLabelOfJob = job.getIdentifier().getStageIdentifier().ccTrayLastBuildLabel();
        Date lastBuildTimeOfJob = job.getStartedDateFor(JobState.Completed);
        assertThat(status.projects(),
                hasItem(new ProjectStatus("cruise :: dev :: linux-firefox", "Sleeping", "Failure",
                        lastBuildLabelOfJob,
                        lastBuildTimeOfJob, job.getIdentifier().webUrl())));

        stage = scheduleStage("cruise", "label", "dev", "linux-firefox");
        job = stage.getJobInstances().first();
        status.stageStatusChanged(stage);

        assertThat(status.projects().size(), is(2));
        assertThat(status.projects(),
                hasItem(new ProjectStatus("cruise :: dev", "Building", "Failure",
                        lastBuildLabelOfStage, lastBuildTimeOfStage, stage.getIdentifier().webUrl())));

        assertThat(status.projects(),
                hasItem(new ProjectStatus("cruise :: dev :: linux-firefox", "Building", "Failure",
                        lastBuildLabelOfJob,
                        lastBuildTimeOfJob, job.getIdentifier().webUrl())));
    }

    @Test
    public void shouldNotUpdateProjectStatusIfGotNullStage() throws Exception {
        CcTrayStatus status = new CcTrayStatus(materialRepo, stageDao);

        status.stageStatusChanged(NullStage.createNullStage(StageConfigMother.stageConfig("dev")));
        assertThat(status.projects().size(), is(0));
    }


    @Test
    public void shouldCacheNullStageButNotReturnIt() throws Exception {
        CcTrayStatus status = new CcTrayStatus(materialRepo, stageDao);
        status.updateStatusFor("foo :: dev", NullStage.createNullStage(StageConfigMother.stageConfig("dev")));
        assertThat(status.hasNoActivityFor("foo :: dev"), is(true));
    }

    @Test
    public void shouldRemoveProjectNameFromNoActivityListIfStageComesIntoExistance() throws Exception {
        CcTrayStatus status = new CcTrayStatus(materialRepo, stageDao);
        status.updateStatusFor("foo :: dev", NullStage.createNullStage(StageConfigMother.stageConfig("dev")));
        assertThat(status.hasNoActivityFor("foo :: dev"), is(true));
        Stage stage = StageMother.completedFailedStageInstance("foo", "dev", "job");
        stage.setPipelineId(10l);
        status.stageStatusChanged(stage);
        assertThat(status.hasNoActivityFor("foo :: dev"), is(false));
    }

    @Test
    public void lastBuildLabelShouldContainStageCounterIfStageHasRerun() {
        CcTrayStatus status = new CcTrayStatus(materialRepo, stageDao);

        Stage stage = failedStage("cruise", "label", "dev", "2", "linux-firefox");
        JobInstance job = stage.getJobInstances().first();
        status.stageStatusChanged(stage);

        assertThat(status.projects(), hasItem(projectStatusForFailedStage(stage)));
        assertThat(status.projects(), hasItem(projectStatusForFailedJob(job)));
    }

    private ProjectStatus projectStatusForFailedJob(JobInstance job) {
        return new ProjectStatus("cruise :: dev :: linux-firefox", "Sleeping", "Failure",
                job.getIdentifier().getStageIdentifier().ccTrayLastBuildLabel(),
                job.getStartedDateFor(JobState.Completed), job.getIdentifier().webUrl());
    }

    @Test
    public void shouldDumpProjectToResultIfItExist() {
        CcTrayStatus status = new CcTrayStatus(materialRepo, stageDao);

        Stage stage = failedStage("cruise", "label", "dev", "2", "linux-firefox");
        status.stageStatusChanged(stage);

        ArrayList<ProjectStatus> result = new ArrayList<ProjectStatus>();
        status.dumpProject("cruise :: dev", result);

        assertThat(result, hasItem(projectStatusForFailedStage(stage)));
    }

    private ProjectStatus projectStatusForFailedStage(Stage stage) {
        return new ProjectStatus("cruise :: dev", "Sleeping", "Failure",
                stage.getIdentifier().ccTrayLastBuildLabel(),
                stage.completedDate(), stage.getIdentifier().webUrl());
    }

    private Stage scheduleStage(String pipelineName, String pipelineLabel, String stageName, String jobName) {
        Stage stage = StageMother.scheduledStage(pipelineName, 1, stageName, 1, jobName);
        stage.setIdentifier(new StageIdentifier(pipelineName, -1, pipelineLabel, stageName, "1"));
        for (JobInstance jobInstance : stage.getJobInstances()) {
            jobInstance.setIdentifier(new JobIdentifier(pipelineName, -1, pipelineLabel, stageName, "1", jobName));
        }
        stage.setPipelineId(10l);
        return stage;
    }

    private Stage failedStage(String pipelineName, String pipelineLabel, String stageName, String stageCounter,
                              String jobName) {
        Stage stage = StageMother.completedFailedStageInstance("pipeline-name", stageName, jobName);
        stage.setIdentifier(new StageIdentifier(pipelineName, -1, pipelineLabel, stageName, stageCounter));
        for (JobInstance jobInstance : stage.getJobInstances()) {
            jobInstance.setIdentifier(new JobIdentifier(pipelineName, -1, pipelineLabel, stageName, stageCounter, jobName));
        }
        stage.setPipelineId(10l);
        return stage;
    }

    private JobInstance buildingJob(String pipelineName, String pipelineLabel, String stageName, String jobName) {
        JobInstance job = JobInstanceMother.building(jobName);
        job.setIdentifier(new JobIdentifier(pipelineName,-1, pipelineLabel, stageName, "1", job.getName()));
        return job;
    }

    private JobInstance failedJob(String pipelineName, String pipelineLabel, String stageName, String jobName) {
        JobInstance job = JobInstanceMother.failed(jobName);
        job.setIdentifier(new JobIdentifier(pipelineName,-1, pipelineLabel, stageName, "1", job.getName()));
        return job;
    }

    private JobInstance passedJob(String pipelineName, String pipelineLabel, String stageName, String jobName) {
        JobInstance job = JobInstanceMother.passed(jobName);
        job.setIdentifier(new JobIdentifier(pipelineName,-1, pipelineLabel, stageName, "1", job.getName()));
        return job;
    }
}