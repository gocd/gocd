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

package com.thoughtworks.go.server.security.providers;

import com.thoughtworks.go.config.LdapConfig;
import com.thoughtworks.go.config.SecurityConfig;
import com.thoughtworks.go.config.server.security.ldap.BasesConfig;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.util.SystemEnvironment;
import org.hamcrest.core.Is;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.security.ldap.LdapAuthoritiesPopulator;
import org.springframework.security.providers.UsernamePasswordAuthenticationToken;

import java.io.IOException;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class LdapAuthenticationProviderTest {
    private LdapAuthenticationProvider ldapAuthenticationProvider;
    @Mock private GoConfigService goConfigService;
    @Mock private SystemEnvironment systemEnvironment;
    @Mock private org.springframework.security.providers.ldap.LdapAuthenticator ldapAuthenticator;
    @Mock private LdapAuthoritiesPopulator ldapAuthoritiesPopulator;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        ldapAuthenticationProvider = new LdapAuthenticationProvider(goConfigService, ldapAuthenticator, ldapAuthoritiesPopulator, systemEnvironment);
    }

    @Test
    public void shouldNotSupportAuthenticationIfNoLdapConfig() throws IOException {
        when(goConfigService.security()).thenReturn(new SecurityConfig());
        assertThat(ldapAuthenticationProvider.supports(UsernamePasswordAuthenticationToken.class), is(false));
    }

    @Test
    public void shouldNotSupportAuthenticationIfInbuiltLdapIsDisabled() throws IOException {
        when(goConfigService.security()).thenReturn(new SecurityConfig(new LdapConfig("uri", "", "", "", false, new BasesConfig(),"" ), null, true));
        when(systemEnvironment.inbuiltLdapPasswordAuthEnabled()).thenReturn(false);
        assertThat(ldapAuthenticationProvider.supports(UsernamePasswordAuthenticationToken.class), Is.is(false));
    }

    @Test
    public void shouldSupportAuthenticationIfInbuiltLdapIsEnabledAndConfigured() throws IOException {
        when(goConfigService.security()).thenReturn(new SecurityConfig(new LdapConfig("uri", "", "", "", false, new BasesConfig(),"" ), null, true));
        when(systemEnvironment.inbuiltLdapPasswordAuthEnabled()).thenReturn(true);
        assertThat(ldapAuthenticationProvider.supports(UsernamePasswordAuthenticationToken.class), Is.is(true));
    }


}