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
package com.thoughtworks.go.config.materials.perforce;

import com.thoughtworks.go.config.SecretParam;
import com.thoughtworks.go.config.exceptions.UnresolvedSecretParamException;
import com.thoughtworks.go.domain.MaterialRevision;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.materials.perforce.P4Client;
import com.thoughtworks.go.helper.MaterialsMother;
import com.thoughtworks.go.util.ReflectionUtil;
import com.thoughtworks.go.util.TempDirUtils;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;
import com.thoughtworks.go.util.command.InMemoryStreamConsumer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

public class P4MaterialTest extends P4MaterialTestBase {

    @Test
    void shouldAddServerSideEnvironmentVariablesClientNameEnvironmentVariable() throws IOException {
        File p4_working_dir = TempDirUtils.createRandomDirectoryIn(tempDir).toFile();

        P4Material p4 = new P4Material("host:10", "beautiful", "user");
        p4.setPassword("loser");
        EnvironmentVariableContext envVarCtx;

        envVarCtx = new EnvironmentVariableContext();
        p4.populateEnvironmentContext(envVarCtx, new MaterialRevision(p4, new Modification("loser", "loserish commit", "loser@boozer.com", new Date(), "123")), p4_working_dir);
        assertThat(envVarCtx.getProperty("GO_REVISION")).isEqualTo("123");
        assertThat(envVarCtx.getProperty("GO_TO_REVISION")).isEqualTo("123");
        assertThat(envVarCtx.getProperty("GO_FROM_REVISION")).isEqualTo("123");
    }

    @Test
    void shouldAddClientNameEnvironmentVariable() throws IOException {
        File p4_working_dir = TempDirUtils.createRandomDirectoryIn(tempDir).toFile();

        P4Material p4 = new P4Material("host:10", "beautiful", "user");
        p4.setPassword("loser");
        EnvironmentVariableContext envVarCtx;

        envVarCtx = new EnvironmentVariableContext();
        p4.populateAgentSideEnvironmentContext(envVarCtx, p4_working_dir);
        assertThat(envVarCtx.getProperty("GO_P4_CLIENT")).isEqualTo(p4.clientName(p4_working_dir));
    }

    @Test
    void shouldGenerateTheSameP4ClientValueForCommandAndEnvironment() throws Exception {
        File workingDir = TempDirUtils.createRandomDirectoryIn(tempDir).toFile();

        P4Material p4Material = new P4Material("server:10", "out-of-the-window");
        ReflectionUtil.setField(p4Material, "folder", "crapy_dir");

        P4Client p4Client = p4Material.p4(workingDir, new InMemoryStreamConsumer(), false);

        assertThat(p4Client).isNotNull();
        String client = ReflectionUtil.getField(p4Client, "p4ClientName");
        assertThat(client).isEqualTo(p4Material.clientName(workingDir));
    }

    @Test
    void shouldCreateUniqueHashForFolders() {
        File file = new File("c:a/b/c/d/e");
        File file2 = new File("c:foo\\bar\\baz");
        assertThat(P4Material.filesystemSafeFileHash(file).matches("[0-9a-zA-Z.\\-]*")).isTrue();
        assertThat(P4Material.filesystemSafeFileHash(file2)).isNotEqualTo(P4Material.filesystemSafeFileHash(file));
    }

    @Test
    void shouldNotDisplayPasswordInStringRepresentation() {
        P4Material p4 = new P4Material("host:10", "beautiful");
        p4.setUsername("user");
        p4.setPassword("loser");
        assertThat(p4.toString()).doesNotContain("loser");
    }

    @Test
    void shouldReturnEqualsEvenIfPasswordsAreDifferent() {
        P4Material material = MaterialsMother.p4Material();
        material.setPassword("password");

        P4Material other = MaterialsMother.p4Material();
        other.setPassword("password1");
        assertThat(material).isEqualTo(other);
    }

    @Test
    void shouldNotConsiderPasswordForEqualityCheck() {
        P4Material one = new P4Material("host:123", "through_window");
        one.setPassword("password");
        P4Material two = new P4Material("host:123", "through_window");
        two.setPassword("wordpass");

        assertThat(one).isEqualTo(two);
        assertThat(one.hashCode()).isEqualTo(two.hashCode());
    }

    @Test
    void shouldGetLongDescriptionForMaterial() {
        P4Material material = new P4Material("host:123", "through_window", "user", "folder");
        assertThat(material.getLongDescription()).isEqualTo("URL: host:123, View: through_window, Username: user");
    }

    @Test
    void shouldCopyOverPasswordWhenConvertingToConfig() {
        P4Material material = new P4Material("blah.com", "view");
        material.setPassword("password");

        P4MaterialConfig config = (P4MaterialConfig) material.config();

        assertThat(config.getPassword()).isEqualTo("password");
        assertThat(config.getEncryptedPassword()).isNotNull();
    }

    @Test
    void shouldGetAttributesWithSecureFields() {
        P4Material material = new P4Material("host:1234", "view", "username");
        material.setPassword("password");
        material.setUseTickets(true);
        Map<String, Object> attributes = material.getAttributes(true);

        assertThat(attributes.get("type")).isEqualTo("perforce");
        @SuppressWarnings("unchecked") Map<String, Object> configuration = (Map<String, Object>) attributes.get("perforce-configuration");
        assertThat(configuration.get("url")).isEqualTo("host:1234");
        assertThat(configuration.get("username")).isEqualTo("username");
        assertThat(configuration.get("password")).isEqualTo("password");
        assertThat(configuration.get("view")).isEqualTo("view");
        assertThat(configuration.get("use-tickets")).isEqualTo(true);
    }

    @Test
    void shouldGetAttributesWithoutSecureFields() {
        P4Material material = new P4Material("host:1234", "view", "username");
        material.setPassword("password");
        material.setUseTickets(true);
        Map<String, Object> attributes = material.getAttributes(false);

        assertThat(attributes.get("type")).isEqualTo("perforce");
        @SuppressWarnings("unchecked") Map<String, Object> configuration = (Map<String, Object>) attributes.get("perforce-configuration");
        assertThat(configuration.get("url")).isEqualTo("host:1234");
        assertThat(configuration.get("username")).isEqualTo("username");
        assertThat(configuration.get("password")).isNull();
        assertThat(configuration.get("view")).isEqualTo("view");
        assertThat(configuration.get("use-tickets")).isEqualTo(true);
    }

    @Test
    void shouldSetGO_P4_CLIENT_toTheClientName() {
        P4Material material = new P4Material("host:1234", "view", "username", "destination");
        EnvironmentVariableContext environmentVariableContext = new EnvironmentVariableContext();
        File agentWorkingDirectory = new File("pipelines/pipeline-name");
        material.populateAgentSideEnvironmentContext(environmentVariableContext, agentWorkingDirectory);
        assertThat(environmentVariableContext.getProperty("GO_P4_CLIENT_DESTINATION")).isEqualTo(material.clientName(material.workingdir(agentWorkingDirectory)));
    }

    @Nested
    class hasSecretParams {
        @Test
        void shouldBeTrueIfMaterialUrlHasSecretParams() {
            P4Material p4Material = new P4Material("host:10", "beautiful");
            p4Material.setPassword("{{SECRET:[secret_config_id][lookup_password]}}");

            assertThat(p4Material.hasSecretParams()).isTrue();
        }

        @Test
        void shouldBeFalseInMaterialUrlDoesNotHaveSecretParams() {
            P4Material p4Material = new P4Material("host:10", "beautiful");
            p4Material.setPassword("foo");

            assertThat(p4Material.hasSecretParams()).isFalse();
        }
    }

    @Nested
    class getSecretParams {
        @Test
        void shouldReturnAListOfSecretParams() {
            P4Material p4Material = new P4Material("host:10", "beautiful");
            p4Material.setPassword("{{SECRET:[secret_config_id][lookup_password]}}");

            assertThat(p4Material.getSecretParams())
                    .hasSize(1)
                    .contains(new SecretParam("secret_config_id", "lookup_password"));
        }

        @Test
        void shouldBeAnEmptyListInAbsenceOfSecretParamsInMaterialUrl() {
            P4Material p4Material = new P4Material("host:10", "beautiful");
            p4Material.setPassword("pass");

            assertThat(p4Material.getSecretParams())
                    .hasSize(0);
        }
    }

    @Nested
    class passwordForCommandLine {
        @Test
        void shouldReturnPasswordAsConfigured_IfNotDefinedAsSecretParam() {
            P4Material p4Material = new P4Material("host:10", "beautiful");
            p4Material.setPassword("badger");

            assertThat(p4Material.passwordForCommandLine()).isEqualTo("badger");
        }

        @Test
        void shouldReturnAResolvedPassword_IfPasswordDefinedAsSecretParam() {
            P4Material p4Material = new P4Material("host:10", "beautiful");
            p4Material.setPassword("{{SECRET:[secret_config_id][lookup_pass]}}");

            p4Material.getSecretParams().findFirst("lookup_pass").ifPresent(secretParam -> secretParam.setValue("resolved_password"));

            assertThat(p4Material.passwordForCommandLine()).isEqualTo("resolved_password");
        }

        @Test
        void shouldErrorOutWhenCalledOnAUnResolvedSecretParam_IfPasswordDefinedAsSecretParam() {
            P4Material p4Material = new P4Material("host:10", "beautiful");
            p4Material.setPassword("{{SECRET:[secret_config_id][lookup_pass]}}");

            assertThatCode(p4Material::passwordForCommandLine)
                    .isInstanceOf(UnresolvedSecretParamException.class)
                    .hasMessageContaining("SecretParam 'lookup_pass' is used before it is resolved.");
        }
    }

    @Nested
    class setPassword {
        @Test
        void shouldParsePasswordString_IfDefinedAsSecretParam() {
            P4Material p4Material = new P4Material("host:10", "beautiful");
            p4Material.setPassword("{{SECRET:[secret_config_id][lookup_pass]}}");

            assertThat(p4Material.getSecretParams())
                    .hasSize(1)
                    .contains(new SecretParam("secret_config_id", "lookup_pass"));
        }
    }
}
