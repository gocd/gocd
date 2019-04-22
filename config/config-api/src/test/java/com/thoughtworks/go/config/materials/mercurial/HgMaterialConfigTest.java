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

package com.thoughtworks.go.config.materials.mercurial;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.materials.AbstractMaterialConfig;
import com.thoughtworks.go.config.materials.Filter;
import com.thoughtworks.go.config.materials.IgnoredFiles;
import com.thoughtworks.go.config.materials.ScmMaterialConfig;
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
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HgMaterialConfigTest {
    private HgMaterialConfig hgMaterialConfig;

    @BeforeEach
    void setUp() {
        hgMaterialConfig = new HgMaterialConfig("", null);
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
    class ValidateURL {
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
        void shouldFailValidationIfMaterialURLHasSecretParamsConfiguredOtherThanForUsernamePassword() {
            final ValidationContext validationContext = mockValidationContextForSecretParams();
            hgMaterialConfig.setUrl("https://user:pass@{{SECRET:[secret_config_id][hostname]}}/foo.git");

            assertThat(hgMaterialConfig.validateTree(validationContext)).isFalse();
            assertThat(hgMaterialConfig.errors().on("url")).isEqualTo("Only password can be specified as secret params");
        }

        @Test
        void shouldFailIfSecretParamConfiguredWithSecretConfigIdWhichDoesNotExist() {
            final ValidationContext validationContext = mockValidationContextForSecretParams();
            hgMaterialConfig.setUrl("https://username:{{SECRET:[secret_config_id][pass]}}@host/foo.git");

            assertThat(hgMaterialConfig.validateTree(validationContext)).isFalse();
            assertThat(hgMaterialConfig.errors().on("url")).isEqualTo("Secret config with ids `secret_config_id` does not exist.");
        }

        @Test
        void shouldNotFailIfSecretConfigWithIdPresentForConfiguredSecretParams() {
            final SecretConfig secretConfig = new SecretConfig("secret_config_id", "cd.go.secret.file");
            final ValidationContext validationContext = mockValidationContextForSecretParams(secretConfig);
            hgMaterialConfig.setUrl("https://username:{{SECRET:[secret_config_id][username]}}@host/foo.git");

            assertThat(hgMaterialConfig.validateTree(validationContext)).isTrue();
            assertThat(hgMaterialConfig.errors().getAll()).isEmpty();
        }
    }

    private ValidationContext mockValidationContextForSecretParams(SecretConfig... secretConfigs) {
        final ValidationContext validationContext = mock(ValidationContext.class);
        final CruiseConfig cruiseConfig = mock(CruiseConfig.class);
        when(validationContext.getCruiseConfig()).thenReturn(cruiseConfig);
        when(cruiseConfig.getSecretConfigs()).thenReturn(new SecretConfigs(secretConfigs));
        return validationContext;
    }

    @Nested
    class FingerPrintShouldNotChangeBecauseOfUrlDenormalize {
        @Test
        void shouldNotChangeFingerprintForHttpUrlWithCredentials() {
            HgMaterialConfig migratedConfig = new HgMaterialConfig("http://github.com/gocd/gocd", "my-branch");
            migratedConfig.setUserName("bobfoo@example.com");
            migratedConfig.setPassword("p@ssw&rd:");
            assertThat(migratedConfig.getFingerprint()).isEqualTo("ff407f3ab9623d2a87c7c7037388863e30711ccda837fee54685ae490cea9b1b");

        }

        @Test
        void shouldNotChangeFingerprintForHttpsUrlWithCredentials() {
            HgMaterialConfig migratedConfig = new HgMaterialConfig("https://github.com/gocd/gocd", "my-branch");
            migratedConfig.setUserName("bobfoo@example.com");
            migratedConfig.setPassword("p@ssw&rd:");
            assertThat(migratedConfig.getFingerprint()).isEqualTo("0128b4baa42f594edebf0aa8b03accb775437f87e24c091df43f7089d9273379");

        }

        @Test
        void shouldNotChangeFingerprintForHttpUrlWithUsername() {
            HgMaterialConfig migratedConfig = new HgMaterialConfig("https://github.com/gocd/gocd", "my-branch");
            migratedConfig.setUserName("some-hex-key");

            assertThat(migratedConfig.getFingerprint()).isEqualTo("740752da427d67093b8e41d2484d0408caa7a6e6aa39df670789a35d36a1c4fd");
        }

        @Test
        void shouldChangeFingerprintForHttpUrlWithUsernameAndColonWithNoPassword() {
            HgMaterialConfig config = new HgMaterialConfig("https://some-hex-key:@github.com/gocd/gocd", "my-branch");
            assertThat(config.getFingerprint()).isNotEqualTo("2a8d3901b89ab34c75b5a5a0ce2fccaf1deef76e30e9534c9770e123534813ba");
            assertThat(config.getFingerprint()).isEqualTo("740752da427d67093b8e41d2484d0408caa7a6e6aa39df670789a35d36a1c4fd");

            HgMaterialConfig migratedConfig = new HgMaterialConfig("https://github.com/gocd/gocd", "my-branch");
            migratedConfig.setUserName("some-hex-key");

            assertThat(config.getFingerprint()).isEqualTo(migratedConfig.getFingerprint());
        }

        @Test
        void shouldNotChangeFingerprintForHttpUrlWithPassword() {
            HgMaterialConfig config = new HgMaterialConfig("https://:some-hex-key@github.com/gocd/gocd", "my-branch");
            assertThat(config.getFingerprint()).isEqualTo("a8fa1c0729bd9687f31493e97281339cc8987779264e1f59d741be264c738f53");

            HgMaterialConfig migratedConfig = new HgMaterialConfig("https://github.com/gocd/gocd", "my-branch");
            migratedConfig.setPassword("some-hex-key");

            assertThat(config.getFingerprint()).isEqualTo(migratedConfig.getFingerprint());
        }
    }
}
