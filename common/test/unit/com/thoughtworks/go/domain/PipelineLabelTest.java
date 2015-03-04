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

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.materials.mercurial.HgMaterial;
import com.thoughtworks.go.domain.label.PipelineLabel;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.helper.MaterialsMother;
import com.thoughtworks.go.helper.ModificationsMother;
import org.hamcrest.core.Is;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
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
}
