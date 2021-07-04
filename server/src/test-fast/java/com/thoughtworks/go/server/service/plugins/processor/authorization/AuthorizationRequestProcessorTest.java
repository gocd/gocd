/*
 * Copyright 2021 ThoughtWorks, Inc.
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
package com.thoughtworks.go.server.service.plugins.processor.authorization;

import com.thoughtworks.go.config.SecurityAuthConfigs;
import com.thoughtworks.go.config.SecurityConfig;
import com.thoughtworks.go.plugin.access.authorization.AuthorizationExtension;
import com.thoughtworks.go.plugin.api.request.DefaultGoApiRequest;
import com.thoughtworks.go.plugin.api.request.GoApiRequest;
import com.thoughtworks.go.plugin.api.response.GoApiResponse;
import com.thoughtworks.go.plugin.infra.PluginRequestProcessorRegistry;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.PluginRoleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.thoughtworks.go.server.service.plugins.processor.authorization.AuthorizationRequestProcessor.Request.INVALIDATE_CACHE_REQUEST;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthorizationRequestProcessorTest {

    @Mock
    private PluginRequestProcessorRegistry registry;
    @Mock
    private AuthorizationExtension authorizationExtension;
    @Mock
    private GoPluginDescriptor pluginDescriptor;
    @Mock
    private GoConfigService goConfigService;
    @Mock
    private SecurityConfig securityConfig;
    private SecurityAuthConfigs securityAuthConfigsSpy;

    @BeforeEach
    public void setUp() throws Exception {
        when(pluginDescriptor.id()).thenReturn("cd.go.authorization.github");
        lenient().when(goConfigService.security()).thenReturn(securityConfig);
        securityAuthConfigsSpy = spy(new SecurityAuthConfigs());
        lenient().when(securityConfig.securityAuthConfigs()).thenReturn(securityAuthConfigsSpy);
    }

    @Test
    public void shouldProcessInvalidateCacheRequest() throws Exception {
        PluginRoleService pluginRoleService = mock(PluginRoleService.class);

        GoApiRequest request = new DefaultGoApiRequest(INVALIDATE_CACHE_REQUEST.requestName(), "1.0", null);
        AuthorizationRequestProcessor authorizationRequestProcessor = new AuthorizationRequestProcessor(registry, pluginRoleService);

        GoApiResponse response = authorizationRequestProcessor.process(pluginDescriptor, request);

        assertThat(response.responseCode(), is(200));
        verify(pluginRoleService).invalidateRolesFor("cd.go.authorization.github");
    }
}
