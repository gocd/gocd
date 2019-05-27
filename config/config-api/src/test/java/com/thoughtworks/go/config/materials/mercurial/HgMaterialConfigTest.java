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
package com.thoughtworks.go.config.materials.mercurial;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.materials.*;
import com.thoughtworks.go.config.rules.Allow;
import com.thoughtworks.go.config.rules.Rules;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.security.GoCipher;
import com.thoughtworks.go.util.ReflectionUtil;
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
import static org.mockito.Mockito.*;

class HgMaterialConfigTest {
    private HgMaterialConfig hgMaterialConfig;

    @BeforeEach
    void setUp() {
        hgMaterialConfig = new HgMaterialConfig("", null);
    }

    @Test
    void shouldBePasswordAwareMaterial() {
        assertThat(hgMaterialConfig).isInstanceOf(PasswordAwareMaterial.class);
    }

    @Test
    void shouldSetConfigAttributes() {
        HgMaterialConfig hgMaterialConfig = new HgMaterialConfig("", null);

        Map<String, String> map = new HashMap<>();
        map.put(HgMaterialConfig.URL, "url");
        map.put(ScmMaterialConfig.FOLDER, "folder");
        map.put(ScmMaterialConfig.AUTO_UPDATE, "0");
        map.put(ScmMaterialConfig.FILTER, "/root,/**/*.help");
        map.put(AbstractMaterialConfig.MATERIAL_NAME, "material-name");

        hgMaterialConfig.setConfigAttributes(map);

        assertThat(hgMaterialConfig.getUrl()).isEqualTo("url");
        assertThat(hgMaterialConfig.getFolder()).isEqualTo("folder");
        assertThat(hgMaterialConfig.getName()).isEqualTo(new CaseInsensitiveString("material-name"));
        assertThat(hgMaterialConfig.isAutoUpdate()).isFalse();
        assertThat(hgMaterialConfig.filter()).isEqualTo(new Filter(new IgnoredFiles("/root"), new IgnoredFiles("/**/*.help")));
    }

    @Test
    void setConfigAttributes_shouldUpdatePasswordWhenPasswordChangedBooleanChanged() throws Exception {
        HgMaterialConfig hgMaterialConfig = new HgMaterialConfig();
        Map<String, String> map = new HashMap<>();
        map.put(HgMaterialConfig.PASSWORD, "secret");
        map.put(HgMaterialConfig.PASSWORD_CHANGED, "1");

        hgMaterialConfig.setConfigAttributes(map);
        assertThat(ReflectionUtil.getField(hgMaterialConfig, "password")).isNull();
        assertThat(hgMaterialConfig.getPassword()).isEqualTo("secret");
        assertThat(hgMaterialConfig.getEncryptedPassword()).isEqualTo(new GoCipher().encrypt("secret"));

        //Dont change
        map.put(HgMaterialConfig.PASSWORD, "Hehehe");
        map.put(HgMaterialConfig.PASSWORD_CHANGED, "0");
        hgMaterialConfig.setConfigAttributes(map);

        assertThat(ReflectionUtil.getField(hgMaterialConfig, "password")).isNull();
        assertThat(hgMaterialConfig.getPassword()).isEqualTo("secret");
        assertThat(hgMaterialConfig.getEncryptedPassword()).isEqualTo(new GoCipher().encrypt("secret"));

        map.put(HgMaterialConfig.PASSWORD, "");
        map.put(HgMaterialConfig.PASSWORD_CHANGED, "1");
        hgMaterialConfig.setConfigAttributes(map);

        assertThat(hgMaterialConfig.getPassword()).isNull();
        assertThat(hgMaterialConfig.getEncryptedPassword()).isNull();
    }


    @Test
    void validate_shouldEnsureUrlIsNotBlank() {
        HgMaterialConfig hgMaterialConfig = new HgMaterialConfig("", null);
        hgMaterialConfig.validate(new ConfigSaveValidationContext(null));
        assertThat(hgMaterialConfig.errors().on(HgMaterialConfig.URL)).isEqualTo("URL cannot be blank");
    }

    @Test
    void shouldReturnIfAttributeMapIsNull() {
        HgMaterialConfig hgMaterialConfig = new HgMaterialConfig("", null);

        hgMaterialConfig.setConfigAttributes(null);

        assertThat(hgMaterialConfig).isEqualTo(new HgMaterialConfig("", null));
    }

    @Test
    void shouldReturnTheUrl() {
        String url = "git@github.com/my/repo";
        HgMaterialConfig config = new HgMaterialConfig(url, null);

        assertThat(config.getUrl()).isEqualTo(url);
    }

    @Test
    void shouldReturnNullIfUrlForMaterialNotSpecified() {
        HgMaterialConfig config = new HgMaterialConfig();

        assertThat(config.getUrl()).isNull();
    }

    @Test
    void shouldSetUrlForAMaterial() {
        String url = "git@github.com/my/repo";
        HgMaterialConfig config = new HgMaterialConfig();

        config.setUrl(url);

        assertThat(config.getUrl()).isEqualTo(url);
    }

    @Test
    void shouldHandleNullWhenSettingUrlForAMaterial() {
        HgMaterialConfig config = new HgMaterialConfig();

        config.setUrl(null);

        assertThat(config.getUrl()).isNull();
    }

    @Nested
    class Equals {
        @Test
        void shouldBeEqualIfObjectsHaveSameUrlBranch() {
            final HgMaterialConfig material_1 = new HgMaterialConfig("http://example.com", "master");
            material_1.setUserName("bob");
            material_1.setBranchAttribute("feature");

            final HgMaterialConfig material_2 = new HgMaterialConfig("http://example.com", "master");
            material_2.setUserName("alice");
            material_2.setBranchAttribute("feature");

            assertThat(material_1.equals(material_2)).isTrue();
        }
    }

    @Nested
    class Fingerprint {
        @Test
        void shouldGenerateFingerprintForGivenMaterialUrl() {
            HgMaterialConfig hgMaterialConfig = new HgMaterialConfig("https://bob:pass@github.com/gocd#feature", "dest");

            assertThat(hgMaterialConfig.getFingerprint()).isEqualTo("d84d91f37da0367a9bd89fff0d48638f5c1bf993d637735ec26f13c21c23da19");
        }

        @Test
        void shouldConsiderBranchWhileGeneratingFingerprint_IfBranchSpecifiedAsAnAttribute() {
            HgMaterialConfig hgMaterialConfig = new HgMaterialConfig("https://bob:pass@github.com/gocd", "dest");
            hgMaterialConfig.setBranchAttribute("feature");

            assertThat(hgMaterialConfig.getFingerprint()).isEqualTo("db13278ed2b804fc5664361103bcea3d7f5106879683085caed4311aa4d2f888");
        }

        @Test
        void branchInUrlShouldGenerateFingerprintWhichIsOtherFromBranchInAttribute() {
            HgMaterialConfig hgMaterialConfigWithBranchInUrl = new HgMaterialConfig("https://github.com/gocd#feature", "dest");

            HgMaterialConfig hgMaterialConfigWithBranchAsAttribute = new HgMaterialConfig("https://github.com/gocd", "dest");
            hgMaterialConfigWithBranchAsAttribute.setBranchAttribute("feature");

            assertThat(hgMaterialConfigWithBranchInUrl.getFingerprint())
                    .isNotEqualTo(hgMaterialConfigWithBranchAsAttribute.getFingerprint());
        }
    }

    @Nested
    class validate {
        @Test
        void shouldEnsureUrlIsNotBlank() {
            hgMaterialConfig.setUrl("");
            hgMaterialConfig.validate(new ConfigSaveValidationContext(null));

            assertThat(hgMaterialConfig.errors().on(ScmMaterialConfig.URL)).isEqualTo("URL cannot be blank");
        }

        @Test
        void shouldEnsureUrlIsNotNull() {
            hgMaterialConfig.setUrl(null);

            hgMaterialConfig.validate(new ConfigSaveValidationContext(null));

            assertThat(hgMaterialConfig.errors().on(URL)).isEqualTo("URL cannot be blank");
        }

        @Test
        void shouldEnsureMaterialNameIsValid() {
            hgMaterialConfig.validate(new ConfigSaveValidationContext(null));
            assertThat(hgMaterialConfig.errors().on(MATERIAL_NAME)).isNull();

            hgMaterialConfig.setName(new CaseInsensitiveString(".bad-name-with-dot"));
            hgMaterialConfig.validate(new ConfigSaveValidationContext(null));
            assertThat(hgMaterialConfig.errors().on(MATERIAL_NAME)).isEqualTo("Invalid material name '.bad-name-with-dot'. This must be alphanumeric and can contain underscores and periods (however, it cannot start with a period). The maximum allowed length is 255 characters.");
        }

        @Test
        void shouldEnsureDestFilePathIsValid() {
            hgMaterialConfig.setConfigAttributes(Collections.singletonMap(FOLDER, "../a"));
            hgMaterialConfig.validate(new ConfigSaveValidationContext(null));
            assertThat(hgMaterialConfig.errors().on(FOLDER)).isEqualTo("Dest folder '../a' is not valid. It must be a sub-directory of the working folder.");
        }

        @Test
        void shouldEnsureUserNameIsNotProvidedInBothUrlAsWellAsAttributes() {
            HgMaterialConfig hgMaterialConfig = new HgMaterialConfig("http://bob:pass@example.com", null);
            hgMaterialConfig.setUserName("user");

            hgMaterialConfig.validate(new ConfigSaveValidationContext(null));

            assertThat(hgMaterialConfig.errors().on(HgMaterialConfig.URL)).isEqualTo("Ambiguous credentials, must be provided either in URL or as attributes.");
        }

        @Test
        void shouldEnsurePasswordIsNotProvidedInBothUrlAsWellAsAttributes() {
            HgMaterialConfig hgMaterialConfig = new HgMaterialConfig("http://bob:pass@example.com", null);
            hgMaterialConfig.setPassword("pass");

            hgMaterialConfig.validate(new ConfigSaveValidationContext(null));

            assertThat(hgMaterialConfig.errors().on(HgMaterialConfig.URL)).isEqualTo("Ambiguous credentials, must be provided either in URL or as attributes.");
        }

        @Test
        void shouldIgnoreInvalidUrlForCredentialValidation() {
            HgMaterialConfig hgMaterialConfig = new HgMaterialConfig("http://bob:pass@example.com##dobule-hash-is-invalid-in-url", null);
            hgMaterialConfig.setUserName("user");
            hgMaterialConfig.setPassword("password");

            hgMaterialConfig.validate(new ConfigSaveValidationContext(null));

            assertThat(hgMaterialConfig.errors().on(HgMaterialConfig.URL)).isNull();
        }

        @Test
        void shouldBeValidWhenCredentialsAreProvidedOnlyInUrl() {
            HgMaterialConfig hgMaterialConfig = new HgMaterialConfig("http://bob:pass@example.com", null);

            hgMaterialConfig.validate(new ConfigSaveValidationContext(null));

            assertThat(hgMaterialConfig.errors().on(HgMaterialConfig.URL)).isNull();
        }

        @Test
        void shouldBeValidWhenCredentialsAreProvidedOnlyAsAttributes() {
            HgMaterialConfig hgMaterialConfig = new HgMaterialConfig("http://example.com", null);
            hgMaterialConfig.setUserName("bob");
            hgMaterialConfig.setPassword("badger");

            hgMaterialConfig.validate(new ConfigSaveValidationContext(null));

            assertThat(hgMaterialConfig.errors().on(HgMaterialConfig.URL)).isNull();
        }

        @Test
        void shouldEnsureBranchIsNotProvidedInBothUrlAsWellAsAttributes() {
            HgMaterialConfig hgMaterialConfig = new HgMaterialConfig("http://bob:pass@example.com#some-branch", null);
            hgMaterialConfig.setBranchAttribute("branch-in-attribute");

            hgMaterialConfig.validate(new ConfigSaveValidationContext(null));

            assertThat(hgMaterialConfig.errors().on(HgMaterialConfig.URL)).isEqualTo("Ambiguous branch, must be provided either in URL or as an attribute.");
        }
    }

    @Nested
    class ValidateTree {
        @Test
        void shouldCallValidate() {
            final MaterialConfig materialConfig = spy(new HgMaterialConfig("https://example.repo", null));
            final ValidationContext validationContext = mockValidationContextForSecretParams();

            materialConfig.validateTree(validationContext);

            verify(materialConfig).validate(validationContext);
        }

        @Test
        void shouldFailIfSecretConfigCannotBeUsedInPipelineGroupWhereCurrentMaterialIsDefined() {
            HgMaterialConfig material = new HgMaterialConfig("https://example.repo", null);
            material.setUserName("bob");
            material.setPassword("{{SECRET:[secret_config_id][pass]}}");
            final Rules directives = new Rules(new Allow("refer", PIPELINE_GROUP.getType(), "group_2"));
            final SecretConfig secretConfig = new SecretConfig("secret_config_id", "cd.go.secret.file", directives);
            final ValidationContext validationContext = mockValidationContextForSecretParams(secretConfig);
            when(validationContext.getPipelineGroup()).thenReturn(createGroup("group_1", "up42"));

            assertThat(material.validateTree(validationContext)).isFalse();

            assertThat(material.errors().get("encryptedPassword"))
                    .contains("Secret config with ids `secret_config_id` is not allowed to use in `pipelines` with name `group_1`.");
        }

        @Test
        void shouldPassIfSecretConfigCanBeReferredInPipelineGroupWhereCurrentMaterialIsDefined() {
            HgMaterialConfig material = new HgMaterialConfig("https://example.repo", null);
            material.setUserName("bob");
            material.setPassword("{{SECRET:[secret_config_id][pass]}}");
            final Rules directives = new Rules(
                    new Allow("refer", PIPELINE_GROUP.getType(), "group_2"),
                    new Allow("refer", PIPELINE_GROUP.getType(), "group_1")
            );
            final SecretConfig secretConfig = new SecretConfig("secret_config_id", "cd.go.secret.file", directives);
            final ValidationContext validationContext = mockValidationContextForSecretParams(secretConfig);
            when(validationContext.getPipelineGroup()).thenReturn(createGroup("group_1", "up42"));

            assertThat(material.validateTree(validationContext)).isTrue();

            assertThat(material.errors().getAll()).isEmpty();
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
