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

package com.thoughtworks.go.server.security;

import com.thoughtworks.go.config.AdminsConfig;
import com.thoughtworks.go.config.LdapConfig;
import com.thoughtworks.go.config.PasswordFileConfig;
import com.thoughtworks.go.config.server.security.ldap.BaseConfig;
import com.thoughtworks.go.config.server.security.ldap.BasesConfig;
import com.thoughtworks.go.util.GoConfigFileHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.mockito.Mockito.*;


public class LdapConfigChangedListenerTest {
    private GoConfigFileHelper helper;

    @Before
    public void setUp() throws IOException {
        helper = new GoConfigFileHelper();
        helper.onSetUp();
    }

    @After
    public void tearDown() {

        helper.onTearDown();
    }

    @Test
    public void shouldTriggerReintializeOfContextFactoryOnChangeOnLdapConfig() {
        LdapConfig oldLdapConfig = new LdapConfig("oldOne", "manager", "pwd", null, true, new BasesConfig(new BaseConfig("foo")), "bar", "foo");
        LdapConfig newLdapConfig = new LdapConfig("newOne", "manager", "pwd", null, true, new BasesConfig(new BaseConfig("foo")), "bar", "foo");
        helper.addLdapSecurityWith(oldLdapConfig, true, new PasswordFileConfig(), new AdminsConfig());

        LdapContextFactory mockContextFactory = mock(LdapContextFactory.class);
        LdapConfigChangedListener listener = new LdapConfigChangedListener(oldLdapConfig, mockContextFactory);

        helper.addLdapSecurityWith(newLdapConfig, true, new PasswordFileConfig(), new AdminsConfig());

        listener.onConfigChange(helper.currentConfig());
        verify(mockContextFactory, times(1)).initializeDelegator();
    }

    @Test
    public void shouldNotTriggerReintializeOfContextFactoryWhenLdapConfigDoesNotChange() {
        LdapConfig oldLdapConfig = new LdapConfig("oldOne", "manager", "pwd", null, true, new BasesConfig(new BaseConfig("foo")), "bar", "foo");
        helper.addLdapSecurityWith(oldLdapConfig, true, new PasswordFileConfig(), new AdminsConfig());

        LdapContextFactory mockContextFactory = mock(LdapContextFactory.class);
        LdapConfigChangedListener listener = new LdapConfigChangedListener(oldLdapConfig, mockContextFactory);

        helper.addLdapSecurityWith(oldLdapConfig, true, new PasswordFileConfig(), new AdminsConfig());

        listener.onConfigChange(helper.currentConfig());
        verify(mockContextFactory, never()).initializeDelegator();
    }

    @Test
    public void shouldReinitializeDelegator_whenLdapManagerPasswordChanges() {
        LdapConfig oldLdapConfig = new LdapConfig("oldOne", "manager", "pwd", null, true, new BasesConfig(new BaseConfig("foo")), "bar", "foo");
        helper.addLdapSecurityWith(oldLdapConfig, true, new PasswordFileConfig(), new AdminsConfig());

        LdapContextFactory mockContextFactory = mock(LdapContextFactory.class);
        LdapConfigChangedListener listener = new LdapConfigChangedListener(oldLdapConfig, mockContextFactory);

        LdapConfig newLdapConfig = new LdapConfig("oldOne", "manager", "new_pwd", null, true, new BasesConfig(new BaseConfig("foo")), "bar", "foo");

        helper.addLdapSecurityWith(newLdapConfig, true, new PasswordFileConfig(), new AdminsConfig());

        listener.onConfigChange(helper.currentConfig());

        verify(mockContextFactory).initializeDelegator();
    }

}
