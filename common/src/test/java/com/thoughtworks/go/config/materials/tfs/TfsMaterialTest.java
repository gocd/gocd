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
package com.thoughtworks.go.config.materials.tfs;

import com.thoughtworks.go.config.PasswordEncrypter;
import com.thoughtworks.go.config.SecretParam;
import com.thoughtworks.go.config.exceptions.UnresolvedSecretParamException;
import com.thoughtworks.go.config.materials.AbstractMaterial;
import com.thoughtworks.go.config.materials.PasswordAwareMaterial;
import com.thoughtworks.go.config.materials.ScmMaterial;
import com.thoughtworks.go.domain.MaterialRevision;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.materials.TestSubprocessExecutionContext;
import com.thoughtworks.go.domain.materials.mercurial.StringRevision;
import com.thoughtworks.go.domain.materials.tfs.TfsCommand;
import com.thoughtworks.go.security.CryptoException;
import com.thoughtworks.go.security.GoCipher;
import com.thoughtworks.go.util.ReflectionUtil;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;
import com.thoughtworks.go.util.command.UrlArgument;
import org.junit.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.migrationsupport.rules.EnableRuleMigrationSupport;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static com.thoughtworks.go.config.materials.AbstractMaterial.SQL_CRITERIA_TYPE;
import static com.thoughtworks.go.domain.materials.ValidationBean.valid;
import static com.thoughtworks.go.util.DataStructureUtils.m;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@EnableRuleMigrationSupport
public class TfsMaterialTest {
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    private TfsMaterial tfsMaterialFirstCollectionFirstProject;
    private TfsMaterial tfsMaterialFirstCollectionSecondProject;
    private final String DOMAIN = "domain";
    private final String USERNAME = "username";
    private final String PASSWORD = "password";
    private final String TFS_FIRST_COLLECTION_URL = "http://some.tfs.repo.local";
    private final String TFS_FIRST_PROJECT = "$/first_project";
    private final String TFS_SECOND_PROJECT = "$/second_project";

    @BeforeEach
    void setUp() {
        GoCipher goCipher = mock(GoCipher.class);
        tfsMaterialFirstCollectionFirstProject = new TfsMaterial(new UrlArgument(TFS_FIRST_COLLECTION_URL), USERNAME, DOMAIN, PASSWORD, TFS_FIRST_PROJECT);
        tfsMaterialFirstCollectionSecondProject = new TfsMaterial(new UrlArgument(TFS_FIRST_COLLECTION_URL), USERNAME, DOMAIN, PASSWORD, TFS_SECOND_PROJECT);
    }

    @Test
    void shouldShowLatestModification() throws IOException {
        File dir = temporaryFolder.newFolder("tfs-dir");
        TestSubprocessExecutionContext execCtx = new TestSubprocessExecutionContext();
        TfsMaterial spy = spy(tfsMaterialFirstCollectionSecondProject);
        TfsCommand tfsCommand = mock(TfsCommand.class);
        when(tfsCommand.latestModification(dir)).thenReturn(new ArrayList<>());
        doReturn(tfsCommand).when(spy).tfs(execCtx);

        List<Modification> actual = spy.latestModification(dir, execCtx);

        assertThat(actual).isEqualTo(new ArrayList<Modification>());
        verify(tfsCommand).latestModification(dir);
    }

    @Test
    void shouldLoadAllModificationsSinceAGivenRevision() throws IOException {
        File dir = temporaryFolder.newFolder("tfs-dir");
        TestSubprocessExecutionContext execCtx = new TestSubprocessExecutionContext();
        TfsMaterial spy = spy(tfsMaterialFirstCollectionFirstProject);
        TfsCommand tfsCommand = mock(TfsCommand.class);
        when(tfsCommand.modificationsSince(dir, new StringRevision("5"))).thenReturn(new ArrayList<>());
        doReturn(tfsCommand).when(spy).tfs(execCtx);

        List<Modification> actual = spy.modificationsSince(dir, new StringRevision("5"), execCtx);

        assertThat(actual).isEqualTo(new ArrayList<Modification>());
        verify(tfsCommand).modificationsSince(dir, new StringRevision("5"));
    }

    @Test
    void shouldInjectAllRelevantAttributesInSqlCriteriaMap() {
        TfsMaterial tfsMaterial = new TfsMaterial(new UrlArgument("my-url"), "loser", DOMAIN, "foo_bar_baz", "/dev/null");
        assertThat(tfsMaterial.getSqlCriteria()).isEqualTo(m(
                SQL_CRITERIA_TYPE, (Object) "TfsMaterial",
                "url", "my-url",
                "username", "loser",
                "projectPath", "/dev/null", "domain", DOMAIN));
    }

    @Test
    void shouldInjectAllRelevantAttributesInAttributeMap() {
        TfsMaterial tfsMaterial = new TfsMaterial(new UrlArgument("my-url"), "loser", DOMAIN, "foo_bar_baz", "/dev/null");
        assertThat(tfsMaterial.getAttributesForXml()).isEqualTo(m(
                AbstractMaterial.SQL_CRITERIA_TYPE, (Object) "TfsMaterial",
                "url", "my-url",
                "username", "loser",
                "projectPath", "/dev/null", "domain", DOMAIN));
    }

    @Test
    void shouldReturnUrlForCommandLine_asUrl_IfSet() {
        TfsMaterial tfsMaterial = new TfsMaterial(new UrlArgument("http://foo:bar@my-url.com"), "loser", DOMAIN, "foo_bar_baz", "/dev/null"
        );
        assertThat(tfsMaterial.getUrl()).isEqualTo("http://foo:bar@my-url.com");

        tfsMaterial = new TfsMaterial(null, "loser", DOMAIN, "foo_bar_baz", "/dev/null");
        assertThat(tfsMaterial.getUrl()).isNull();
    }

    @Test
    void shouldReturnUrlForCommandLine_asLocation_IfSet() {
        TfsMaterial tfsMaterial = new TfsMaterial(new UrlArgument("http://foo:bar@my-url.com"), "loser", DOMAIN, "foo_bar_baz", "/dev/null"
        );
        assertThat(tfsMaterial.getLocation()).isEqualTo("http://foo:******@my-url.com");

        tfsMaterial = new TfsMaterial(null, "loser", DOMAIN, "foo_bar_baz", "/dev/null");
        assertThat(tfsMaterial.getLocation()).isNull();
    }

    @Test
    void shouldNotDecryptPasswordIfPasswordIsNotNull() throws Exception {
        GoCipher mockGoCipher = mock(GoCipher.class);
        when(mockGoCipher.encrypt("password")).thenReturn("encrypted");
        when(mockGoCipher.decrypt("encrypted")).thenReturn("password");

        TfsMaterial material = new TfsMaterial(new UrlArgument("/foo"), "username", DOMAIN, "password", "");
        material.ensureEncrypted();
        when(mockGoCipher.encrypt("new_password")).thenReturn("new_encrypted");
        material.setPassword("new_password");
        when(mockGoCipher.decrypt("new_encrypted")).thenReturn("new_password");

        assertThat(material.getPassword()).isEqualTo("new_password");
    }

    @Test
    void shouldBePasswordAware() {
        assertThat(PasswordAwareMaterial.class.isAssignableFrom(TfsMaterial.class)).isTrue();
    }

    @Test
    void shouldBePasswordEncrypter() {
        assertThat(PasswordEncrypter.class.isAssignableFrom(TfsMaterial.class)).isTrue();
    }

    @Test
    void shouldKnowItsType() {
        assertThat(tfsMaterialFirstCollectionFirstProject.getTypeForDisplay()).isEqualTo("Tfs");
    }

    @Test
    void shouldCheckConnection() {
        TestSubprocessExecutionContext execCtx = new TestSubprocessExecutionContext();
        TfsCommand tfsCommand = mock(TfsCommand.class);
        doNothing().when(tfsCommand).checkConnection();
        TfsMaterial spy = spy(tfsMaterialFirstCollectionFirstProject);
        doReturn(tfsCommand).when(spy).tfs(execCtx);
        assertThat(spy.checkConnection(execCtx)).isEqualTo(valid());
        verify(tfsCommand, times(1)).checkConnection();
    }

    @Test
    void shouldGetLongDescriptionForMaterial() {
        TfsMaterial material = new TfsMaterial(new UrlArgument("http://url/"), "user", "domain", "password", "$project/path/");
        assertThat(material.getLongDescription()).isEqualTo("URL: http://url/, Username: user, Domain: domain, ProjectPath: $project/path/");
    }

    @Test
    void shouldCopyOverPasswordWhenConvertingToConfig() throws Exception {
        TfsMaterial material = new TfsMaterial(new UrlArgument("http://url/"), "user", "domain", "password", "$project/path/");

        TfsMaterialConfig config = (TfsMaterialConfig) material.config();

        assertThat(config.getPassword()).isEqualTo("password");
        assertThat(config.getEncryptedPassword()).isNotNull();
    }

    @Test
    void shouldGetAttributesWithSecureFields() {
        TfsMaterial material = new TfsMaterial(new UrlArgument("http://username:password@tfsrepo.com"), "username", "domain", "password", "$project/path/");
        Map<String, Object> attributes = material.getAttributes(true);

        assertThat(attributes.get("type")).isEqualTo("tfs");
        Map<String, Object> configuration = (Map<String, Object>) attributes.get("tfs-configuration");
        assertThat(configuration.get("url")).isEqualTo("http://username:password@tfsrepo.com");
        assertThat(configuration.get("domain")).isEqualTo("domain");
        assertThat(configuration.get("username")).isEqualTo("username");
        assertThat(configuration.get("password")).isEqualTo("password");
        assertThat(configuration.get("project-path")).isEqualTo("$project/path/");
    }

    @Test
    void shouldGetAttributesWithoutSecureFields() {
        TfsMaterial material = new TfsMaterial(new UrlArgument("http://username:password@tfsrepo.com"), "username", "domain", "password", "$project/path/");
        Map<String, Object> attributes = material.getAttributes(false);

        assertThat(attributes.get("type")).isEqualTo("tfs");
        Map<String, Object> configuration = (Map<String, Object>) attributes.get("tfs-configuration");
        assertThat(configuration.get("url")).isEqualTo("http://username:******@tfsrepo.com");
        assertThat(configuration.get("domain")).isEqualTo("domain");
        assertThat(configuration.get("username")).isEqualTo("username");
        assertThat(configuration.get("password")).isNull();
        assertThat(configuration.get("project-path")).isEqualTo("$project/path/");
    }

    @Nested
    class passwordForCommandLine {
        @Test
        void shouldReturnPasswordAsConfigured_IfNotDefinedAsSecretParam() {
            TfsMaterial tfsMaterial = new TfsMaterial(new UrlArgument("some-url"), null, null, "badger", null);

            assertThat(tfsMaterial.passwordForCommandLine()).isEqualTo("badger");
        }

        @Test
        void shouldReturnAResolvedPassword_IfPasswordDefinedAsSecretParam() {
            TfsMaterial tfsMaterial = new TfsMaterial(new UrlArgument("some-url"), null, null, "{{SECRET:[secret_config_id][lookup_pass]}}", null);

            tfsMaterial.getSecretParams().findFirst("lookup_pass").ifPresent(secretParam -> secretParam.setValue("resolved_password"));

            assertThat(tfsMaterial.passwordForCommandLine()).isEqualTo("resolved_password");
        }

        @Test
        void shouldErrorOutWhenCalledOnAUnResolvedSecretParam_IfPasswordDefinedAsSecretParam() {
            TfsMaterial tfsMaterial = new TfsMaterial(new UrlArgument("some-url"), null, null, "{{SECRET:[secret_config_id][lookup_pass]}}", null);

            assertThatCode(tfsMaterial::passwordForCommandLine)
                    .isInstanceOf(UnresolvedSecretParamException.class)
                    .hasMessageContaining("SecretParam 'lookup_pass' is used before it is resolved.");
        }
    }

    @Nested
    class setPassword {
        @Test
        void shouldParsePasswordString_IfDefinedAsSecretParam() {
            TfsMaterial tfsMaterial = new TfsMaterial(new UrlArgument("some-url"), null, null, "{{SECRET:[secret_config_id][lookup_pass]}}", null);

            assertThat(tfsMaterial.getSecretParams())
                    .hasSize(1)
                    .contains(new SecretParam("secret_config_id", "lookup_pass"));
        }
    }

    @Test
    void populateEnvContextShouldSetMaterialEnvVars() {
        EnvironmentVariableContext ctx = new EnvironmentVariableContext();
        final ArrayList<Modification> modifications = new ArrayList<>();

        modifications.add(new Modification("user2", "comment2", "email2", new Date(), "24"));
        modifications.add(new Modification("user1", "comment1", "email1", new Date(), "23"));

        MaterialRevision materialRevision = new MaterialRevision(tfsMaterialFirstCollectionFirstProject, modifications);
        assertThat(ctx.getProperty(ScmMaterial.GO_MATERIAL_URL)).isNull();
        assertThat(ctx.getProperty(TfsMaterial.GO_MATERIAL_DOMAIN)).isNull();

        tfsMaterialFirstCollectionFirstProject.populateEnvironmentContext(ctx, materialRevision, new File("."));

        assertThat(ctx.getProperty(ScmMaterial.GO_MATERIAL_URL)).isEqualTo(TFS_FIRST_COLLECTION_URL);
        assertThat(ctx.getProperty(TfsMaterial.GO_MATERIAL_DOMAIN)).isEqualTo(DOMAIN);
    }

    @Test
    void shouldOnlyPopulateDomainEnvVarIfPresent() {
        TfsMaterial material = new TfsMaterial(new UrlArgument(TFS_FIRST_COLLECTION_URL), USERNAME, "", PASSWORD, TFS_FIRST_PROJECT);
        EnvironmentVariableContext ctx = new EnvironmentVariableContext();
        final ArrayList<Modification> modifications = new ArrayList<>();

        modifications.add(new Modification("user2", "comment2", "email2", new Date(), "24"));
        modifications.add(new Modification("user1", "comment1", "email1", new Date(), "23"));

        MaterialRevision materialRevision = new MaterialRevision(material, modifications);
        material.populateEnvironmentContext(ctx, materialRevision, new File("."));

        assertThat(ctx.hasProperty(ScmMaterial.GO_MATERIAL_URL)).isTrue();
        assertThat(ctx.hasProperty(TfsMaterial.GO_MATERIAL_DOMAIN)).isFalse();
    }
}
