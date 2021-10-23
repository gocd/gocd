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
package com.thoughtworks.go.config.materials.svn;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.materials.AbstractMaterialConfig;
import com.thoughtworks.go.config.materials.Filter;
import com.thoughtworks.go.config.materials.IgnoredFiles;
import com.thoughtworks.go.config.materials.ScmMaterialConfig;
import com.thoughtworks.go.config.materials.git.GitMaterialConfig;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.security.GoCipher;
import com.thoughtworks.go.util.ReflectionUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.thoughtworks.go.helper.MaterialConfigsMother.svn;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class SvnMaterialConfigTest {
    private SvnMaterialConfig svnMaterialConfig;

    @BeforeEach
    void setUp() {
        svnMaterialConfig = svn();
    }

    @Test
    void shouldSetConfigAttributes() {
        SvnMaterialConfig svnMaterialConfig = svn("", "", "", false);

        Map<String, String> map = new HashMap<>();
        map.put(SvnMaterialConfig.URL, "url");
        map.put(SvnMaterialConfig.USERNAME, "username");
        map.put(SvnMaterialConfig.CHECK_EXTERNALS, "true");
        map.put(ScmMaterialConfig.FOLDER, "folder");
        map.put(ScmMaterialConfig.AUTO_UPDATE, "0");
        map.put(ScmMaterialConfig.FILTER, "/root,/**/*.help");
        map.put(AbstractMaterialConfig.MATERIAL_NAME, "material-name");

        svnMaterialConfig.setConfigAttributes(map);

        assertThat(svnMaterialConfig.getUrl()).isEqualTo("url");
        assertThat(svnMaterialConfig.getUserName()).isEqualTo("username");
        assertThat(svnMaterialConfig.isCheckExternals()).isTrue();
        assertThat(svnMaterialConfig.getFolder()).isEqualTo("folder");
        assertThat(svnMaterialConfig.getName()).isEqualTo(new CaseInsensitiveString("material-name"));
        assertThat(svnMaterialConfig.isAutoUpdate()).isFalse();
        assertThat(svnMaterialConfig.filter()).isEqualTo(new Filter(new IgnoredFiles("/root"), new IgnoredFiles("/**/*.help")));
    }

    @Test
    void setConfigAttributes_shouldUpdatePasswordWhenPasswordChangedBooleanChanged() throws Exception {
        SvnMaterialConfig svnMaterial = svn("", "", "notSoSecret", false);
        Map<String, String> map = new HashMap<>();
        map.put(SvnMaterialConfig.PASSWORD, "secret");
        map.put(SvnMaterialConfig.PASSWORD_CHANGED, "1");

        svnMaterial.setConfigAttributes(map);
        assertThat(ReflectionUtil.getField(svnMaterial, "password")).isNull();
        assertThat(svnMaterial.getPassword()).isEqualTo("secret");
        assertThat(svnMaterial.getEncryptedPassword()).isEqualTo(new GoCipher().encrypt("secret"));

        //Dont change
        map.put(SvnMaterialConfig.PASSWORD, "Hehehe");
        map.put(SvnMaterialConfig.PASSWORD_CHANGED, "0");
        svnMaterial.setConfigAttributes(map);

        assertThat(ReflectionUtil.getField(svnMaterial, "password")).isNull();
        assertThat(svnMaterial.getPassword()).isEqualTo("secret");
        assertThat(svnMaterial.getEncryptedPassword()).isEqualTo(new GoCipher().encrypt("secret"));

        map.put(SvnMaterialConfig.PASSWORD, "");
        map.put(SvnMaterialConfig.PASSWORD_CHANGED, "1");
        svnMaterial.setConfigAttributes(map);

        assertThat(svnMaterial.getPassword()).isNull();
        assertThat(svnMaterial.getEncryptedPassword()).isNull();
    }

    @Test
    void shouldReturnTheUrl() {
        String url = "git@github.com/my/repo";
        SvnMaterialConfig config = svn();
        config.setUrl(url);

        assertThat(config.getUrl()).isEqualTo(url);
    }

    @Test
    void shouldReturnNullIfUrlForMaterialNotSpecified() {
        SvnMaterialConfig config = svn();

        assertThat(config.getUrl()).isNull();
    }

    @Test
    void shouldHandleNullWhenSettingUrlForAMaterial() {
        SvnMaterialConfig config = svn();

        config.setUrl(null);

        assertThat(config.getUrl()).isNull();
    }

    @Nested
    class ValidateURL {
        @Test
        void shouldEnsureUrlIsNotBlank() {
            svnMaterialConfig.setUrl("");

            svnMaterialConfig.validate(new ConfigSaveValidationContext(null));

            assertThat(svnMaterialConfig.errors().on(GitMaterialConfig.URL)).isEqualTo("URL cannot be blank");
        }

        @Test
        void shouldEnsureUrlIsNotNull() {
            svnMaterialConfig.validate(new ConfigSaveValidationContext(null));

            assertThat(svnMaterialConfig.errors().on(SvnMaterialConfig.URL)).isEqualTo("URL cannot be blank");
        }

        @Test
        void shouldEnsureMaterialNameIsValid() {
            svnMaterialConfig.validate(new ConfigSaveValidationContext(null));
            assertThat(svnMaterialConfig.errors().on(SvnMaterialConfig.MATERIAL_NAME)).isNull();

            svnMaterialConfig.setName(new CaseInsensitiveString(".bad-name-with-dot"));
            svnMaterialConfig.validate(new ConfigSaveValidationContext(null));

            assertThat(svnMaterialConfig.errors().on(SvnMaterialConfig.MATERIAL_NAME)).isEqualTo("Invalid material name '.bad-name-with-dot'. This must be alphanumeric and can contain underscores, hyphens and periods (however, it cannot start with a period). The maximum allowed length is 255 characters.");
        }

        @Test
        void shouldEnsureDestFilePathIsValid() {
            svnMaterialConfig.setConfigAttributes(Collections.singletonMap(ScmMaterialConfig.FOLDER, "../a"));
            svnMaterialConfig.validate(new ConfigSaveValidationContext(null));

            assertThat(svnMaterialConfig.errors().on(SvnMaterialConfig.FOLDER)).isEqualTo("Dest folder '../a' is not valid. It must be a sub-directory of the working folder.");
        }

        @Test
        void rejectsObviouslyWrongURL() {
            assertTrue(validating(svn("-url-not-starting-with-an-alphanumeric-character", false)).errors().containsKey(SvnMaterialConfig.URL));
            assertTrue(validating(svn("_url-not-starting-with-an-alphanumeric-character", false)).errors().containsKey(SvnMaterialConfig.URL));
            assertTrue(validating(svn("@url-not-starting-with-an-alphanumeric-character", false)).errors().containsKey(SvnMaterialConfig.URL));

            assertFalse(validating(svn("url-starting-with-an-alphanumeric-character", false)).errors().containsKey(SvnMaterialConfig.URL));
            assertFalse(validating(svn("#{url}", false)).errors().containsKey(SvnMaterialConfig.URL));
        }

        private SvnMaterialConfig validating(SvnMaterialConfig svn) {
            svn.validate(new ConfigSaveValidationContext(null));
            return svn;
        }
    }

    @Nested
    class ValidateTree {
        @BeforeEach
        void setUp() {
            svnMaterialConfig.setUrl("foo/bar");
        }

        @Test
        void shouldCallValidate() {
            final MaterialConfig materialConfig = spy(svnMaterialConfig);
            final ValidationContext validationContext = mockValidationContextForSecretParams();

            materialConfig.validateTree(validationContext);

            verify(materialConfig).validate(validationContext);
        }

        @Test
        void shouldFailIfEncryptedPasswordIsIncorrect() {
            svnMaterialConfig.setEncryptedPassword("encryptedPassword");

            final boolean validationResult = svnMaterialConfig.validateTree(new ConfigSaveValidationContext(null));

            assertThat(validationResult).isFalse();
            assertThat(svnMaterialConfig.errors().on("encryptedPassword")).isEqualTo("Encrypted password value for SvnMaterial with url 'foo/bar' is invalid. This usually happens when the cipher text is modified to have an invalid value.");
        }

        @Test
        void shouldPassIfPasswordIsNotSpecifiedAsSecretParams() {
            svnMaterialConfig.setPassword("badger");

            assertThat(svnMaterialConfig.validateTree(null)).isTrue();
            assertThat(svnMaterialConfig.errors().getAll()).isEmpty();
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
