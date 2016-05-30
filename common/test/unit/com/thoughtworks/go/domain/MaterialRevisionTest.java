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

package com.thoughtworks.go.domain;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.materials.Filter;
import com.thoughtworks.go.config.materials.IgnoredFiles;
import com.thoughtworks.go.config.materials.ScmMaterialConfig;
import com.thoughtworks.go.config.materials.SubprocessExecutionContext;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterial;
import com.thoughtworks.go.config.materials.mercurial.HgMaterial;
import com.thoughtworks.go.config.materials.svn.SvnMaterial;
import com.thoughtworks.go.domain.materials.*;
import com.thoughtworks.go.domain.materials.dependency.DependencyMaterialRevision;
import com.thoughtworks.go.domain.materials.mercurial.StringRevision;
import com.thoughtworks.go.helper.*;
import com.thoughtworks.go.util.TestFileUtil;
import com.thoughtworks.go.util.command.InMemoryStreamConsumer;
import org.apache.commons.io.FileUtils;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static com.thoughtworks.go.helper.ModificationsMother.createHgMaterialWithMultipleRevisions;
import static com.thoughtworks.go.helper.ModificationsMother.multipleModificationsInHg;
import static com.thoughtworks.go.helper.ModificationsMother.oneModifiedFile;
import static com.thoughtworks.go.util.command.ProcessOutputStreamConsumer.inMemoryConsumer;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

public class MaterialRevisionTest {
    private static final StringRevision REVISION_0 = new StringRevision("b61d12de515d82d3a377ae3aae6e8abe516a2651");
    private static final StringRevision REVISION_2 = new StringRevision("ca3ebb67f527c0ad7ed26b789056823d8b9af23f");
    private HgMaterial hgMaterial;
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();
    private File workingFolder;

    @Before
    public void setUp() throws Exception {
        HgTestRepo hgTestRepo = new HgTestRepo("hgTestRepo1");
        hgMaterial = MaterialsMother.hgMaterial(hgTestRepo.projectRepositoryUrl());
        workingFolder = temporaryFolder.newFolder();
    }

    @After
    public void teardown() {
        temporaryFolder.delete();
        TestRepo.internalTearDown();
    }

    @Test
    public void shouldGetModifiedTimeFromTheLatestModification() throws Exception {
        final MaterialRevision materialRevision = new MaterialRevision(MaterialsMother.hgMaterial(), multipleModificationsInHg());
        assertThat(materialRevision.getDateOfLatestModification(), is(ModificationsMother.TODAY_CHECKIN));
    }

    @Test
    public void shouldDetectChangesAfterACheckin() throws Exception {
        MaterialRevision original = new MaterialRevision(hgMaterial,
                hgMaterial.modificationsSince(workingFolder, REVISION_0, new TestSubprocessExecutionContext()));

        checkInOneFile(hgMaterial);
        checkInOneFile(hgMaterial);
        checkInOneFile(hgMaterial);

        final MaterialRevision after = findNewRevision(original, hgMaterial, workingFolder, new TestSubprocessExecutionContext());

        assertThat(after, not(original));
        assertThat(after.numberOfModifications(), is(3));
        assertThat(after.getRevision(), is(not(original.getRevision())));
        assertThat(after.hasChangedSince(original), is(true));

    }

    @Test
    public void shouldMarkRevisionAsChanged() throws Exception {
        MaterialRevision original = new MaterialRevision(hgMaterial, hgMaterial.modificationsSince(workingFolder, REVISION_0, new TestSubprocessExecutionContext()));

        checkInFiles(hgMaterial, "user.doc");

        MaterialRevision newRev = findNewRevision(original, hgMaterial, workingFolder, new TestSubprocessExecutionContext());
        assertThat(newRev.isChanged(), is(true));
    }

    @Test
    public void shouldMarkRevisionAsNotChanged() throws Exception {
        List<Modification> modifications = hgMaterial.latestModification(workingFolder, new TestSubprocessExecutionContext());
        MaterialRevision original = new MaterialRevision(hgMaterial, modifications);
        checkInFiles(hgMaterial, "user.doc");
        original = findNewRevision(original, hgMaterial, workingFolder, new TestSubprocessExecutionContext());

        MaterialRevision newRev = findNewRevision(original, hgMaterial, workingFolder, new TestSubprocessExecutionContext());
        assertThat(newRev.isChanged(), is(false));
    }

    @Test
    public void shouldIgnoreDocumentCheckin() throws Exception {
        MaterialRevision previousRevision = new MaterialRevision(hgMaterial, hgMaterial.modificationsSince(workingFolder, REVISION_0, new TestSubprocessExecutionContext()));

        Filter filter = new Filter(new IgnoredFiles("**/*.doc"));
        hgMaterial.setFilter(filter);

        checkInFiles(hgMaterial, "user.doc");

        MaterialRevision newRevision = findNewRevision(previousRevision, hgMaterial, workingFolder, new TestSubprocessExecutionContext());
        assertThat(newRevision.filter(previousRevision), is(previousRevision));
    }

    @Test
    public void shouldIgnoreDocumentWhenCheckin() throws Exception {
        MaterialRevision original = new MaterialRevision(hgMaterial, hgMaterial.modificationsSince(workingFolder, REVISION_0, new TestSubprocessExecutionContext()));

        Filter filter = new Filter(new IgnoredFiles("helper/**/*.*"));
        hgMaterial.setFilter(filter);

        checkInFiles(hgMaterial,
                "helper/topics/installing_go_agent.xml",
                "helper/topics/installing_go_server.xml");

        MaterialRevision newRev = findNewRevision(original, hgMaterial, workingFolder, new TestSubprocessExecutionContext());
        assertThat(newRev.filter(original), is(original));
    }

    @Test
    public void shouldIgnoreDocumentsWithSemanticallyEqualsIgnoreFilter() throws Exception {
        MaterialRevision original = new MaterialRevision(hgMaterial, hgMaterial.modificationsSince(workingFolder, REVISION_0, new TestSubprocessExecutionContext()));

        Filter filter = new Filter(new IgnoredFiles("**/*.doc"), new IgnoredFiles("*.doc"));
        hgMaterial.setFilter(filter);

        checkInFiles(hgMaterial, "user.doc");

        MaterialRevision newRev = findNewRevision(original, hgMaterial, workingFolder, new TestSubprocessExecutionContext());
        assertThat(newRev.filter(original), is(original));
    }

    @Test
    public void shouldIncludeJavaFileWithSemanticallyEqualsIgnoreFilter() throws Exception {
        MaterialRevision original = new MaterialRevision(hgMaterial,
                hgMaterial.modificationsSince(workingFolder, REVISION_0, new TestSubprocessExecutionContext()));

        Filter filter = new Filter(new IgnoredFiles("**/*.doc"), new IgnoredFiles("*.doc"));
        GoConfigMother.createPipelineConfig(filter, (ScmMaterialConfig) hgMaterial.config());
        checkInFiles(hgMaterial, "A.java");
        checkInFiles(hgMaterial, "B.doc");
        checkInFiles(hgMaterial, "C.pdf");

        MaterialRevision newRev = findNewRevision(original, hgMaterial, workingFolder, new TestSubprocessExecutionContext());
        assertThat(newRev.filter(original), is(newRev));
    }

    @Test
    public void shouldNotIgnoreJavaFile() throws Exception {
        MaterialRevision original = new MaterialRevision(hgMaterial,
                hgMaterial.modificationsSince(workingFolder, REVISION_0, new TestSubprocessExecutionContext()));

        Filter filter = new Filter(new IgnoredFiles("**/*.doc"));
        GoConfigMother.createPipelineConfig(filter, (ScmMaterialConfig) hgMaterial.config());
        checkInFiles(hgMaterial, "A.java");

        MaterialRevision newRev = findNewRevision(original, hgMaterial, workingFolder, new TestSubprocessExecutionContext());
        assertThat(newRev.filter(original), is(newRev));
    }

    @Test
    public void shouldNotIgnoreAnyFileIfFilterIsNotDefinedForTheGivenMaterial() throws Exception {
        MaterialRevision original = new MaterialRevision(hgMaterial,
                hgMaterial.modificationsSince(workingFolder, REVISION_0, new TestSubprocessExecutionContext()));

        Filter filter = new Filter();
        GoConfigMother.createPipelineConfig(filter, (ScmMaterialConfig) hgMaterial.config());
        checkInFiles(hgMaterial, "A.java");

        MaterialRevision newRev = findNewRevision(original, hgMaterial, workingFolder, new TestSubprocessExecutionContext());
        assertThat(newRev.filter(original), is(newRev));
    }

    @Test
    public void shouldMarkRevisionChangeFalseIfNoNewChangesAvailable() throws Exception {
        Modification modificationForRevisionTip = new Modification(new Date(), REVISION_2.getRevision(), "MOCK_LABEL-12", null);
        MaterialRevision revision = new MaterialRevision(hgMaterial, modificationForRevisionTip);
        MaterialRevision unchangedRevision = findNewRevision(revision, hgMaterial, workingFolder, new TestSubprocessExecutionContext());

        assertThat(unchangedRevision.isChanged(), is(false));
    }

    @Test
    public void shouldReturnOnlyLatestModificationIfNoNewChangesAvailable() throws Exception {
        Modification modificationForRevisionTip = new Modification("Unknown", "Unknown", null, new Date(), REVISION_2.getRevision());
        Modification olderModification = new Modification("Unknown", "Unknown", null, new Date(), REVISION_0.getRevision());
        MaterialRevision revision = new MaterialRevision(hgMaterial, modificationForRevisionTip, olderModification);
        MaterialRevision unchangedRevision = findNewRevision(revision, hgMaterial, workingFolder, new TestSubprocessExecutionContext());

        assertThat(unchangedRevision.getModifications().size(), is(1));
        assertThat(unchangedRevision.getModifications().get(0), is(modificationForRevisionTip));
    }

    @Test
    public void shouldNotConsiderChangedFlagAsPartOfEqualityAndHashCodeCheck() {
        Modification modification = oneModifiedFile("revision1");
        SvnMaterial material = MaterialsMother.svnMaterial();

        MaterialRevision notChanged = new MaterialRevision(material, false, modification);
        MaterialRevision changed = new MaterialRevision(material, true, modification);
        changed.markAsChanged();

        assertThat(changed, is(notChanged));
        assertThat(changed.hashCode(), is(notChanged.hashCode()));
    }

    @Test
    public void shouldDetectChangedRevision() {
        Modification modification1 = oneModifiedFile("revision1");
        Modification modification2 = oneModifiedFile("revision2");
        SvnMaterial material = MaterialsMother.svnMaterial();
        MaterialRevision materialRevision1 = new MaterialRevision(material, modification1);
        MaterialRevision materialRevision2 = new MaterialRevision(material, modification2);
        assertThat(materialRevision1.hasChangedSince(materialRevision2), is(true));
    }

    @Test
    public void shouldDisplayRevisionAsBuildCausedByForDependencyMaterial() {
        DependencyMaterial dependencyMaterial = new DependencyMaterial(new CaseInsensitiveString("upstream"), new CaseInsensitiveString("stage"));
        MaterialRevision materialRevision = new MaterialRevision(dependencyMaterial, new Modification(new Date(), "upstream/2/stage/1", "1.3-2", null));
        assertThat(materialRevision.buildCausedBy(), is("upstream/2/stage/1"));
    }

    @Test
    public void shouldUseLatestMaterial() throws Exception {
        MaterialRevision original = new MaterialRevision(hgMaterial,
                hgMaterial.modificationsSince(workingFolder, REVISION_0, new TestSubprocessExecutionContext()));

        HgMaterial newMaterial = MaterialsMother.hgMaterial(hgMaterial.getUrl());
        newMaterial.setFilter(new Filter(new IgnoredFiles("**/*.txt")));
        final MaterialRevision after = findNewRevision(original, newMaterial, workingFolder, new TestSubprocessExecutionContext());

        assertThat(after.getMaterial(), is((Material) newMaterial));
    }

    @Test
    public void shouldDetectLatestAndOldestModification() throws Exception {
        MaterialRevision materialRevision = new MaterialRevision(hgMaterial, modification("3"), modification("2"), modification("1"));

        assertThat(materialRevision.getLatestModification(), is(modification("3")));
        assertThat(materialRevision.getOldestModification(), is(modification("1")));
    }

    @Test
    public void shouldDetectLatestRevision() throws Exception {
        MaterialRevision materialRevision = new MaterialRevision(hgMaterial, modification("3"), modification("2"), modification("1"));
        assertThat((StringRevision) materialRevision.getRevision(), is(new StringRevision("3")));
    }

    @Test
    public void shouldDetectOldestScmRevision() throws Exception {
        MaterialRevision materialRevision = new MaterialRevision(hgMaterial, modification("3"), modification("2"), modification("1"));
        assertThat((StringRevision) materialRevision.getOldestRevision(), is(new StringRevision("1")));
    }

    @Test
    public void shouldDetectOldestAndLatestDependencyRevision() throws Exception {
        DependencyMaterial dependencyMaterial = new DependencyMaterial(new CaseInsensitiveString("upstream"), new CaseInsensitiveString("stage"));
        MaterialRevision materialRevision = new MaterialRevision(dependencyMaterial, new Modification(new Date(), "upstream/3/stage/1", "1.3-3", null),
                new Modification(new Date(), "upstream/2/stage/1", "1.3-2", null));
        assertThat((DependencyMaterialRevision) materialRevision.getOldestRevision(), is(DependencyMaterialRevision.create("upstream/2/stage/1", "1.3-2")));
        assertThat((DependencyMaterialRevision) materialRevision.getRevision(), is(DependencyMaterialRevision.create("upstream/3/stage/1", "1.3-3")));
    }

    @Test
    public void shouldReturnNullRevisionWhenThereIsNoMaterial() throws Exception {
        Revision revision = new MaterialRevision(null).getRevision();
        assertThat(revision, is(not(nullValue())));
        assertThat(revision.getRevision(), is(""));
    }

    @Test
    public void shouldReturnFullRevisionForTheLatestModification() throws Exception {
        assertThat(hgRevision().getLatestRevisionString(), is("012345678901234567890123456789"));
    }

    private MaterialRevision hgRevision() {
        return new MaterialRevision(hgMaterial, modification("012345678901234567890123456789"), modification("2"), modification("1"));
    }

    @Test
    public void shouldReturnShortRevisionForTheLatestModification() throws Exception {
        assertThat(hgRevision().getLatestShortRevision(), is("012345678901"));
    }

    @Test
    public void shouldReturnMaterialName() throws Exception {
        assertThat(hgRevision().getMaterialName(), Matchers.is(hgMaterial.getDisplayName()));
    }

    @Test
    public void shouldReturnTruncatedMaterialName() throws Exception {
        assertThat(hgRevision().getTruncatedMaterialName(), Matchers.is(hgMaterial.getTruncatedDisplayName()));
    }

    @Test
    public void shouldReturnMaterialType() throws Exception {
        assertThat(hgRevision().getMaterialType(), is("Mercurial"));
    }

    @Test
    public void shouldReturnLatestComments() throws Exception {
        assertThat(hgRevision().getLatestComment(), is("Checkin 012345678901234567890123456789"));
    }

    @Test
    public void shouldReturnLatestUser() throws Exception {
        assertThat(hgRevision().getLatestUser(), is("user"));
    }

    @Test
    public void shouldRecomputeIsChanged() throws Exception {
        MaterialRevision original = createHgMaterialWithMultipleRevisions(1, oneModifiedFile("rev2"), oneModifiedFile("rev1")).getMaterialRevision(0);
        MaterialRevision recomputed = createHgMaterialWithMultipleRevisions(1, oneModifiedFile("rev1")).getMaterialRevision(0);
        MaterialRevision recomputedAnother = createHgMaterialWithMultipleRevisions(1, oneModifiedFile("rev0")).getMaterialRevision(0);

        recomputed.updateRevisionChangedStatus(original);
        recomputedAnother.updateRevisionChangedStatus(original);
        assertThat(recomputed.isChanged(), is(false));
        assertThat(recomputedAnother.isChanged(), is(false));

        original.markAsChanged();
        recomputed.updateRevisionChangedStatus(original);
        recomputedAnother.updateRevisionChangedStatus(original);
        assertThat(recomputed.isChanged(), is(true));
        assertThat(recomputedAnother.isChanged(), is(false));
    }

    @Test
    public void shouldRemoveFromThisWhateverModificationIsPresentInThePassedInRevision() throws Exception {
        MaterialRevision revision = createHgMaterialWithMultipleRevisions(1, oneModifiedFile("rev2"), oneModifiedFile("rev1")).getMaterialRevision(0);
        MaterialRevision passedIn = createHgMaterialWithMultipleRevisions(1, oneModifiedFile("rev1")).getMaterialRevision(0);

        MaterialRevision expected = createHgMaterialWithMultipleRevisions(1, oneModifiedFile("rev2")).getMaterialRevision(0);

        assertThat(revision.subtract(passedIn), is(expected));
    }

    @Test
    public void shouldReturnCurrentIfThePassedInDoesNotHaveAnythingThatCurrentHas() throws Exception {
        MaterialRevision revision = createHgMaterialWithMultipleRevisions(1, oneModifiedFile("rev2")).getMaterialRevision(0);
        MaterialRevision passedIn = createHgMaterialWithMultipleRevisions(1, oneModifiedFile("rev1")).getMaterialRevision(0);

        MaterialRevision expected = createHgMaterialWithMultipleRevisions(1, oneModifiedFile("rev2")).getMaterialRevision(0);
        assertThat(revision.subtract(passedIn), is(expected));
    }

    private Modification modification(String revision) {
        return new Modification("user", "Checkin "
                + revision, null, null, revision);
    }

    private void checkInOneFile(HgMaterial hgMaterial) throws Exception {
        checkInFiles(hgMaterial, UUID.randomUUID().toString());
    }

    private void checkInFiles(HgMaterial hgMaterial, String... fileNames) throws Exception {
        final File localDir = TestFileUtil.createTempFolder("foo");
        InMemoryStreamConsumer consumer = inMemoryConsumer();
        Revision revision = latestRevision(hgMaterial, workingFolder, new TestSubprocessExecutionContext());
        hgMaterial.updateTo(consumer, localDir, new RevisionContext(revision), new TestSubprocessExecutionContext());
        for (String fileName : fileNames) {
            File file = new File(localDir, fileName);
            FileUtils.writeStringToFile(file, "");
            hgMaterial.add(localDir, consumer, file);
        }
        hgMaterial.commit(localDir, consumer, "Adding a new file.", "TEST");
        hgMaterial.push(localDir, consumer);
    }

    private Revision latestRevision(HgMaterial material, File workingDir, TestSubprocessExecutionContext execCtx) {
        List<Modification> modifications = material.latestModification(workingDir, execCtx);
        return new Modifications(modifications).latestRevision(material);
    }

    public MaterialRevision findNewRevision(MaterialRevision materialRevision, HgMaterial material, File workingFolder, final SubprocessExecutionContext execCtx) {
        List<Modification> newModifications = material.modificationsSince(workingFolder, materialRevision.getRevision(), execCtx);
        return materialRevision.latestChanges(material, materialRevision.getModifications(), newModifications);
    }
}
