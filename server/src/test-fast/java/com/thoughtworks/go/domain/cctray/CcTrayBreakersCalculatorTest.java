/*
 * Copyright 2021 ThoughtWorks, Inc.
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
package com.thoughtworks.go.domain.cctray;

import com.thoughtworks.go.domain.MaterialRevision;
import com.thoughtworks.go.domain.MaterialRevisions;
import com.thoughtworks.go.domain.Stage;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.helper.MaterialsMother;
import com.thoughtworks.go.helper.ModificationsMother;
import com.thoughtworks.go.helper.StageMother;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Date;
import java.util.Set;

import static com.thoughtworks.go.util.DataStructureUtils.s;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CcTrayBreakersCalculatorTest {
    @Mock
    private MaterialRepository materialRepo;

    @Test
    public void shouldCaptureUniqueModificationAuthorNamesAsBreakers_inCaseOfFailure() throws Exception {
        Modification user1Commit = ModificationsMother.checkinWithComment("123", "comment 1", "user1", "user1@domain1.com", new Date(), "foo.c");
        Modification user2Commit = ModificationsMother.checkinWithComment("124", "comment 2", "user2", "user2@domain2.com", new Date(), "bar.c");
        Modification otherCommitOfUser1 = ModificationsMother.checkinWithComment("125", "comment 3", "user1", "user1@different-email.com", new Date(), "baz.c");

        MaterialRevision revision = new MaterialRevision(MaterialsMother.gitMaterial("foo.com"), user1Commit, user2Commit, otherCommitOfUser1);
        revision.markAsChanged();
        when(materialRepo.findMaterialRevisionsForPipeline(12l)).thenReturn(new MaterialRevisions(revision));


        CcTrayBreakersCalculator status = new CcTrayBreakersCalculator(materialRepo);
        Set<String> actualBreakers = status.calculateFor(failedStage());


        assertThat(actualBreakers, is(s("user1", "user2")));
    }

    @Test
    public void shouldCaptureAuthorNamesOfChangedRevisionsOnlyAsBreakers() throws Exception {
        Modification user1Commit = ModificationsMother.checkinWithComment("123", "comment 1", "user1", "user1@domain1.com", new Date(), "foo.c");
        Modification user2Commit = ModificationsMother.checkinWithComment("124", "comment 2", "user2", "user2@domain2.com", new Date(), "bar.c");
        MaterialRevision changedRevision = new MaterialRevision(MaterialsMother.gitMaterial("foo.com"), user1Commit, user2Commit);
        changedRevision.markAsChanged();

        Modification user3CommitForUnchangedRevision = ModificationsMother.checkinWithComment("125", "comment 1", "user3", "user3@domain2.com", new Date(), "bar.c");
        MaterialRevision unchangedRevision = new MaterialRevision(MaterialsMother.gitMaterial("bar.com"), user3CommitForUnchangedRevision);

        MaterialRevisions revisions = new MaterialRevisions(changedRevision, unchangedRevision);
        when(materialRepo.findMaterialRevisionsForPipeline(12l)).thenReturn(revisions);


        CcTrayBreakersCalculator status = new CcTrayBreakersCalculator(materialRepo);
        Set<String> actualBreakers = status.calculateFor(failedStage());


        assertThat(actualBreakers, is(s("user1", "user2")));
    }

    @Test
    public void shouldCaptureAuthorNamesOfUnchangedRevisionsIfThereAreNoChangedRevisions() throws Exception {
        Modification user1Commit = ModificationsMother.checkinWithComment("123", "comment 1", "user1", "user1@domain1.com", new Date(), "foo.c");
        Modification user2Commit = ModificationsMother.checkinWithComment("124", "comment 2", "user2", "user2@domain2.com", new Date(), "bar.c");
        MaterialRevision firstUnchangedRevision = new MaterialRevision(MaterialsMother.gitMaterial("foo.com"), user1Commit, user2Commit);

        Modification user3Commit = ModificationsMother.checkinWithComment("125", "comment 1", "user3", "user3@domain2.com", new Date(), "bar.c");
        MaterialRevision secondUnchangedRevision = new MaterialRevision(MaterialsMother.gitMaterial("bar.com"), user3Commit);

        MaterialRevisions revisions = new MaterialRevisions(firstUnchangedRevision, secondUnchangedRevision);
        when(materialRepo.findMaterialRevisionsForPipeline(12l)).thenReturn(revisions);


        CcTrayBreakersCalculator status = new CcTrayBreakersCalculator(materialRepo);
        Set<String> actualBreakers = status.calculateFor(failedStage());


        assertThat(actualBreakers, is(s("user1", "user2", "user3")));
    }

    @Test
    public void shouldNotCaptureAuthorNamesForDependencyMaterial() throws Exception {
        Modification user1Commit = ModificationsMother.checkinWithComment("123", "comment 1", "user1", "user1@domain1.com", new Date(), "foo.c");

        MaterialRevision changedRevision = new MaterialRevision(MaterialsMother.gitMaterial("foo.com"), user1Commit);
        changedRevision.markAsChanged();

        MaterialRevision depMaterialRevision = ModificationsMother.dependencyMaterialRevision("dep-pipe", 1, "pipe-1", "dep-stage", 1, new Date());
        depMaterialRevision.markAsChanged();

        MaterialRevisions revisions = new MaterialRevisions(changedRevision, depMaterialRevision);
        when(materialRepo.findMaterialRevisionsForPipeline(12l)).thenReturn(revisions);


        CcTrayBreakersCalculator status = new CcTrayBreakersCalculator(materialRepo);
        Set<String> actualBreakers = status.calculateFor(failedStage());


        assertThat(actualBreakers, is(s("user1")));
    }

    @Test
    public void shouldNotHaveAnyBreakersIfStageHasNotFailed() throws Exception {
        Modification user1Commit = ModificationsMother.checkinWithComment("123", "comment 1", "user1", "user1@domain1.com", new Date(), "foo.c");
        MaterialRevision revision = new MaterialRevision(MaterialsMother.gitMaterial("foo.com"), user1Commit);
        revision.markAsChanged();

        CcTrayBreakersCalculator status = new CcTrayBreakersCalculator(materialRepo);
        Set<String> actualBreakers = status.calculateFor(StageMother.createPassedStage("pipeline1", 1, "stage1", 1, "job1", new Date()));


        assertThat(actualBreakers, is(Collections.<String>emptySet()));
    }

    private Stage failedStage() {
        Stage stage = StageMother.completedFailedStageInstance("pipeline1", "stage1", "job1");
        stage.setPipelineId(12l);
        return stage;
    }
}
