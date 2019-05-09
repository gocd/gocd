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
    }

    private ValidationContext mockValidationContextForSecretParams(SecretConfig... secretConfigs) {
        final ValidationContext validationContext = mock(ValidationContext.class);
        final CruiseConfig cruiseConfig = mock(CruiseConfig.class);
        when(validationContext.getCruiseConfig()).thenReturn(cruiseConfig);
        when(cruiseConfig.getSecretConfigs()).thenReturn(new SecretConfigs(secretConfigs));
        return validationContext;
    }
}
