/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.config.update;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.httpclient.HttpStatus;
import org.junit.Test;

import static net.javacrumbs.jsonunit.JsonMatchers.jsonEquals;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class ConfigUpdateAjaxResponseTest {

    @Test
    public void shouldGetJsonRepresentationForFailure() throws Exception {
        HashMap<String, List<String>> fieldErrors = new HashMap<String, List<String>>();
        fieldErrors.put("field1", Arrays.asList("error 1"));
        fieldErrors.put("field2", Arrays.asList("error 2"));
        ConfigUpdateAjaxResponse response = ConfigUpdateAjaxResponse.failure("id", HttpStatus.SC_BAD_REQUEST, "Save failed", fieldErrors, Arrays.asList("global1", "global2"));
        String jsonString = response.toJson();
        assertThat(response.getStatusCode(),is(HttpStatus.SC_BAD_REQUEST));
        assertThat(jsonString,
                jsonEquals("{\"fieldErrors\":{\"field1\":[\"error 1\"], \"field2\":[\"error 2\"]},\"globalErrors\":[\"global1\",\"global2\"],\"message\":\"Save failed\",\"isSuccessful\":false,\"subjectIdentifier\":\"id\"}"));
    }

    @Test
    public void shouldGetJsonRepresentationForSuccess() throws Exception {
        ConfigUpdateAjaxResponse response = ConfigUpdateAjaxResponse.success("id", HttpStatus.SC_OK, "saved successful");
        String jsonString = response.toJson();
        assertThat(jsonString, jsonEquals("{\"fieldErrors\":{},\"globalErrors\":[],\"message\":\"saved successful\",\"isSuccessful\":true,\"subjectIdentifier\":\"id\"}"));
    }
}
