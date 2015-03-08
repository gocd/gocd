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

import java.util.HashMap;
import java.util.Map;
import java.util.regex.*;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.materials.mercurial.HgMaterial;
import com.thoughtworks.go.config.materials.ScmMaterial;
import com.thoughtworks.go.domain.label.PipelineLabel;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.helper.MaterialsMother;
import com.thoughtworks.go.helper.ModificationsMother;
import org.hamcrest.core.Is;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

public class PipelineLabelTest {
    private final String testingTemplate = "testing." + PipelineLabel.COUNT_TEMPLATE + ".label";

    @Test
    public void shouldUseCounterAsDefaultTemplate() throws Exception {
        PipelineLabel defaultLabel = PipelineLabel.defaultLabel();
        assertThat(defaultLabel.toString(), is("${COUNT}"));
    }

    @Test
    public void shouldFormatLabelAccordingToCountingTemplate() throws Exception {
        PipelineLabel label = PipelineLabel.create(testingTemplate);
        label.updateLabel(getNamedRevision(99));
        assertThat(label.toString(), is("testing.99.label"));
    }

    @Test
    public void shouldIgnoreCaseInCountingTemplate() throws Exception {
        PipelineLabel label = PipelineLabel.create(testingTemplate);
        label.updateLabel(getNamedRevision(2));
        assertThat(label.toString(), is("testing.2.label"));
    }

    @Test
    public void shouldReplaceTheTemplateWithMaterialRevision() throws Exception {
        PipelineLabel label = PipelineLabel.create("release-${svnMaterial}");
        MaterialRevisions materialRevisions = ModificationsMother.oneUserOneFile();
        label.updateLabel(materialRevisions.getNamedRevisions());
        assertThat(label.toString(), is("release-" + ModificationsMother.currentRevision()));
    }

    @Test
    public void shouldReplaceTheTemplateCaseInsensitively() throws Exception {
        PipelineLabel label = PipelineLabel.create("release-${SVNMaterial}");
        MaterialRevisions materialRevisions = ModificationsMother.oneUserOneFile();
        label.updateLabel(materialRevisions.getNamedRevisions());
        assertThat(label.toString(), is("release-" + ModificationsMother.currentRevision()));
    }

    @Test
    public void shouldReplaceTheTemplateWithMultipleMaterialRevision() throws Exception {
        PipelineLabel label = PipelineLabel.create("release-${svnMaterial}-${hg}");
        MaterialRevisions materialRevisions = ModificationsMother.oneUserOneFile();
        HgMaterial material = MaterialsMother.hgMaterial();
        material.setName(new CaseInsensitiveString("hg"));
        Modification modification = new Modification();
        modification.setRevision("ae09876hj");

        materialRevisions.addRevision(material, modification);
        label.updateLabel(materialRevisions.getNamedRevisions());
        assertThat(label.toString(), is("release-" + ModificationsMother.currentRevision() + "-ae09876hj"));
    }

    @Test
    public void shouldReplaceTheTemplateWithGitMaterialRevision() throws Exception {
        PipelineLabel label = PipelineLabel.create("release-${svnMaterial}-${git}");
        MaterialRevisions materialRevisions = ModificationsMother.oneUserOneFile();
        ScmMaterial material = MaterialsMother.gitMaterial("");
        material.setName(new CaseInsensitiveString("git"));
        Modification modification = new Modification();
        modification.setRevision("8c8a273e12a45e57fed5ce978d830eb482f6f666");

        materialRevisions.addRevision(material, modification);
        label.updateLabel(materialRevisions.getNamedRevisions());
        assertThat(label.toString(), is("release-" + ModificationsMother.currentRevision() + "-8c8a273e12a45e57fed5ce978d830eb482f6f666"));
    }

    @Test
    public void shouldTruncateMaterialRevision() throws Exception {
        PipelineLabel label = PipelineLabel.create("release-${svnMaterial}-${git[:6]}");
        MaterialRevisions materialRevisions = ModificationsMother.oneUserOneFile();
        ScmMaterial material = MaterialsMother.gitMaterial("");
        material.setName(new CaseInsensitiveString("git"));
        Modification modification = new Modification();
        modification.setRevision("8c8a273e12a45e57fed5ce978d830eb482f6f666");

        materialRevisions.addRevision(material, modification);
        label.updateLabel(materialRevisions.getNamedRevisions());
        assertThat(label.toString(), is("release-" + ModificationsMother.currentRevision() + "-8c8a27"));
    }

    @Test
    public void shouldTrimLongLabelTo255() {
        PipelineLabel label = PipelineLabel.create("Pipeline-${upstream}");
        HashMap<String, String> namedRevisions = new HashMap<String, String>();
        namedRevisions.put("upstream", longLabel(300));

        label.updateLabel(namedRevisions);
        assertThat(label.toString().length(), Is.is(255));
    }

    @Test
    public void shouldKeepLabelIfLessThan255() {
        PipelineLabel label = PipelineLabel.create("${upstream}");
        HashMap<String, String> namedRevisions = new HashMap<String, String>();
        namedRevisions.put("upstream", longLabel(154));

        label.updateLabel(namedRevisions);
        assertThat(label.toString().length(), Is.is(154));
    }

    private String longLabel(int length) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < length; i++) {
            builder.append("a");
        }
        return builder.toString();
    }

    @Test
    public void shouldCreateDefaultLabelIfTemplateIsNull() {
        PipelineLabel label = PipelineLabel.create(null);
        assertThat(label, Is.is(PipelineLabel.defaultLabel()));
    }

    @Test
    public void shouldCreateDefaultLabelIfTemplateIsEmtpty() {
        PipelineLabel label = PipelineLabel.create("");
        assertThat(label, Is.is(PipelineLabel.defaultLabel()));
    }

    @Test
    public void shouldCreateLabelIfTemplateIsProvided() {
        PipelineLabel label = PipelineLabel.create("Pipeline-${ABC}");
        assertThat(label, Is.is(new PipelineLabel("Pipeline-${ABC}")));
    }

    private HashMap<String, String> getNamedRevision(final Integer counter) {
        return new HashMap<String, String>() {
            {
                put("COUNT", counter.toString());
            }
        };
    }


    @Test
    public void shouldNotReplaceTemplateWithoutMaterial() throws Exception {
        final String label = PipelineLabel.replaceRevisionsInLabel("1.5.0", new HashMap<String, String>());
        assertThat(label, is("1.5.0"));
    }

    public static final String SVN_REVISION = "3456";
    public static final String GIT_REVISION = "c42c0bfa57d00a25496ba899b1f476e6ec8872bd";
    public static final int GIT_REV_LENGTH = GIT_REVISION.length();

    private String assertLabelGroupsMatchingAndReplace(String labelTemplate, String[][] expectedGroups) throws Exception {
        assertLabelGroupsMatching(labelTemplate, expectedGroups);

        return PipelineLabel.replaceRevisionsInLabel(labelTemplate, MATERIAL_REVISIONS);
    }

    private void assertLabelGroupsMatching(String labelTemplate, String[][] expectedGroups) {
        java.util.regex.Matcher matcher = PipelineLabel.PATTERN.matcher(labelTemplate);

        for (String[] groups : expectedGroups) {
            assertThat(matcher.find(), is(true));

            final String truncationLengthLiteral = matcher.group(3);
            if (groups.length != 2) {
                assertNull(truncationLengthLiteral);
            } else {
                assertThat(truncationLengthLiteral, is(groups[1]));
            }
        }

        final boolean actual = matcher.find();
        assertThat(actual, is(false));
    }

    public static final Map<String, String> MATERIAL_REVISIONS = new HashMap<String, String>();

    @BeforeClass
    public static void setup() {
        MATERIAL_REVISIONS.put("svnRepo.verynice", SVN_REVISION);
        MATERIAL_REVISIONS.put("svn", SVN_REVISION);
        MATERIAL_REVISIONS.put("git", GIT_REVISION);
    }

    @Test
    public void canMatchMaterialName() throws Exception {
        final String[][] expectedGroups = { { "git" } };
        String res = assertLabelGroupsMatchingAndReplace("release-${git}", expectedGroups);
        assertThat(res, is("release-" + GIT_REVISION));
    }

    @Test
    public void canMatchMaterialNameWithTrial() throws Exception {
        final String[][] expectedGroups = { { "git" } };
        String res = assertLabelGroupsMatchingAndReplace("release-${git}.alpha.0", expectedGroups);
        assertThat(res, is("release-" + GIT_REVISION + ".alpha.0"));
    }

    @Test
    public void canHandleWrongMaterialName() throws Exception {
        final String[][] expectedGroups = { { "gitUnused" } };
        String res = assertLabelGroupsMatchingAndReplace("release-${gitUnused}", expectedGroups);
        assertThat(res, is("release-${gitUnused}"));
    }

    @Test
    public void canMatchWithoutTruncation() throws Exception {
        final String[][] expectedGroups = { { "svnRepo.verynice" }, { "git" } };
        String res = assertLabelGroupsMatchingAndReplace("release-${svnRepo.verynice}-${git}", expectedGroups);
        assertThat(res, is("release-" + SVN_REVISION + "-" + GIT_REVISION));
    }

    @Test
    public void canMatchWithOneGitTruncation() throws Exception {
        final String[][] expectedGroups = { { "git", "7" } };
        String res = assertLabelGroupsMatchingAndReplace("release-${git[:7]}", expectedGroups);
        assertThat(res, is("release-" + GIT_REVISION.substring(0, 7)));
    }

    @Test
    public void canMatchWithOneGitTruncationTooLongToTruncate() throws Exception {
        final String[][] expectedGroups = { { "git", "9999" } };
        String res = assertLabelGroupsMatchingAndReplace("release-${git[:9999]}", expectedGroups);
        assertThat(res, is("release-" + GIT_REVISION));
    }

    @Test
    public void canMatchWithOneGitTruncationAlmostTruncated() throws Exception {
        final String[][] expectedGroups = { { "git", GIT_REV_LENGTH + "" } };
        String res = assertLabelGroupsMatchingAndReplace("release-${git[:" + GIT_REV_LENGTH + "]}", expectedGroups);
        assertThat(res, is("release-" + GIT_REVISION));
    }

    @Test
    public void canMatchWithOneGitTruncationByOneChar() throws Exception {
        final int size = GIT_REV_LENGTH - 1;
        final String[][] expectedGroups = { { "git", size + "" } };
        String res = assertLabelGroupsMatchingAndReplace("release-${git[:" + size + "]}", expectedGroups);
        assertThat(res, is("release-" + GIT_REVISION.substring(0, GIT_REV_LENGTH - 1)));
    }

    @Test
    public void canMatchWithOneTruncationAsFirstRevision() throws Exception {
        final String[][] expectedGroups = { {"git", "4"}, { "svn" } };
        String res = assertLabelGroupsMatchingAndReplace("release-${git[:4]}-${svn}", expectedGroups);
        assertThat(res, is("release-" + GIT_REVISION.substring(0, 4)  + "-" + SVN_REVISION));
    }

    @Test
    public void canMatchWithTwoTruncation() throws Exception {
        final String[][] expectedGroups = { { "git", "5" }, {"svn", "3"}};
        String res = assertLabelGroupsMatchingAndReplace("release-${git[:5]}-${svn[:3]}", expectedGroups);
        assertThat(res, is("release-" + GIT_REVISION.substring(0, 5) + "-" + SVN_REVISION.substring(0, 3)));
    }
}
