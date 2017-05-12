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

import java.io.File;
import java.util.Date;
import java.util.Map;

import com.thoughtworks.go.domain.MaterialRevision;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.materials.perforce.P4Client;
import com.thoughtworks.go.helper.MaterialsMother;
import com.thoughtworks.go.helper.P4TestRepo;
import com.thoughtworks.go.security.GoCipher;
import com.thoughtworks.go.util.ReflectionUtil;
import com.thoughtworks.go.util.TempFiles;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;
import com.thoughtworks.go.util.command.InMemoryStreamConsumer;
import org.hamcrest.Matchers;
import org.junit.Test;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class P4MaterialTest extends P4MaterialTestBase {

    @Override
    protected P4TestRepo createTestRepo() throws Exception {
        P4TestRepo repo = P4TestRepo.createP4TestRepo();
        repo.onSetup();
        return repo;
    }

    @Test
    public void dummyTestSoIntelliJNoticesMe() {
    }

    @Test
    public void shouldAddClientNameEnvironmentVariable() {
        TempFiles tempFiles = new TempFiles();
        File p4_working_dir = tempFiles.mkdir("p4_working_dir");

        P4Material p4 = new P4Material("host:10", "beautiful", "user");
        p4.setPassword("loser");
        EnvironmentVariableContext envVarCtx;

        envVarCtx = new EnvironmentVariableContext();
        p4.populateEnvironmentContext(envVarCtx, new MaterialRevision(p4, new Modification("loser", "loserish commit", "loser@boozer.com", new Date(), "123")), p4_working_dir);
        assertThat(envVarCtx.getProperty("GO_P4_CLIENT"), is(p4.clientName(p4_working_dir)));

        assertThat(envVarCtx.getProperty("GO_REVISION"), is("123")); //sanity check
    }

    @Test
    public void shouldGenerateTheSameP4ClientValueForCommandAndEnvironment() throws Exception {

        P4Material p4Material = new P4Material("server:10", "out-of-the-window");
        ReflectionUtil.setField(p4Material, "folder", "crapy_dir");

        P4Client p4Client = p4Material._p4(tempDir, new InMemoryStreamConsumer(), false);

        assertThat(p4Client, is(not(nullValue())));
        String client = (String) ReflectionUtil.getField(p4Client, "p4ClientName");
        assertThat(client, is(p4Material.clientName(tempDir)));
    }

    @Test
    public void shouldNotDisplayPasswordInStringRepresentation() {
        P4Material p4 = new P4Material("host:10", "beautiful");
        p4.setUsername("user");
        p4.setPassword("loser");
        assertThat(p4.toString(), not(containsString("loser")));
    }


    @Test
    public void shouldEncryptP4Password() throws Exception {
        GoCipher mockGoCipher = mock(GoCipher.class);
        when(mockGoCipher.encrypt("password")).thenReturn("encrypted");

        P4Material p4Material = new P4Material("example.com:1818", "view", mockGoCipher);
        p4Material.setPassword("password");
        p4Material.ensureEncrypted();

        assertThat(p4Material.getEncryptedPassword(), is("encrypted"));
        assertThat(p4Material.getPassword(), is(nullValue()));
    }

    @Test
    public void shouldDecryptP4Password() throws Exception {
        GoCipher mockGoCipher = mock(GoCipher.class);
        when(mockGoCipher.decrypt("encrypted")).thenReturn("password");

        P4Material p4Material = new P4Material("example.com:1818", "view", mockGoCipher);
        ReflectionUtil.setField(p4Material, "encryptedPassword", "encrypted");
        p4Material.getPassword();

        assertThat(p4Material.getPassword(), is("password"));
    }

    @Test
    public void shouldReturnEqualsEvenIfPasswordsAreDifferent() throws Exception {
        P4Material material = MaterialsMother.p4Material();
        material.setPassword("password");

        P4Material other = MaterialsMother.p4Material();
        other.setPassword("password1");
        assertThat(material, is(other));
    }

    @Test
    public void shouldNotConsiderPasswordForEqualityCheck() {
        P4Material one = new P4Material("host:123", "through_window");
        one.setPassword("password");
        P4Material two = new P4Material("host:123", "through_window");
        two.setPassword("wordpass");

        assertThat(one, is(two));
        assertThat(one.hashCode(), is(two.hashCode()));
    }

    @Test
    public void shouldGetLongDescriptionForMaterial(){
        P4Material material = new P4Material("host:123", "through_window", "user", "folder");
        assertThat(material.getLongDescription(), is("URL: host:123, View: through_window, Username: user"));
    }

    @Test
    public void shouldCopyOverPasswordWhenConvertingToConfig() throws Exception {
        P4Material material = new P4Material("blah.com","view");
        material.setPassword("password");

        P4MaterialConfig config = (P4MaterialConfig) material.config();

        assertThat(config.getPassword(), is("password"));
        assertThat(config.getEncryptedPassword(), is(Matchers.not(Matchers.nullValue())));
    }

    @Test
    public void shouldGetAttributesWithSecureFields() {
        P4Material material = new P4Material("host:1234", "view", "username");
        material.setPassword("password");
        material.setUseTickets(true);
        Map<String, Object> attributes = material.getAttributes(true);

        assertThat(attributes.get("type"), is("perforce"));
        Map<String, Object> configuration = (Map<String, Object>) attributes.get("perforce-configuration");
        assertThat(configuration.get("url"), is("host:1234"));
        assertThat(configuration.get("username"), is("username"));
        assertThat(configuration.get("password"), is("password"));
        assertThat(configuration.get("view"), is("view"));
        assertThat(configuration.get("use-tickets"), is(true));
    }

    @Test
    public void shouldGetAttributesWithoutSecureFields() {
        P4Material material = new P4Material("host:1234", "view", "username");
        material.setPassword("password");
        material.setUseTickets(true);
        Map<String, Object> attributes = material.getAttributes(false);

        assertThat(attributes.get("type"), is("perforce"));
        Map<String, Object> configuration = (Map<String, Object>) attributes.get("perforce-configuration");
        assertThat(configuration.get("url"), is("host:1234"));
        assertThat(configuration.get("username"), is("username"));
        assertThat(configuration.get("password"), is(nullValue()));
        assertThat(configuration.get("view"), is("view"));
        assertThat(configuration.get("use-tickets"), is(true));
    }
}
