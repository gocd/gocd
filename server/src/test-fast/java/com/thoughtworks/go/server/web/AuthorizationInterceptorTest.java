/*
 * Copyright 2020 ThoughtWorks, Inc.
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
package com.thoughtworks.go.server.web;

import com.thoughtworks.go.ClearSingleton;
import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.server.service.SecurityService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static com.thoughtworks.go.server.domain.Username.ANONYMOUS;
import static com.thoughtworks.go.server.newsecurity.SessionUtilsHelper.loginAsAnonymous;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AuthorizationInterceptorTest {

    private AuthorizationInterceptor permissionInterceptor;
    private SecurityService securityService;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @Before
    public void setup() throws Exception {
        securityService = mock(SecurityService.class);
        permissionInterceptor = new AuthorizationInterceptor(securityService);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        loginAsAnonymous(request);
    }

    @After
    public void tearDown() throws Exception {
        ClearSingleton.clearSingletons();
    }

    @Test
    public void shouldCheckViewPermissionForGetRequestIfPipelineNamePresent() throws Exception {
        request.setParameter("pipelineName", "cruise");
        request.setMethod("get");
        assumeUserHasViewPermission();
        assertThat("shouldCheckViewPermissionIfPipelineNamePresent",
                permissionInterceptor.preHandle(request, response, null),
                is(true));
    }

    @Test
    public void shouldNotCheckViewPermissionIfPipelineNameNotPresent() throws Exception {
        assertThat("shouldCheckViewPermissionIfPipelineNamePresent",
                permissionInterceptor.preHandle(request, response, null),
                is(true));
    }

    @Test
    public void shouldCheckOperatePermissionForPostRequest() throws Exception {
        request.setParameter("pipelineName", "cruise");
        request.setMethod("post");
        assumeUserHasOperatePermissionForPipeline();
        assertThat(permissionInterceptor.preHandle(request, response, null), is(true));
    }

    @Test
    public void shouldCheckOperatePermissionForStageOperationRequest() throws Exception {
        request.setParameter("pipelineName", "cruise");
        request.setParameter("stageName", "dev");
        request.setMethod("post");
        assumeUserHasOperatePermissionForStage();
        assertThat(permissionInterceptor.preHandle(request, response, null), is(true));
    }

    @Test
    public void shouldCheckOperatePermissionForPutRequest() throws Exception {
        request.setParameter("pipelineName", "cruise");
        request.setMethod("put");
        assumeUserHasOperatePermissionForPipeline();
        assertThat(permissionInterceptor.preHandle(request, response, null), is(true));
    }

    @Test
    public void shouldNotCheckOperatePermissionForEditingConfigurationRequest() throws Exception {
        request.setParameter("pipelineName", "cruise");
        request.setRequestURI("/admin/restful/configuration");
        request.setMethod("post");
        assertThat(permissionInterceptor.preHandle(request, response, null), is(true));
    }

    private void assumeUserHasViewPermission() {
        when(securityService.hasViewPermissionForPipeline(ANONYMOUS, "cruise")).thenReturn(true);
    }

    private void assumeUserHasOperatePermissionForPipeline() {
        when(securityService.hasOperatePermissionForPipeline(ANONYMOUS.getUsername(), "cruise")).thenReturn(true);
    }

    private void assumeUserHasOperatePermissionForStage() {
        when(securityService.hasOperatePermissionForStage("cruise", "dev", CaseInsensitiveString.str(ANONYMOUS.getUsername()))).thenReturn(true);
    }
}
