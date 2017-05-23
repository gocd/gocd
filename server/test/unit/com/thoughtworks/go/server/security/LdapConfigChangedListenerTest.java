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

import java.io.IOException;

import com.thoughtworks.go.config.AdminsConfig;
import com.thoughtworks.go.config.LdapConfig;
import com.thoughtworks.go.config.PasswordFileConfig;
import com.thoughtworks.go.config.server.security.ldap.BaseConfig;
import com.thoughtworks.go.config.server.security.ldap.BasesConfig;
import com.thoughtworks.go.util.GoConfigFileHelper;
import com.thoughtworks.go.util.SystemEnvironment;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;


public class LdapConfigChangedListenerTest {
    private GoConfigFileHelper helper;

    @Before
    public void setUp() throws IOException {
        helper = new GoConfigFileHelper();
        helper.onSetUp();
        new SystemEnvironment().set(SystemEnvironment.INBUILT_LDAP_PASSWORD_AUTH_ENABLED, true);
    }

    @After
    public void tearDown() {
        helper.onTearDown();
        new SystemEnvironment().set(SystemEnvironment.INBUILT_LDAP_PASSWORD_AUTH_ENABLED, false);
    }

    @Test
    public void shouldTriggerReintializeOfContextFactoryOnChangeOnLdapConfig() {
        LdapConfig oldLdapConfig = new LdapConfig("oldOne", "manager", "pwd", null, true, new BasesConfig(new BaseConfig("foo")), "bar");
        LdapConfig newLdapConfig = new LdapConfig("newOne", "manager", "pwd", null, true, new BasesConfig(new BaseConfig("foo")), "bar");
        helper.addLdapSecurityWith(oldLdapConfig, true, new PasswordFileConfig(), new AdminsConfig());

        LdapContextFactory mockContextFactory = mock(LdapContextFactory.class);
        LdapConfigChangedListener listener = new LdapConfigChangedListener(oldLdapConfig, mockContextFactory);

        helper.addLdapSecurityWith(newLdapConfig, true, new PasswordFileConfig(), new AdminsConfig());

        listener.onConfigChange(helper.currentConfig());
        verify(mockContextFactory, times(1)).initializeDelegator();
    }

    @Test
    public void shouldNotTriggerReintializeOfContextFactoryWhenLdapConfigDoesNotChange() {
        LdapConfig oldLdapConfig = new LdapConfig("oldOne", "manager", "pwd", null, true, new BasesConfig(new BaseConfig("foo")), "bar");
        helper.addLdapSecurityWith(oldLdapConfig, true, new PasswordFileConfig(), new AdminsConfig());

        LdapContextFactory mockContextFactory = mock(LdapContextFactory.class);
        LdapConfigChangedListener listener = new LdapConfigChangedListener(oldLdapConfig, mockContextFactory);

        helper.addLdapSecurityWith(oldLdapConfig, true, new PasswordFileConfig(), new AdminsConfig());

        listener.onConfigChange(helper.currentConfig());
        verify(mockContextFactory, never()).initializeDelegator();
    }

    @Test
    public void shouldReinitializeDelegator_whenLdapManagerPasswordChanges() {
        LdapConfig oldLdapConfig = new LdapConfig("oldOne", "manager", "pwd", null, true, new BasesConfig(new BaseConfig("foo")), "bar");
        helper.addLdapSecurityWith(oldLdapConfig, true, new PasswordFileConfig(), new AdminsConfig());

        LdapContextFactory mockContextFactory = mock(LdapContextFactory.class);
        LdapConfigChangedListener listener = new LdapConfigChangedListener(oldLdapConfig, mockContextFactory);

        LdapConfig newLdapConfig = new LdapConfig("oldOne", "manager", "new_pwd", null, true, new BasesConfig(new BaseConfig("foo")), "bar");

        helper.addLdapSecurityWith(newLdapConfig, true, new PasswordFileConfig(), new AdminsConfig());
        
        listener.onConfigChange(helper.currentConfig());

        verify(mockContextFactory).initializeDelegator();
    }

}
