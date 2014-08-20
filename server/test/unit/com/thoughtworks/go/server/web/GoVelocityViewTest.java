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
import javax.servlet.http.HttpSession;

import com.thoughtworks.go.server.security.GoAuthority;
import com.thoughtworks.go.util.FileUtil;
import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.context.Context;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.handler.ContextHandler;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.GrantedAuthority;
import org.springframework.security.GrantedAuthorityImpl;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.context.HttpSessionContextIntegrationFilter.SPRING_SECURITY_CONTEXT_KEY;
import org.springframework.security.context.SecurityContextImpl;
import org.springframework.security.providers.TestingAuthenticationToken;
import org.springframework.security.userdetails.User;
import org.springframework.security.userdetails.ldap.LdapUserDetailsImpl;

import java.io.File;
import java.util.UUID;

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

    @Test
    public void shouldSetJavascriptPathVariableWhenRailsNewWithCompressedJavascriptsIsUsed() throws Exception {
        File assetsDir = FileUtil.createTempFolder("assets-" + UUID.randomUUID().toString());
        FileUtil.writeContentToFile(json, new File(assetsDir, "manifest-digest.json"));
        SystemEnvironment systemEnvironment = mock(SystemEnvironment.class);
        when(systemEnvironment.get(SystemEnvironment.USE_NEW_RAILS)).thenReturn(true);
        when(systemEnvironment.useCompressedJs()).thenReturn(true);
        GoVelocityView view = new GoVelocityView(systemEnvironment);
        Request servletRequest = mock(Request.class);
        ContextHandler.SContext context = mock(ContextHandler.SContext.class);
        when(context.getInitParameter("rails.root")).thenReturn("");
        when(context.getRealPath(Matchers.<String>any())).thenReturn(assetsDir.getAbsolutePath());

        when(servletRequest.getContext()).thenReturn(context);
        when(servletRequest.getSession()).thenReturn(mock(HttpSession.class));

        view.exposeHelpers(velocityContext, servletRequest);
        assertThat((String) velocityContext.get(GoVelocityView.COMPRESSED_JAVASCRIPT_FILE_PATH), is("assets/application-bfdbd4fff63b0cd45c50ce7a79fe0f53.js"));
        assertThat((String) velocityContext.get(GoVelocityView.COMPRESSED_APPLICATION_CSS_FILE_PATH), is("assets/application-4b25c82f986c0bef78151a4ab277c3e4.css"));
        assertThat((String) velocityContext.get(GoVelocityView.COMPRESSED_VM_CSS_FILE_PATH), is("assets/vm/application-4b25c82f986c0bef78151a4ab277c3f5.css"));
        assertThat((String) velocityContext.get(GoVelocityView.COMPRESSED_CSS_CSS_FILE_PATH), is("assets/css/application-4b25c82f986c0bef78151a4ab277c3g6.css"));
    }

    @Test
    public void shouldSetJavascriptPathVariableWhenRailsNewIsUsedWithoutCompressedJavascripts() throws Exception {
        SystemEnvironment systemEnvironment = mock(SystemEnvironment.class);
        when(systemEnvironment.get(SystemEnvironment.USE_NEW_RAILS)).thenReturn(true);
        when(systemEnvironment.useCompressedJs()).thenReturn(false);
        GoVelocityView view = new GoVelocityView(systemEnvironment);
        view.exposeHelpers(velocityContext, request);
        assertThat((String)velocityContext.get(GoVelocityView.JAVASCRIPTS_PATH_IN_DEV_ENV), is("assets"));
        assertThat(velocityContext.get(GoVelocityView.COMPRESSED_JAVASCRIPT_FILE_PATH), is(nullValue()));
    }

    private String json = "{\n" +
            "    \"files\": {\n" +
            "        \"application-bfdbd4fff63b0cd45c50ce7a79fe0f53.js\": {\n" +
            "            \"logical_path\": \"application.js\",\n" +
            "            \"mtime\": \"2014-08-26T12:39:43+05:30\",\n" +
            "            \"size\": 1091366,\n" +
            "            \"digest\": \"bfdbd4fff63b0cd45c50ce7a79fe0f53\"\n" +
            "        },\n" +
            "        \"application-4b25c82f986c0bef78151a4ab277c3e4.css\": {\n" +
            "            \"logical_path\": \"application.css\",\n" +
            "            \"mtime\": \"2014-08-26T13:45:30+05:30\",\n" +
            "            \"size\": 513,\n" +
            "            \"digest\": \"4b25c82f986c0bef78151a4ab277c3e4\"\n" +
            "        },\n" +
            "        \"vm/application-4b25c82f986c0bef78151a4ab277c3f5.css\": {\n" +
            "            \"logical_path\": \"vm/application.css\",\n" +
            "            \"mtime\": \"2014-08-26T13:45:30+05:30\",\n" +
            "            \"size\": 513,\n" +
            "            \"digest\": \"4b25c82f986c0bef78151a4ab277c3f5\"\n" +
            "        },\n" +
            "        \"css/application-4b25c82f986c0bef78151a4ab277c3g6.css\": {\n" +
            "            \"logical_path\": \"css/application.css\",\n" +
            "            \"mtime\": \"2014-08-26T13:45:30+05:30\",\n" +
            "            \"size\": 513,\n" +
            "            \"digest\": \"4b25c82f986c0bef78151a4ab277c3g6\"\n" +
            "        }\n" +
            "    },\n" +
            "    \"assets\": {\n" +
            "        \"application.js\": \"application-bfdbd4fff63b0cd45c50ce7a79fe0f53.js\",\n" +
            "        \"application.css\": \"application-4b25c82f986c0bef78151a4ab277c3e4.css\",\n" +
            "        \"vm/application.css\": \"vm/application-4b25c82f986c0bef78151a4ab277c3f5.css\",\n" +
            "        \"css/application.css\": \"css/application-4b25c82f986c0bef78151a4ab277c3g6.css\"\n" +
            "    }\n" +
            "}";


    @Test
    public void shouldSetJavascriptsPathVariableWhenRails2IsUsed() throws Exception {
        SystemEnvironment systemEnvironment = mock(SystemEnvironment.class);
        when(systemEnvironment.get(SystemEnvironment.USE_NEW_RAILS)).thenReturn(false);
        GoVelocityView view = new GoVelocityView(systemEnvironment);
        view.exposeHelpers(velocityContext, request);
        assertThat((String)velocityContext.get(GoVelocityView.JAVASCRIPTS_PATH_IN_DEV_ENV), is("javascripts"));
        assertThat((String)velocityContext.get(GoVelocityView.COMPRESSED_JAVASCRIPT_FILE_PATH), is("compressed/all.js?#include(\"admin/admin_version.txt.vm\")"));
    }
}
