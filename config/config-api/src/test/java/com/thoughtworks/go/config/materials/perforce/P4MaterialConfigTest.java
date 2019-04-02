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

package com.thoughtworks.go.config.materials.perforce;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.materials.AbstractMaterialConfig;
import com.thoughtworks.go.config.materials.Filter;
import com.thoughtworks.go.config.materials.IgnoredFiles;
import com.thoughtworks.go.config.materials.ScmMaterialConfig;
import com.thoughtworks.go.config.materials.svn.SvnMaterialConfig;
import com.thoughtworks.go.security.GoCipher;
import com.thoughtworks.go.util.ReflectionUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class P4MaterialConfigTest {
    private P4MaterialConfig p4MaterialConfig;

    @BeforeEach
    void setUp() {
        p4MaterialConfig = new P4MaterialConfig("/foo/bar", "some-view");
    }

    @Test
    void shouldSetConfigAttributes() {
        P4MaterialConfig p4MaterialConfig = new P4MaterialConfig("", "");

        Map<String, String> map = new HashMap<>();
        map.put(P4MaterialConfig.SERVER_AND_PORT, "serverAndPort");
        map.put(P4MaterialConfig.USERNAME, "username");
        map.put(P4MaterialConfig.PASSWORD, "password");
        map.put(P4MaterialConfig.USE_TICKETS, "true");
        map.put(P4MaterialConfig.VIEW, "some-view");
        map.put(ScmMaterialConfig.FOLDER, "folder");
        map.put(ScmMaterialConfig.AUTO_UPDATE, "false");
        map.put(ScmMaterialConfig.FILTER, "/root,/**/*.help");
        map.put(AbstractMaterialConfig.MATERIAL_NAME, "material-name");

        p4MaterialConfig.setConfigAttributes(map);

        assertThat(p4MaterialConfig.getServerAndPort()).isEqualTo("serverAndPort");
        assertThat(p4MaterialConfig.getUserName()).isEqualTo("username");
        assertThat(p4MaterialConfig.getView()).isEqualTo("some-view");
        assertThat(p4MaterialConfig.getUseTickets()).isTrue();
        assertThat(p4MaterialConfig.getFolder()).isEqualTo("folder");
        assertThat(p4MaterialConfig.getName()).isEqualTo(new CaseInsensitiveString("material-name"));
        assertThat(p4MaterialConfig.isAutoUpdate()).isFalse();
        assertThat(p4MaterialConfig.filter()).isEqualTo(new Filter(new IgnoredFiles("/root"), new IgnoredFiles("/**/*.help")));
    }

    @Test
    void validate_shouldEnsureThatViewIsNotBlank() {
        assertError("example.com:1233", "", P4MaterialConfig.VIEW, "P4 view cannot be empty.");
    }

    @Test
    void shouldNotDoAnyValidationOnP4PortExceptToEnsureThatItIsNotEmpty() throws Exception {
        assertError("", "view", P4MaterialConfig.SERVER_AND_PORT, "P4 port cannot be empty.");
        assertError(" ", "view", P4MaterialConfig.SERVER_AND_PORT, "P4 port cannot be empty.");

        assertNoError("example.com:1818", "view", P4MaterialConfig.SERVER_AND_PORT);
        assertNoError("ssl:host:1234", "view", P4MaterialConfig.SERVER_AND_PORT);
        assertNoError("ssl:host:non_numerical_port", "view", P4MaterialConfig.SERVER_AND_PORT);
        assertNoError("complete_junk:::abc:::123:::def", "view", P4MaterialConfig.SERVER_AND_PORT);
        assertNoError(":1234", "view", P4MaterialConfig.SERVER_AND_PORT);
        assertNoError(":abc", "view", P4MaterialConfig.SERVER_AND_PORT);
        assertNoError("1234", "view", P4MaterialConfig.SERVER_AND_PORT);
        assertNoError("tcp:abc:1234", "view", P4MaterialConfig.SERVER_AND_PORT);
    }

    @Test
    void shouldReturnIfAttributeMapIsNull() {
        P4MaterialConfig p4MaterialConfig = new P4MaterialConfig("", "");
        p4MaterialConfig.setConfigAttributes(null);
        assertThat(p4MaterialConfig).isEqualTo(new P4MaterialConfig("", ""));
    }

    @Test
    void setConfigAttributes_shouldUpdatePasswordWhenPasswordChangedBooleanChanged() throws Exception {
        P4MaterialConfig materialConfig = new P4MaterialConfig("", "");
        materialConfig.setPassword("notSecret");
        Map<String, String> map = new HashMap<>();
        map.put(P4MaterialConfig.PASSWORD, "secret");
        map.put(P4MaterialConfig.PASSWORD_CHANGED, "1");

        materialConfig.setConfigAttributes(map);
        assertThat(ReflectionUtil.getField(materialConfig, "password")).isNull();
        assertThat(materialConfig.getPassword()).isEqualTo("secret");
        assertThat(materialConfig.getEncryptedPassword()).isEqualTo(new GoCipher().encrypt("secret"));

        //Dont change
        map.put(SvnMaterialConfig.PASSWORD, "Hehehe");
        map.put(SvnMaterialConfig.PASSWORD_CHANGED, "0");
        materialConfig.setConfigAttributes(map);

        assertThat(ReflectionUtil.getField(materialConfig, "password")).isNull();
        assertThat(materialConfig.getPassword()).isEqualTo("secret");
        assertThat(materialConfig.getEncryptedPassword()).isEqualTo(new GoCipher().encrypt("secret"));

        //Dont change
        map.put(SvnMaterialConfig.PASSWORD, "");
        map.put(SvnMaterialConfig.PASSWORD_CHANGED, "1");
        materialConfig.setConfigAttributes(map);

        assertThat(materialConfig.getPassword()).isNull();
        assertThat(materialConfig.getEncryptedPassword()).isNull();
    }

    @Test
    void shouldNotSetUseTicketsIfNotInConfigAttributesMap() {
        P4MaterialConfig p4MaterialConfig = new P4MaterialConfig("", "");

        HashMap<String, String> map = new HashMap<>();
        map.put(P4MaterialConfig.USE_TICKETS, "true");
        p4MaterialConfig.setConfigAttributes(map);
        assertThat(p4MaterialConfig.getUseTickets()).isTrue();

        p4MaterialConfig.setConfigAttributes(new HashMap());
        assertThat(p4MaterialConfig.getUseTickets()).isFalse();
    }

    @Nested
    class ValidatePassword {
        @Test
        void shouldFailIfEncryptedPasswordIsIncorrect() {
            p4MaterialConfig.setEncryptedPassword("encryptedPassword");

            final boolean validationResult = p4MaterialConfig.validateTree(new ConfigSaveValidationContext(null));

            assertThat(validationResult).isFalse();
            assertThat(p4MaterialConfig.errors().on("encryptedPassword")).isEqualTo("Encrypted password value for P4Material with url '/foo/bar' is invalid. This usually happens when the cipher text is modified to have an invalid value.");
        }

        @Test
        void shouldPassIfPasswordIsNotSpecifiedAsSecretParams() {
            p4MaterialConfig.setPassword("badger");

            assertThat(p4MaterialConfig.validateTree(null)).isTrue();
            assertThat(p4MaterialConfig.errors().getAll()).isEmpty();
        }

        @Test
        void shouldPassIfPasswordSpecifiedAsSecretParamIsValid() {
            final ValidationContext validationContext = mockValidationContextForSecretParams(new SecretConfig("secret_config_id", "cd.go.secret.file"));
            p4MaterialConfig.setPassword("{{SECRET:[secret_config_id][password]}}");

            assertThat(p4MaterialConfig.validateTree(validationContext)).isTrue();
            assertThat(p4MaterialConfig.errors().getAll()).isEmpty();
        }

        @Test
        void shouldFailIfSecretConfigForPasswordSpecifiedAsSecretParamDoesNotExist() {
            final ValidationContext validationContext = mockValidationContextForSecretParams();
            p4MaterialConfig.setPassword("{{SECRET:[secret_config_id][password]}}");

            assertThat(p4MaterialConfig.validateTree(validationContext)).isFalse();
            assertThat(p4MaterialConfig.errors().on("encryptedPassword")).isEqualTo("Secret configs secret_config_id does not exist");
        }
    }

    private ValidationContext mockValidationContextForSecretParams(SecretConfig... secretConfigs) {
        final ValidationContext validationContext = mock(ValidationContext.class);
        final CruiseConfig cruiseConfig = mock(CruiseConfig.class);
        when(validationContext.getCruiseConfig()).thenReturn(cruiseConfig);
        when(cruiseConfig.getSecretConfigs()).thenReturn(new SecretConfigs(secretConfigs));
        return validationContext;
    }

    private void assertNoError(String port, String view, String expectedKeyForError) {
        P4MaterialConfig p4MaterialConfig = new P4MaterialConfig(port, view);
        p4MaterialConfig.validate(new ConfigSaveValidationContext(null));
        assertThat(p4MaterialConfig.errors().on(expectedKeyForError)).isNull();
    }

    private void assertError(String port, String view, String expectedKeyForError, String expectedErrorMessage) {
        P4MaterialConfig p4MaterialConfig = new P4MaterialConfig(port, view);
        p4MaterialConfig.validate(new ConfigSaveValidationContext(null));
        assertThat(p4MaterialConfig.errors().on(expectedKeyForError)).isEqualTo(expectedErrorMessage);
    }
}
