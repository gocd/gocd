/*
 * Copyright Thoughtworks, Inc.
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

import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.SecretParam;
import com.thoughtworks.go.domain.MaterialInstance;
import com.thoughtworks.go.domain.MaterialRevision;
import com.thoughtworks.go.domain.materials.*;
import com.thoughtworks.go.util.command.ConsoleOutputStreamConsumer;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;
import com.thoughtworks.go.util.command.UrlArgument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static com.thoughtworks.go.config.CaseInsensitiveString.cis;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScmMaterialTest {
    private DummyMaterial material;

    @BeforeEach
    void setUp() {
        material = new DummyMaterial();
    }

    @Test
    void shouldSmudgePasswordForDescription() {
        material.setUrl("http://user:password@localhost:8000/foo");
        assertThat(material.getDescription()).isEqualTo("http://user:******@localhost:8000/foo");
    }

    @Test
    void displayNameShouldReturnUrlWhenNameNotSet() {
        material.setUrl("http://user:password@localhost:8000/foo");
        assertThat(material.getDisplayName()).isEqualTo("http://user:******@localhost:8000/foo");
    }

    @Test
    void displayNameShouldReturnNameWhenSet() {
        material.setName(cis("blah-name"));
        assertThat(material.getDisplayName()).isEqualTo("blah-name");
    }

    @Test
    void willNeverBeUsedInAFetchArtifact() {
        assertThat(material.isUsedInFetchArtifact(new PipelineConfig())).isFalse();
    }

    @Test
    void populateEnvironmentContextShouldSetFromAndToRevisionEnvironmentVariables() {
        material.setUrl("https://user:password@example.github.com");

        EnvironmentVariableContext ctx = new EnvironmentVariableContext();
        final List<Modification> modifications = new ArrayList<>();

        modifications.add(new Modification("user2", "comment2", "email2", new Date(), "24"));
        modifications.add(new Modification("user1", "comment1", "email1", new Date(), "23"));

        MaterialRevision materialRevision = new MaterialRevision(material, modifications);
        assertThat(ctx.getProperty(ScmMaterial.GO_FROM_REVISION)).isNull();
        assertThat(ctx.getProperty(ScmMaterial.GO_TO_REVISION)).isNull();
        assertThat(ctx.getProperty(ScmMaterial.GO_REVISION)).isNull();

        material.populateEnvironmentContext(ctx, materialRevision, new File("."));

        assertThat(ctx.getProperty(ScmMaterial.GO_FROM_REVISION)).isEqualTo("23");
        assertThat(ctx.getProperty(ScmMaterial.GO_TO_REVISION)).isEqualTo("24");
        assertThat(ctx.getProperty(ScmMaterial.GO_REVISION)).isEqualTo("24");
    }

    @Test
    void populateEnvContextShouldSetMaterialEnvVars() {
        material.setUrl("https://user:password@example.github.com");

        EnvironmentVariableContext ctx = new EnvironmentVariableContext();
        final List<Modification> modifications = new ArrayList<>();

        modifications.add(new Modification("user2", "comment2", "email2", new Date(), "24"));
        modifications.add(new Modification("user1", "comment1", "email1", new Date(), "23"));

        MaterialRevision materialRevision = new MaterialRevision(material, modifications);
        assertThat(ctx.getProperty(ScmMaterial.GO_MATERIAL_URL)).isNull();

        material.populateEnvironmentContext(ctx, materialRevision, new File("."));

        assertThat(ctx.getProperty(ScmMaterial.GO_MATERIAL_URL)).isEqualTo("https://example.github.com");
    }

    @Test
    void shouldIncludeMaterialNameInEnvVariableNameIfAvailable() {
        EnvironmentVariableContext context = new EnvironmentVariableContext();
        material.setVariableWithName(context, "value", "GO_PROPERTY");
        assertThat(context.getProperty("GO_PROPERTY")).isEqualTo("value");

        context = new EnvironmentVariableContext();
        material.setName(cis("dummy"));
        material.setVariableWithName(context, "value", "GO_PROPERTY");
        assertThat(context.getProperty("GO_PROPERTY_DUMMY")).isEqualTo("value");
        assertThat(context.getProperty("GO_PROPERTY")).isNull();
    }

    @Test
    void shouldIncludeDestFolderInEnvVariableNameIfMaterialNameNotAvailable() {
        EnvironmentVariableContext context = new EnvironmentVariableContext();
        material.setVariableWithName(context, "value", "GO_PROPERTY");
        assertThat(context.getProperty("GO_PROPERTY")).isEqualTo("value");

        context = new EnvironmentVariableContext();
        material.setFolder("foo_dir");
        material.setVariableWithName(context, "value", "GO_PROPERTY");
        assertThat(context.getProperty("GO_PROPERTY_FOO_DIR")).isEqualTo("value");
        assertThat(context.getProperty("GO_PROPERTY")).isNull();
    }

    @Test
    void shouldEscapeHyphenFromMaterialNameWhenUsedInEnvVariable() {
        EnvironmentVariableContext context = new EnvironmentVariableContext();
        material.setName(cis("material-name"));
        material.setVariableWithName(context, "value", "GO_PROPERTY");
        assertThat(context.getProperty("GO_PROPERTY_MATERIAL_NAME")).isEqualTo("value");
        assertThat(context.getProperty("GO_PROPERTY")).isNull();
    }

    @Test
    void shouldEscapeHyphenFromFolderNameWhenUsedInEnvVariable() {
        EnvironmentVariableContext context = new EnvironmentVariableContext();
        material.setFolder("folder-name");
        material.setVariableWithName(context, "value", "GO_PROPERTY");
        assertThat(context.getProperty("GO_PROPERTY_FOLDER_NAME")).isEqualTo("value");
        assertThat(context.getProperty("GO_PROPERTY")).isNull();
    }

    @Test
    void shouldReturnTrueForAnScmMaterial_supportsDestinationFolder() {
        assertThat(material.supportsDestinationFolder()).isTrue();
    }

    @Test
    void shouldGetMaterialNameForEnvironmentMaterial() {
        assertThat(material.getMaterialNameForEnvironmentVariable()).isEmpty();
        material.setFolder("dest-folder");
        assertThat(material.getMaterialNameForEnvironmentVariable()).isEqualTo("DEST_FOLDER");
        material.setName(cis("some-material"));
        assertThat(material.getMaterialNameForEnvironmentVariable()).isEqualTo("SOME_MATERIAL");
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

            assertThat(dummyMaterial.getSecretParams().size()).isEqualTo(1);
            assertThat(dummyMaterial.getSecretParams().getFirst()).isEqualTo(new SecretParam("secret_config_id", "lookup_password"));
        }

        @Test
        void shouldBeAnEmptyListInAbsenceOfSecretParamsInMaterialPassword() {
            DummyMaterial dummyMaterial = new DummyMaterial();

            assertThat(dummyMaterial.getSecretParams()).isNull();
        }
    }

    private static final class DummyMaterial extends ScmMaterial {
        private String url;

        public DummyMaterial() {
            super("DummyMaterial");
        }

        @Override
        public String getUrl() {
            return url;
        }

        @Override
        public String urlForCommandLine() {
            return url;
        }

        @Override
        protected UrlArgument getUrlArgument() {
            return new UrlArgument(url);
        }

        @Override
        public String getLongDescription() {
            return "Dummy";
        }

        @Override
        public MaterialConfig config() {
            throw unsupported();
        }

        @Override
        public Map<String, Object> getAttributes(boolean addSecureFields) {
            throw unsupported();
        }

        public void setUrl(String url) {
            this.url = url;
        }

        @Override
        protected String getLocation() {
            return getUrl();
        }

        @Override
        public String getTypeForDisplay() {
            return "Dummy";
        }

        @Override
        public Class<MaterialInstance> getInstanceType() {
            throw unsupported();
        }

        @Override
        public MaterialInstance createMaterialInstance() {
            throw unsupported();
        }

        @Override
        public void updateTo(ConsoleOutputStreamConsumer outputStreamConsumer, File baseDir, RevisionContext revisionContext, final SubprocessExecutionContext execCtx) {
            throw unsupported();
        }

        @Override
        public void checkout(File baseDir, Revision revision, SubprocessExecutionContext execCtx) {
            throw unsupported();
        }

        @Override
        public ValidationBean checkConnection(final SubprocessExecutionContext execCtx) {
            throw unsupported();
        }

        @Override
        public boolean isCheckExternals() {
            throw unsupported();
        }

        private UnsupportedOperationException unsupported() {
            throw new UnsupportedOperationException();
        }

        @Override
        protected void appendCriteria(Map<String, Object> parameters) {
        }

        @Override
        protected void appendAttributes(Map<String, Object> parameters) {
            throw unsupported();
        }

    }
}
