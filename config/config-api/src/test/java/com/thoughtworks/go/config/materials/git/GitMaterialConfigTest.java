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
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GitMaterialConfigTest {
    @Test
    void shouldBePasswordAwareMaterial() {
        assertTrue(PasswordAwareMaterial.class.isAssignableFrom(GitMaterialConfig.class));
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

        assertEquals("url", gitMaterialConfig.getUrl());
        assertEquals("folder", gitMaterialConfig.getFolder());
        assertEquals("some-branch", gitMaterialConfig.getBranch());
        assertEquals(new CaseInsensitiveString("material-name"), gitMaterialConfig.getName());
        assertFalse(gitMaterialConfig.isAutoUpdate());
        assertTrue(gitMaterialConfig.isShallowClone());
        assertEquals(new Filter(new IgnoredFiles("/root"), new IgnoredFiles("/**/*.help")), gitMaterialConfig.filter());
    }

    @Test
    void setConfigAttributes_shouldUpdatePasswordWhenPasswordChangedBooleanChanged() throws Exception {
        GitMaterialConfig gitMaterialConfig = git("");
        Map<String, String> map = new HashMap<>();
        map.put(GitMaterialConfig.PASSWORD, "secret");
        map.put(GitMaterialConfig.PASSWORD_CHANGED, "1");

        gitMaterialConfig.setConfigAttributes(map);
        assertNull(ReflectionUtil.getField(gitMaterialConfig, "password"));
        assertEquals("secret", gitMaterialConfig.getPassword());
        assertEquals(new GoCipher().encrypt("secret"), gitMaterialConfig.getEncryptedPassword());

        //Dont change
        map.put(GitMaterialConfig.PASSWORD, "Hehehe");
        map.put(GitMaterialConfig.PASSWORD_CHANGED, "0");
        gitMaterialConfig.setConfigAttributes(map);

        assertNull(ReflectionUtil.getField(gitMaterialConfig, "password"));
        assertEquals("secret", gitMaterialConfig.getPassword());
        assertEquals(new GoCipher().encrypt("secret"), gitMaterialConfig.getEncryptedPassword());

        map.put(GitMaterialConfig.PASSWORD, "");
        map.put(GitMaterialConfig.PASSWORD_CHANGED, "1");
        gitMaterialConfig.setConfigAttributes(map);

        assertNull(gitMaterialConfig.getPassword());
        assertNull(gitMaterialConfig.getEncryptedPassword());
    }

    @Test
    void byDefaultShallowCloneShouldBeOff() {
        assertFalse(git("http://url", "foo").isShallowClone());
        assertFalse(git("http://url", "foo", false).isShallowClone());
        assertFalse(git("http://url", "foo", null).isShallowClone());
        assertTrue(git("http://url", "foo", true).isShallowClone());
    }

    @Test
    void shouldReturnIfAttributeMapIsNull() {
        GitMaterialConfig gitMaterialConfig = git("");
        gitMaterialConfig.setConfigAttributes(null);
        assertEquals(git(""), gitMaterialConfig);
    }

    @Test
    void shouldReturnTheUrl() {
        String url = "git@github.com/my/repo";
        GitMaterialConfig config = git(url);

        assertEquals(url, config.getUrl());
    }

    @Test
    void shouldReturnNullIfUrlForMaterialNotSpecified() {
        GitMaterialConfig config = git();

        assertNull(config.getUrl());
    }

    @Test
    void shouldSetUrlForAMaterial() {
        String url = "git@github.com/my/repo";
        GitMaterialConfig config = git();

        config.setUrl(url);

        assertEquals(url, config.getUrl());
    }

    @Test
    void shouldHandleNullWhenSettingUrlForAMaterial() {
        GitMaterialConfig config = git();

        config.setUrl(null);

        assertNull(config.getUrl());
    }

    @Test
    void shouldHandleNullUrlAtTheTimeOfGitMaterialConfigCreation() {
        GitMaterialConfig config = git(null);

        assertNull(config.getUrl());
    }

    @Test
    void shouldHandleNullBranchWhileSettingConfigAttributes() {
        GitMaterialConfig gitMaterialConfig = git("http://url", "foo");
        gitMaterialConfig.setConfigAttributes(Collections.singletonMap(GitMaterialConfig.BRANCH, null));
        assertEquals("master", gitMaterialConfig.getBranch());
    }

    @Test
    void shouldHandleEmptyBranchWhileSettingConfigAttributes() {
        GitMaterialConfig gitMaterialConfig = git("http://url", "foo");
        gitMaterialConfig.setConfigAttributes(Collections.singletonMap(GitMaterialConfig.BRANCH, "     "));
        assertEquals("master", gitMaterialConfig.getBranch());
    }

    @Nested
    class Validate {
        @Test
        void allowsBlankBranch() {
            assertFalse(validating(git("/my/repo", null)).errors().present());
            assertFalse(validating(git("/my/repo", "")).errors().present());
            assertFalse(validating(git("/my/repo", " ")).errors().present());
        }

        @Test
        void rejectsBranchWithWildcard() {
            assertEquals("Branch names may not contain '*'", validating(git("/foo", "branch-*")).
                    errors().on(GitMaterialConfig.BRANCH));
        }

        @Test
        void rejectsMalformedRefSpec() {
            assertEquals("Refspec is missing a source ref",
                    String.join(";", validating(git("/foo", ":a")).errors().
                            getAllOn(GitMaterialConfig.BRANCH)));

            assertEquals("Refspec is missing a source ref",
                    String.join(";", validating(git("/foo", "   :b")).errors().
                            getAllOn(GitMaterialConfig.BRANCH)));

            assertEquals("Refspec is missing a destination ref",
                    String.join(";", validating(git("/foo", "refs/foo: ")).errors().
                            getAllOn(GitMaterialConfig.BRANCH)));

            assertEquals("Refspec is missing a destination ref",
                    String.join(";", validating(git("/foo", "refs/bar:")).errors().
                            getAllOn(GitMaterialConfig.BRANCH)));

            assertEquals("Refspec is missing a source ref;Refspec is missing a destination ref",
                    String.join(";", validating(git("/foo", ":")).errors().
                            getAllOn(GitMaterialConfig.BRANCH)));

            assertEquals("Refspec is missing a source ref;Refspec is missing a destination ref",
                    String.join(";", validating(git("/foo", " : ")).errors().
                            getAllOn(GitMaterialConfig.BRANCH)));

            assertEquals("Refspec source must be an absolute ref (must start with `refs/`)",
                    String.join(";", validating(git("/foo", "a:b")).errors().
                            getAllOn(GitMaterialConfig.BRANCH)));

            assertEquals("Refspecs may not contain wildcards; source and destination refs must be exact",
                    String.join(";", validating(git("/foo", "refs/heads/*:my-branch")).errors().
                            getAllOn(GitMaterialConfig.BRANCH)));

            assertEquals("Refspecs may not contain wildcards; source and destination refs must be exact",
                    String.join(";", validating(git("/foo", "refs/heads/foo:branches/*")).errors().
                            getAllOn(GitMaterialConfig.BRANCH)));

            assertEquals("Refspecs may not contain wildcards; source and destination refs must be exact",
                    String.join(";", validating(git("/foo", "refs/heads/*:branches/*")).errors().
                            getAllOn(GitMaterialConfig.BRANCH)));
        }

        @Test
        void acceptsValidRefSpecs() {
            assertTrue(validating(git("/foo", "refs/pull/123/head:pr-123")).errors().isEmpty());
            assertTrue(validating(git("/foo", "refs/pull/123/head:refs/my-prs/123")).errors().isEmpty());
        }

        @Test
        void shouldEnsureUrlIsNotBlank() {
            assertEquals("URL cannot be blank", validating(git("")).errors().on(GitMaterialConfig.URL));
        }

        @Test
        void shouldEnsureUserNameIsNotProvidedInBothUrlAsWellAsAttributes() {
            GitMaterialConfig gitMaterialConfig = git("http://bob:pass@example.com");
            gitMaterialConfig.setUserName("user");

            assertEquals("Ambiguous credentials, must be provided either in URL or as attributes.", validating(gitMaterialConfig).errors().on(GitMaterialConfig.URL));
        }

        @Test
        void shouldEnsurePasswordIsNotProvidedInBothUrlAsWellAsAttributes() {
            GitMaterialConfig gitMaterialConfig = git("http://bob:pass@example.com");
            gitMaterialConfig.setPassword("pass");

            assertEquals("Ambiguous credentials, must be provided either in URL or as attributes.", validating(gitMaterialConfig).errors().on(GitMaterialConfig.URL));
        }

        @Test
        void shouldIgnoreInvalidUrlForCredentialValidation() {
            GitMaterialConfig gitMaterialConfig = git("http://bob:pass@example.com##dobule-hash-is-invalid-in-url");
            gitMaterialConfig.setUserName("user");
            gitMaterialConfig.setPassword("password");

            assertFalse(validating(gitMaterialConfig).errors().containsKey(GitMaterialConfig.URL));
        }

        @Test
        void shouldBeValidWhenCredentialsAreProvidedOnlyInUrl() {
            assertFalse(validating(git("http://bob:pass@example.com")).errors().containsKey(GitMaterialConfig.URL));
        }

        @Test
        void shouldBeValidWhenCredentialsAreProvidedOnlyAsAttributes() {
            GitMaterialConfig gitMaterialConfig = git("http://example.com");
            gitMaterialConfig.setUserName("bob");
            gitMaterialConfig.setPassword("badger");

            assertFalse(validating(gitMaterialConfig).errors().containsKey(GitMaterialConfig.URL));
        }

        private GitMaterialConfig validating(GitMaterialConfig git) {
            git.validate(new ConfigSaveValidationContext(null));
            return git;
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

            assertFalse(validationResult);
            assertEquals("Encrypted password value for GitMaterial with url 'http://example.com' is " +
                            "invalid. This usually happens when the cipher text is modified to have an invalid value.",
                    gitMaterialConfig.errors().on("encryptedPassword"));
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

            assertTrue(material_1.equals(material_2));
        }
    }

    @Nested
    class Fingerprint {
        @Test
        void shouldGenerateFingerprintForGivenMaterialUrlAndBranch() {
            GitMaterialConfig gitMaterialConfig = git("https://bob:pass@github.com/gocd", "feature");

            assertEquals("755da7fb7415c8674bdf5f8a4ba48fc3e071e5de429b1308ccf8949d215bdb08", gitMaterialConfig.getFingerprint());
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
