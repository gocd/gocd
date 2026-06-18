/*
 * Copyright Thoughtworks, Inc.
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

package com.thoughtworks.go.spark.spring;

import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.SecurityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import spark.HaltException;
import spark.Request;
import spark.Spark;

import java.util.Optional;

import static com.thoughtworks.go.config.CaseInsensitiveString.cis;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AbstractAuthorizationHelperTest {
    @Mock
    private SecurityService securityService;
    @Mock
    private GoConfigService goConfigService;

    private AbstractAuthorizationHelper helper;

    @BeforeEach
    void setUp() {
        helper = new AbstractAuthorizationHelper(securityService, goConfigService) {
            @Override
            protected HaltException renderForbiddenResponse() {
                return Spark.halt("Forbidden");
            }

            @Override
            protected HaltException renderForbiddenResponse(String message) {
                return Spark.halt("Forbidden: " + message);
            }
        };
    }

    @Nested
    class PipelineGroupFromRequest {
        private final Request request = mock(Request.class);

        @Test
        void shouldPreferGroupFromPathParam() {
            when(request.params("group_name")).thenReturn("group-from-path");
            when(request.queryParams("group_name")).thenReturn("group-from-query");

            assertThat(helper.getPipelineGroupFrom(request)).isEqualTo("group-from-path");
            verifyNoInteractions(goConfigService);
        }

        @Test
        void shouldFallbackToGroupFromQueryParam() {
            when(request.queryParams("group_name")).thenReturn("group-from-query");

            assertThat(helper.getPipelineGroupFrom(request)).isEqualTo("group-from-query");
            verifyNoInteractions(goConfigService);
        }

        @Test
        void shouldFallbackToPipelinePathParam() {
            when(request.params("pipeline_name")).thenReturn("pipeline-from-path");
            when(request.queryParams("pipeline_name")).thenReturn("pipeline-from-query");
            when(goConfigService.findGroupNameByPipelineOptional(cis("pipeline-from-path"))).thenReturn(Optional.of("group-from-pipeline"));

            assertThat(helper.getPipelineGroupFrom(request)).isEqualTo("group-from-pipeline");
        }

        @Test
        void shouldFallbackToPipelineFromQueryParam() {
            when(request.queryParams("pipeline_name")).thenReturn("pipeline-from-query");
            when(goConfigService.findGroupNameByPipelineOptional(cis("pipeline-from-query"))).thenReturn(Optional.of("group-from-pipeline"));

            assertThat(helper.getPipelineGroupFrom(request)).isEqualTo("group-from-pipeline");
        }

        @Test
        void shouldAllowBothGroupAndPipelineNameIfMatching() {
            when(request.queryParams("group_name")).thenReturn("group-from-query");
            when(request.queryParams("pipeline_name")).thenReturn("pipeline-from-query");
            when(goConfigService.findGroupNameByPipelineOptional(cis("pipeline-from-query"))).thenReturn(Optional.of("group-from-query"));

            assertThat(helper.getPipelineGroupFrom(request)).isEqualTo("group-from-query");
        }

        @Test
        void shouldFailWhenPipelineSpecifiedByGroupNotFound() {
            when(request.queryParams("pipeline_name")).thenReturn("pipeline-from-query");
            when(goConfigService.findGroupNameByPipelineOptional(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> helper.getPipelineGroupFrom(request))
                .isInstanceOf(RecordNotFoundException.class)
                .hasMessage("Pipeline with name 'pipeline-from-query' was not found!");
        }

        @Test
        void shouldFailWhenPipelineSpecifiedByGroupDoesNotMatchGroup() {
            when(request.queryParams("group_name")).thenReturn("group-from-query");
            when(request.queryParams("pipeline_name")).thenReturn("pipeline-from-query");
            when(goConfigService.findGroupNameByPipelineOptional(cis("pipeline-from-query"))).thenReturn(Optional.of("another-group"));

            assertThatThrownBy(() -> helper.getPipelineGroupFrom(request))
                .isInstanceOf(HaltException.class);
        }

        @ParameterizedTest
        @ValueSource(strings = {"group_name", "pipeline_name"})
        void shouldFilterBlankParams(String param) {
            when(request.params(param)).thenReturn("   ");
            when(request.queryParams(param)).thenReturn("   ");
            assertThatThrownBy(() -> helper.getPipelineGroupFrom(request))
                .isInstanceOf(HaltException.class);

            verifyNoInteractions(goConfigService);
        }
    }
}