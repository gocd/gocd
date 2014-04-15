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

package com.thoughtworks.go.server.util;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.security.X509AuthoritiesPopulator;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import com.thoughtworks.go.server.security.userdetail.GoUserPrinciple;
import org.junit.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.security.Authentication;
import org.springframework.security.GrantedAuthority;
import org.springframework.security.GrantedAuthorityImpl;
import org.springframework.security.context.SecurityContext;
import org.springframework.security.context.SecurityContextHolder;
import org.springframework.security.providers.TestingAuthenticationToken;
import org.springframework.security.userdetails.User;
import org.springframework.security.userdetails.ldap.LdapUserDetailsImpl;

public class UserHelperTest {

    @Test
    public void shouldUnderstandByACEGIRoleWetherAgent() {
        stubSecurityContextForRole(X509AuthoritiesPopulator.ROLE_AGENT);
        assertThat(UserHelper.isAgent(), is(true));
        stubSecurityContextForRole("junk");
        assertThat(UserHelper.isAgent(), is(false));
        stubSecurityContextForRole(null);
        assertThat(UserHelper.isAgent(), is(false));
        stubSecurityContextForGrantedAuthorities(null);
        assertThat(UserHelper.isAgent(), is(false));
    }

    public static void stubSecurityContextForRole(String roleName) {
        GrantedAuthority agentAuth = mock(GrantedAuthority.class);
        when(agentAuth.getAuthority()).thenReturn(roleName);
        GrantedAuthority[] grantedAuthorities = roleName == null ? new GrantedAuthority[] {} : new GrantedAuthority[] {agentAuth};
        stubSecurityContextForGrantedAuthorities(grantedAuthorities);
    }

    private static void stubSecurityContextForGrantedAuthorities(GrantedAuthority[] grantedAuthorities) {
        SecurityContext context = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);
        when(context.getAuthentication()).thenReturn(authentication);
        when(authentication.getAuthorities()).thenReturn(grantedAuthorities);
        SecurityContextHolder.setContext(context);
    }

    @Test
    public void shouldGetNameFromUserDetails() {
        TestingAuthenticationToken authentication = new TestingAuthenticationToken(
                new User("user", "pass", true, false, true, true, new GrantedAuthority[0]), null, null);
        assertThat(UserHelper.getUserName(authentication).getDisplayName(), is("user"));
    }

    @Test
    public void shouldGetFullNameFromLdapUserDetails() {
        TestingAuthenticationToken authentication = new TestingAuthenticationToken(new LdapUserDetailsImpl() {
            public String getUsername() {
                return "test1";
            }

            public String getDn() {
                return "cn=Test User, ou=Beijing, ou=Employees, ou=Enterprise, ou=Principal";
            }
        }, null, null);
        assertThat(UserHelper.getUserName(authentication).getDisplayName(), is("Test User"));
    }

    @Test
    public void shouldGetNameFromLdapUserDetailsIfCannotGetFullName() {
        TestingAuthenticationToken authentication = new TestingAuthenticationToken(new LdapUserDetailsImpl() {
            public String getUsername() {
                return "test1";
            }

            public String getDn() {
                return "n=Test User, ou=Beijing, ou=Employees, ou=Enterprise, ou=Principal";
            }
        }, null, null);
        assertThat(UserHelper.getUserName(authentication).getDisplayName(), is("test1"));
    }

    @Test
    public void shouldReturnTrueWhenCheckIsAgentIfGrantedAuthorityContainsAgentRole() {
        TestingAuthenticationToken authentication = new TestingAuthenticationToken(null, null,
                new GrantedAuthorityImpl[]{new GrantedAuthorityImpl("ROLE_AGENT")});
        assertThat(UserHelper.matchesRole(authentication, X509AuthoritiesPopulator.ROLE_AGENT), is(true));
    }

    @Test
    public void shouldReturnFalseWhenCheckIsAgentIfGrantedAuthorityNotContainsAgentRole() {
        TestingAuthenticationToken authentication = new TestingAuthenticationToken(null, null,
                new GrantedAuthorityImpl[]{new GrantedAuthorityImpl("anything")});
        assertThat(UserHelper.matchesRole(authentication, X509AuthoritiesPopulator.ROLE_AGENT), is(false));
    }

    @Test
    public void shouldGetDisplayNameForAPasswordFileUser() {
        GrantedAuthority[] authorities = {new GrantedAuthorityImpl("anything")};
        TestingAuthenticationToken authentication = new TestingAuthenticationToken(new GoUserPrinciple("user", "Full Name", "password", true, true, true, true, authorities), null, authorities);
        assertThat(UserHelper.getUserName(authentication), is(new Username(new CaseInsensitiveString("user"), "Full Name")));
    }

    @Test
    public void shouldSetUserIdIntoSession(){
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpSession session = mock(HttpSession.class);
        when(request.getSession()).thenReturn(session);

        UserHelper.setUserId(request, 123L);

        verify(session).setAttribute("USERID",123L);
    }
    @Test
    public void shouldGetUserIdFromSession(){
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpSession session = mock(HttpSession.class);
        when(request.getSession()).thenReturn(session);
        when(session.getAttribute("USERID")).thenReturn(123L);
        assertThat(UserHelper.getUserId(request), is(123L));
    }
}
