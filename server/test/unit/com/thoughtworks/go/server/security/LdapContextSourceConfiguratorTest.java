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

import com.thoughtworks.go.config.LdapConfig;
import com.thoughtworks.go.config.server.security.ldap.BaseConfig;
import com.thoughtworks.go.config.server.security.ldap.BasesConfig;
import com.thoughtworks.go.security.GoCipher;
import org.junit.Test;
import org.springframework.ldap.core.support.AbstractContextSource;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class LdapContextSourceConfiguratorTest {
    @Test
    public void shouldSet_ManagerDnAndPassword_OnContextSource() {
        LdapContextSourceConfigurator configurator = new LdapContextSourceConfigurator(new LdapConfig("uri", "managerDn", "managerPass", null, true, new BasesConfig(new BaseConfig("searchBase")),
                "searchFilter", "displayName"));
        AbstractContextSource ctxSrc = mock(AbstractContextSource.class);
        configurator.configure(ctxSrc);
        verify(ctxSrc).setPassword("managerPass");
        verify(ctxSrc).setUserDn("managerDn");
    }

    @Test
    public void shouldNotSet_EitherManagerDn_OrPassword_OnContextSource_WhenNoManagerDnConfigured() {
        GoCipher goCipher = new GoCipher();
        LdapContextSourceConfigurator configurator = new LdapContextSourceConfigurator(new LdapConfig(goCipher));
        AbstractContextSource ctxSrc = mock(AbstractContextSource.class);
        configurator.configure(ctxSrc);
        verify(ctxSrc, never()).setPassword(any(String.class));
        verify(ctxSrc, never()).setUserDn(any(String.class));
    }
}
