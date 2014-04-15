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
import com.thoughtworks.go.config.PasswordFileConfig;
import com.thoughtworks.go.config.SecurityConfig;
import com.thoughtworks.go.security.GoCipher;
import com.thoughtworks.go.server.service.GoConfigService;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class LdapContextFactoryTest {

    private GoConfigService goConfigService;
    private GoCipher goCipher;

    @Before
    public void setUp() {
        goConfigService = mock(GoConfigService.class);
        goCipher = mock(GoCipher.class);
    }

    @Test
    public void shouldNotInitializeDelegatorWhenAnLDAPConfigurationIsRemovedFromOurConfig() {
        LdapContextFactory factory = new LdapContextFactory(goConfigService);
        when(goConfigService.security()).thenReturn(new SecurityConfig(new LdapConfig(goCipher), new PasswordFileConfig(), true));
        try {
            factory.initializeDelegator();
        } catch (Exception e) {
            e.printStackTrace();
            fail("should not have thrown an execption");
        }
        verify(goConfigService).security();
    }
}
