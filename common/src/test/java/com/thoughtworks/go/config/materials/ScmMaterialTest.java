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
package com.thoughtworks.go.config.materials;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.SecretParam;
import com.thoughtworks.go.domain.MaterialRevision;
import com.thoughtworks.go.domain.materials.DummyMaterial;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.*;

class ScmMaterialTest {
    private DummyMaterial material;

    @BeforeEach
    void setUp() throws Exception {
        material = new DummyMaterial();
    }

    @Test
    void shouldSmudgePasswordForDescription() throws Exception {
        material.setUrl("http://user:password@localhost:8000/foo");
        assertThat(material.getDescription(), is("http://user:******@localhost:8000/foo"));
    }

    @Test
    void displayNameShouldReturnUrlWhenNameNotSet() throws Exception {
        material.setUrl("http://user:password@localhost:8000/foo");
        assertThat(material.getDisplayName(), is("http://user:******@localhost:8000/foo"));
    }

    @Test
    void displayNameShouldReturnNameWhenSet() throws Exception {
        material.setName(new CaseInsensitiveString("blah-name"));
        assertThat(material.getDisplayName(), is("blah-name"));
    }

    @Test
    void willNeverBeUsedInAFetchArtifact() {
        assertThat(material.isUsedInFetchArtifact(new PipelineConfig()), is(false));
    }

    @Test
    void populateEnvironmentContextShouldSetFromAndToRevisionEnvironmentVariables() {
        material.setUrl("https://user:password@example.github.com");

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
    void populateEnvContextShouldSetMaterialEnvVars() {
        material.setUrl("https://user:password@example.github.com");

        EnvironmentVariableContext ctx = new EnvironmentVariableContext();
        final ArrayList<Modification> modifications = new ArrayList<>();

        modifications.add(new Modification("user2", "comment2", "email2", new Date(), "24"));
        modifications.add(new Modification("user1", "comment1", "email1", new Date(), "23"));

        MaterialRevision materialRevision = new MaterialRevision(material, modifications);
        assertThat(ctx.getProperty(ScmMaterial.GO_MATERIAL_URL), is(nullValue()));

        material.populateEnvironmentContext(ctx, materialRevision, new File("."));

        assertThat(ctx.getProperty(ScmMaterial.GO_MATERIAL_URL), is("https://example.github.com"));
    }

    @Test
    void shouldIncludeMaterialNameInEnvVariableNameIfAvailable() {
        EnvironmentVariableContext context = new EnvironmentVariableContext();
        material.setVariableWithName(context, "value", "GO_PROPERTY");
        assertThat(context.getProperty("GO_PROPERTY"), is("value"));

        context = new EnvironmentVariableContext();
        material.setName(new CaseInsensitiveString("dummy"));
        material.setVariableWithName(context, "value", "GO_PROPERTY");
        assertThat(context.getProperty("GO_PROPERTY_DUMMY"), is("value"));
        assertThat(context.getProperty("GO_PROPERTY"), is(nullValue()));
    }

    @Test
    void shouldIncludeDestFolderInEnvVariableNameIfMaterialNameNotAvailable() {
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
    void shouldEscapeHyphenFromMaterialNameWhenUsedInEnvVariable() {
        EnvironmentVariableContext context = new EnvironmentVariableContext();
        material.setName(new CaseInsensitiveString("material-name"));
        material.setVariableWithName(context, "value", "GO_PROPERTY");
        assertThat(context.getProperty("GO_PROPERTY_MATERIAL_NAME"), is("value"));
        assertThat(context.getProperty("GO_PROPERTY"), is(nullValue()));
    }

    @Test
    void shouldEscapeHyphenFromFolderNameWhenUsedInEnvVariable() {
        EnvironmentVariableContext context = new EnvironmentVariableContext();
        material.setFolder("folder-name");
        material.setVariableWithName(context, "value", "GO_PROPERTY");
        assertThat(context.getProperty("GO_PROPERTY_FOLDER_NAME"), is("value"));
        assertThat(context.getProperty("GO_PROPERTY"), is(nullValue()));
    }

    @Test
    void shouldReturnTrueForAnScmMaterial_supportsDestinationFolder() throws Exception {
        assertThat(material.supportsDestinationFolder(), is(true));
    }

    @Test
    void shouldGetMaterialNameForEnvironmentMaterial() {
        assertThat(material.getMaterialNameForEnvironmentVariable(), is(""));
        material.setFolder("dest-folder");
        assertThat(material.getMaterialNameForEnvironmentVariable(), is("DEST_FOLDER"));
        material.setName(new CaseInsensitiveString("some-material"));
        assertThat(material.getMaterialNameForEnvironmentVariable(), is("SOME_MATERIAL"));
    }

    @Nested
    class hasSecretParams {
        @Test
        void shouldBeTrueIfPasswordHasSecretParam() {
            DummyMaterial dummyMaterial = new DummyMaterial();
            dummyMaterial.setPassword("{{SECRET:[secret_config_id][lookup_password]}}");

            assertTrue(dummyMaterial.hasSecretParams());
        }

        @Test
        void shouldBeFalseIfMaterialUrlAndPasswordDoesNotHaveSecretParams() {
            DummyMaterial dummyMaterial = new DummyMaterial();

            assertFalse(dummyMaterial.hasSecretParams());
        }
    }

    @Nested
    class getSecretParams {
        @Test
        void shouldReturnAListOfSecretParams() {
            DummyMaterial dummyMaterial = new DummyMaterial();
            dummyMaterial.setPassword("{{SECRET:[secret_config_id][lookup_password]}}");

            assertThat(dummyMaterial.getSecretParams().size(), is(1));
            assertThat(dummyMaterial.getSecretParams().get(0), is(new SecretParam("secret_config_id", "lookup_password")));
        }

        @Test
        void shouldBeAnEmptyListInAbsenceOfSecretParamsinMaterialPassword() {
            DummyMaterial dummyMaterial = new DummyMaterial();

            assertThat(dummyMaterial.getSecretParams(), is(nullValue()));
        }
    }

}
