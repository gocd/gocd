/*
 * Copyright 2016 ThoughtWorks, Inc.
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

package com.thoughtworks.go.server.service.plugins.processor.authentication;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.plugin.access.authentication.JsonMessageHandler1_0;
import com.thoughtworks.go.plugin.access.authentication.model.User;
import com.thoughtworks.go.plugin.api.GoPluginIdentifier;
import com.thoughtworks.go.plugin.api.request.GoApiRequest;
import com.thoughtworks.go.plugin.api.response.GoApiResponse;
import com.thoughtworks.go.plugin.infra.PluginRequestProcessorRegistry;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.security.AuthorityGranter;
import com.thoughtworks.go.server.security.GoAuthority;
import com.thoughtworks.go.server.security.userdetail.GoUserPrinciple;
import com.thoughtworks.go.server.service.UserService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.security.GrantedAuthority;
import org.springframework.security.context.SecurityContext;
import org.springframework.security.providers.preauth.PreAuthenticatedAuthenticationToken;

import java.util.Map;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class AuthenticationRequestProcessorTest {
    @Mock
    private PluginRequestProcessorRegistry applicationAccessor;
    @Mock
    private AuthorityGranter authorityGranter;
    @Mock
    private UserService userService;
    @Mock
    private JsonMessageHandler1_0 jsonMessageHandler;
    @Mock
    private SecurityContext securityContext;

    private GrantedAuthority userAuthority;
    private AuthenticationRequestProcessor processor;
    @Mock
    private GoPluginDescriptor pluginDescriptor;

    @Before
    public void setUp() {
        initMocks(this);

        userAuthority = GoAuthority.ROLE_USER.asAuthority();
        when(authorityGranter.authorities("username")).thenReturn(new GrantedAuthority[]{userAuthority});

        processor = new AuthenticationRequestProcessor(applicationAccessor, authorityGranter, userService);
        processor.getMessageHandlerMap().put("1.0", jsonMessageHandler);
    }

    @Test
    public void shouldRegisterItselfForRequestProcessing() {
        verify(applicationAccessor).registerProcessorFor(AuthenticationRequestProcessor.AUTHENTICATE_USER_REQUEST, processor);
    }

    @Test
    public void shouldHandleIncorrectAPIVersion() {
        GoApiResponse response = processor.process(pluginDescriptor, getGoPluginApiRequest("1.1", null));
        assertThat(response.responseCode(), is(500));
    }

    @Test
    public void shouldAuthenticateUser() {
        String responseBody = "expected-response-body";
        User user = new User("username", "display name", "test@test.com");
        when(jsonMessageHandler.responseMessageForAuthenticateUser(responseBody)).thenReturn(user);

        AuthenticationRequestProcessor processorSpy = spy(processor);
        doReturn(securityContext).when(processorSpy).getSecurityContext();

        GoApiResponse response = processorSpy.process(pluginDescriptor, getGoPluginApiRequest("1.0", responseBody));

        assertThat(response.responseCode(), is(200));
        verify(userService).addUserIfDoesNotExist(new Username(new CaseInsensitiveString("username"), "display name"));
        GoUserPrinciple goUserPrincipal = processorSpy.getGoUserPrincipal(user);
        assertThat(goUserPrincipal.getUsername(), is("username"));
        assertThat(goUserPrincipal.getDisplayName(), is("display name"));
        verifyGrantAuthorities(goUserPrincipal.getAuthorities());
        PreAuthenticatedAuthenticationToken authenticationToken = processorSpy.getAuthenticationToken(goUserPrincipal);
        assertThat((GoUserPrinciple) authenticationToken.getPrincipal(), is(goUserPrincipal));
        verifyGrantAuthorities(authenticationToken.getAuthorities());
        verify(securityContext).setAuthentication(authenticationToken);
    }

    @Test
    public void shouldHandleEmptyRequestBody() {
        GoApiResponse response = processor.process(pluginDescriptor, getGoPluginApiRequest("1.0", "{}"));
        assertThat(response.responseCode(), is(500));
    }

    private void verifyGrantAuthorities(GrantedAuthority[] authorities) {
        assertThat(authorities.length, is(1));
        assertThat(authorities[0], is(userAuthority));
    }

    private GoApiRequest getGoPluginApiRequest(final String apiVersion, final String requestBody) {
        return new GoApiRequest() {
            @Override
            public String api() {
                return AuthenticationRequestProcessor.AUTHENTICATE_USER_REQUEST;
            }

            @Override
            public String apiVersion() {
                return apiVersion;
            }

            @Override
            public GoPluginIdentifier pluginIdentifier() {
                return null;
            }

            @Override
            public Map<String, String> requestParameters() {
                return null;
            }

            @Override
            public Map<String, String> requestHeaders() {
                return null;
            }

            @Override
            public String requestBody() {
                return requestBody;
            }
        };
    }
}
