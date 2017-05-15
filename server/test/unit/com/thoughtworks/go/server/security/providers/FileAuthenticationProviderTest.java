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

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.LdapConfig;
import com.thoughtworks.go.config.PasswordFileConfig;
import com.thoughtworks.go.config.SecurityConfig;
import com.thoughtworks.go.security.GoCipher;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.security.AuthorityGranter;
import com.thoughtworks.go.server.security.GoAuthority;
import com.thoughtworks.go.server.security.userdetail.GoUserPrinciple;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.SecurityService;
import com.thoughtworks.go.server.service.UserService;
import com.thoughtworks.go.util.StringUtil;
import com.thoughtworks.go.util.TestFileUtil;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FileAuthenticationProviderTest {
    private GoConfigService goConfigService;
    private SecurityService securityService;

    private static final String SHA1_BADGER = StringUtil.sha1Digest("badger".getBytes());
    private UserService userService;

    @Before
    public void setup() {
        securityService = mock(SecurityService.class);
        goConfigService = mock(GoConfigService.class);
        userService = mock(UserService.class);
    }

    @Test
    public void shouldRetrieveDetailsIfUsernameSpecifiedInFile() throws Exception {
        setupFile("jez=" + SHA1_BADGER);
        AuthorityGranter authorityGranter = new AuthorityGranter(securityService);
        when(securityService.isUserAdmin(new Username(new CaseInsensitiveString("jez")))).thenReturn(true);
        when(userService.findUserByName("jez")).thenReturn(new com.thoughtworks.go.domain.User("jez", "Jezz Humbles", "jez@humble.com"));

        FileAuthenticationProvider provider = new FileAuthenticationProvider(goConfigService, authorityGranter, userService, securityService);
        final UserDetails details = provider.retrieveUser("jez", null);
        assertThat(new ArrayList<>(details.getAuthorities()).get(0), is(new SimpleGrantedAuthority("ROLE_SUPERVISOR")));
        assertThat(details.isAccountNonExpired(), is(true));
        assertThat(details.isAccountNonLocked(), is(true));
        assertThat(details.isCredentialsNonExpired(), is(true));
        assertThat(details.isEnabled(), is(true));
        assertThat(details.getUsername(), is("jez"));
        assertThat(details.getPassword(), is(SHA1_BADGER));
    }

    @Test(expected = UsernameNotFoundException.class)
    public void shouldThrowExceptionIfUsernameIsNotSpecifiedInFile() throws Exception {
        setupFile("jez=" + SHA1_BADGER);
        AuthorityGranter authorityGranter = new AuthorityGranter(securityService);
        FileAuthenticationProvider provider = new FileAuthenticationProvider(goConfigService, authorityGranter, userService, securityService);
        provider.retrieveUser("blah", null);
    }

    @Test(expected = UsernameNotFoundException.class)
    public void shouldThrowExceptionIfFileDoesNotExist() throws Exception {
        when(goConfigService.security()).thenReturn(new SecurityConfig(new LdapConfig(new GoCipher()), new PasswordFileConfig("ueyrweiyri"), true, null));

        AuthorityGranter authorityGranter = new AuthorityGranter(securityService);
        FileAuthenticationProvider provider = new FileAuthenticationProvider(goConfigService, authorityGranter, userService, securityService);
        provider.retrieveUser("blah", null);
    }

    @Test(expected = BadCredentialsException.class)
    public void shouldNotUserWithoutValidPassword() throws Exception {
        AuthorityGranter authorityGranter = new AuthorityGranter(securityService);
        FileAuthenticationProvider provider = new FileAuthenticationProvider(goConfigService, authorityGranter, userService, securityService);
        UserDetails user = new User("jez", "something", true, true, true, true, Collections.emptyList());
        provider.additionalAuthenticationChecks(user, new UsernamePasswordAuthenticationToken("jez", "nothing"));
    }

    @Test
    public void shouldAuthenticateUserWithValidPassword() throws Exception {
        AuthorityGranter authorityGranter = new AuthorityGranter(securityService);
        FileAuthenticationProvider provider = new FileAuthenticationProvider(goConfigService, authorityGranter, userService, securityService);
        UserDetails user = new User("jez", SHA1_BADGER, true, true, true, true, Collections.emptyList());
        provider.additionalAuthenticationChecks(user, new UsernamePasswordAuthenticationToken("jez", "badger"));
    }

    @Test
    public void shouldStripOutAuthoritiesThatIsSpecifiedInPasswordFile() throws Exception {
        setupFile("jez=" + SHA1_BADGER + ",ROLE_OF_GOD");
        when(securityService.isUserAdmin(new Username(new CaseInsensitiveString("jez")))).thenReturn(true);
        when(userService.findUserByName("jez")).thenReturn(new com.thoughtworks.go.domain.User("jez", "Jezz Humbles", "jez@humble.com"));

        AuthorityGranter authorityGranter = new AuthorityGranter(securityService);
        FileAuthenticationProvider provider = new FileAuthenticationProvider(goConfigService, authorityGranter, userService, securityService);
        final GoUserPrinciple details = (GoUserPrinciple) provider.retrieveUser("jez", null);
        assertThat(details.getUsername(), is("jez"));
        assertThat(details.getDisplayName(), is("Jezz Humbles"));
        assertThat(details.getAuthorities().size(), is(2));

        List<GrantedAuthority> grantedAuthorities = new ArrayList<>(details.getAuthorities());
        assertThat(grantedAuthorities.get(0), is(GoAuthority.ROLE_SUPERVISOR.asAuthority()));
        assertThat(grantedAuthorities.get(1), is(GoAuthority.ROLE_USER.asAuthority()));
    }

    @Test
    public void shouldReturnUserPrincipleWithTheRightDisplayName() throws Exception {
        setupFile(String.format("jez=%s\ncharan=%s\nbabe=%s", SHA1_BADGER, SHA1_BADGER, SHA1_BADGER));
        when(userService.findUserByName("jez")).thenReturn(new com.thoughtworks.go.domain.User("jez", "Jezz Humbles", "jez@humble.com"));
        when(userService.findUserByName("charan")).thenReturn(new com.thoughtworks.go.domain.User("charan", "", "ch@ar.an"));

        FileAuthenticationProvider provider = new FileAuthenticationProvider(goConfigService, new AuthorityGranter(securityService), userService, securityService);
        GoUserPrinciple details = (GoUserPrinciple) provider.retrieveUser("jez", null);

        assertThat(details.getUsername(), is("jez"));
        assertThat(details.getDisplayName(), is("Jezz Humbles"));

        details = (GoUserPrinciple) provider.retrieveUser("charan", null);

        assertThat(details.getUsername(), is("charan"));
        assertThat(details.getDisplayName(), is("charan"));

        details = (GoUserPrinciple) provider.retrieveUser("babe", null);

        assertThat(details.getUsername(), is("babe"));
        assertThat(details.getDisplayName(), is("babe"));
    }

    @Test
    public void shouldHandleApacheFormatFile() throws IOException {
        setupFile("cread:{SHA}OPhRtj5TCERacn3mvwItERz8uCk=");
        when(securityService.isUserAdmin(new Username(new CaseInsensitiveString("cread")))).thenReturn(true);
        when(userService.findUserByName("cread")).thenReturn(new com.thoughtworks.go.domain.User("cread", "Chriss Readds", "cread@humble.com"));

        AuthorityGranter authorityGranter = new AuthorityGranter(securityService);
        FileAuthenticationProvider provider = new FileAuthenticationProvider(goConfigService, authorityGranter, userService, securityService);
        final UserDetails details = provider.retrieveUser("cread", null);
        assertThat(new ArrayList<>(details.getAuthorities()).get(0), is(new SimpleGrantedAuthority("ROLE_SUPERVISOR")));
        assertThat(details.isAccountNonExpired(), is(true));
        assertThat(details.isAccountNonLocked(), is(true));
        assertThat(details.isCredentialsNonExpired(), is(true));
        assertThat(details.isEnabled(), is(true));
        assertThat(details.getUsername(), is("cread"));
        assertThat(details.getPassword(), is("OPhRtj5TCERacn3mvwItERz8uCk="));
    }

    private void setupFile(String userAndPasswordAndRoles) throws IOException {
        final File passwordFile = TestFileUtil.createTempFile("password.properties");
        passwordFile.deleteOnExit();
        FileUtils.writeStringToFile(passwordFile, userAndPasswordAndRoles);
        final SecurityConfig securityConfig = new SecurityConfig(new LdapConfig(new GoCipher()),
                new PasswordFileConfig(passwordFile.getAbsolutePath()), true, null);
        when(goConfigService.security()).thenReturn(securityConfig);
    }

    @Test
    public void shouldNotEngageWhenPasswordFileIsNotConfigured() throws Exception {
        FileAuthenticationProvider provider = new FileAuthenticationProvider(goConfigService, null, userService, securityService);
        when(goConfigService.security()).thenReturn(new SecurityConfig(null, new PasswordFileConfig(), true));
        assertThat(provider.supports(UsernamePasswordAuthenticationToken.class), is(false));
    }
}
