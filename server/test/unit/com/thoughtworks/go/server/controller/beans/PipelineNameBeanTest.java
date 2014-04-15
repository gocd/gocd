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

package com.thoughtworks.go.server.controller.beans;

import com.thoughtworks.go.util.json.JsonMap;
import static com.thoughtworks.go.validation.PipelineGroupValidator.ERRORR_MESSAGE;
import static com.thoughtworks.go.util.GoConstants.ERROR_FOR_JSON;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import org.junit.Test;

public class PipelineNameBeanTest {
   @Test
    public void shouldReturnValidJson() throws Exception {
        PipelineNameBean bean = new PipelineNameBean("09AZ_-", false);
        JsonMap json = (JsonMap) bean.validate().toJson();
        assertThat(json.getJsonString("isValid").withoutQuote(), is("true"));
        assertThat(json.getJsonString(ERROR_FOR_JSON).withoutQuote(), is(""));
    }

    @Test
    public void shouldReturnErrorMessageIfProjectNameIsBlank() throws Exception {
        PipelineNameBean bean = new PipelineNameBean("     ", false);
        JsonMap json = (JsonMap) bean.validate().toJson();
        assertThat(json.getJsonString("isValid").withoutQuote(), is("false"));
        assertThat(json.getJsonString(ERROR_FOR_JSON).withoutQuote(),
                is(ERRORR_MESSAGE));
    }

    @Test
    public void shouldReturnErrorMessageIfProjectNameIsEmpty() throws Exception {
        PipelineNameBean bean = new PipelineNameBean("", false);
        JsonMap json = (JsonMap) bean.validate().toJson();
        assertThat(json.getJsonString("isValid").withoutQuote(), is("false"));
        assertThat(json.getJsonString(ERROR_FOR_JSON).withoutQuote(), is("Pipeline name is required"));
    }

    @Test
    public void shouldReturnErrorMessageIfProjectNameIsNotValid() throws Exception {
        PipelineNameBean bean = new PipelineNameBean("*&719", false);
        JsonMap json = (JsonMap) bean.validate().toJson();
        assertThat(json.getJsonString("isValid").withoutQuote(), is("false"));
        assertThat(json.getJsonString(ERROR_FOR_JSON).withoutQuote(),
                is(ERRORR_MESSAGE));
    }

    @Test
    public void shouldAllowDotInPipelineName() throws Exception {
        PipelineNameBean bean = new PipelineNameBean("pipeline.719", false);
        JsonMap json = (JsonMap) bean.validate().toJson();
        assertThat(json.getJsonString("isValid").withoutQuote(), is("true"));
    }

    @Test
    public void shouldReturnErrorMessageIfProjectNameAlreadyExist() throws Exception {
        PipelineNameBean bean = new PipelineNameBean("abc", true);
        JsonMap json = (JsonMap) bean.validate().toJson();
        assertThat(json.getJsonString("isValid").withoutQuote(), is("false"));
        assertThat(json.getJsonString(ERROR_FOR_JSON).withoutQuote(),
                is("Pipeline name already exists, please choose another one."));
    }
}
