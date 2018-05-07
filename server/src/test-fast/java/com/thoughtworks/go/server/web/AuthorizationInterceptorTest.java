/*
 * Copyright 2018 ThoughtWorks, Inc.
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
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.newsecurity.utils.SessionUtils;
import com.thoughtworks.go.server.service.SecurityService;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.HandlerMapping;

import java.util.HashMap;
import java.util.Map;

import static com.thoughtworks.go.server.domain.Username.ANONYMOUS;
import static com.thoughtworks.go.server.newsecurity.SessionUtilsHelper.loginAsAnonymous;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

class AuthorizationInterceptorTest {

    private static final String PIPELINE_NAME_KEY = "pipelineName";
    private static final String PIPELINE_NAME = "cruise";
    private static final String STAGE_NAME_KEY = "stageName";
    private static final String STAGE_NAME = "dev";

    private AuthorizationInterceptor permissionInterceptor;
    private SecurityService securityService;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setup() {
        securityService = mock(SecurityService.class);
        permissionInterceptor = new AuthorizationInterceptor(securityService);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        loginAsAnonymous(request);
    }

    @AfterEach
    void tearDown() {
        ClearSingleton.clearSingletons();
    }

    @Test
    void shouldReturnTrueWhenNoDataIsPresentInRequestParamsOrPathParams() throws Exception {
        assertThat(permissionInterceptor.preHandle(request, response, null), is(true));
    }

    @Nested
    class WhenPipelineNameIsPresentInRequestParams {

        @BeforeEach
        void setUp() {
            request.setParameter(PIPELINE_NAME_KEY, PIPELINE_NAME);
        }

        @Test
        void shouldReturnTrueWhenViewPermissionOnPipelineForGetCall() throws Exception {
            request.setMethod(GET.name());
            assumeUserHasViewPermission();
            Username username = SessionUtils.currentUsername();
            assertThat(permissionInterceptor.preHandle(request, response, null), is(true));
            verify(securityService).hasViewPermissionForPipeline(username, PIPELINE_NAME);
        }

        @Test
        void shouldReturnFalseWhenNoViewPermissionOnPipelineForGetCall() throws Exception {
            request.setMethod(GET.name());
            assumeUserHasNoViewPermission();
            Username username = SessionUtils.currentUsername();
            assertThat(permissionInterceptor.preHandle(request, response, null), is(false));
            verify(securityService).hasViewPermissionForPipeline(username, PIPELINE_NAME);
            assertThat(response.getStatus(), is(HttpStatus.SC_FORBIDDEN));
        }

        @Test
        void shouldReturnTrueForEditingConfigurationRequest() throws Exception {
            request.setRequestURI("/admin/restful/configuration");
            request.setMethod(POST.name());
            assertThat(permissionInterceptor.preHandle(request, response, null), is(true));
        }

        //Same for post and put calls
        @Nested
        class WhenStageNameIsNotPresentInRequestParams {

            @Test
            void shouldReturnTrueWhenOperatePermissionOnPipelineForPostCall() throws Exception {
                request.setMethod(POST.name());
                assumeUserHasOperatePermissionForPipeline();
                Username username = SessionUtils.currentUsername();
                assertThat(permissionInterceptor.preHandle(request, response, null), is(true));
                verify(securityService).hasOperatePermissionForPipeline(username.getUsername(), PIPELINE_NAME);
            }
            @Test
            void shouldReturnFalseWhenNoOperatePermissionOnPipelineForPostCall() throws Exception {
                request.setMethod(POST.name());
                assumeUserHasNoOperatePermissionForPipeline();
                Username username = SessionUtils.currentUsername();
                assertThat(permissionInterceptor.preHandle(request, response, null), is(false));
                verify(securityService).hasOperatePermissionForPipeline(username.getUsername(), PIPELINE_NAME);
                assertThat(response.getStatus(), is(HttpStatus.SC_FORBIDDEN));
            }

        }
        //Same for post and put calls
        @Nested
        class WhenStageNameIsPresentInRequestParams {

            @BeforeEach
            void setUp() {
                request.setMethod(POST.name());
                request.setParameter(STAGE_NAME_KEY, STAGE_NAME);
            }

            @Test
            void shouldReturnTrueWhenOperatePermissionOnStageForPostCall() throws Exception {
                assumeUserHasOperatePermissionForStage();
                Username username = SessionUtils.currentUsername();
                String name = CaseInsensitiveString.str(username.getUsername());
                assertThat(permissionInterceptor.preHandle(request, response, null), is(true));
                verify(securityService).hasOperatePermissionForStage(PIPELINE_NAME, STAGE_NAME, name);
            }

            @Test
            void shouldReturnFalseWhenNoOperatePermissionOnStageForPostCall() throws Exception {
                assumeUserHasNoOperatePermissionForStage();
                Username username = SessionUtils.currentUsername();
                String name = CaseInsensitiveString.str(username.getUsername());
                assertThat(permissionInterceptor.preHandle(request, response, null), is(false));
                verify(securityService).hasOperatePermissionForStage(PIPELINE_NAME, STAGE_NAME, name);
                assertThat(response.getStatus(), is(HttpStatus.SC_FORBIDDEN));
            }

        }


    }

    @Nested
    class WhenPipelineNameIsPresentInPathVariables {

        @Test
        void shouldReturnTrueWhenViewPermissionOnPipelineForGetCall() throws Exception {
            request.setMethod(GET.name());
            addPipelineNameAsPathVariableToRequest();
            assumeUserHasViewPermission();
            Username username = SessionUtils.currentUsername();
            assertThat(permissionInterceptor.preHandle(request, response, null), is(true));
            verify(securityService).hasViewPermissionForPipeline(username, PIPELINE_NAME);
        }

        @Test
        void shouldReturnFalseWhenNoViewPermissionOnPipelineForPostCall() throws Exception {
            request.setMethod(GET.name());
            addPipelineNameAsPathVariableToRequest();
            assumeUserHasNoViewPermission();
            Username username = SessionUtils.currentUsername();
            assertThat(permissionInterceptor.preHandle(request, response, null), is(false));
            verify(securityService).hasViewPermissionForPipeline(username, PIPELINE_NAME);
            assertThat(response.getStatus(), is(HttpStatus.SC_FORBIDDEN));
        }

        @Nested
        class WhenStageNameIsNotPresentInPathVariables {

            @Test
            void shouldReturnTrueWhenOperatePermissionOnPipelineForPostCall() throws Exception {
                request.setMethod(POST.name());
                addPipelineNameAsPathVariableToRequest();
                assumeUserHasOperatePermissionForPipeline();
                Username username = SessionUtils.currentUsername();
                assertThat(permissionInterceptor.preHandle(request, response, null), is(true));
                verify(securityService).hasOperatePermissionForPipeline(username.getUsername(), PIPELINE_NAME);
            }
            @Test
            void shouldReturnFalseWhenNoOperatePermissionOnPipelineForGetCall() throws Exception {
                request.setMethod(POST.name());
                addPipelineNameAsPathVariableToRequest();
                assumeUserHasNoOperatePermissionForPipeline();
                Username username = SessionUtils.currentUsername();
                assertThat(permissionInterceptor.preHandle(request, response, null), is(false));
                verify(securityService).hasOperatePermissionForPipeline(username.getUsername(), PIPELINE_NAME);
                assertThat(response.getStatus(), is(HttpStatus.SC_FORBIDDEN));
            }

        }
        @Nested
        class WhenStageNameIsPresentInPathVariables {

            @Test
            void shouldReturnTrueWhenOperatePermissionOnStageForPostCall() throws Exception {
                request.setMethod(POST.name());
                addPipelineNameAndStageNameAsPathVariablesToRequest();
                assumeUserHasOperatePermissionForStage();
                Username username = SessionUtils.currentUsername();
                String name = CaseInsensitiveString.str(username.getUsername());
                assertThat(permissionInterceptor.preHandle(request, response, null), is(true));
                verify(securityService).hasOperatePermissionForStage(PIPELINE_NAME, STAGE_NAME, name);
            }
            @Test
            void shouldReturnFalseWhenNoOperatePermissionOnStageForPostCall() throws Exception {
                request.setMethod(POST.name());
                addPipelineNameAndStageNameAsPathVariablesToRequest();
                assumeUserHasNoOperatePermissionForStage();
                Username username = SessionUtils.currentUsername();
                String name = CaseInsensitiveString.str(username.getUsername());
                assertThat(permissionInterceptor.preHandle(request, response, null), is(false));
                verify(securityService).hasOperatePermissionForStage(PIPELINE_NAME, STAGE_NAME, name);
                assertThat(response.getStatus(), is(HttpStatus.SC_FORBIDDEN));
            }

        }

        private void addPipelineNameAsPathVariableToRequest() {
            Map<String, String> pathVariables = new HashMap<>();
            pathVariables.put(PIPELINE_NAME_KEY, PIPELINE_NAME);
            request.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, pathVariables);
        }

        private void addPipelineNameAndStageNameAsPathVariablesToRequest() {
            Map<String, String> pathVariables = new HashMap<>();
            pathVariables.put(PIPELINE_NAME_KEY, PIPELINE_NAME);
            pathVariables.put(STAGE_NAME_KEY, STAGE_NAME);
            request.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, pathVariables);
        }
    }

    private void assumeUserHasViewPermission() {
        when(securityService.hasViewPermissionForPipeline(ANONYMOUS, PIPELINE_NAME)).thenReturn(true);
    }

    private void assumeUserHasNoViewPermission() {
        when(securityService.hasViewPermissionForPipeline(ANONYMOUS, PIPELINE_NAME)).thenReturn(false);
    }

    private void assumeUserHasOperatePermissionForPipeline() {
        when(securityService.hasOperatePermissionForPipeline(ANONYMOUS.getUsername(), PIPELINE_NAME)).thenReturn(true);
    }

    private void assumeUserHasNoOperatePermissionForPipeline() {
        when(securityService.hasOperatePermissionForPipeline(ANONYMOUS.getUsername(), PIPELINE_NAME)).thenReturn(false);
    }

    private void assumeUserHasOperatePermissionForStage() {
        when(securityService.hasOperatePermissionForStage(PIPELINE_NAME, STAGE_NAME, CaseInsensitiveString.str(ANONYMOUS.getUsername()))).thenReturn(true);
    }

    private void assumeUserHasNoOperatePermissionForStage() {
        when(securityService.hasOperatePermissionForStage(PIPELINE_NAME, STAGE_NAME, CaseInsensitiveString.str(ANONYMOUS.getUsername()))).thenReturn(false);
    }
}
