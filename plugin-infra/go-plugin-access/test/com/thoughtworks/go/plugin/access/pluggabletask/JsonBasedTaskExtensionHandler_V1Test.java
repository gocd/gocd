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

import com.google.gson.GsonBuilder;
import com.thoughtworks.go.plugin.api.config.Property;
import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse;
import com.thoughtworks.go.plugin.api.response.execution.ExecutionResult;
import com.thoughtworks.go.plugin.api.response.validation.ValidationError;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.plugin.api.task.*;
import com.thoughtworks.go.util.ListUtil;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static junit.framework.Assert.assertTrue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JsonBasedTaskExtensionHandler_V1Test {
    @Test
    public void shouldConvertTaskConfigJsonToTaskConfig() {
        String json = "{\"URL\":{\"default-value\":\"\",\"secure\":false,\"required\":true,\"display-name\":\"URL\",\"display-order\":\"0\"}," +
                "\"USER\":{\"default-value\":\"foo\",\"secure\":true,\"required\":true,\"display-name\":\"User\",\"display-order\":\"1\"}," +
                "\"PASSWORD\":{}," +
                "\"FOO\":null" +
                "}";
        TaskConfig config = new JsonBasedTaskExtensionHandler_V1().convertJsonToTaskConfig(json);

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

        Property foo = config.get("FOO");
        assertThat(foo.getOption(Property.REQUIRED), is(true));
        assertThat(foo.getOption(Property.SECURE), is(false));
        assertThat(foo.getOption(Property.DISPLAY_ORDER), is(0));
        assertThat(foo.getOption(Property.DISPLAY_NAME), is(""));
    }

    @Test
    public void shouldConvertJsonToTaskConfigObject() {
        TaskConfig taskConfig = new TaskConfig();
        TaskConfigProperty p1 = new TaskConfigProperty("k1", "value1");
        p1.with(Property.DISPLAY_ORDER, 10);
        p1.with(Property.SECURE, true);
        p1.with(Property.DISPLAY_NAME, "display name for k1");
        p1.with(Property.REQUIRED, true);

        TaskConfigProperty p2 = new TaskConfigProperty("k2", "value2");
        p2.with(Property.DISPLAY_ORDER, 1);
        p2.with(Property.SECURE, false);
        p2.with(Property.DISPLAY_NAME, "display name for k2");
        p2.with(Property.REQUIRED, true);
        p2.with(Property.REQUIRED, true);

        taskConfig.add(p1);
        taskConfig.add(p2);

        String json = new JsonBasedTaskExtensionHandler_V1().convertTaskConfigToJson(taskConfig);
        Map taskConfigMap = (Map) new GsonBuilder().create().fromJson(json, Object.class);

        Map property1 = (Map) taskConfigMap.get("k1");
        assertThat(property1.get("value").toString(), is("value1"));
        assertThat(property1.get("display-name").toString(), is("display name for k1"));
        assertThat((Boolean) property1.get("secure"), is(true));
        assertThat(property1.get("display-order").toString(), is("10"));
        assertThat((Boolean) property1.get("required"), is(true));

        Map property2 = (Map) taskConfigMap.get("k2");
        assertThat(property2.get("value").toString(), is("value2"));
        assertThat(property2.get("display-name").toString(), is("display name for k2"));
        assertThat((Boolean) property2.get("secure"), is(false));
        assertThat(property2.get("display-order").toString(), is("1"));
        assertThat((Boolean) property2.get("required"), is(true));
    }

    @Test
    public void shouldConvertJsonResponseToValidationResultWhenValidationFails() {
        String jsonResponse = "{\"errors\":{\"key1\":\"err1\",\"key2\":\"err2\"}}";

        TaskConfig configuration = new TaskConfig();
        TaskConfigProperty property = new TaskConfigProperty("URL", "http://foo");
        property.with(Property.SECURE, false);
        property.with(Property.REQUIRED, true);
        property.with(Property.DISPLAY_NAME, "URL");
        property.with(Property.DISPLAY_ORDER, 0);
        configuration.add(property);
        GoPluginApiResponse response = mock(GoPluginApiResponse.class);
        when(response.responseBody()).thenReturn(jsonResponse);

        ValidationResult result = new JsonBasedTaskExtensionHandler_V1().toValidationResult(response);

        Assert.assertThat(result.isSuccessful(), CoreMatchers.is(false));

        ValidationError error1 = findErrorOn(result, "key1");
        ValidationError error2 = findErrorOn(result, "key2");

        Assert.assertThat(error1.getKey(), CoreMatchers.is("key1"));
        Assert.assertThat(error1.getMessage(), CoreMatchers.is("err1"));
        Assert.assertThat(error2.getKey(), CoreMatchers.is("key2"));
        Assert.assertThat(error2.getMessage(), CoreMatchers.is("err2"));
    }

    @Test
    public void shouldConvertJsonResponseToValidationResultWhenValidationPasses() {
        String jsonResponse = "{}";

        TaskConfig configuration = new TaskConfig();
        TaskConfigProperty property = new TaskConfigProperty("URL", "http://foo");
        property.with(Property.SECURE, false);
        property.with(Property.REQUIRED, true);
        property.with(Property.DISPLAY_NAME, "URL");
        property.with(Property.DISPLAY_ORDER, 0);
        configuration.add(property);
        GoPluginApiResponse response = mock(GoPluginApiResponse.class);
        when(response.responseBody()).thenReturn(jsonResponse);

        ValidationResult result = new JsonBasedTaskExtensionHandler_V1().toValidationResult(response);

        Assert.assertThat(result.isSuccessful(), CoreMatchers.is(true));
    }

    private ValidationError findErrorOn(ValidationResult result, final String key) {
        return ListUtil.find(result.getErrors(), new ListUtil.Condition() {
            @Override
            public <T> boolean isMet(T item) {
                ValidationError e = (ValidationError) item;
                return e.getKey().equals(key);
            }
        });
    }

    @Test
    public void shouldCreateTaskViewFromResponse() {
        String jsonResponse = "{\"displayValue\":\"MyTaskPlugin\", \"template\":\"<html>junk</html>\"}";
        GoPluginApiResponse response = mock(GoPluginApiResponse.class);
        when(response.responseBody()).thenReturn(jsonResponse);

        TaskView view = new JsonBasedTaskExtensionHandler_V1().toTaskView(response);
        Assert.assertThat(view.displayValue(), CoreMatchers.is("MyTaskPlugin"));
        Assert.assertThat(view.template(), CoreMatchers.is("<html>junk</html>"));
    }

    @Test
    public void shouldConstructExecutionResultFromSuccessfulExecutionResponse() {
        GoPluginApiResponse response = mock(GoPluginApiResponse.class);
        when(response.responseBody()).thenReturn("{\"success\":true,\"message\":\"message1\"}");

        ExecutionResult result = new JsonBasedTaskExtensionHandler_V1().toExecutionResult(response);
        assertThat(result.isSuccessful(), is(true));
        assertThat(result.getMessagesForDisplay(), is("message1"));
    }

    @Test
    public void shouldConstructExecutionResultFromFailureExecutionResponse() {
        GoPluginApiResponse response = mock(GoPluginApiResponse.class);
        when(response.responseBody()).thenReturn("{\"success\":false,\"message\":\"error1\"}");

        ExecutionResult result = new JsonBasedTaskExtensionHandler_V1().toExecutionResult(response);
        assertThat(result.isSuccessful(), is(false));
        assertThat(result.getMessagesForDisplay(), is("error1"));
    }

    @Test
    public void shouldReturnOneDotZeroForVersion() {
        assertThat(new JsonBasedTaskExtensionHandler_V1().version(), is("1.0"));
    }

    @Test
    public void shouldReturnRequestBodyForTaskExecution() {
        TaskExecutionContext context = mock(TaskExecutionContext.class);
        String workingDir = "working-dir";
        TaskConfig config = new TaskConfig();
        config.add(new TaskConfigProperty("Property1", "Value1"));
        config.add(new TaskConfigProperty("Property2", "Value2"));

        when(context.workingDir()).thenReturn(workingDir);
        when(context.environment()).thenReturn(getEnvironmentVariables());

        String requestBody = new JsonBasedTaskExtensionHandler_V1().getTaskExecutionBody(config, context);
        Map result = (Map) new GsonBuilder().create().fromJson(requestBody, Object.class);
        Map taskExecutionContextFromRequest = (Map) result.get("context");

        assertThat((String) taskExecutionContextFromRequest.get("workingDirectory"), is(workingDir));
        Map environmentVariables = (Map) taskExecutionContextFromRequest.get("environmentVariables");
        assertThat(environmentVariables.size(), is(2));
        assertThat(environmentVariables.get("ENV1").toString(), is("VAL1"));
        assertThat(environmentVariables.get("ENV2").toString(), is("VAL2"));

        Map taskConfigMap = (Map) result.get("config");

        assertThat(taskConfigMap.size(), is(2));
        Map property1 = (Map) taskConfigMap.get("Property1");
        Map property2 = (Map) taskConfigMap.get("Property2");
        assertThat(property1.get("value").toString(), is("Value1"));
        assertThat(property2.get("value").toString(), is("Value2"));
    }

    private EnvironmentVariables getEnvironmentVariables() {
        return new EnvironmentVariables() {
            @Override
            public Map<String, String> asMap() {
                final HashMap<String, String> map = new HashMap<String, String>();
                map.put("ENV1", "VAL1");
                map.put("ENV2", "VAL2");
                return map;
            }

            @Override
            public void writeTo(Console console) {
            }

            @Override
            public Console.SecureEnvVarSpecifier secureEnvSpecifier() {
                return null;
            }
        };
    }
}