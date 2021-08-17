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
package com.thoughtworks.go.config.update;

import net.javacrumbs.jsonunit.fluent.JsonFluentAssert;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ConfigUpdateAjaxResponseTest {

    @Test
    public void shouldGetJsonRepresentationForFailure() throws Exception {
        HashMap<String, List<String>> fieldErrors = new HashMap<>();
        fieldErrors.put("field1", Arrays.asList("error 1"));
        fieldErrors.put("field2", Arrays.asList("error 2"));
        ConfigUpdateAjaxResponse response = ConfigUpdateAjaxResponse.failure("id", SC_BAD_REQUEST, "Save failed", fieldErrors, Arrays.asList("global1", "global2"));
        String jsonString = response.toJson();
        assertThat(response.getStatusCode(),is(SC_BAD_REQUEST));
        JsonFluentAssert.assertThatJson(jsonString).isEqualTo("{\"fieldErrors\":{\"field1\":[\"error 1\"], \"field2\":[\"error 2\"]},\"globalErrors\":[\"global1\",\"global2\"],\"message\":\"Save failed\",\"isSuccessful\":false,\"subjectIdentifier\":\"id\"}");
    }

    @Test
    public void shouldGetJsonRepresentationForSuccess() throws Exception {
        ConfigUpdateAjaxResponse response = ConfigUpdateAjaxResponse.success("id", SC_OK, "saved successful");
        String jsonString = response.toJson();
        JsonFluentAssert.assertThatJson(jsonString).isEqualTo("{\"fieldErrors\":{},\"globalErrors\":[],\"message\":\"saved successful\",\"isSuccessful\":true,\"subjectIdentifier\":\"id\"}");
    }
}
