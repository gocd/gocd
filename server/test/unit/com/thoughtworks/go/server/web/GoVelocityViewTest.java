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

package com.thoughtworks.go.server.web;

import javax.servlet.http.HttpServletRequest;

import com.thoughtworks.go.server.security.GoAuthority;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.context.Context;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.GrantedAuthority;
import org.springframework.security.GrantedAuthorityImpl;

import static org.springframework.security.context.HttpSessionContextIntegrationFilter.SPRING_SECURITY_CONTEXT_KEY;
import org.springframework.security.context.SecurityContextImpl;
import org.springframework.security.providers.TestingAuthenticationToken;
import org.springframework.security.userdetails.User;
import org.springframework.security.userdetails.ldap.LdapUserDetailsImpl;

public class GoVelocityViewTest {
    private GoVelocityView view;
    private HttpServletRequest request;
    private Context velocityContext;
    private SecurityContextImpl securityContext;

    @Before
    public void setUp() throws Exception {
        view = new GoVelocityView();
        request = new MockHttpServletRequest();
        velocityContext = new VelocityContext();
        securityContext = new SecurityContextImpl();
    }

    @Test
    public void shouldRetriveLdapCompleteNameFromSessionWhenAuthenticated() throws Exception {
        securityContext.setAuthentication(new TestingAuthenticationToken(new LdapUserDetailsImpl() {
            public String getUsername() {
                return "test1";
            }
            public String getDn() {
                return "cn=Test User, ou=Beijing, ou=Employees, ou=Enterprise, ou=Principal";
            }
        }, null, null));
        request.getSession().setAttribute(SPRING_SECURITY_CONTEXT_KEY, securityContext);
        view.exposeHelpers(velocityContext, request);
        assertThat((String) velocityContext.get(GoVelocityView.PRINCIPAL), is("Test User"));
    }

    @Test
    public void shouldSetAdministratorIfUserIsAdministrator() throws Exception {
        securityContext.setAuthentication(
                new TestingAuthenticationToken("jez", "badger",
                        new GrantedAuthority[]{new GrantedAuthorityImpl(GoAuthority.ROLE_SUPERVISOR.toString())}));
        request.getSession().setAttribute(SPRING_SECURITY_CONTEXT_KEY, securityContext);
        view.exposeHelpers(velocityContext, request);
        assertThat((Boolean) velocityContext.get(GoVelocityView.ADMINISTRATOR), is(true));
    }

    @Test
    public void shouldSetTemplateAdministratorIfUserIsTemplateAdministrator() throws Exception {
        securityContext.setAuthentication(
                new TestingAuthenticationToken("jez", "badger",
                        new GrantedAuthority[]{new GrantedAuthorityImpl(GoAuthority.ROLE_TEMPLATE_SUPERVISOR.toString())}));
        request.getSession().setAttribute(SPRING_SECURITY_CONTEXT_KEY, securityContext);
        view.exposeHelpers(velocityContext, request);
        assertThat((Boolean) velocityContext.get(GoVelocityView.TEMPLATE_ADMINISTRATOR), is(true));
    }

    @Test
    public void shouldSetViewAdministratorRightsIfUserHasAnyLevelOfAdministratorRights() throws Exception {
        securityContext.setAuthentication(new TestingAuthenticationToken("jez", "badger", new GrantedAuthority[]{new GrantedAuthorityImpl(GoAuthority.ROLE_TEMPLATE_SUPERVISOR.toString())}));
        request.getSession().setAttribute(SPRING_SECURITY_CONTEXT_KEY, securityContext);
        view.exposeHelpers(velocityContext, request);
        assertThat((Boolean) velocityContext.get(GoVelocityView.VIEW_ADMINISTRATOR_RIGHTS), is(true));

        securityContext.setAuthentication(new TestingAuthenticationToken("jez", "badger", new GrantedAuthority[]{new GrantedAuthorityImpl(GoAuthority.ROLE_GROUP_SUPERVISOR.toString())}));
        request.getSession().setAttribute(SPRING_SECURITY_CONTEXT_KEY, securityContext);
        view.exposeHelpers(velocityContext, request);
        assertThat((Boolean) velocityContext.get(GoVelocityView.VIEW_ADMINISTRATOR_RIGHTS), is(true));

        securityContext.setAuthentication(new TestingAuthenticationToken("jez", "badger", new GrantedAuthority[]{new GrantedAuthorityImpl(GoAuthority.ROLE_SUPERVISOR.toString())}));
        request.getSession().setAttribute(SPRING_SECURITY_CONTEXT_KEY, securityContext);
        view.exposeHelpers(velocityContext, request);
        assertThat((Boolean) velocityContext.get(GoVelocityView.VIEW_ADMINISTRATOR_RIGHTS), is(true));

        securityContext.setAuthentication(new TestingAuthenticationToken("jez", "badger", new GrantedAuthority[]{new GrantedAuthorityImpl(GoAuthority.ROLE_USER.toString())}));
        request.getSession().setAttribute(SPRING_SECURITY_CONTEXT_KEY, securityContext);
        view.exposeHelpers(velocityContext, request);
        assertThat(velocityContext.get(GoVelocityView.VIEW_ADMINISTRATOR_RIGHTS), is(nullValue()));
    }

    @Test
    public void shouldSetGroupAdministratorIfUserIsAPipelineGroupAdministrator() throws Exception {
        securityContext.setAuthentication(
                new TestingAuthenticationToken("jez", "badger",
                        new GrantedAuthority[]{new GrantedAuthorityImpl(GoAuthority.ROLE_GROUP_SUPERVISOR.toString())}));
        request.getSession().setAttribute(SPRING_SECURITY_CONTEXT_KEY, securityContext);
        view.exposeHelpers(velocityContext, request);
        assertThat((Boolean) velocityContext.get(GoVelocityView.ADMINISTRATOR), is(nullValue()));
        assertThat((Boolean) velocityContext.get(GoVelocityView.GROUP_ADMINISTRATOR), is(true));
    }

    @Test
    public void shouldNotSetPrincipalIfNoSession() throws Exception {
        view.exposeHelpers(velocityContext, request);
        assertNull("Principal should be null", velocityContext.get(GoVelocityView.PRINCIPAL));
    }

    @Test
    public void shouldNotSetPrincipalIfAuthenticationInformationNotAvailable() throws Exception {
        request.getSession().setAttribute(SPRING_SECURITY_CONTEXT_KEY, securityContext);
        view.exposeHelpers(velocityContext, request);
        assertNull("Principal should be null", velocityContext.get(GoVelocityView.PRINCIPAL));
    }

    @Test
    public void principalIsTheUsernameWhenNothingElseAvailable() throws Exception {
        request.getSession().setAttribute(SPRING_SECURITY_CONTEXT_KEY, securityContext);
        securityContext.setAuthentication(
                new TestingAuthenticationToken(
                        new User("Test User", "pwd", true, new GrantedAuthority[]{new GrantedAuthorityImpl("nowt")}),
                        null, null));
        view.exposeHelpers(velocityContext, request);
        assertThat((String) velocityContext.get(GoVelocityView.PRINCIPAL), is("Test User"));
    }
}
