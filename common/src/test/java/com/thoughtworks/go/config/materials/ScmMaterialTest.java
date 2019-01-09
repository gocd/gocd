/*
 * Copyright 2017 ThoughtWorks, Inc.
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

package com.thoughtworks.go.config.materials;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.materials.git.GitMaterial;
import com.thoughtworks.go.domain.MaterialRevision;
import com.thoughtworks.go.domain.materials.DummyMaterial;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class ScmMaterialTest {
    private DummyMaterial material;

    @Before
    public void setUp() throws Exception {
        material = new DummyMaterial();
    }

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
        final ArrayList<Modification> modifications = new ArrayList<>();

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
        assertThat(context.getProperty("GO_PROPERTY_DUMMY"), is("value"));
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
        assertThat(context.getProperty("GO_PROPERTY_FOO_DIR"), is("value"));
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
        assertThat(material.supportsDestinationFolder(), is(true));
    }

    @Test
    public void shouldGetMaterialNameForEnvironmentMaterial(){
        assertThat(material.getMaterialNameForEnvironmentVariable(), is(""));
        material.setFolder("dest-folder");
        assertThat(material.getMaterialNameForEnvironmentVariable(), is("DEST_FOLDER"));
        material.setName(new CaseInsensitiveString("some-material"));
        assertThat(material.getMaterialNameForEnvironmentVariable(), is("SOME_MATERIAL"));
    }
}
