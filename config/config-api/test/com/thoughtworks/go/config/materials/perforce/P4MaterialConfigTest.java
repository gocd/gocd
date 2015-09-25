/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.config.materials.perforce;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.ValidationContext;
import com.thoughtworks.go.config.materials.AbstractMaterialConfig;
import com.thoughtworks.go.config.materials.Filter;
import com.thoughtworks.go.config.materials.IgnoredFiles;
import com.thoughtworks.go.config.materials.ScmMaterialConfig;
import com.thoughtworks.go.config.materials.svn.SvnMaterialConfig;
import com.thoughtworks.go.security.GoCipher;
import com.thoughtworks.go.util.ReflectionUtil;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

public class P4MaterialConfigTest {

    @Test
    public void shouldSetConfigAttributes() {
        P4MaterialConfig p4MaterialConfig = new P4MaterialConfig("", "");

        Map<String, String> map = new HashMap<String, String>();
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

        assertThat(p4MaterialConfig.getServerAndPort(), is("serverAndPort"));
        assertThat(p4MaterialConfig.getUserName(), is("username"));
        assertThat(p4MaterialConfig.getView(), is("some-view"));
        assertThat(p4MaterialConfig.getUseTickets(), is(true));
        assertThat(p4MaterialConfig.getFolder(), is("folder"));
        assertThat(p4MaterialConfig.getName(), is(new CaseInsensitiveString("material-name")));
        assertThat(p4MaterialConfig.isAutoUpdate(), is(false));
        assertThat(p4MaterialConfig.filter(), is(new Filter(new IgnoredFiles("/root"), new IgnoredFiles("/**/*.help"))));
    }

    @Test
    public void validate_shouldEnsureThatViewIsNotBlank() {
        assertError("example.com:1233", "", P4MaterialConfig.VIEW, "P4 view cannot be empty.");
    }

    @Test
    public void shouldNotDoAnyValidationOnP4PortExceptToEnsureThatItIsNotEmpty() throws Exception {
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
    public void shouldReturnIfAttributeMapIsNull() {
        P4MaterialConfig p4MaterialConfig = new P4MaterialConfig("", "");
        p4MaterialConfig.setConfigAttributes(null);
        assertThat(p4MaterialConfig, is(new P4MaterialConfig("", "")));
    }

    @Test
    public void setConfigAttributes_shouldUpdatePasswordWhenPasswordChangedBooleanChanged() throws Exception {
        P4MaterialConfig materialConfig = new P4MaterialConfig("","");
        materialConfig.setPassword("notSecret");
        Map<String, String> map = new HashMap<String, String>();
        map.put(P4MaterialConfig.PASSWORD, "secret");
        map.put(P4MaterialConfig.PASSWORD_CHANGED, "1");

        materialConfig.setConfigAttributes(map);
        assertThat(ReflectionUtil.getField(materialConfig, "password"), is(nullValue()));
        assertThat(materialConfig.getPassword(), is("secret"));
        assertThat(materialConfig.getEncryptedPassword(), is(new GoCipher().encrypt("secret")));

        //Dont change
        map.put(SvnMaterialConfig.PASSWORD, "Hehehe");
        map.put(SvnMaterialConfig.PASSWORD_CHANGED, "0");
        materialConfig.setConfigAttributes(map);

        assertThat(ReflectionUtil.getField(materialConfig, "password"), is(nullValue()));
        assertThat(materialConfig.getPassword(), is("secret"));
        assertThat(materialConfig.getEncryptedPassword(), is(new GoCipher().encrypt("secret")));

        //Dont change
        map.put(SvnMaterialConfig.PASSWORD, "");
        map.put(SvnMaterialConfig.PASSWORD_CHANGED, "1");
        materialConfig.setConfigAttributes(map);

        assertThat(materialConfig.getPassword(), is(nullValue()));
        assertThat(materialConfig.getEncryptedPassword(), is(nullValue()));
    }

    @Test
    public void shouldNotSetUseTicketsIfNotInConfigAttributesMap() {
        P4MaterialConfig p4MaterialConfig = new P4MaterialConfig("", "");

        HashMap<String, String> map = new HashMap<String, String>();
        map.put(P4MaterialConfig.USE_TICKETS, "true");
        p4MaterialConfig.setConfigAttributes(map);
        assertThat(p4MaterialConfig.getUseTickets(), is(true));

        p4MaterialConfig.setConfigAttributes(new HashMap());
        assertThat(p4MaterialConfig.getUseTickets(), is(false));
    }

    private void assertNoError(String port, String view, String expectedKeyForError) {
        P4MaterialConfig p4MaterialConfig = new P4MaterialConfig(port, view);
        p4MaterialConfig.validate(new ValidationContext(null));
        assertThat(p4MaterialConfig.errors().on(expectedKeyForError), is(nullValue()));
    }

    private void assertError(String port, String view, String expectedKeyForError, String expectedErrorMessage) {
        P4MaterialConfig p4MaterialConfig = new P4MaterialConfig(port, view);
        p4MaterialConfig.validate(new ValidationContext(null));
        assertThat(p4MaterialConfig.errors().on(expectedKeyForError), is(expectedErrorMessage));
    }
}
