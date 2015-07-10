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

package com.thoughtworks.go.config.materials;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.materials.git.GitMaterial;
import com.thoughtworks.go.domain.MaterialRevision;
import com.thoughtworks.go.domain.materials.DummyMaterial;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

public class ScmMaterialTest {
    DummyMaterial material = new DummyMaterial();

    @Test
    public void shouldSmudgePasswordForDescription() throws Exception{
        material.setUrl("http://user:password@localhost:8000/foo");
        assertThat(material.getDescription(), is("http://user:******@localhost:8000/foo"));
    }


    @Test
    public void displayNameShouldReturnUrlWhenNameNotSet() throws Exception{
        material.setUrl("http://user:password@localhost:8000/foo");
        assertThat(material.getDisplayName(), is("http://user:******@localhost:8000/foo"));
    }

    @Test
    public void displayNameShouldReturnNameWhenSet() throws Exception{
        material.setName(new CaseInsensitiveString("blah-name"));
        assertThat(material.getDisplayName(), is("blah-name"));
    }

    @Test
    public void willNeverBeUsedInAFetchArtifact() {
        assertThat(material.isUsedInFetchArtifact(new PipelineConfig()), is(false));
    }

    @Test
    public void populateEnvironmentContextShouldSetFromAndToRevisionEnvironmentVariables() {
        
        EnvironmentVariableContext ctx = new EnvironmentVariableContext();
        final ArrayList<Modification> modifications = new ArrayList<Modification>();

        modifications.add(new Modification("user2", "comment2", "email2", new Date(), "24"));
        modifications.add(new Modification("user1", "comment1", "email1", new Date(), "23"));

        MaterialRevision materialRevision = new MaterialRevision(material, modifications);
        assertThat(ctx.getProperty(ScmMaterial.GO_FROM_REVISION), is(nullValue()));
        assertThat(ctx.getProperty(ScmMaterial.GO_TO_REVISION), is(nullValue()));
        assertThat(ctx.getProperty(ScmMaterial.GO_REVISION), is(nullValue()));

        material.populateEnvironmentContext(ctx, materialRevision, new File("."));
        
        assertThat(ctx.getProperty(ScmMaterial.GO_FROM_REVISION), is("23"));
        assertThat(ctx.getProperty(ScmMaterial.GO_TO_REVISION), is("24"));
        assertThat(ctx.getProperty(ScmMaterial.GO_REVISION), is("24"));
    }

    @Test
    public void shouldIncludeMaterialNameInEnvVariableNameIfAvailable() {
        EnvironmentVariableContext context = new EnvironmentVariableContext();
        material.setVariableWithName(context, "value", "GO_PROPERTY");
        assertThat(context.getProperty("GO_PROPERTY"), is("value"));

        context = new EnvironmentVariableContext();
        material.setName( new CaseInsensitiveString("dummy"));
        material.setVariableWithName(context, "value", "GO_PROPERTY");
        assertThat(context.getProperty("GO_PROPERTY_" + material.getName().toUpper()), is("value"));
        assertThat(context.getProperty("GO_PROPERTY"), is(nullValue()));
    }

    @Test
    public void shouldIncludeDestFolderInEnvVariableNameIfMaterialNameNotAvailable() {
        EnvironmentVariableContext context = new EnvironmentVariableContext();
        material.setVariableWithName(context, "value", "GO_PROPERTY");
        assertThat(context.getProperty("GO_PROPERTY"), is("value"));

        context = new EnvironmentVariableContext();
        material.setFolder("foo_dir");
        material.setVariableWithName(context, "value", "GO_PROPERTY");
        assertThat(context.getProperty("GO_PROPERTY_" + material.getFolder().toUpperCase()), is("value"));
        assertThat(context.getProperty("GO_PROPERTY"), is(nullValue()));
    }

    @Test
    public void shouldEscapeHyphenFromMaterialNameWhenUsedInEnvVariable() {
        EnvironmentVariableContext context = new EnvironmentVariableContext();
        material.setName( new CaseInsensitiveString("material-name"));
        material.setVariableWithName(context, "value", "GO_PROPERTY");
        assertThat(context.getProperty("GO_PROPERTY_MATERIAL_NAME"), is("value"));
        assertThat(context.getProperty("GO_PROPERTY"), is(nullValue()));
    }

    @Test
    public void shouldEscapeHyphenFromFolderNameWhenUsedInEnvVariable() {
        EnvironmentVariableContext context = new EnvironmentVariableContext();
        material.setFolder("folder-name");
        material.setVariableWithName(context, "value", "GO_PROPERTY");
        assertThat(context.getProperty("GO_PROPERTY_FOLDER_NAME"), is("value"));
        assertThat(context.getProperty("GO_PROPERTY"), is(nullValue()));
    }

    @Test
    public void shouldReturnTrueForAnScmMaterial_supportsDestinationFolder() throws Exception {
        ScmMaterial material = new GitMaterial("http://some-url.com", "some-branch");
        assertThat(material.supportsDestinationFolder(), is(true));
    }
}