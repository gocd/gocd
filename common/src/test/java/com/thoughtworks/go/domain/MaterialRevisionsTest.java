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
package com.thoughtworks.go.domain;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.materials.Filter;
import com.thoughtworks.go.config.materials.Materials;
import com.thoughtworks.go.config.materials.svn.SvnMaterial;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.materials.Modifications;
import com.thoughtworks.go.domain.materials.ModifiedAction;
import com.thoughtworks.go.helper.MaterialsMother;
import com.thoughtworks.go.helper.ModificationsMother;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;
import java.util.Map;

import static com.thoughtworks.go.helper.ModificationsMother.*;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

public class MaterialRevisionsTest {

    private Modification yesterdayMod;
    private Modification oneHourAgoMod;
    private Modification nowMod;
    private SvnMaterial material;
    private static final String MOD_USER_COMMITTER = "committer";
    private static final Filter FILTER_DOC_PDF = Filter.create("*.doc", "*.pdf");

    @Before
    public void setUp() {
        nowMod = new Modification("user3", "fixed the build.", null, new DateTime().toDate(), "100");
        nowMod.createModifiedFile("foo.java", ".", ModifiedAction.modified);
        oneHourAgoMod = new Modification("user2", "fixed the build.", null, new DateTime().minusHours(1).toDate(), "89");
        oneHourAgoMod.createModifiedFile("foo.java", ".", ModifiedAction.modified);
        yesterdayMod = new Modification("user1", "fixed the build.", null, new DateTime().minusDays(1).toDate(), "9");
        yesterdayMod.createModifiedFile("foo.java", ".", ModifiedAction.modified);

        material = MaterialsMother.svnMaterial("foo");
        material.setName(new CaseInsensitiveString("Foo"));
    }

    @Test
    public void shouldReturnModificationsForAGivenMaterial() {
        MaterialRevisions oneMaterialRevision = new MaterialRevisions(new MaterialRevision(material, yesterdayMod));
        assertThat(oneMaterialRevision.getModifications(material), is(new Modifications(yesterdayMod)));

        MaterialRevisions emptyMaterialRevision = new MaterialRevisions();
        assertThat(emptyMaterialRevision.getModifications(material).isEmpty(), is(true));

        MaterialRevisions differentMaterialRevision = new MaterialRevisions(new MaterialRevision(MaterialsMother.hgMaterial(), yesterdayMod));
        assertThat(differentMaterialRevision.getModifications(material).isEmpty(), is(true));
    }

    @Test
    public void shouldKnowDateOfLatestModification() {
        //modifications are ordered, the first modification is the latest one
        MaterialRevisions materialRevisions = new MaterialRevisions(
                svnMaterialRevision(yesterdayMod, oneHourAgoMod)
        );
        assertThat(materialRevisions.getDateOfLatestModification(), is(yesterdayMod.getModifiedTime()));
    }

    @Test
    public void shouldReturnMostRecentModifiedTimeIfExist() {
        MaterialRevisions materialRevisions = multipleModifications();
        assertThat(materialRevisions.getDateOfLatestModification(), is(ModificationsMother.TODAY_CHECKIN));
    }

    @Test
    public void shouldReturnNullIfMostRecentModifiedTimeNotExist() {
        MaterialRevisions materialRevisions = empty();
        assertThat(materialRevisions.getDateOfLatestModification(), is(nullValue()));
    }

    @Test
    public void shouldReturnOrginalChangeSet() {
        MaterialRevisions first = new MaterialRevisions(
                svnMaterialRevision("folder1", FILTER_DOC_PDF, aCheckIn("99", "/a.java")),
                svnMaterialRevision("folder2", FILTER_DOC_PDF, aCheckIn("99", "/b.java"))
        );
        MaterialRevisions second = new MaterialRevisions(
                svnMaterialRevision("folder1", FILTER_DOC_PDF, aCheckIn("100", "/a.doc"), aCheckIn("100", "/b.doc")),
                svnMaterialRevision("folder2", FILTER_DOC_PDF, aCheckIn("100", "/a.pdf"))
        );
        assertThat(second.hasChangedSince(first), is(false));
    }

    @Test
    public void shouldReturnLatestChangeSetForDependencyMaterial() {
        Materials materials = new Materials(MaterialsMother.dependencyMaterial());
        MaterialRevisions original = modifyOneFile(materials, "1");
        MaterialRevisions newRevisions = modifyOneFile(materials, "2");

        assertThat(newRevisions.hasChangedSince(original), is(true));
    }

    @Test
    // #3122
    public void shouldNotIgnoreChangesInUpstreamPipeline() {
        MaterialRevisions original = new MaterialRevisions(dependencyMaterialRevision("cruise", 365, "1.3-365", "dist-zip", 1, new Date()));
        MaterialRevisions current = new MaterialRevisions(dependencyMaterialRevision("cruise", 370, "1.3-370", "dist-zip", 1, new Date()));

        assertThat(current.hasChangedSince(original), is(true));
    }

    @Test
    public void shouldReturnLatestChangeSetIfAnyChangeSetShouldNotIgnore() {
        MaterialRevisions first = new MaterialRevisions(
                svnMaterialRevision("folder1", FILTER_DOC_PDF, aCheckIn("99", "/b.java")),
                svnMaterialRevision("folder2", FILTER_DOC_PDF, aCheckIn("5", "/a.html"))
        );
        MaterialRevisions second = new MaterialRevisions(
                svnMaterialRevision("folder1", FILTER_DOC_PDF, aCheckIn("100", "/a.doc"), aCheckIn("100", "/b.doc")),
                svnMaterialRevision("folder2", FILTER_DOC_PDF, aCheckIn("5", "/a.html"))
        );
        assertThat(second.hasChangedSince(first), is(false));
    }

    @Test
    public void shouldBeAbleToDetectWhenMaterialRevisionsHaveChanged() {
        MaterialRevisions first = new MaterialRevisions(
                svnMaterialRevision(oneHourAgoMod, yesterdayMod)
        );
        MaterialRevisions second = new MaterialRevisions(
                svnMaterialRevision(nowMod)
        );

        assertThat(second.hasChangedSince(first), is(true));
    }

    @Test
    public void shouldBeAbleToDetectWhenMaterialsDontMatch() {
        MaterialRevisions first = new MaterialRevisions(
                svnMaterialRevision("1", FILTER_DOC_PDF, oneHourAgoMod, yesterdayMod),
                svnMaterialRevision("2", FILTER_DOC_PDF, nowMod)
        );
        MaterialRevisions second = new MaterialRevisions(
                svnMaterialRevision("1", FILTER_DOC_PDF, oneHourAgoMod, yesterdayMod),
                svnMaterialRevision("3", FILTER_DOC_PDF, nowMod)
        );

        assertThat(second.hasChangedSince(first), is(true));
    }

    @Test
    public void shouldBeAbleToDetectWhenThereAreMultipleMaterials() {
        MaterialRevisions first = new MaterialRevisions(
                svnMaterialRevision(oneHourAgoMod, yesterdayMod)
        );

        MaterialRevisions second = new MaterialRevisions(
                svnMaterialRevision(oneHourAgoMod, yesterdayMod),
                svnMaterialRevision(nowMod)
        );

        assertThat(second.hasChangedSince(first), is(true));
    }

    @Test
    public void shouldBeAbleToDetectWhenThereAreMultipleMaterialsInADifferentOrder() {
        MaterialRevisions first = new MaterialRevisions(
                svnMaterialRevision("1", FILTER_DOC_PDF, oneHourAgoMod, yesterdayMod),
                svnMaterialRevision("2", FILTER_DOC_PDF, nowMod)
        );
        MaterialRevisions second = new MaterialRevisions(
                svnMaterialRevision("2", FILTER_DOC_PDF, nowMod),
                svnMaterialRevision("1", FILTER_DOC_PDF, oneHourAgoMod, yesterdayMod)
        );
        assertThat(second.hasChangedSince(first), is(false));
    }

    @Test
    public void shouldNotBeAbleToAddANullModification() {
        MaterialRevisions materialRevisions = empty();
        try {
            materialRevisions.addRevision(MaterialsMother.svnMaterial(), (Modification) null);
            Assert.fail("Should not be able to add a null modification");
        } catch (Exception ignored) {
        }
    }

    @Test
    public void shouldMatchByComment() {
        MaterialRevisions materialRevisions = new MaterialRevisions(
                svnMaterialRevision(aCheckIn("100", "README"))
        );
        assertThat(materialRevisions.containsMyCheckin(new Matcher("readme")), is(false));
    }

    @Test
    public void shouldMatchCommentWhenCaseAreSame() {
        MaterialRevisions materialRevisions = new MaterialRevisions(
                svnMaterialRevision(aCheckIn("100", "README"))
        );
        assertThat(materialRevisions.containsMyCheckin(new Matcher("README")), is(true));
    }

    @Test
    public void shouldMatchMultiLineComment() {
        MaterialRevisions materialRevisions = new MaterialRevisions(
            svnMaterialRevision(checkinWithComment("100", "Line1\nLine2\nLine3", "Committer1", EMAIL_ADDRESS, TODAY_CHECKIN, "README"))
        );
        assertThat(materialRevisions.containsMyCheckin(new Matcher("Committer1")), is(true));
        assertThat(materialRevisions.containsMyCheckin(new Matcher("Line1")), is(true));
    }

    @Test
    public void shouldMatchByUserName() {
        MaterialRevisions materialRevisions = new MaterialRevisions(
                svnMaterialRevision(aCheckIn("100", "README"))
        );
        assertThat(materialRevisions.containsMyCheckin(new Matcher(MOD_USER_COMMITTER)), is(true));
    }

    @Test
    public void shouldNotMatchWhenUserNameDoesNotMatch() {
        MaterialRevisions materialRevisions = new MaterialRevisions(
                svnMaterialRevision(aCheckIn("100", "README"))
        );
        assertThat(materialRevisions.containsMyCheckin(new Matcher("someone")), is(false));
    }

    @Test
    public void shouldNotMatchWhenMatcherIsNull() throws Exception {
        MaterialRevisions materialRevisions = new MaterialRevisions(
                svnMaterialRevision(aCheckIn("100", "README"))
        );
        assertThat(materialRevisions.containsMyCheckin(new Matcher((String) null)), is(false));
    }

    @Test
    public void shouldReturnTrueIfOneUsernameMatchesInMutipleModifications() throws Exception {
        MaterialRevisions materialRevisions = multipleModifications();
        assertThat(materialRevisions.containsMyCheckin(new Matcher(MOD_USER_COMMITTER)), is(true));
    }

    @Test
    public void shouldReturnTrueIfUsernameMatchesInOneOfMaterialRevisions() throws Exception {
        MaterialRevisions materialRevisions = new MaterialRevisions(
                svnMaterialRevision(aCheckIn("100", "test.txt")),
                svnMaterialRevision(oneModifiedFile("revision"))
        );
        assertThat(materialRevisions.containsMyCheckin(new Matcher("Fixing")), is(true));
    }

    @Test
    public void shouldReturnFalseWhenMatcherIsNotAlphaNumberic() throws Exception {
        MaterialRevisions materialRevisions = multipleModifications();
        assertThat(materialRevisions.containsMyCheckin(new Matcher("committer.*")), is(false));
    }

    @Test
    public void shouldNotMatchMaterialRevisionsWhenEmailMeIsDisabled() throws Exception {
        MaterialRevisions materialRevisions = multipleModifications();
        assertThat(materialRevisions.containsMyCheckin(new Matcher(MOD_USER_COMMITTER)), is(true));
    }

    @Test
    public void shouldBeSameIfHasSameHeads() {
        MaterialRevisions first = new MaterialRevisions(
                svnMaterialRevision(oneModifiedFile("revision2"), oneModifiedFile("revision1"))
        );
        MaterialRevisions second = new MaterialRevisions(
                svnMaterialRevision(oneModifiedFile("revision2"))
        );
        assertThat(first.isSameAs(second), is(true));
    }

    @Test
    public void shouldBeTheSameIfCurrentHasNoChanges() {
        MaterialRevisions first = new MaterialRevisions(
                svnMaterialRevision(oneModifiedFile("svn revision 2")),
                hgMaterialRevision(oneModifiedFile("hg revision 1"))
        );
        MaterialRevisions second = new MaterialRevisions(
                svnMaterialRevision(false, oneModifiedFile("svn revision 2")),
                hgMaterialRevision(false, oneModifiedFile("hg revision 1"))
        );

        assertThat(first.isSameAs(second), is(true));
    }

    @Test public void shouldNotBeSameIfOneMaterialRevisionIsNew() throws Exception {
        MaterialRevisions first = new MaterialRevisions(
                svnMaterialRevision(oneModifiedFile("svn revision 2"), oneModifiedFile("svn revision 1")),
                hgMaterialRevision(oneModifiedFile("hg revision 1"))
        );
        MaterialRevisions second = new MaterialRevisions(
                svnMaterialRevision(oneModifiedFile("svn revision 2")),
                hgMaterialRevision(oneModifiedFile("hg revision 2"))
        );
        assertThat(first.isSameAs(second), is(false));
    }

    @Test public void shouldNotBeSameIfOneMaterialRevisionIsNewAndOldOneHadOnlyOneRevision() throws Exception {
        MaterialRevisions first = new MaterialRevisions(
                svnMaterialRevision(oneModifiedFile("svn revision 2")),
                hgMaterialRevision(oneModifiedFile("hg revision 1"))
        );
        MaterialRevisions second = new MaterialRevisions(
                svnMaterialRevision(oneModifiedFile("svn revision 2")),
                hgMaterialRevision(oneModifiedFile("hg revision 2"))
        );
        assertThat(first.isSameAs(second), is(false));
    }

    @Test
    public void shouldNotBeSameOnNewModification() {
        MaterialRevisions first = new MaterialRevisions(
                svnMaterialRevision(oneModifiedFile("revision1"))
        );
        MaterialRevisions second = new MaterialRevisions(
                svnMaterialRevision(oneModifiedFile("revision2"))
        );
        assertThat(first.isSameAs(second), is(false));
    }

    @Test
    public void shouldNotBeSameIfMaterialsAreDifferent() {
        MaterialRevisions first = new MaterialRevisions(
                svnMaterialRevision("folder1", FILTER_DOC_PDF, oneModifiedFile("revision1"))
        );
        MaterialRevisions second = new MaterialRevisions(
                svnMaterialRevision("folder2", FILTER_DOC_PDF, oneModifiedFile("revision1"))
        );
        assertThat(first.isSameAs(second), is(false));
    }

    @Test
    public void shouldUseFirstMaterialAsBuildCauseMessage() throws Exception {
        MaterialRevisions materialRevisions = madeChanges(new boolean[] {true, true},
                oneModifiedFile("user1", "svnRev", TWO_DAYS_AGO_CHECKIN),
                oneModifiedFile("user2", "hgRev", new Date()));

        assertThat(materialRevisions.buildCauseMessage(), is("modified by user1"));
    }

    @Test
    public void shouldUseFirstChangedMaterialAsBuildCauseMessage() throws Exception {
        MaterialRevisions materialRevisions = madeChanges(new boolean[] {false, true},
                oneModifiedFile("user1", "svnRev", TWO_DAYS_AGO_CHECKIN),
                oneModifiedFile("user2", "hgRev", new Date()));

        assertThat(materialRevisions.buildCauseMessage(), is("modified by user2"));
    }

    @Test
    public void shouldUseFirstMaterialAsbuildCausedBy() throws Exception {
        MaterialRevisions materialRevisions = madeChanges(new boolean[] {true, true},
                oneModifiedFile("user1", "svnRev", TWO_DAYS_AGO_CHECKIN),
                oneModifiedFile("user2", "hgRev", new Date()));

        assertThat(materialRevisions.buildCausedBy(), is("user1"));
    }


    @Test
    public void shouldUseFirstChangedMaterialAsbuildCausedBy() throws Exception {
        MaterialRevisions materialRevisions = madeChanges(new boolean[] {false, true},
                oneModifiedFile("user1", "svnRev", TWO_DAYS_AGO_CHECKIN),
                oneModifiedFile("user2", "hgRev", new Date()));

        assertThat(materialRevisions.buildCausedBy(), is("user2"));
    }

    @Test
    public void shouldUseFirstMaterialAsbuildDate() throws Exception {
        MaterialRevisions materialRevisions = madeChanges(new boolean[] {true, true},
                oneModifiedFile("user1", "svnRev", TWO_DAYS_AGO_CHECKIN),
                oneModifiedFile("user2", "hgRev", new Date()));

        assertThat(materialRevisions.getDateOfLatestModification(), is(TWO_DAYS_AGO_CHECKIN));
    }


    @Test
    public void shouldUseFirstChangedMaterialAsDate() throws Exception {
        Date now = new Date();
        MaterialRevisions materialRevisions = madeChanges(new boolean[] {false, true},
                oneModifiedFile("user1", "svnRev", TWO_DAYS_AGO_CHECKIN),
                oneModifiedFile("user2", "hgRev", now));

        assertThat(materialRevisions.getDateOfLatestModification(), is(now));
    }

    @Test
    public void shouldUseFirstMaterialAsLatestRevision() throws Exception {
        MaterialRevisions materialRevisions = madeChanges(new boolean[] {true, true},
                oneModifiedFile("user1", "Rev.1", TWO_DAYS_AGO_CHECKIN),
                oneModifiedFile("user2", "Rev.2", new Date()));

        assertThat(materialRevisions.latestRevision(), is("Rev.1"));
    }

    @Test
    public void shouldUseFirstChangedMaterialAsLatestRevision() throws Exception {
        Date now = new Date();
        MaterialRevisions materialRevisions = madeChanges(new boolean[] {false, true},
                oneModifiedFile("user1", "Rev.1", TWO_DAYS_AGO_CHECKIN),
                oneModifiedFile("user2", "Rev.2", now));

        assertThat(materialRevisions.latestRevision(), is("Rev.2"));
    }

    @Test
    public void shouldReturnFirstLatestRevisionIfNoChanged() throws Exception {
        MaterialRevisions materialRevisions = new MaterialRevisions(
                svnMaterialRevision(yesterdayMod, oneHourAgoMod),
                svnMaterialRevision(nowMod)
        );

        assertThat(materialRevisions.latestRevision(), is(yesterdayMod.getRevision()));
    }

    @Test
    public void shouldReturnMapKeyedByGivenMaterialName() {
        MaterialRevisions materialRevisions = new MaterialRevisions(
                svnMaterialRevision(yesterdayMod, oneHourAgoMod)
        );
        assertThat(materialRevisions.getNamedRevisions().size(), is(1));
        assertThat(materialRevisions.getNamedRevisions().get(new CaseInsensitiveString("Foo")), is("9"));
    }

    @Test
    public void shouldNotAddMaterialWithEmptyNameIntoNamedRevisions() {
        MaterialRevisions materialRevisions = new MaterialRevisions(
                hgMaterialRevision(oneModifiedFile("hg revision 1"))
        );
        assertThat(materialRevisions.getNamedRevisions().size(), is(0));
    }

    @Test
    public void shouldFindDependencyMaterialRevisionByPipelineName() {
        MaterialRevision revision1 = dependencyMaterialRevision("cruise", 365, "1.3-365", "dist-zip", 1, new Date());
        MaterialRevision revision2 = dependencyMaterialRevision("mingle", 370, "2.3-370", "dist-zip", 1, new Date());
        MaterialRevisions materialRevisions = new MaterialRevisions(revision1, revision2);

        assertThat(materialRevisions.findDependencyMaterialRevision("cruise"), is(revision1.getRevision()));
    }

    @Test
    public void shouldFindDependencyMaterialRevisionByPipelineNameWhenCaseDoNotMatch() {
        MaterialRevision revision1 = dependencyMaterialRevision("cruise", 365, "1.3-365", "dist-zip", 1, new Date());
        MaterialRevision revision2 = dependencyMaterialRevision("mingle", 370, "2.3-370", "dist-zip", 1, new Date());
        MaterialRevisions materialRevisions = new MaterialRevisions(revision1, revision2);

        assertThat(materialRevisions.findDependencyMaterialRevision("cruise"), is(revision1.getRevision()));
        assertThat(materialRevisions.findDependencyMaterialRevision("Cruise"), is(revision1.getRevision()));
        assertThat(materialRevisions.findDependencyMaterialRevision("CRUISE"), is(revision1.getRevision()));
    }

    @Test
    public void shouldReturnTrueForMissingModificationsForEmptyList() {
        assertThat(new MaterialRevisions().isMissingModifications(),is(true));
    }

    @Test
    public void shouldUseUpstreamPipelineLabelForDependencyMaterial() {
        CaseInsensitiveString pipelineName = new CaseInsensitiveString("upstream");
        String pipelineLabel = "1.3.0-1234";
        MaterialRevision materialRevision = ModificationsMother.dependencyMaterialRevision(pipelineName.toString(), 2, pipelineLabel, "dev", 1, new Date());
        MaterialRevisions materialRevisions = new MaterialRevisions(materialRevision);

        Map<CaseInsensitiveString, String> namedRevisions = materialRevisions.getNamedRevisions();

        assertThat(namedRevisions.get(pipelineName), is(pipelineLabel));
    }

    @Test
    public void shouldPopulateEnvironmentVariablesForEachOfTheMaterialsWithTheValueForChangedFlag(){
        EnvironmentVariableContext context = new EnvironmentVariableContext();
        MaterialRevisions revisions = new MaterialRevisions();
        revisions.addRevision(new MaterialRevision(MaterialsMother.hgMaterial("empty-dest-and-no-name", null), true, ModificationsMother.multipleModificationList()));
        revisions.addRevision(new MaterialRevision(MaterialsMother.hgMaterial("url", "hg-folder1"), true, ModificationsMother.multipleModificationList()));
        revisions.addRevision(new MaterialRevision(MaterialsMother.hgMaterial("url", "hg-folder2"), false, ModificationsMother.multipleModificationList()));
        revisions.addRevision(new MaterialRevision(MaterialsMother.dependencyMaterial("p1", "s1"), true, ModificationsMother.changedDependencyMaterialRevision("p3", 1, "1", "s", 1, new Date()).getModifications()));
        revisions.addRevision(new MaterialRevision(MaterialsMother.dependencyMaterial("p2", "s2"), false, ModificationsMother.changedDependencyMaterialRevision("p3", 1, "1", "s", 1, new Date()).getModifications()));
        revisions.addRevision(new MaterialRevision(MaterialsMother.pluggableSCMMaterial("scm1", "scm1name"), true, ModificationsMother.multipleModificationList()));
        revisions.addRevision(new MaterialRevision(MaterialsMother.pluggableSCMMaterial("scm2", "scm2name"), false, ModificationsMother.multipleModificationList()));
        revisions.addRevision(new MaterialRevision(MaterialsMother.packageMaterial("repo1", "repo1name", "pkg1", "pkg1name"), true, ModificationsMother.multipleModificationList()));
        revisions.addRevision(new MaterialRevision(MaterialsMother.packageMaterial("repo2", "repo2name", "pkg2", "pkg2name"), false, ModificationsMother.multipleModificationList()));

        revisions.populateEnvironmentVariables(context, null);

        assertThat(context.getProperty("GO_MATERIAL_HAS_CHANGED"), is("true"));
        assertThat(context.getProperty("GO_MATERIAL_HG_FOLDER1_HAS_CHANGED"), is("true"));
        assertThat(context.getProperty("GO_MATERIAL_HG_FOLDER2_HAS_CHANGED"), is("false"));
        assertThat(context.getProperty("GO_MATERIAL_P1_HAS_CHANGED"), is("true"));
        assertThat(context.getProperty("GO_MATERIAL_P2_HAS_CHANGED"), is("false"));
        assertThat(context.getProperty("GO_MATERIAL_SCM1NAME_HAS_CHANGED"), is("true"));
        assertThat(context.getProperty("GO_MATERIAL_SCM2NAME_HAS_CHANGED"), is("false"));
        assertThat(context.getProperty("GO_MATERIAL_REPO1NAME_PKG1NAME_HAS_CHANGED"), is("true"));
        assertThat(context.getProperty("GO_MATERIAL_REPO2NAME_PKG2NAME_HAS_CHANGED"), is("false"));
    }

    private MaterialRevisions madeChanges(boolean[] changed, Modification... modifications) {
        MaterialRevisions materialRevisions = new MaterialRevisions();
        for (int i = 0; i < modifications.length; i++) {
            Modification modification = modifications[i];
            materialRevisions.addRevision(new MaterialRevision(MaterialsMother.svnMaterial(), changed[i], modification));
        }
        return materialRevisions;
    }

    private MaterialRevision hgMaterialRevision(Modification... modifications) {
        return new MaterialRevision(MaterialsMother.hgMaterial(), modifications);
    }

    private MaterialRevision hgMaterialRevision(boolean changed, Modification... modifications) {
        return new MaterialRevision(MaterialsMother.hgMaterial(), changed, modifications);
    }

    private MaterialRevision svnMaterialRevision(Modification... modifications) {
        return new MaterialRevision(material, modifications);
    }

    private MaterialRevision svnMaterialRevision(boolean changed, Modification... modifications) {
        return new MaterialRevision(material, changed, modifications);
    }

    private MaterialRevision svnMaterialRevision(String folder, Filter filter, Modification... modifications) {
        return new MaterialRevision(svnMaterial(folder, filter), modifications);
    }

    private SvnMaterial svnMaterial(String folder, Filter filter) {
        SvnMaterial material = MaterialsMother.svnMaterial();
        material.setFolder(folder);
        material.setFilter(filter);
        return material;
    }


}
