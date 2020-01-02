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
package com.thoughtworks.go.plugin.access.analytics;

import com.google.gson.Gson;
import com.thoughtworks.go.plugin.access.analytics.V1.AnalyticsMessageConverterV1;
import com.thoughtworks.go.plugin.domain.analytics.AnalyticsData;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class AnalyticsMessageConverterV1Test {

    private AnalyticsMessageConverterV1 converter;
    private static final Gson GSON = new Gson();

    @Before
    public void setUp() throws Exception {
        converter = new AnalyticsMessageConverterV1();
    }

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void shouldBeAbleToGetAnalyticsDataFromValidJSONResponse() {
        String response = "{\"data\":\"foo\", \"view_path\":\"bar.html\"}";

        AnalyticsData actual = converter.getAnalyticsFromResponseBody(response);

        assertEquals("foo", actual.getData());
        assertEquals("bar.html", actual.getViewPath());
    }

    @Test
    public void shouldThrowExceptionIfDataKeyIsMissing() {
        String response = "{\"foo\": \"bar\"}";
        thrown.expect(com.thoughtworks.go.plugin.access.analytics.V1.models.AnalyticsData.MissingRequiredKeyException.class);
        thrown.expectMessage("Missing \"data\" key in analytics payload");

        converter.getAnalyticsFromResponseBody(response);
    }

    @Test
    public void shouldThrowExceptionIfViewPathKeyIsMissing() {
        String response = "{\"data\": \"hi\", \"foo\": \"bar\"}";
        thrown.expect(com.thoughtworks.go.plugin.access.analytics.V1.models.AnalyticsData.MissingRequiredKeyException.class);
        thrown.expectMessage("Missing \"view_path\" key in analytics payload");

        converter.getAnalyticsFromResponseBody(response);
    }

    @Test
    public void shouldBuildRequestBodyForAnalyticsRequest() throws Exception {
        String analyticsRequestBody = converter.getAnalyticsRequestBody("pipeline", "pipeline_with_highest_wait_time", Collections.singletonMap("pipeline_name", "test_pipeline"));

        String expectedRequestBody = "{" +
                "\"type\":\"pipeline\"," +
                "\"id\":\"pipeline_with_highest_wait_time\"," +
                " \"params\":{\"pipeline_name\": \"test_pipeline\"}}";

        assertEquals(GSON.fromJson(expectedRequestBody, Map.class), GSON.fromJson(analyticsRequestBody, Map.class));
    }
}