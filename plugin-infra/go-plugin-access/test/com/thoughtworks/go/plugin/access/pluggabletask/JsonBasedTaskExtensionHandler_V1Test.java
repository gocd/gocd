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

package com.thoughtworks.go.plugin.access.pluggabletask;

import com.thoughtworks.go.plugin.api.config.Property;
import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse;
import com.thoughtworks.go.plugin.api.response.execution.ExecutionResult;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.plugin.api.task.TaskConfig;
import com.thoughtworks.go.plugin.api.task.TaskConfigProperty;
import com.thoughtworks.go.plugin.api.task.TaskView;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JsonBasedTaskExtensionHandler_V1Test {
    @Test
    public void shouldConvertTaskConfigJsonToTaskConfig() {
        String json = "[" +
                "{\"key\":\"URL\",\"default-value\":\"\",\"secure\":false,\"required\":true,\"display-name\":\"URL\",\"display-order\":\"0\"}," +
                "{\"key\":\"USER\",\"default-value\":\"foo\",\"secure\":true,\"required\":true,\"display-name\":\"User\",\"display-order\":\"1\"}," +
                "{\"key\":\"PASSWORD\"}" +
                "]";
        final TaskConfig config = new JsonBasedTaskExtensionHandler_V1().convertJsonToTaskConfig(json);

        Property url = config.get("URL");
        assertThat(url.getOption(Property.REQUIRED), is(true));
        assertThat(url.getOption(Property.SECURE), is(false));
        assertThat(url.getOption(Property.DISPLAY_NAME), is("URL"));
        assertThat(url.getOption(Property.DISPLAY_ORDER), is(0));

        Property user = config.get("USER");
        assertThat(user.getOption(Property.REQUIRED), is(true));
        assertThat(user.getOption(Property.SECURE), is(true));
        assertThat(user.getOption(Property.DISPLAY_NAME), is("User"));
        assertThat(user.getOption(Property.DISPLAY_ORDER), is(1));

        Property password = config.get("PASSWORD");
        assertThat(password.getOption(Property.REQUIRED), is(true));
        assertThat(password.getOption(Property.SECURE), is(false));
        assertThat(password.getOption(Property.DISPLAY_ORDER), is(0));
        assertThat(password.getOption(Property.DISPLAY_NAME), is(""));
    }

    @Test
    public void shouldConvertJsonToTaskConfigObject(){
        final TaskConfig taskConfig = new TaskConfig();
        final TaskConfigProperty p1 = new TaskConfigProperty("k1", "value1");
        p1.with(Property.DISPLAY_ORDER, 10);
        p1.with(Property.SECURE, true);
        p1.with(Property.DISPLAY_NAME, "display name for k1");
        p1.with(Property.REQUIRED, true);
        final TaskConfigProperty p2 = new TaskConfigProperty("k2", "value1");
        p2.with(Property.DISPLAY_ORDER, 1);
        p2.with(Property.SECURE, false);
        p2.with(Property.DISPLAY_NAME, "display name for k2");
        p2.with(Property.REQUIRED, true);
        p2.with(Property.REQUIRED, true);
        taskConfig.add(p1);
        taskConfig.add(p2);
        final String json = new JsonBasedTaskExtensionHandler_V1().convertTaskConfigToJson(taskConfig);
        assertThat(json, is("[{\"display-name\":\"display name for k1\",\"secure\":true,\"value\":\"value1\",\"display-order\":\"10\",\"required\":true,\"key\":\"k1\"},{\"display-name\":\"display name for k2\",\"secure\":false,\"value\":\"value1\",\"display-order\":\"1\",\"required\":true,\"key\":\"k2\"}]"));
    }

    @Test
    public void shouldConvertJsonResponseToValidationResult() {
        String jsonResponse = "{\"errors\":[{\"key\":\"key1\", \"message\":\"err1\"},{\"key\":\"key1\", \"message\":\"err2\"},{\"key\":\"key2\", \"message\":\"err3\"}]}";

        TaskConfig configuration = new TaskConfig();
        final TaskConfigProperty property = new TaskConfigProperty("URL", "http://foo");
        property.with(Property.SECURE, false);
        property.with(Property.REQUIRED, true);
        property.with(Property.DISPLAY_NAME, "URL");
        property.with(Property.DISPLAY_ORDER, 0);
        configuration.add(property);
        final GoPluginApiResponse response = mock(GoPluginApiResponse.class);
        when(response.responseBody()).thenReturn(jsonResponse);

        ValidationResult result = new JsonBasedTaskExtensionHandler_V1().toValidationResult(response);

        Assert.assertThat(result.isSuccessful(), CoreMatchers.is(false));
        Assert.assertThat(result.getErrors().get(0).getKey(), CoreMatchers.is("key1"));
        Assert.assertThat(result.getErrors().get(0).getMessage(), CoreMatchers.is("err1"));
        Assert.assertThat(result.getErrors().get(1).getKey(), CoreMatchers.is("key1"));
        Assert.assertThat(result.getErrors().get(1).getMessage(), CoreMatchers.is("err2"));
        Assert.assertThat(result.getErrors().get(2).getKey(), CoreMatchers.is("key2"));
        Assert.assertThat(result.getErrors().get(2).getMessage(), CoreMatchers.is("err3"));
    }

    @Test
    public void shouldCreateTaskViewFromResponse() {
        String jsonResponse = "{\"displayValue\":\"MyTaskPlugin\", \"template\":\"<html>junk</html>\"}";
        final GoPluginApiResponse response = mock(GoPluginApiResponse.class);
        when(response.responseBody()).thenReturn(jsonResponse);

        final TaskView view = new JsonBasedTaskExtensionHandler_V1().toTaskView(response);
        Assert.assertThat(view.displayValue(), CoreMatchers.is("MyTaskPlugin"));
        Assert.assertThat(view.template(), CoreMatchers.is("<html>junk</html>"));
    }

    @Test
    public void shouldConstructExecutionResultFromSuccessfulExecutionResponse() {
        GoPluginApiResponse response = mock(GoPluginApiResponse.class);
        when(response.responseBody()).thenReturn("{\"success\":true,\"messages\":[\"message1\",\"message2\"]}");

        final ExecutionResult result = new JsonBasedTaskExtensionHandler_V1().toExecutionResult(response);
        assertThat(result.isSuccessful(), is(true));
        assertThat(result.getMessagesForDisplay(), is("message1\nmessage2"));
    }

    @Test
    public void shouldConstructExecutionResultFromFailureExecutionResponse() {
        GoPluginApiResponse response = mock(GoPluginApiResponse.class);
        when(response.responseBody()).thenReturn("{\"success\":false,\"messages\":[\"error1\",\"error2\"]}");

        final ExecutionResult result = new JsonBasedTaskExtensionHandler_V1().toExecutionResult(response);
        assertThat(result.isSuccessful(), is(false));
        assertThat(result.getMessagesForDisplay(), is("error1\nerror2"));
    }

    @Test
    public void shouldReturnOneDotZeroForVersion(){
        assertThat(new JsonBasedTaskExtensionHandler_V1().version(), is("1.0"));
    }
}
