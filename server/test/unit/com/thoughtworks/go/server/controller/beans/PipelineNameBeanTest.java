/*
 * Copyright 2015 ThoughtWorks, Inc.
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

package com.thoughtworks.go.server.controller.beans;

import org.junit.Test;

import java.util.Map;

import static com.thoughtworks.go.util.GoConstants.ERROR_FOR_JSON;
import static com.thoughtworks.go.validation.PipelineGroupValidator.ERRORR_MESSAGE;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class PipelineNameBeanTest {
   @Test
    public void shouldReturnValidJson() throws Exception {
        PipelineNameBean bean = new PipelineNameBean("09AZ_-", false);
        Map<String, String> json = (Map) bean.validate().toJson();
        assertThat(json.get("isValid"), is("true"));
        assertThat(json.get(ERROR_FOR_JSON), is(""));
    }

    @Test
    public void shouldReturnErrorMessageIfProjectNameIsBlank() throws Exception {
        PipelineNameBean bean = new PipelineNameBean("     ", false);
        Map<String, String> json = (Map) bean.validate().toJson();
        assertThat(json.get("isValid"), is("false"));
        assertThat(json.get(ERROR_FOR_JSON),
                is(ERRORR_MESSAGE));
    }

    @Test
    public void shouldReturnErrorMessageIfProjectNameIsEmpty() throws Exception {
        PipelineNameBean bean = new PipelineNameBean("", false);
        Map<String, String> json = (Map) bean.validate().toJson();
        assertThat(json.get("isValid"), is("false"));
        assertThat(json.get(ERROR_FOR_JSON), is("Pipeline name is required"));
    }

    @Test
    public void shouldReturnErrorMessageIfProjectNameIsNotValid() throws Exception {
        PipelineNameBean bean = new PipelineNameBean("*&719", false);
        Map<String, String> json = (Map) bean.validate().toJson();
        assertThat(json.get("isValid"), is("false"));
        assertThat(json.get(ERROR_FOR_JSON),
                is(ERRORR_MESSAGE));
    }

    @Test
    public void shouldAllowDotInPipelineName() throws Exception {
        PipelineNameBean bean = new PipelineNameBean("pipeline.719", false);
        Map<String, String> json = (Map) bean.validate().toJson();
        assertThat(json.get("isValid"), is("true"));
    }

    @Test
    public void shouldReturnErrorMessageIfProjectNameAlreadyExist() throws Exception {
        PipelineNameBean bean = new PipelineNameBean("abc", true);
        Map<String, String> json = (Map) bean.validate().toJson();
        assertThat(json.get("isValid"), is("false"));
        assertThat(json.get(ERROR_FOR_JSON),
                is("Pipeline name already exists, please choose another one."));
    }
}
