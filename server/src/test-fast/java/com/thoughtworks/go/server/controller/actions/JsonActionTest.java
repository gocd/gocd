/*
 * Copyright 2016 ThoughtWorks, Inc.
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

package com.thoughtworks.go.server.controller.actions;

import com.thoughtworks.go.config.validation.GoConfigValidity;
import com.thoughtworks.go.util.json.JsonAware;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import static javax.servlet.http.HttpServletResponse.SC_CONFLICT;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class JsonActionTest {

    private JsonAware jsonAware;
    private GoConfigValidity configValidity;

    @Before
    public void setUp() throws Exception {
        jsonAware = mock(JsonAware.class);
        configValidity = mock(GoConfigValidity.class);
    }

    @Test
    public void shouldReturnJsonConflictInNormalConflict() throws Exception {
        when(configValidity.isType(GoConfigValidity.VT_CONFLICT)).thenReturn(true);
        JsonAction action = JsonAction.jsonByValidity(jsonAware, configValidity);
        MockHttpServletResponse response = new MockHttpServletResponse();
        action.respond(response);
        assertThat(response.getStatus(), is(SC_CONFLICT));
        verify(configValidity).isType(GoConfigValidity.VT_CONFLICT);
    }

    @Test
    public void shouldReturnJsonConflictInCaseOfConfigMergeConflict() throws Exception {
        when(configValidity.isType(GoConfigValidity.VT_MERGE_OPERATION_ERROR)).thenReturn(true);
        JsonAction action = JsonAction.jsonByValidity(jsonAware, configValidity);
        MockHttpServletResponse response = new MockHttpServletResponse();
        action.respond(response);
        assertThat(response.getStatus(), is(SC_CONFLICT));
        verify(configValidity).isType(GoConfigValidity.VT_MERGE_OPERATION_ERROR);
    }

    @Test
    public void shouldReturnJsonConflictInCaseOfPostMergeValidationError() throws Exception {
        when(configValidity.isType(GoConfigValidity.VT_MERGE_POST_VALIDATION_ERROR)).thenReturn(true);
        JsonAction action = JsonAction.jsonByValidity(jsonAware, configValidity);
        MockHttpServletResponse response = new MockHttpServletResponse();
        action.respond(response);
        assertThat(response.getStatus(), is(SC_CONFLICT));
        verify(configValidity).isType(GoConfigValidity.VT_MERGE_POST_VALIDATION_ERROR);
    }

    @Test
    public void shouldReturnJsonConflictInCaseOfPreMergeValidationError() throws Exception {
        when(configValidity.isType(GoConfigValidity.VT_MERGE_PRE_VALIDATION_ERROR)).thenReturn(true);
        JsonAction action = JsonAction.jsonByValidity(jsonAware, configValidity);
        MockHttpServletResponse response = new MockHttpServletResponse();
        action.respond(response);
        assertThat(response.getStatus(), is(SC_CONFLICT));
        verify(configValidity).isType(GoConfigValidity.VT_MERGE_PRE_VALIDATION_ERROR);
    }
}
