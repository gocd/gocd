/*
 * Copyright Thoughtworks, Inc.
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
package com.thoughtworks.go.plugin.access.pluggabletask;

import com.google.gson.GsonBuilder;
import com.thoughtworks.go.plugin.api.config.Property;
import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse;
import com.thoughtworks.go.plugin.api.response.execution.ExecutionResult;
import com.thoughtworks.go.plugin.api.response.validation.ValidationError;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.plugin.api.task.*;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JsonBasedTaskExtensionHandler_V1Test {
    @Test
    public void shouldConvertTaskConfigJsonToTaskConfig() {

        String json = "{\"URL\":{\"default-value\":\"\",\"secure\":false,\"required\":true,\"display-name\":\"Url\",\"display-order\":\"0\"}," +
                "\"USER\":{\"default-value\":\"foo\",\"secure\":true,\"required\":false,\"display-order\":\"1\"}," +
                "\"PASSWORD\":{}," +
                "\"FOO\":null" +
                "}";
        TaskConfig config = new JsonBasedTaskExtensionHandler_V1().convertJsonToTaskConfig(json);

        Property url = config.get("URL");
        assertThat(url.getOption(Property.REQUIRED)).isTrue();
        assertThat(url.getOption(Property.SECURE)).isFalse();
        assertThat(url.getOption(Property.DISPLAY_NAME)).isEqualTo("Url");
        assertThat(url.getOption(Property.DISPLAY_ORDER)).isEqualTo(0);

        Property user = config.get("USER");
        assertThat(user.getOption(Property.REQUIRED)).isFalse();
        assertThat(user.getOption(Property.SECURE)).isTrue();
        assertThat(user.getOption(Property.DISPLAY_NAME)).isEqualTo("USER");
        assertThat(user.getOption(Property.DISPLAY_ORDER)).isEqualTo(1);

        Property password = config.get("PASSWORD");
        assertThat(password.getOption(Property.REQUIRED)).isTrue();
        assertThat(password.getOption(Property.SECURE)).isFalse();
        assertThat(password.getOption(Property.DISPLAY_NAME)).isEqualTo("PASSWORD");
        assertThat(password.getOption(Property.DISPLAY_ORDER)).isEqualTo(0);

        Property foo = config.get("FOO");
        assertThat(foo.getOption(Property.REQUIRED)).isTrue();
        assertThat(foo.getOption(Property.SECURE)).isFalse();
        assertThat(foo.getOption(Property.DISPLAY_NAME)).isEqualTo("FOO");
        assertThat(foo.getOption(Property.DISPLAY_ORDER)).isEqualTo(0);
    }

    @Test
    public void shouldKeepTheConfigInTheOrderOfDisplayOrder(){
        String json = "{\"URL\":{\"default-value\":\"\",\"secure\":false,\"required\":true,\"display-name\":\"Url\",\"display-order\":\"0\"}," +
                "\"PASSWORD\":{\"display-order\":\"2\"}," +
                "\"USER\":{\"default-value\":\"foo\",\"secure\":true,\"required\":false,\"display-order\":\"1\"}" +
                "}";
        TaskConfig config = new JsonBasedTaskExtensionHandler_V1().convertJsonToTaskConfig(json);
        assertThat(config.list().get(0).getKey()).isEqualTo("URL");
        assertThat(config.list().get(1).getKey()).isEqualTo("USER");
        assertThat(config.list().get(2).getKey()).isEqualTo("PASSWORD");
    }

    @Test
    public void shouldConvertTaskConfigObjectToJson() {
        TaskConfig taskConfig = new TaskConfig();
        TaskConfigProperty p1 = new TaskConfigProperty("k1", "value1");
        p1.with(Property.SECURE, true);
        p1.with(Property.REQUIRED, true);

        TaskConfigProperty p2 = new TaskConfigProperty("k2", "value2");
        p2.with(Property.SECURE, false);
        p2.with(Property.REQUIRED, true);

        taskConfig.add(p1);
        taskConfig.add(p2);

        String json = new JsonBasedTaskExtensionHandler_V1().convertTaskConfigToJson(taskConfig);
        Map taskConfigMap = (Map) new GsonBuilder().create().fromJson(json, Object.class);

        Map property1 = (Map) taskConfigMap.get("k1");
        assertThat(property1.get("value").toString()).isEqualTo("value1");
        assertThat(property1.get("secure")).isEqualTo(true);
        assertThat(property1.get("required")).isEqualTo(true);

        Map property2 = (Map) taskConfigMap.get("k2");
        assertThat(property2.get("value").toString()).isEqualTo("value2");
        assertThat(property2.get("secure")).isEqualTo(false);
        assertThat(property2.get("required")).isEqualTo(true);
    }

    @Test
    public void shouldThrowExceptionForWrongJsonWhileConvertingJsonToTaskConfig() {
        String json1 = "{}";
        try {
            new JsonBasedTaskExtensionHandler_V1().convertJsonToTaskConfig(json1);
            fail("should throw exception");
        } catch (Exception e) {
            assertThat(e.getMessage()).isEqualTo("Error occurred while converting the Json to Task Config. Error: The Json for Task Config cannot be empty.");
        }

        String json2 = "{\"URL\":{\"default-value\":true,\"secure\":false,\"required\":true}}";
        try {
            new JsonBasedTaskExtensionHandler_V1().convertJsonToTaskConfig(json2);
            fail("should throw exception");
        } catch (Exception e) {
            assertThat(e.getMessage()).isEqualTo("Error occurred while converting the Json to Task Config. Error: Key: 'URL' - The Json for Task Config should contain a not-null 'default-value' of type String.");
        }

        String json3 = "{\"URL\":{\"default-value\":\"some value\",\"secure\":\"string\",\"required\":true}}";
        try {
            new JsonBasedTaskExtensionHandler_V1().convertJsonToTaskConfig(json3);
            fail("should throw exception");
        } catch (Exception e) {
            assertThat(e.getMessage()).isEqualTo("Error occurred while converting the Json to Task Config. Error: Key: 'URL' - The Json for Task Config should contain a 'secure' field of type Boolean.");
        }

        String json4 = "{\"URL\":{\"default-value\":\"some value\",\"secure\":false,\"required\":\"string\"}}";
        try {
            new JsonBasedTaskExtensionHandler_V1().convertJsonToTaskConfig(json4);
            fail("should throw exception");
        } catch (Exception e) {
            assertThat(e.getMessage()).isEqualTo("Error occurred while converting the Json to Task Config. Error: Key: 'URL' - The Json for Task Config should contain a 'required' field of type Boolean.");
        }

        String json5 = "{\"URL1\":{\"default-value\":true,\"secure\":null,\"required\":true}," +
                "\"URL2\":{\"default-value\":\"some value\",\"secure\":\"some-string\",\"required\":false}," +
                "\"URL3\":{\"default-value\":\"some value\",\"secure\":true,\"required\":\"some-string\"}}";
        try {
            new JsonBasedTaskExtensionHandler_V1().convertJsonToTaskConfig(json5);
            fail("should throw exception");
        } catch (Exception e) {
            assertThat(e.getMessage()).isEqualTo("Error occurred while converting the Json to Task Config. Error: Key: 'URL1' - The Json for Task Config should contain a not-null 'default-value' of type String, Key: 'URL1' - The Json for Task Config should contain a 'secure' field of type Boolean, Key: 'URL2' - The Json for Task Config should contain a 'secure' field of type Boolean, Key: 'URL3' - The Json for Task Config should contain a 'required' field of type Boolean.");
        }

        assertThat(new JsonBasedTaskExtensionHandler_V1().convertJsonToTaskConfig("{\"URL\":{\"display-order\":\"1\"}}").get("URL").getOption(Property.DISPLAY_ORDER)).isEqualTo(1);

        try {
            new JsonBasedTaskExtensionHandler_V1().convertJsonToTaskConfig("{\"URL\":{\"display-order\":\"first\"}}");
            fail("should throw exception");
        } catch (Exception e) {
            assertThat(e.getMessage()).isEqualTo("Error occurred while converting the Json to Task Config. Error: Key: 'URL' - 'display-order' should be a String containing a numerical value.");
        }

        try {
            new JsonBasedTaskExtensionHandler_V1().convertJsonToTaskConfig("{\"URL\":{\"display-order\":1}}");
            fail("should throw exception");
        } catch (Exception e) {
            assertThat(e.getMessage()).isEqualTo("Error occurred while converting the Json to Task Config. Error: Key: 'URL' - 'display-order' should be a String containing a numerical value.");
        }

        assertThat(new JsonBasedTaskExtensionHandler_V1().convertJsonToTaskConfig("{\"URL\":{\"display-name\":\"Uniform Resource Locator\"}}").get("URL").getOption(Property.DISPLAY_NAME)).isEqualTo("Uniform Resource Locator");

        try {
            new JsonBasedTaskExtensionHandler_V1().convertJsonToTaskConfig("{\"URL\":{\"display-name\":{}}}");
            fail("should throw exception");
        } catch (Exception e) {
            assertThat(e.getMessage()).isEqualTo("Error occurred while converting the Json to Task Config. Error: Key: 'URL' - 'display-name' should be of type String.");
        }

        try {
            new JsonBasedTaskExtensionHandler_V1().convertJsonToTaskConfig("{\"URL\":{\"display-name\":1}}");
            fail("should throw exception");
        } catch (Exception e) {
            assertThat(e.getMessage()).isEqualTo("Error occurred while converting the Json to Task Config. Error: Key: 'URL' - 'display-name' should be of type String.");
        }
    }

    @Test
    public void shouldConvertJsonResponseToValidationResultWhenValidationFails() {
        String jsonResponse = "{\"errors\":{\"key1\":\"err1\",\"key2\":\"err2\"}}";

        TaskConfig configuration = new TaskConfig();
        TaskConfigProperty property = new TaskConfigProperty("URL", "http://foo");
        property.with(Property.SECURE, false);
        property.with(Property.REQUIRED, true);
        configuration.add(property);

        ValidationResult result = new JsonBasedTaskExtensionHandler_V1().toValidationResult(jsonResponse);

       assertThat(result.isSuccessful()).isFalse();


        ValidationError error1 = result.getErrors().get(0);
        ValidationError error2 = result.getErrors().get(1);

       assertThat(error1.getKey()).isEqualTo("key1");
       assertThat(error1.getMessage()).isEqualTo("err1");
       assertThat(error2.getKey()).isEqualTo("key2");
       assertThat(error2.getMessage()).isEqualTo("err2");
    }

    @Test
    public void shouldConvertJsonResponseToValidationResultWhenValidationPasses() {
        String jsonResponse = "{}";

        TaskConfig configuration = new TaskConfig();
        TaskConfigProperty property = new TaskConfigProperty("URL", "http://foo");
        property.with(Property.SECURE, false);
        property.with(Property.REQUIRED, true);
        configuration.add(property);

        ValidationResult result = new JsonBasedTaskExtensionHandler_V1().toValidationResult(jsonResponse);

       assertThat(result.isSuccessful()).isTrue();
    }

    @Test
    public void shouldThrowExceptionForWrongJsonWhileConvertingJsonResponseToValidation() {
       assertTrue(new JsonBasedTaskExtensionHandler_V1().toValidationResult("{\"errors\":{}}").isSuccessful());
       assertTrue(new JsonBasedTaskExtensionHandler_V1().toValidationResult("{}").isSuccessful());
       assertTrue(new JsonBasedTaskExtensionHandler_V1().toValidationResult("").isSuccessful());
       assertTrue(new JsonBasedTaskExtensionHandler_V1().toValidationResult(null).isSuccessful());

        String jsonResponse2 = "{\"errors\":{\"key1\":\"err1\",\"key2\":true}}";
        try {
            new JsonBasedTaskExtensionHandler_V1().toValidationResult(jsonResponse2);
            fail("should throw exception");
        } catch (Exception e) {
            assertThat(e.getMessage()).isEqualTo("Error occurred while converting the Json to Validation Result. Error: Key: 'key2' - The Json for Validation Request must contain a not-null error message of type String.");
        }

        String jsonResponse3 = "{\"errors\":{\"key1\":null}}";
        try {
            new JsonBasedTaskExtensionHandler_V1().toValidationResult(jsonResponse3);
            fail("should throw exception");
        } catch (Exception e) {
            assertThat(e.getMessage()).isEqualTo("Error occurred while converting the Json to Validation Result. Error: Key: 'key1' - The Json for Validation Request must contain a not-null error message of type String.");
        }

        String jsonResponse4 = "{\"errors\":{\"key1\":true,\"key2\":\"err2\",\"key3\":null}}";
        try {
            new JsonBasedTaskExtensionHandler_V1().toValidationResult(jsonResponse4);
            fail("should throw exception");
        } catch (Exception e) {
            assertThat(e.getMessage()).isEqualTo("Error occurred while converting the Json to Validation Result. Error: Key: 'key1' - The Json for Validation Request must contain a not-null error message of type String, Key: 'key3' - The Json for Validation Request must contain a not-null error message of type String.");
        }
    }

    @Test
    public void shouldCreateTaskViewFromResponse() {
        String jsonResponse = "{\"displayValue\":\"MyTaskPlugin\", \"template\":\"<html>junk</html>\"}";

        TaskView view = new JsonBasedTaskExtensionHandler_V1().toTaskView(jsonResponse);

       assertThat(view.displayValue()).isEqualTo("MyTaskPlugin");
       assertThat(view.template()).isEqualTo("<html>junk</html>");
    }

    @Test
    public void shouldThrowExceptionForWrongJsonWhileCreatingTaskViewFromResponse() {
        String jsonResponse1 = "{}";
        try {
            new JsonBasedTaskExtensionHandler_V1().toTaskView(jsonResponse1);
            fail("should throw exception");
        } catch (Exception e) {
            assertThat(e.getMessage()).isEqualTo("Error occurred while converting the Json to Task View. Error: The Json for Task View cannot be empty.");
        }

        String jsonResponse2 = "{\"template\":\"<html>junk</html>\"}";
        try {
            new JsonBasedTaskExtensionHandler_V1().toTaskView(jsonResponse2);
            fail("should throw exception");
        } catch (Exception e) {
            assertThat(e.getMessage()).isEqualTo("Error occurred while converting the Json to Task View. Error: The Json for Task View must contain a not-null 'displayValue' of type String.");
        }

        String jsonResponse3 = "{\"displayValue\":\"MyTaskPlugin\"}";
        try {
            new JsonBasedTaskExtensionHandler_V1().toTaskView(jsonResponse3);
            fail("should throw exception");
        } catch (Exception e) {
            assertThat(e.getMessage()).isEqualTo("Error occurred while converting the Json to Task View. Error: The Json for Task View must contain a not-null 'template' of type String.");
        }

        String jsonResponse4 = "{\"displayValue\":null, \"template\":\"<html>junk</html>\"}";
        try {
            new JsonBasedTaskExtensionHandler_V1().toTaskView(jsonResponse4);
            fail("should throw exception");
        } catch (Exception e) {
            assertThat(e.getMessage()).isEqualTo("Error occurred while converting the Json to Task View. Error: The Json for Task View must contain a not-null 'displayValue' of type String.");
        }

        String jsonResponse5 = "{\"displayValue\":\"MyTaskPlugin\", \"template\":true}";
        try {
            new JsonBasedTaskExtensionHandler_V1().toTaskView(jsonResponse5);
            fail("should throw exception");
        } catch (Exception e) {
            assertThat(e.getMessage()).isEqualTo("Error occurred while converting the Json to Task View. Error: The Json for Task View must contain a not-null 'template' of type String.");
        }

        String jsonResponse6 = "{\"displayValue\":true, \"template\":null}";
        try {
            new JsonBasedTaskExtensionHandler_V1().toTaskView(jsonResponse6);
            fail("should throw exception");
        } catch (Exception e) {
            assertThat(e.getMessage()).isEqualTo("Error occurred while converting the Json to Task View. Error: The Json for Task View must contain a not-null 'displayValue' of type String, The Json for Task View must contain a not-null 'template' of type String.");
        }
    }

    @Test
    public void shouldConstructExecutionResultFromSuccessfulExecutionResponse() {
        GoPluginApiResponse response = mock(GoPluginApiResponse.class);
        when(response.responseBody()).thenReturn("{\"success\":true,\"message\":\"message1\"}");

        ExecutionResult result = new JsonBasedTaskExtensionHandler_V1().toExecutionResult(response.responseBody());
        assertThat(result.isSuccessful()).isTrue();
        assertThat(result.getMessagesForDisplay()).isEqualTo("message1");
    }

    @Test
    public void shouldConstructExecutionResultFromFailureExecutionResponse() {
        GoPluginApiResponse response = mock(GoPluginApiResponse.class);
        when(response.responseBody()).thenReturn("{\"success\":false,\"message\":\"error1\"}");

        ExecutionResult result = new JsonBasedTaskExtensionHandler_V1().toExecutionResult(response.responseBody());
        assertThat(result.isSuccessful()).isFalse();
        assertThat(result.getMessagesForDisplay()).isEqualTo("error1");
    }

    @Test
    public void shouldThrowExceptionForWrongJsonWhileConstructingExecutionResultFromExecutionResponse() {
        String json1 = "{}";
        try {
            new JsonBasedTaskExtensionHandler_V1().toExecutionResult(json1);
            fail("should throw exception");
        } catch (Exception e) {
            assertThat(e.getMessage()).isEqualTo("Error occurred while converting the Json to Execution Result. Error: The Json for Execution Result must contain a not-null 'success' field of type Boolean.");
        }

        String json2 = "{\"success\":\"yay\",\"message\":\"error1\"}";
        try {
            new JsonBasedTaskExtensionHandler_V1().toExecutionResult(json2);
            fail("should throw exception");
        } catch (Exception e) {
            assertThat(e.getMessage()).isEqualTo("Error occurred while converting the Json to Execution Result. Error: The Json for Execution Result must contain a not-null 'success' field of type Boolean.");
        }

        String json3 = "{\"success\":false,\"message\":true}";
        try {
            new JsonBasedTaskExtensionHandler_V1().toExecutionResult(json3);
            fail("should throw exception");
        } catch (Exception e) {
            assertThat(e.getMessage()).isEqualTo("Error occurred while converting the Json to Execution Result. Error: If the 'message' key is present in the Json for Execution Result, it must contain a not-null message of type String.");
        }

        String json4 = "{\"message\":null}";
        try {
            new JsonBasedTaskExtensionHandler_V1().toExecutionResult(json4);
            fail("should throw exception");
        } catch (Exception e) {
            assertThat(e.getMessage()).isEqualTo("Error occurred while converting the Json to Execution Result. Error: The Json for Execution Result must contain a not-null 'success' field of type Boolean, If the 'message' key is present in the Json for Execution Result, it must contain a not-null message of type String.");
        }
    }

    @Test
    public void shouldReturnOneDotZeroForVersion() {
        assertThat(new JsonBasedTaskExtensionHandler_V1().version()).isEqualTo("1.0");
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

        assertThat(taskExecutionContextFromRequest.get("workingDirectory")).isEqualTo(workingDir);
        Map environmentVariables = (Map) taskExecutionContextFromRequest.get("environmentVariables");
        assertThat(environmentVariables.size()).isEqualTo(2);
        assertThat(environmentVariables.get("ENV1").toString()).isEqualTo("VAL1");
        assertThat(environmentVariables.get("ENV2").toString()).isEqualTo("VAL2");

        Map<String,Object> taskConfigMap = (Map<String,Object>) result.get("config");

        assertThat(taskConfigMap.size()).isEqualTo(2);
        Map property1 = (Map) taskConfigMap.get("Property1");
        Map property2 = (Map) taskConfigMap.get("Property2");
        assertThat(property1.get("value").toString()).isEqualTo("Value1");
        assertThat(property2.get("value").toString()).isEqualTo("Value2");
    }

    private EnvironmentVariables getEnvironmentVariables() {
        return new EnvironmentVariables() {
            @Override
            public Map<String, String> asMap() {
                final HashMap<String, String> map = new HashMap<>();
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
