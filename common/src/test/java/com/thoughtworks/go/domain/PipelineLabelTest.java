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
import com.thoughtworks.go.config.materials.ScmMaterial;
import com.thoughtworks.go.config.materials.mercurial.HgMaterial;
import com.thoughtworks.go.config.materials.svn.SvnMaterial;
import com.thoughtworks.go.domain.label.PipelineLabel;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.helper.MaterialsMother;
import com.thoughtworks.go.helper.ModificationsMother;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.thoughtworks.go.domain.InsecureEnvironmentVariables.EMPTY_ENV_VARS;
import static com.thoughtworks.go.domain.label.PipelineLabel.defaultLabel;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

public class PipelineLabelTest {
    private final String testingTemplate = "testing." + PipelineLabel.COUNT_TEMPLATE + ".label";

    @Test
    public void shouldUseCounterAsDefaultTemplate() {
        PipelineLabel defaultLabel = PipelineLabel.defaultLabel();
        assertThat(defaultLabel.toString(), is("${COUNT}"));
    }

    @Test
    public void shouldFormatLabelAccordingToCountingTemplate() {
        PipelineLabel label = PipelineLabel.create(testingTemplate, InsecureEnvironmentVariables.EMPTY_ENV_VARS);
        label.updateLabel(Collections.emptyMap(), 99);
        assertThat(label.toString(), is("testing.99.label"));
    }

    @Test
    public void shouldIgnoreCaseInCountingTemplate() {
        PipelineLabel label = PipelineLabel.create(testingTemplate, InsecureEnvironmentVariables.EMPTY_ENV_VARS);
        label.updateLabel(Collections.emptyMap(), 2);
        assertThat(label.toString(), is("testing.2.label"));
    }

    @Test
    public void shouldReplaceTheTemplateWithMaterialRevision() {
        PipelineLabel label = PipelineLabel.create("release-${svnMaterial}", InsecureEnvironmentVariables.EMPTY_ENV_VARS);
        MaterialRevisions materialRevisions = ModificationsMother.oneUserOneFile();
        label.updateLabel(materialRevisions.getNamedRevisions(), 1);
        assertThat(label.toString(), is("release-" + ModificationsMother.currentRevision()));
    }

    @Test
    public void shouldReplaceTheTemplateCaseInsensitively() {
        EnvironmentVariables envVars = new EnvironmentVariables();
        envVars.add("VAR", "var_value");
        PipelineLabel label = PipelineLabel.create("release-${SVNMaterial}-${EnV:Var}", envVars);
        MaterialRevisions materialRevisions = ModificationsMother.oneUserOneFile();
        label.updateLabel(materialRevisions.getNamedRevisions(), 1);
        assertThat(label.toString(), is("release-" + ModificationsMother.currentRevision() + "-var_value"));
    }

    @Test
    public void shouldReplaceTheTemplateWithMultipleMaterialRevision() {
        PipelineLabel label = PipelineLabel.create("release-${svnMaterial}-${hg}", InsecureEnvironmentVariables.EMPTY_ENV_VARS);
        MaterialRevisions materialRevisions = ModificationsMother.oneUserOneFile();
        HgMaterial material = MaterialsMother.hgMaterial();
        material.setName(new CaseInsensitiveString("hg"));
        Modification modification = new Modification();
        modification.setRevision("ae09876hj");

        materialRevisions.addRevision(material, modification);
        label.updateLabel(materialRevisions.getNamedRevisions(), 1);
        assertThat(label.toString(), is("release-" + ModificationsMother.currentRevision() + "-ae09876hj"));
    }

    @Test
    public void shouldReplaceTheTemplateWithGitMaterialRevision() {
        PipelineLabel label = PipelineLabel.create("release-${svnMaterial}-${git}", InsecureEnvironmentVariables.EMPTY_ENV_VARS);
        MaterialRevisions materialRevisions = ModificationsMother.oneUserOneFile();
        ScmMaterial material = MaterialsMother.gitMaterial("");
        material.setName(new CaseInsensitiveString("git"));
        Modification modification = new Modification();
        modification.setRevision("8c8a273e12a45e57fed5ce978d830eb482f6f666");

        materialRevisions.addRevision(material, modification);
        label.updateLabel(materialRevisions.getNamedRevisions(), 1);
        assertThat(label.toString(), is("release-" + ModificationsMother.currentRevision() + "-8c8a273e12a45e57fed5ce978d830eb482f6f666"));
    }

    @Test
    public void shouldTruncateMaterialRevision() {
        PipelineLabel label = PipelineLabel.create("release-${svnMaterial}-${git[:6]}", InsecureEnvironmentVariables.EMPTY_ENV_VARS);
        MaterialRevisions materialRevisions = ModificationsMother.oneUserOneFile();
        ScmMaterial material = MaterialsMother.gitMaterial("");
        material.setName(new CaseInsensitiveString("git"));
        Modification modification = new Modification();
        modification.setRevision("8c8a273e12a45e57fed5ce978d830eb482f6f666");

        materialRevisions.addRevision(material, modification);
        label.updateLabel(materialRevisions.getNamedRevisions(), 1);
        assertThat(label.toString(), is("release-" + ModificationsMother.currentRevision() + "-8c8a27"));
    }

    @Test
    public void shouldTrimLongLabelTo255() {
        PipelineLabel label = PipelineLabel.create("Pipeline-${upstream}", InsecureEnvironmentVariables.EMPTY_ENV_VARS);
        HashMap<CaseInsensitiveString, String> namedRevisions = new HashMap<>();
        namedRevisions.put(new CaseInsensitiveString("upstream"), longLabel(300));

        label.updateLabel(namedRevisions, 1);
        assertThat(label.toString().length(), is(255));
    }

    @Test
    public void shouldKeepLabelIfLessThan255() {
        PipelineLabel label = PipelineLabel.create("${upstream}", InsecureEnvironmentVariables.EMPTY_ENV_VARS);
        HashMap<CaseInsensitiveString, String> namedRevisions = new HashMap<>();
        namedRevisions.put(new CaseInsensitiveString("upstream"), longLabel(154));

        label.updateLabel(namedRevisions, 1);
        assertThat(label.toString().length(), is(154));
    }

    @Test
    public void shouldCreateDefaultLabelIfTemplateIsNull() {
        PipelineLabel label = PipelineLabel.create(null, InsecureEnvironmentVariables.EMPTY_ENV_VARS);
        assertThat(label, is(defaultLabel()));
    }

    @Test
    public void shouldCreateDefaultLabelIfTemplateIsEmtpty() {
        PipelineLabel label = PipelineLabel.create("", InsecureEnvironmentVariables.EMPTY_ENV_VARS);
        assertThat(label, is(defaultLabel()));
    }

    @Test
    public void shouldCreateLabelIfTemplateIsProvided() {
        PipelineLabel label = PipelineLabel.create("Pipeline-${ABC}", InsecureEnvironmentVariables.EMPTY_ENV_VARS);
        assertThat(label, is(new PipelineLabel("Pipeline-${ABC}", EMPTY_ENV_VARS)));
    }

    @Test
    public void shouldNotReplaceTemplateWithoutMaterial() {
        PipelineLabel label = new PipelineLabel("1.5.0", InsecureEnvironmentVariables.EMPTY_ENV_VARS);
        label.updateLabel(Collections.emptyMap(), 1);
        assertThat(label, is(new PipelineLabel("1.5.0", InsecureEnvironmentVariables.EMPTY_ENV_VARS)));
    }

    @Test
    public void canMatchMaterialName() {
        final String[][] expectedGroups = {{"git"}};
        String res = assertLabelGroupsMatchingAndReplace("release-${git}", expectedGroups);
        assertThat(res, is("release-" + GIT_REVISION));
    }

    @Test
    public void canMatchMaterialNameWithTrial() {
        final String[][] expectedGroups = {{"git"}};
        String res = assertLabelGroupsMatchingAndReplace("release-${git}.alpha.0", expectedGroups);
        assertThat(res, is("release-" + GIT_REVISION + ".alpha.0"));
    }

    @Test
    public void canHandleWrongMaterialName() {
        final String[][] expectedGroups = {{"gitUnused"}};
        String res = assertLabelGroupsMatchingAndReplace("release-${gitUnused}", expectedGroups);
        assertThat(res, is("release-${gitUnused}"));
    }

    @Test
    public void canMatchWithoutTruncation() {
        final String[][] expectedGroups = {{"svnRepo.verynice"}, {"git"}};
        String res = assertLabelGroupsMatchingAndReplace("release-${svnRepo.verynice}-${git}", expectedGroups);
        assertEquals(res, "release-" + SVN_REVISION + "-" + GIT_REVISION);
    }

    @Test
    public void canMatchWithOneGitTruncation() {
        final String[][] expectedGroups = {{"git", "7"}};
        String res = assertLabelGroupsMatchingAndReplace("release-${git[:7]}", expectedGroups);
        assertThat(res, is("release-" + GIT_REVISION.substring(0, 7)));
    }

    @Test
    public void canMatchWithOneGitTruncationTooLongToTruncate() {
        final String[][] expectedGroups = {{"git", "9999"}};
        String res = assertLabelGroupsMatchingAndReplace("release-${git[:9999]}", expectedGroups);
        assertThat(res, is("release-" + GIT_REVISION));
    }

    @Test
    public void canMatchWithOneGitTruncationAlmostTruncated() {
        final String[][] expectedGroups = {{"git", GIT_REV_LENGTH + ""}};
        String res = assertLabelGroupsMatchingAndReplace("release-${git[:" + GIT_REV_LENGTH + "]}", expectedGroups);
        assertThat(res, is("release-" + GIT_REVISION));
    }

    @Test
    public void canMatchWithOneGitTruncationByOneChar() {
        final int size = GIT_REV_LENGTH - 1;
        final String[][] expectedGroups = {{"git", size + ""}};
        String res = assertLabelGroupsMatchingAndReplace("release-${git[:" + size + "]}", expectedGroups);
        assertThat(res, is("release-" + GIT_REVISION.substring(0, GIT_REV_LENGTH - 1)));
    }

    @Test
    public void canMatchWithOneTruncationAsFirstRevision() {
        final String[][] expectedGroups = {{"git", "4"}, {"svn"}};
        String res = assertLabelGroupsMatchingAndReplace("release-${git[:4]}-${svn}", expectedGroups);
        assertThat(res, is("release-" + GIT_REVISION.substring(0, 4) + "-" + SVN_REVISION));
    }

    @Test
    public void canMatchWithTwoTruncation() {
        final String[][] expectedGroups = {{"git", "5"}, {"svn", "3"}};
        String res = assertLabelGroupsMatchingAndReplace("release-${git[:5]}-${svn[:3]}", expectedGroups);
        assertThat(res, is("release-" + GIT_REVISION.substring(0, 5) + "-" + SVN_REVISION.substring(0, 3)));
    }

    @Test
    public void canNotMatchWithTruncationWhenMaterialNameHasAColon() {
        final String[][] expectedGroups = {{"git:one", "7"}};
        String res = assertLabelGroupsMatchingAndReplace("release-${git:one[:7]}", expectedGroups);
        assertThat(res, is("release-${git:one[:7]}"));
    }

    @Test
    public void shouldReplaceTheTemplateWithSpecialCharacters() {
        ensureLabelIsReplaced("SVNMaterial");
        ensureLabelIsReplaced("SVN-Material");
        ensureLabelIsReplaced("SVN_Material");
        ensureLabelIsReplaced("SVN!Material");
        ensureLabelIsReplaced("SVN__##Material_1023_WithNumbers");
        ensureLabelIsReplaced("SVN_Material-_!!_");
        ensureLabelIsReplaced("svn_Material'WithQuote");
        ensureLabelIsReplaced("SVN_Material.With.Period");
        ensureLabelIsReplaced("SVN_Material#With#Hash");
        ensureLabelIsReplaced("SVN_Material:With:Colon");
        ensureLabelIsReplaced("SVN_Material~With~Tilde");
    }

    @Test
    public void shouldReplaceTheTemplateWithEnvironmentVariable() {
        EnvironmentVariables envVars = new EnvironmentVariables();
        envVars.add("VAR", "var_value");
        PipelineLabel label = PipelineLabel.create("release-${ENV:VAR}", envVars);
        label.updateLabel(Collections.emptyMap(), 1);
        assertThat(label.toString(), is("release-var_value"));
    }

    @Test
    public void shouldReplaceTheTemplateWithMultipleEnvironmentVariable() {
        EnvironmentVariables envVars = new EnvironmentVariables();
        envVars.add("VAR1", "1");
        envVars.add("VAR2", "2");
        PipelineLabel label = PipelineLabel.create("release-${ENV:VAR1}, ${ENV:VAR2}", envVars);
        label.updateLabel(Collections.emptyMap(), 1);
        assertThat(label.toString(), is("release-1, 2"));
    }

    @Test
    public void shouldReturnEmptyIfThereIsNoMatchingEnvironmentVariable() {
        PipelineLabel label = PipelineLabel.create("release-${env:VAR}", InsecureEnvironmentVariables.EMPTY_ENV_VARS);
        label.updateLabel(Collections.emptyMap(), 1);
        assertThat(label.toString(), is("release-"));
    }

    @BeforeClass
    public static void setup() {
        MATERIAL_REVISIONS.put(new CaseInsensitiveString("svnRepo.verynice"), SVN_REVISION);
        MATERIAL_REVISIONS.put(new CaseInsensitiveString("svn"), SVN_REVISION);
        MATERIAL_REVISIONS.put(new CaseInsensitiveString("git"), GIT_REVISION);
    }

    private static final Map<CaseInsensitiveString, String> MATERIAL_REVISIONS = new HashMap<>();

    private static final String SVN_REVISION = "3456";
    private static final String GIT_REVISION = "c42c0bfa57d00a25496ba899b1f476e6ec8872bd";
    private static final int GIT_REV_LENGTH = GIT_REVISION.length();

    private String assertLabelGroupsMatchingAndReplace(String labelTemplate, String[][] expectedGroups) {
        assertLabelGroupsMatching(labelTemplate, expectedGroups);

        PipelineLabel label = new PipelineLabel(labelTemplate, InsecureEnvironmentVariables.EMPTY_ENV_VARS);
        label.updateLabel(MATERIAL_REVISIONS, 1);
        return label.toString();
    }

    private void assertLabelGroupsMatching(String labelTemplate, String[][] expectedGroups) {
        java.util.regex.Matcher matcher = PipelineLabel.PATTERN.matcher(labelTemplate);

        for (String[] groups : expectedGroups) {
            assertTrue(matcher.find());

            assertEquals(groups[0], matcher.group("name"));
            if (groups.length > 1) {
                final String truncationLengthLiteral = matcher.group("truncation");
                assertEquals(groups[1], truncationLengthLiteral);
            }
        }

        final boolean actual = matcher.find();
        assertThat(actual, is(false));
    }

    private String longLabel(int length) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < length; i++) {
            builder.append("a");
        }
        return builder.toString();
    }

    private void ensureLabelIsReplaced(String name) {
        PipelineLabel label = getReplacedLabelFor(name, String.format("release-${%s}", name));
        assertThat(label.toString(), is("release-" + ModificationsMother.currentRevision()));
    }

    private PipelineLabel getReplacedLabelFor(String name, String labelFormat) {
        MaterialRevisions materialRevisions = ModificationsMother.oneUserOneFile();
        PipelineLabel label = PipelineLabel.create(labelFormat, InsecureEnvironmentVariables.EMPTY_ENV_VARS);
        ((SvnMaterial) materialRevisions.getRevisions().get(0).getMaterial()).setName(new CaseInsensitiveString(name));
        label.updateLabel(materialRevisions.getNamedRevisions(), 1);
        return label;
    }
}
