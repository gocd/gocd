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

package com.thoughtworks.go.apiv1.webhook.request;

import com.thoughtworks.go.apiv1.webhook.request.payload.Payload;
import com.thoughtworks.go.config.exceptions.BadRequestException;
import org.junit.jupiter.api.Test;
import spark.QueryParamsMap;
import spark.Request;

import java.util.Set;

import static com.thoughtworks.go.apiv1.webhook.request.WebhookRequest.KEY_SCM_NAME;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.*;

class WebhookRequestTest {
    @Test
    void onlySupportsJsonPayload() {
        assertEquals(Set.of(APPLICATION_JSON, APPLICATION_JSON_UTF8), webhook(mockRequest("")).supportedContentTypes());
    }

    @Test
    void rejectsNonGitPayloads() {
        assertEquals("GoCD currently only supports webhooks on `git` repositories; `repo` is a `svn` repository",
                assertThrows(BadRequestException.class, () ->
                        webhook(mockRequest("{}")).parsePayload(NonGitPayload.class)).getMessage()
        );

    }

    @Test
    void returnsEmptyScmNamesWhenNoneProvided() {
        assertTrue(webhook(new Request() {
            @Override
            public QueryParamsMap queryMap() {
                return new QueryParamsMap() {
                };
            }
        }).scmNamesQuery().isEmpty());
    }

    @Test
    void returnsScmNamesWhenProvided() {
        assertEquals(singletonList("scm1"), webhook(new Request() {
            @Override
            public QueryParamsMap queryMap() {
                return new QueryParamsMap(KEY_SCM_NAME, "scm1") {
                };
            }
        }).scmNamesQuery());
    }

    private WebhookRequest webhook(Request req) {
        return new WebhookRequest(req) {
            @Override
            public String authToken() {
                return "auth";
            }

            @Override
            public String event() {
                return "event";
            }
        };
    }

    private Request mockRequest(String body) {
        final Request req = mock(Request.class);
        when(req.body()).thenReturn(body);
        when(req.contentType()).thenReturn(APPLICATION_JSON_VALUE);
        return req;
    }

    private static class NonGitPayload implements Payload {
        @Override
        public String hostname() {
            return "host";
        }

        @Override
        public String fullName() {
            return "repo";
        }

        @Override
        public String scmType() {
            return "svn";
        }

        @Override
        public String descriptor() {
            return getClass().getSimpleName();
        }
    }
}