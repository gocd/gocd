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
package com.thoughtworks.go.config.update;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;

public class ConfigUpdateAjaxResponseTest {

    @Test
    public void shouldGetJsonRepresentationForFailure() {
        HashMap<String, List<String>> fieldErrors = new HashMap<>();
        fieldErrors.put("field1", List.of("error 1"));
        fieldErrors.put("field2", List.of("error 2"));
        ConfigUpdateAjaxResponse response = ConfigUpdateAjaxResponse.failure("id", SC_BAD_REQUEST, "Save failed", fieldErrors, List.of("global1", "global2"));
        String jsonString = response.toJson();
        assertThat(response.getStatusCode()).isEqualTo(SC_BAD_REQUEST);
        assertThatJson(jsonString).isEqualTo("{\"fieldErrors\":{\"field1\":[\"error 1\"], \"field2\":[\"error 2\"]},\"globalErrors\":[\"global1\",\"global2\"],\"message\":\"Save failed\",\"isSuccessful\":false,\"subjectIdentifier\":\"id\"}");
    }

    @Test
    public void shouldGetJsonRepresentationForSuccess() {
        ConfigUpdateAjaxResponse response = ConfigUpdateAjaxResponse.success("id", SC_OK, "saved successful");
        String jsonString = response.toJson();
        assertThatJson(jsonString).isEqualTo("{\"fieldErrors\":{},\"globalErrors\":[],\"message\":\"saved successful\",\"isSuccessful\":true,\"subjectIdentifier\":\"id\"}");
    }
}
