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

import java.util.HashMap;
import java.util.Map;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.ValidationContext;
import com.thoughtworks.go.config.materials.AbstractMaterialConfig;
import com.thoughtworks.go.config.materials.Filter;
import com.thoughtworks.go.config.materials.IgnoredFiles;
import com.thoughtworks.go.config.materials.ScmMaterialConfig;
import com.thoughtworks.go.config.materials.svn.SvnMaterialConfig;
import com.thoughtworks.go.security.GoCipher;
import com.thoughtworks.go.util.ReflectionUtil;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

public class P4MaterialConfigTest {

    @Before
    public void setUp() throws Exception {
    }

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
    @Ignore("the xsd validation doesn't allow \\d+ without a prefixed (:), but p4 actually supports \\d+ as the value of port(according to http://www.perforce.com/perforce/doc.current/manuals/cmdref/env.P4PORT.html)")
    public void shouldAllowPortWithoutPrefixedColon() {
        P4MaterialConfig p4MaterialConfig = new P4MaterialConfig("1818", "view");
        p4MaterialConfig.validate(new ValidationContext(null));
        assertThat(p4MaterialConfig.errors().isEmpty(), is(true));
    }

    @Test
    public void validate_shouldEnsureThatViewIsNotBlank() {
        P4MaterialConfig p4MaterialConfig = new P4MaterialConfig("example.com:1233", "");
        p4MaterialConfig.validate(new ValidationContext(null));
        assertThat(p4MaterialConfig.errors().on(P4MaterialConfig.VIEW), is("P4 view cannot be empty."));
    }

    @Test
    public void validate_shouldThrowValidationErrorsWhenP4PortIsNotInCorrectFormat() {
        P4MaterialConfig p4MaterialConfig = new P4MaterialConfig("", "view");
        p4MaterialConfig.validate(new ValidationContext(null));
        assertThat(p4MaterialConfig.errors().on(P4MaterialConfig.SERVER_AND_PORT), is("P4 port cannot be empty."));

        p4MaterialConfig = new P4MaterialConfig("INVALID_PORT", "view");
        p4MaterialConfig.validate(new ValidationContext(null));
        assertThat(p4MaterialConfig.errors().on(P4MaterialConfig.SERVER_AND_PORT), is("Invalid format for P4 port. It should be host:port"));
        p4MaterialConfig = new P4MaterialConfig("12332", "view");
        p4MaterialConfig.validate(new ValidationContext(null));
        assertThat(p4MaterialConfig.errors().on(P4MaterialConfig.SERVER_AND_PORT), is("Invalid format for P4 port. It should be host:port"));
        p4MaterialConfig = new P4MaterialConfig(":1818", "view");
        p4MaterialConfig.validate(new ValidationContext(null));
        assertThat(p4MaterialConfig.errors().on(P4MaterialConfig.SERVER_AND_PORT), is("Invalid format for P4 port. It should be host:port"));
    }

    @Test
    public void validate_shouldNotThrowValidationErrorsIfP4PortIsInCorrectFormat() {
        P4MaterialConfig p4MaterialConfig = new P4MaterialConfig("example.com:1818", "view");
        p4MaterialConfig.validate(new ValidationContext(null));
        assertThat(p4MaterialConfig.errors().isEmpty(), is(true));

        p4MaterialConfig = new P4MaterialConfig("198.168.0.123:1818", "view");
        p4MaterialConfig.validate(new ValidationContext(null));
        assertThat(p4MaterialConfig.errors().isEmpty(), is(true));
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

    @Test
    public void shouldGetAttributesWithSecureFields() {
        P4MaterialConfig material = new P4MaterialConfig("host:1234", "view", "username");
        material.setPassword("password");
        material.setUseTickets(true);
        Map<String, Object> attributes = material.getAttributes(true);

        assertThat((String) attributes.get("type"), is("perforce"));
        Map<String, Object> configuration = (Map<String, Object>) attributes.get("perforce-configuration");
        assertThat((String) configuration.get("url"), is("host:1234"));
        assertThat((String) configuration.get("username"), is("username"));
        assertThat((String) configuration.get("password"), is("password"));
        assertThat((String) configuration.get("view"), is("view"));
        assertThat((Boolean) configuration.get("use-tickets"), is(true));
    }

    @Test
    public void shouldGetAttributesWithoutSecureFields() {
        P4MaterialConfig material = new P4MaterialConfig("host:1234", "view", "username");
        material.setPassword("password");
        material.setUseTickets(true);
        Map<String, Object> attributes = material.getAttributes(false);

        assertThat((String) attributes.get("type"), is("perforce"));
        Map<String, Object> configuration = (Map<String, Object>) attributes.get("perforce-configuration");
        assertThat((String) configuration.get("url"), is("host:1234"));
        assertThat((String) configuration.get("username"), is("username"));
        assertThat(configuration.get("password"), is(nullValue()));
        assertThat((String) configuration.get("view"), is("view"));
        assertThat((Boolean) configuration.get("use-tickets"), is(true));
    }
}
