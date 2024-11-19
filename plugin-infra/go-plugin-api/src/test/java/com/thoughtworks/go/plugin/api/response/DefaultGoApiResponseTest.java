/*
 * Copyright 2024 Thoughtworks, Inc.
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
package com.thoughtworks.go.plugin.api.response;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

public class DefaultGoApiResponseTest {

    @Test
    public void shouldBeInstanceOfGoApiResponse() {
        DefaultGoApiResponse response = new DefaultGoApiResponse(0);
        assertThat(response).isInstanceOf(GoApiResponse.class);
    }

    @Test
    public void shouldReturnUnmodifiableResponseHeaders() {
        DefaultGoApiResponse response = new DefaultGoApiResponse(0);
        Map<String, String> headers = response.responseHeaders();
        try {
            headers.put("new-key", "new-value");
            fail("Should not allow modification of response headers");
        } catch (UnsupportedOperationException ignored) {
        }
        try {
            headers.remove("key");
            fail("Should not allow modification of response headers");
        } catch (UnsupportedOperationException ignored) {
        }
    }

    @Test
    public void shouldBeAbleToSetAndGetResponseBody() {
        DefaultGoApiResponse response = new DefaultGoApiResponse(0);
        String responseBody = "response-body";
        response.setResponseBody(responseBody);
        assertThat(response.responseBody()).isEqualTo(responseBody);
    }

    @Test
    public void shouldBeAbleToInitializeResponse() {
        int responseCode = 0;
        GoApiResponse response = new DefaultGoApiResponse(responseCode);
        assertThat(response.responseCode()).isEqualTo(responseCode);
    }

    @Test
    public void shouldReturnResponseForBadRequest() {
        DefaultGoApiResponse response = DefaultGoApiResponse.badRequest("responseBody");
        assertThat(response.responseCode()).isEqualTo(400);
        assertThat(response.responseBody()).isEqualTo("responseBody");
    }

    @Test
    public void shouldReturnResponseForIncompleteRequest() {
        DefaultGoApiResponse response = DefaultGoApiResponse.incompleteRequest("responseBody");
        assertThat(response.responseCode()).isEqualTo(412);
        assertThat(response.responseBody()).isEqualTo("responseBody");
    }

    @Test
    public void shouldReturnResponseForErrorRequest() {
        DefaultGoApiResponse response = DefaultGoApiResponse.error("responseBody");
        assertThat(response.responseCode()).isEqualTo(500);
        assertThat(response.responseBody()).isEqualTo("responseBody");
    }

    @Test
    public void shouldReturnResponseForSuccessRequest() {
        DefaultGoApiResponse response = DefaultGoApiResponse.success("responseBody");
        assertThat(response.responseCode()).isEqualTo(200);
        assertThat(response.responseBody()).isEqualTo("responseBody");
    }
}
