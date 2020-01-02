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
package com.thoughtworks.go.config.materials.git;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.materials.*;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.security.GoCipher;
import com.thoughtworks.go.util.ReflectionUtil;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.thoughtworks.go.helper.MaterialConfigsMother.git;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class GitMaterialConfigTest {
    @Test
    void shouldBePasswordAwareMaterial() {
        assertThat(git()).isInstanceOf(PasswordAwareMaterial.class);
    }

    @Test
    void shouldSetConfigAttributes() {
        GitMaterialConfig gitMaterialConfig = git("");

        Map<String, String> map = new HashMap<>();
        map.put(GitMaterialConfig.URL, "url");
        map.put(GitMaterialConfig.BRANCH, "some-branch");
        map.put(GitMaterialConfig.SHALLOW_CLONE, "true");
        map.put(ScmMaterialConfig.FOLDER, "folder");
        map.put(ScmMaterialConfig.AUTO_UPDATE, null);
        map.put(ScmMaterialConfig.FILTER, "/root,/**/*.help");
        map.put(AbstractMaterialConfig.MATERIAL_NAME, "material-name");

        gitMaterialConfig.setConfigAttributes(map);

        assertThat(gitMaterialConfig.getUrl()).isEqualTo("url");
        assertThat(gitMaterialConfig.getFolder()).isEqualTo("folder");
        assertThat(gitMaterialConfig.getBranch()).isEqualTo("some-branch");
        assertThat(gitMaterialConfig.getName()).isEqualTo(new CaseInsensitiveString("material-name"));
        assertThat(gitMaterialConfig.isAutoUpdate()).isFalse();
        assertThat(gitMaterialConfig.isShallowClone()).isTrue();
        assertThat(gitMaterialConfig.filter()).isEqualTo(new Filter(new IgnoredFiles("/root"), new IgnoredFiles("/**/*.help")));
    }

    @Test
    void setConfigAttributes_shouldUpdatePasswordWhenPasswordChangedBooleanChanged() throws Exception {
        GitMaterialConfig gitMaterialConfig = git("");
        Map<String, String> map = new HashMap<>();
        map.put(GitMaterialConfig.PASSWORD, "secret");
        map.put(GitMaterialConfig.PASSWORD_CHANGED, "1");

        gitMaterialConfig.setConfigAttributes(map);
        assertThat(ReflectionUtil.getField(gitMaterialConfig, "password")).isNull();
        assertThat(gitMaterialConfig.getPassword()).isEqualTo("secret");
        assertThat(gitMaterialConfig.getEncryptedPassword()).isEqualTo(new GoCipher().encrypt("secret"));

        //Dont change
        map.put(GitMaterialConfig.PASSWORD, "Hehehe");
        map.put(GitMaterialConfig.PASSWORD_CHANGED, "0");
        gitMaterialConfig.setConfigAttributes(map);

        assertThat(ReflectionUtil.getField(gitMaterialConfig, "password")).isNull();
        assertThat(gitMaterialConfig.getPassword()).isEqualTo("secret");
        assertThat(gitMaterialConfig.getEncryptedPassword()).isEqualTo(new GoCipher().encrypt("secret"));

        map.put(GitMaterialConfig.PASSWORD, "");
        map.put(GitMaterialConfig.PASSWORD_CHANGED, "1");
        gitMaterialConfig.setConfigAttributes(map);

        assertThat(gitMaterialConfig.getPassword()).isNull();
        assertThat(gitMaterialConfig.getEncryptedPassword()).isNull();
    }

    @Test
    void byDefaultShallowCloneShouldBeOff() {
        assertThat(git("http://url", "foo").isShallowClone()).isFalse();
        assertThat(git("http://url", "foo", false).isShallowClone()).isFalse();
        assertThat(git("http://url", "foo", null).isShallowClone()).isFalse();
        assertThat(git("http://url", "foo", true).isShallowClone()).isTrue();
    }

    @Test
    void shouldReturnIfAttributeMapIsNull() {
        GitMaterialConfig gitMaterialConfig = git("");
        gitMaterialConfig.setConfigAttributes(null);
        assertThat(gitMaterialConfig).isEqualTo(git(""));
    }

    @Test
    void shouldReturnTheUrl() {
        String url = "git@github.com/my/repo";
        GitMaterialConfig config = git(url);

        assertThat(config.getUrl()).isEqualTo(url);
    }

    @Test
    void shouldReturnNullIfUrlForMaterialNotSpecified() {
        GitMaterialConfig config = git();

        assertThat(config.getUrl()).isNull();
    }

    @Test
    void shouldSetUrlForAMaterial() {
        String url = "git@github.com/my/repo";
        GitMaterialConfig config = git();

        config.setUrl(url);

        assertThat(config.getUrl()).isEqualTo(url);
    }

    @Test
    void shouldHandleNullWhenSettingUrlForAMaterial() {
        GitMaterialConfig config = git();

        config.setUrl(null);

        assertThat(config.getUrl()).isNull();
    }

    @Test
    void shouldHandleNullUrlAtTheTimeOfGitMaterialConfigCreation() {
        GitMaterialConfig config = git(null);

        assertThat(config.getUrl()).isNull();
    }

    @Test
    void shouldHandleNullBranchWhileSettingConfigAttributes() {
        GitMaterialConfig gitMaterialConfig = git("http://url", "foo");
        gitMaterialConfig.setConfigAttributes(Collections.singletonMap(GitMaterialConfig.BRANCH, null));
        assertThat(gitMaterialConfig.getBranch()).isEqualTo("master");
    }

    @Test
    void shouldHandleEmptyBranchWhileSettingConfigAttributes() {
        GitMaterialConfig gitMaterialConfig = git("http://url", "foo");
        gitMaterialConfig.setConfigAttributes(Collections.singletonMap(GitMaterialConfig.BRANCH, "     "));
        assertThat(gitMaterialConfig.getBranch()).isEqualTo("master");
    }

    @Nested
    class Validate {
        @Test
        void shouldEnsureUrlIsNotBlank() {
            GitMaterialConfig gitMaterialConfig = git("");
            gitMaterialConfig.validate(new ConfigSaveValidationContext(null));
            assertThat(gitMaterialConfig.errors().on(GitMaterialConfig.URL)).isEqualTo("URL cannot be blank");
        }

        @Test
        void shouldEnsureUserNameIsNotProvidedInBothUrlAsWellAsAttributes() {
            GitMaterialConfig gitMaterialConfig = git("http://bob:pass@example.com");
            gitMaterialConfig.setUserName("user");

            gitMaterialConfig.validate(new ConfigSaveValidationContext(null));

            assertThat(gitMaterialConfig.errors().on(GitMaterialConfig.URL)).isEqualTo("Ambiguous credentials, must be provided either in URL or as attributes.");
        }

        @Test
        void shouldEnsurePasswordIsNotProvidedInBothUrlAsWellAsAttributes() {
            GitMaterialConfig gitMaterialConfig = git("http://bob:pass@example.com");
            gitMaterialConfig.setPassword("pass");

            gitMaterialConfig.validate(new ConfigSaveValidationContext(null));

            assertThat(gitMaterialConfig.errors().on(GitMaterialConfig.URL)).isEqualTo("Ambiguous credentials, must be provided either in URL or as attributes.");
        }

        @Test
        void shouldIgnoreInvalidUrlForCredentialValidation() {
            GitMaterialConfig gitMaterialConfig = git("http://bob:pass@example.com##dobule-hash-is-invalid-in-url");
            gitMaterialConfig.setUserName("user");
            gitMaterialConfig.setPassword("password");

            gitMaterialConfig.validate(new ConfigSaveValidationContext(null));

            assertThat(gitMaterialConfig.errors().on(GitMaterialConfig.URL)).isNull();
        }

        @Test
        void shouldBeValidWhenCredentialsAreProvidedOnlyInUrl() {
            GitMaterialConfig gitMaterialConfig = git("http://bob:pass@example.com");

            gitMaterialConfig.validate(new ConfigSaveValidationContext(null));

            assertThat(gitMaterialConfig.errors().on(GitMaterialConfig.URL)).isNull();
        }

        @Test
        void shouldBeValidWhenCredentialsAreProvidedOnlyAsAttributes() {
            GitMaterialConfig gitMaterialConfig = git("http://example.com");
            gitMaterialConfig.setUserName("bob");
            gitMaterialConfig.setPassword("badger");

            gitMaterialConfig.validate(new ConfigSaveValidationContext(null));

            assertThat(gitMaterialConfig.errors().on(GitMaterialConfig.URL)).isNull();
        }
    }

    @Nested
    class ValidateTree {
        @Test
        void shouldCallValidate() {
            final MaterialConfig materialConfig = spy(git("https://example.repo"));
            final ValidationContext validationContext = mockValidationContextForSecretParams();

            materialConfig.validateTree(validationContext);

            verify(materialConfig).validate(validationContext);
        }

        @Test
        void shouldFailIfEncryptedPasswordIsIncorrect() {
            GitMaterialConfig gitMaterialConfig = git("http://example.com");
            gitMaterialConfig.setEncryptedPassword("encryptedPassword");

            final boolean validationResult = gitMaterialConfig.validateTree(new ConfigSaveValidationContext(null));

            assertThat(validationResult).isFalse();
            assertThat(gitMaterialConfig.errors().on("encryptedPassword"))
                    .isEqualTo("Encrypted password value for GitMaterial with url 'http://example.com' is invalid. This usually happens when the cipher text is modified to have an invalid value.");
        }
    }

    @Nested
    class Equals {
        @Test
        void shouldBeEqualIfObjectsHaveSameUrlBranchAndSubModuleFolder() {
            final GitMaterialConfig material_1 = git("http://example.com", "master");
            material_1.setUserName("bob");
            material_1.setSubmoduleFolder("/var/lib/git");

            final GitMaterialConfig material_2 = git("http://example.com", "master");
            material_2.setUserName("alice");
            material_2.setSubmoduleFolder("/var/lib/git");

            assertThat(material_1.equals(material_2)).isTrue();
        }
    }

    @Nested
    class Fingerprint {
        @Test
        void shouldGenerateFingerprintForGivenMaterialUrlAndBranch() {
            GitMaterialConfig gitMaterialConfig = git("https://bob:pass@github.com/gocd", "feature");

            assertThat(gitMaterialConfig.getFingerprint()).isEqualTo("755da7fb7415c8674bdf5f8a4ba48fc3e071e5de429b1308ccf8949d215bdb08");
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
