/*
 * Copyright 2019 ThoughtWorks, Inc.
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

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.materials.Filter;
import com.thoughtworks.go.config.materials.IgnoredFiles;
import com.thoughtworks.go.config.materials.ScmMaterialConfig;
import com.thoughtworks.go.config.rules.Allow;
import com.thoughtworks.go.config.rules.Rules;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.security.CryptoException;
import com.thoughtworks.go.security.GoCipher;
import com.thoughtworks.go.util.ReflectionUtil;
import com.thoughtworks.go.util.command.UrlArgument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.thoughtworks.go.config.materials.AbstractMaterialConfig.MATERIAL_NAME;
import static com.thoughtworks.go.config.materials.ScmMaterialConfig.FOLDER;
import static com.thoughtworks.go.config.materials.ScmMaterialConfig.URL;
import static com.thoughtworks.go.config.rules.SupportedEntity.PIPELINE_GROUP;
import static com.thoughtworks.go.helper.PipelineConfigMother.createGroup;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.*;

class TfsMaterialConfigTest {
    private TfsMaterialConfig tfsMaterialConfig;

    @BeforeEach
    void setUp() {
        tfsMaterialConfig = new TfsMaterialConfig(new GoCipher(), null, "loser", "some_domain", "passwd", "walk_this_path");
    }

    @Test
    void shouldSetConfigAttributes() {
        TfsMaterialConfig tfsMaterialConfig = new TfsMaterialConfig(new GoCipher(), new UrlArgument("http://10.4.4.101:8080/tfs/Sample"), "loser", "some_domain", "passwd", "walk_this_path");

        Map<String, String> map = new HashMap<>();
        map.put(URL, "http://foo:8080/tfs/HelloWorld");
        map.put(ScmMaterialConfig.USERNAME, "boozer");
        map.put(ScmMaterialConfig.PASSWORD, "secret");
        map.put(FOLDER, "folder");
        map.put(ScmMaterialConfig.AUTO_UPDATE, "0");
        map.put(ScmMaterialConfig.FILTER, "/root,/**/*.help");
        map.put(MATERIAL_NAME, "my-tfs-material-name");
        map.put(TfsMaterialConfig.PROJECT_PATH, "/useless/project");
        map.put(TfsMaterialConfig.DOMAIN, "CORPORATE");

        tfsMaterialConfig.setConfigAttributes(map);
        TfsMaterialConfig newTfsMaterialConfig = new TfsMaterialConfig(new GoCipher(), new UrlArgument("http://foo:8080/tfs/HelloWorld"), "boozer", "CORPORATE", "secret", "/useless/project");
        newTfsMaterialConfig.setName(new CaseInsensitiveString("my-tfs-material-name"));
        newTfsMaterialConfig.setFolder("folder");

        assertThat(tfsMaterialConfig).isEqualTo(newTfsMaterialConfig);
        assertThat(tfsMaterialConfig.getPassword()).isEqualTo("passwd");
        assertThat(tfsMaterialConfig.isAutoUpdate()).isFalse();
        assertThat(tfsMaterialConfig.getDomain()).isEqualTo("CORPORATE");

        assertThat(tfsMaterialConfig.getName()).isEqualTo(new CaseInsensitiveString("my-tfs-material-name"));
        assertThat(tfsMaterialConfig.filter()).isEqualTo(new Filter(new IgnoredFiles("/root"), new IgnoredFiles("/**/*.help")));
    }

    @Test
    void shouldDefaultDomainToEmptyStringWhenNothingIsSet() {
        TfsMaterialConfig tfsMaterialConfig = new TfsMaterialConfig(mock(GoCipher.class));
        assertThat(tfsMaterialConfig.getDomain()).isEqualTo("");
    }

    @Test
    void setConfigAttributes_shouldUpdatePasswordWhenPasswordChangedBooleanChanged() throws Exception {
        TfsMaterialConfig tfsMaterialConfig = new TfsMaterialConfig(new GoCipher(), new UrlArgument("http://10.4.4.101:8080/tfs/Sample"), "loser", "CORPORATE", "passwd", "walk_this_path");
        Map<String, String> map = new HashMap<>();
        map.put(TfsMaterialConfig.PASSWORD, "secret");
        map.put(TfsMaterialConfig.PASSWORD_CHANGED, "1");

        tfsMaterialConfig.setConfigAttributes(map);

        tfsMaterialConfig.setConfigAttributes(map);
        assertThat(ReflectionUtil.getField(tfsMaterialConfig, "password")).isNull();
        assertThat(tfsMaterialConfig.getPassword()).isEqualTo("secret");
        assertThat(tfsMaterialConfig.getEncryptedPassword()).isEqualTo(new GoCipher().encrypt("secret"));

        //Dont change
        map.put(TfsMaterialConfig.PASSWORD, "Hehehe");
        map.put(TfsMaterialConfig.PASSWORD_CHANGED, "0");
        tfsMaterialConfig.setConfigAttributes(map);

        assertThat(ReflectionUtil.getField(tfsMaterialConfig, "password")).isNull();
        assertThat(tfsMaterialConfig.getPassword()).isEqualTo("secret");
        assertThat(tfsMaterialConfig.getEncryptedPassword()).isEqualTo(new GoCipher().encrypt("secret"));

        map.put(TfsMaterialConfig.PASSWORD, "");
        map.put(TfsMaterialConfig.PASSWORD_CHANGED, "1");
        tfsMaterialConfig.setConfigAttributes(map);

        assertThat(tfsMaterialConfig.getPassword()).isNull();
        assertThat(tfsMaterialConfig.getEncryptedPassword()).isNull();
    }

    @Nested
    class Validate {

        @Test
        void shouldEnsureMandatoryFieldsAreNotBlank() {
            TfsMaterialConfig tfsMaterialConfig = new TfsMaterialConfig(new GoCipher(), new UrlArgument(""), "", "CORPORATE", "", "");

            tfsMaterialConfig.validate(new ConfigSaveValidationContext(null));

            assertThat(tfsMaterialConfig.errors().on(URL)).isEqualTo("URL cannot be blank");
            assertThat(tfsMaterialConfig.errors().on(TfsMaterialConfig.USERNAME)).isEqualTo("Username cannot be blank");
            assertThat(tfsMaterialConfig.errors().on(TfsMaterialConfig.PROJECT_PATH)).isEqualTo("Project Path cannot be blank");
        }

        @Test
        void shouldEnsureMaterialNameIsValid() {
            TfsMaterialConfig tfsMaterialConfig = new TfsMaterialConfig(new GoCipher(), new UrlArgument("http://10.4.4.101:8080/tfs/Sample"), "loser", "CORPORATE", "passwd", "walk_this_path");

            tfsMaterialConfig.validate(new ConfigSaveValidationContext(null));

            assertThat(tfsMaterialConfig.errors().on(MATERIAL_NAME)).isNull();
            tfsMaterialConfig.setName(new CaseInsensitiveString(".bad-name-with-dot"));
            tfsMaterialConfig.validate(new ConfigSaveValidationContext(null));
            assertThat(tfsMaterialConfig.errors().on(MATERIAL_NAME)).isEqualTo("Invalid material name '.bad-name-with-dot'. This must be alphanumeric and can contain underscores and periods (however, it cannot start with a period). The maximum allowed length is 255 characters.");
        }

        @Test
        void shouldEnsureDestFilePathIsValid() {
            TfsMaterialConfig tfsMaterialConfig = new TfsMaterialConfig(new GoCipher(), new UrlArgument("http://10.4.4.101:8080/tfs/Sample"), "loser", "CORPORATE", "passwd", "walk_this_path");
            tfsMaterialConfig.setConfigAttributes(Collections.singletonMap(FOLDER, "../a"));

            tfsMaterialConfig.validate(new ConfigSaveValidationContext(null));

            assertThat(tfsMaterialConfig.errors().on(FOLDER)).isEqualTo("Dest folder '../a' is not valid. It must be a sub-directory of the working folder.");
        }

        @Test
        void shouldEnsureUrlIsNotBlank() {
            tfsMaterialConfig.setUrl("");
            tfsMaterialConfig.validate(new ConfigSaveValidationContext(null));

            assertThat(tfsMaterialConfig.errors().on(ScmMaterialConfig.URL)).isEqualTo("URL cannot be blank");
        }

        @Test
        void shouldEnsureUrlIsNotNull() {
            tfsMaterialConfig.setUrl(null);

            tfsMaterialConfig.validate(new ConfigSaveValidationContext(null));

            assertThat(tfsMaterialConfig.errors().on(URL)).isEqualTo("URL cannot be blank");
        }
    }

    @Test
    void shouldEncryptTfsPasswordAndMarkPasswordAsNull() throws Exception {
        GoCipher mockGoCipher = mock(GoCipher.class);
        when(mockGoCipher.encrypt("password")).thenReturn("encrypted");
        when(mockGoCipher.maybeReEncryptForPostConstructWithoutExceptions("encrypted")).thenReturn("encrypted");

        TfsMaterialConfig materialConfig = new TfsMaterialConfig(mockGoCipher, new UrlArgument("http://10.4.4.101:8080/tfs/Sample"), "loser", "CORPORATE", "password", "walk_this_path");
        materialConfig.ensureEncrypted();

        assertThat(materialConfig.getPassword()).isNull();
        assertThat(materialConfig.getEncryptedPassword()).isEqualTo("encrypted");
    }

    @Test
    void shouldDecryptTfsPassword() throws Exception {
        GoCipher mockGoCipher = mock(GoCipher.class);
        when(mockGoCipher.decrypt("encrypted")).thenReturn("password");
        when(mockGoCipher.maybeReEncryptForPostConstructWithoutExceptions("encrypted")).thenReturn("encrypted");

        TfsMaterialConfig materialConfig = new TfsMaterialConfig(mockGoCipher, new UrlArgument("http://10.4.4.101:8080/tfs/Sample"), "loser", "CORPORATE", "secret", "walk_this_path");
        ReflectionUtil.setField(materialConfig, "encryptedPassword", "encrypted");

        materialConfig.ensureEncrypted();
        assertThat(materialConfig.getPassword()).isEqualTo("password");
    }

    @Test
    void shouldNotDecryptTfsPasswordIfPasswordIsNotNull() throws Exception {
        GoCipher mockGoCipher = mock(GoCipher.class);
        when(mockGoCipher.encrypt("password")).thenReturn("encrypted");
        when(mockGoCipher.decrypt("encrypted")).thenReturn("password");

        TfsMaterialConfig materialConfig = new TfsMaterialConfig(mockGoCipher, new UrlArgument("http://10.4.4.101:8080/tfs/Sample"), "loser", "CORPORATE", "password", "walk_this_path");
        materialConfig.ensureEncrypted();
        when(mockGoCipher.encrypt("new_password")).thenReturn("new_encrypted");
        materialConfig.setPassword("new_password");
        when(mockGoCipher.decrypt("new_encrypted")).thenReturn("new_password");

        assertThat(materialConfig.getPassword()).isEqualTo("new_password");
    }

    @Test
    void shouldErrorOutIfDecryptionFails() throws CryptoException {
        GoCipher mockGoCipher = mock(GoCipher.class);
        String fakeCipherText = "fake cipher text";
        when(mockGoCipher.decrypt(fakeCipherText)).thenThrow(new CryptoException("exception"));
        TfsMaterialConfig materialConfig = new TfsMaterialConfig(mockGoCipher, new UrlArgument("http://10.4.4.101:8080/tfs/Sample"), "loser", "CORPORATE", "passwd", "walk_this_path");
        ReflectionUtil.setField(materialConfig, "encryptedPassword", fakeCipherText);
        try {
            materialConfig.getPassword();
            fail("Should have thrown up");
        } catch (Exception e) {
            assertThat(e.getMessage()).isEqualTo("Could not decrypt the password to get the real password");
        }
    }

    @Test
    void shouldErrorOutIfEncryptionFails() throws Exception {
        GoCipher mockGoCipher = mock(GoCipher.class);
        when(mockGoCipher.encrypt("password")).thenThrow(new CryptoException("exception"));
        try {
            new TfsMaterialConfig(mockGoCipher, new UrlArgument("http://10.4.4.101:8080/tfs/Sample"), "loser", "CORPORATE", "password", "walk_this_path");
            fail("Should have thrown up");
        } catch (Exception e) {
            assertThat(e.getMessage()).isEqualTo("Password encryption failed. Please verify your cipher key.");
        }
    }

    @Test
    void shouldReturnTheUrl() {
        String url = "git@github.com/my/repo";
        TfsMaterialConfig config = new TfsMaterialConfig();

        config.setUrl(url);

        assertThat(config.getUrl()).isEqualTo(url);
    }

    @Test
    void shouldReturnNullIfUrlForMaterialNotSpecified() {
        TfsMaterialConfig config = new TfsMaterialConfig();

        assertThat(config.getUrl()).isNull();
    }

    @Test
    void shouldHandleNullWhenSettingUrlForAMaterial() {
        TfsMaterialConfig config = new TfsMaterialConfig();

        config.setUrl(null);

        assertThat(config.getUrl()).isNull();
    }

    @Nested
    class ValidateTree {
        @BeforeEach
        void setUp() {
            tfsMaterialConfig.setUrl("/foo/bar");
        }

        @Test
        void shouldCallValidate() {
            final MaterialConfig materialConfig = spy(tfsMaterialConfig);
            final ValidationContext validationContext = mockValidationContextForSecretParams();

            materialConfig.validateTree(validationContext);

            verify(materialConfig).validate(validationContext);
        }

        @Test
        void shouldFailIfEncryptedPasswordIsIncorrect() {
            tfsMaterialConfig.setEncryptedPassword("encryptedPassword");

            final boolean validationResult = tfsMaterialConfig.validateTree(new ConfigSaveValidationContext(null));

            assertThat(validationResult).isFalse();
            assertThat(tfsMaterialConfig.errors().on("encryptedPassword")).isEqualTo("Encrypted password value for TfsMaterial with url '/foo/bar' is invalid. This usually happens when the cipher text is modified to have an invalid value.");
        }

        @Test
        void shouldPassIfPasswordIsNotSpecifiedAsSecretParams() {
            tfsMaterialConfig.setPassword("badger");

            assertThat(tfsMaterialConfig.validateTree(null)).isTrue();
            assertThat(tfsMaterialConfig.errors().getAll()).isEmpty();
        }

        @Test
        void shouldFailIfSecretConfigForPasswordSpecifiedAsSecretParamDoesNotExist() {
            final ValidationContext validationContext = mockValidationContextForSecretParams();
            tfsMaterialConfig.setPassword("{{SECRET:[secret_config_id][password]}}");

            assertThat(tfsMaterialConfig.validateTree(validationContext)).isFalse();
            assertThat(tfsMaterialConfig.errors().on("encryptedPassword")).isEqualTo("Secret config with ids `secret_config_id` does not exist.");
        }


        @Test
        void shouldFailIfSecretConfigCannotBeUsedInPipelineGroupWhereCurrentMaterialIsDefined() {
            tfsMaterialConfig.setUserName("bob");
            tfsMaterialConfig.setPassword("{{SECRET:[secret_config_id][pass]}}");
            final Rules directives = new Rules(new Allow("refer", PIPELINE_GROUP.getType(), "group_2"));
            final SecretConfig secretConfig = new SecretConfig("secret_config_id", "cd.go.secret.file", directives);
            final ValidationContext validationContext = mockValidationContextForSecretParams(secretConfig);
            when(validationContext.getPipelineGroup()).thenReturn(createGroup("group_1", "up42"));

            assertThat(tfsMaterialConfig.validateTree(validationContext)).isFalse();

            assertThat(tfsMaterialConfig.errors().get("encryptedPassword"))
                    .contains("Secret config with ids `secret_config_id` is not allowed to use in `pipelines` with name `group_1`.");
        }

        @Test
        void shouldPassIfSecretConfigCanBeReferredInPipelineGroupWhereCurrentMaterialIsDefined() {
            tfsMaterialConfig.setUserName("bob");
            tfsMaterialConfig.setPassword("{{SECRET:[secret_config_id][pass]}}");
            final Rules directives = new Rules(
                    new Allow("refer", PIPELINE_GROUP.getType(), "group_2"),
                    new Allow("refer", PIPELINE_GROUP.getType(), "group_1")
            );
            final SecretConfig secretConfig = new SecretConfig("secret_config_id", "cd.go.secret.file", directives);
            final ValidationContext validationContext = mockValidationContextForSecretParams(secretConfig);
            when(validationContext.getPipelineGroup()).thenReturn(createGroup("group_1", "up42"));

            assertThat(tfsMaterialConfig.validateTree(validationContext)).isTrue();

            assertThat(tfsMaterialConfig.errors().getAll()).isEmpty();
        }
    }

    private ValidationContext mockValidationContextForSecretParams(SecretConfig... secretConfigs) {
        final ValidationContext validationContext = mock(ValidationContext.class);
        final CruiseConfig cruiseConfig = mock(CruiseConfig.class);
        when(validationContext.getCruiseConfig()).thenReturn(cruiseConfig);
        when(cruiseConfig.getSecretConfigs()).thenReturn(new SecretConfigs(secretConfigs));
        return validationContext;
    }
}
