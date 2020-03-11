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
package com.thoughtworks.go.server.domain.xml.materials;

import com.thoughtworks.go.config.materials.git.GitMaterial;
import com.thoughtworks.go.domain.MaterialRevision;
import com.thoughtworks.go.domain.XmlWriterContext;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.materials.ModifiedAction;
import com.thoughtworks.go.junit5.FileSource;
import com.thoughtworks.go.server.domain.xml.builder.ElementBuilder;
import com.thoughtworks.go.util.DateUtils;
import com.thoughtworks.go.util.SystemEnvironment;
import org.dom4j.dom.DOMElement;
import org.junit.jupiter.params.ParameterizedTest;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static com.thoughtworks.go.helper.MaterialsMother.gitMaterial;
import static org.xmlunit.assertj.XmlAssert.assertThat;

public class ScmMaterialXmlRepresenterTest {
    @ParameterizedTest
    @FileSource(files = "/feeds/materials/git-material.xml")
    void shouldGenerateXmlFromMaterialRevision(String expectedXML) {
        GitMaterial gitMaterial = gitMaterial("https://material/example.git");
        gitMaterial.setId(100);
        MaterialRevision materialRevision = new MaterialRevision(gitMaterial, modifications());
        DOMElement root = new DOMElement("materials");
        ElementBuilder builder = new ElementBuilder(root);
        XmlWriterContext context = new XmlWriterContext("https://test.host/go", null, null, null, new SystemEnvironment());

        new ScmMaterialXmlRepresenter("up42", 1,materialRevision).populate(builder, context);

        assertThat(root.asXML()).and(expectedXML)
                .ignoreWhitespace()
                .areIdentical();
    }

    private List<Modification> modifications() {
        Date date = DateUtils.parseISO8601("2019-12-31T15:31:49+05:30");
        return Arrays.asList(
                modification(date, "Bob", "Adding build.xml", "3", "build.xml", ModifiedAction.added),
                modification(date, "Sam", "Fixing the not checked in files", "2", "tools/bin/go.jruby", ModifiedAction.added),
                modification(date, "Sam", "Adding .gitignore", "1", ".gitignore", ModifiedAction.modified)
        );
    }

    private Modification modification(Date date, String user, String comment, String revision, String filename, ModifiedAction action) {
        Modification modification = new Modification(user, comment, user.toLowerCase() + "@gocd.org", date, revision, null);
        modification.createModifiedFile(filename, null, action);
        return modification;
    }
}
