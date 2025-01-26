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
package com.thoughtworks.go.plugin.access.scm;

import com.google.gson.GsonBuilder;
import com.thoughtworks.go.plugin.access.scm.material.MaterialPollResult;
import com.thoughtworks.go.plugin.access.scm.revision.ModifiedAction;
import com.thoughtworks.go.plugin.access.scm.revision.ModifiedFile;
import com.thoughtworks.go.plugin.access.scm.revision.SCMRevision;
import com.thoughtworks.go.plugin.api.config.Property;
import com.thoughtworks.go.plugin.api.response.Result;
import com.thoughtworks.go.plugin.api.response.validation.ValidationError;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

@SuppressWarnings("SameParameterValue")
public class JsonMessageHandler1_0Test {
    public static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    private JsonMessageHandler1_0 messageHandler;
    private SCMPropertyConfiguration scmPropertyConfiguration;
    private Map<String, String> materialData;

    @BeforeEach
    public void setUp() throws Exception {
        messageHandler = new JsonMessageHandler1_0();
        scmPropertyConfiguration = new SCMPropertyConfiguration();
        scmPropertyConfiguration.add(new SCMProperty("key-one", "value-one"));
        scmPropertyConfiguration.add(new SCMProperty("key-two", "value-two"));
        materialData = new HashMap<>();
        materialData.put("key-one", "value-one");
    }

    @Test
    public void shouldBuildSCMConfigurationFromResponseBody() {
        String responseBody = "{" +
                "\"key-one\":{}," +
                "\"key-two\":{\"default-value\":\"two\",\"part-of-identity\":true,\"secure\":true,\"required\":true,\"display-name\":\"display-two\",\"display-order\":\"1\"}," +
                "\"key-three\":{\"default-value\":\"three\",\"part-of-identity\":false,\"secure\":false,\"required\":false,\"display-name\":\"display-three\",\"display-order\":\"2\"}" +
                "}";
        SCMPropertyConfiguration scmConfiguration = messageHandler.responseMessageForSCMConfiguration(responseBody);

        assertPropertyConfiguration((SCMProperty) scmConfiguration.get("key-one"), "key-one", "", true, true, false, "", 0);
        assertPropertyConfiguration((SCMProperty) scmConfiguration.get("key-two"), "key-two", "two", true, true, true, "display-two", 1);
        assertPropertyConfiguration((SCMProperty) scmConfiguration.get("key-three"), "key-three", "three", false, false, false, "display-three", 2);
    }

    @Test
    public void shouldBuildSCMViewFromResponse() {
        String jsonResponse = "{\"displayValue\":\"MySCMPlugin\", \"template\":\"<html>junk</html>\"}";

        SCMView view = messageHandler.responseMessageForSCMView(jsonResponse);

        assertThat(view.displayValue()).isEqualTo("MySCMPlugin");
        assertThat(view.template()).isEqualTo("<html>junk</html>");
    }

    @Test
    public void shouldBuildRequestBodyForCheckSCMConfigurationValidRequest() {
        String requestMessage = messageHandler.requestMessageForIsSCMConfigurationValid(scmPropertyConfiguration);

        assertThat(requestMessage).isEqualTo("{\"scm-configuration\":{\"key-one\":{\"value\":\"value-one\"},\"key-two\":{\"value\":\"value-two\"}}}");
    }

    @Test
    public void shouldBuildValidationResultFromCheckSCMConfigurationValidResponse() {
        String responseBody = "[{\"key\":\"key-one\",\"message\":\"incorrect value\"},{\"message\":\"general error\"}]";
        ValidationResult validationResult = messageHandler.responseMessageForIsSCMConfigurationValid(responseBody);

        assertValidationError(validationResult.getErrors().get(0), "key-one", "incorrect value");
        assertValidationError(validationResult.getErrors().get(1), "", "general error");
    }

    @Test
    public void shouldBuildSuccessValidationResultFromCheckSCMConfigurationValidResponse() {
        assertThat(messageHandler.responseMessageForIsSCMConfigurationValid("").isSuccessful()).isTrue();
        assertThat(messageHandler.responseMessageForIsSCMConfigurationValid(null).isSuccessful()).isTrue();
    }

    @Test
    public void shouldBuildRequestBodyForCheckSCMConnectionRequest() {
        String requestMessage = messageHandler.requestMessageForCheckConnectionToSCM(scmPropertyConfiguration);

        assertThat(requestMessage).isEqualTo("{\"scm-configuration\":{\"key-one\":{\"value\":\"value-one\"},\"key-two\":{\"value\":\"value-two\"}}}");
    }

    @Test
    public void shouldBuildSuccessResultFromCheckSCMConnectionResponse() {
        String responseBody = "{\"status\":\"success\",messages=[\"message-one\",\"message-two\"]}";
        Result result = messageHandler.responseMessageForCheckConnectionToSCM(responseBody);

        assertSuccessResult(result, List.of("message-one", "message-two"));
    }

    @Test
    public void shouldBuildFailureResultFromCheckSCMConnectionResponse() {
        String responseBody = "{\"status\":\"failure\",messages=[\"message-one\",\"message-two\"]}";
        Result result = messageHandler.responseMessageForCheckConnectionToSCM(responseBody);

        assertFailureResult(result, List.of("message-one", "message-two"));
    }

    @Test
    public void shouldHandleNullMessagesForCheckSCMConnectionResponse() {
        assertSuccessResult(messageHandler.responseMessageForCheckConnectionToSCM("{\"status\":\"success\"}"), new ArrayList<>());
        assertFailureResult(messageHandler.responseMessageForCheckConnectionToSCM("{\"status\":\"failure\"}"), new ArrayList<>());
    }

    @Test
    public void shouldBuildRequestBodyForLatestRevisionRequest() {
        String requestBody = messageHandler.requestMessageForLatestRevision(scmPropertyConfiguration, materialData, "flyweight");

        assertThat(requestBody).isEqualTo("{\"scm-configuration\":{\"key-one\":{\"value\":\"value-one\"},\"key-two\":{\"value\":\"value-two\"}},\"scm-data\":{\"key-one\":\"value-one\"},\"flyweight-folder\":\"flyweight\"}");
    }

    @Test
    public void shouldBuildSCMRevisionFromLatestRevisionResponse() throws Exception {
        String revisionJSON = "{\"revision\":\"r1\",\"timestamp\":\"2011-07-14T19:43:37.100Z\",\"user\":\"some-user\",\"revisionComment\":\"comment\",\"data\":{\"dataKeyTwo\":\"data-value-two\",\"dataKeyOne\":\"data-value-one\"}," +
                "\"modifiedFiles\":[{\"fileName\":\"f1\",\"action\":\"added\"},{\"fileName\":\"f2\",\"action\":\"modified\"},{\"fileName\":\"f3\",\"action\":\"deleted\"}]}";
        String responseBody = "{\"revision\": " + revisionJSON + "}";
        MaterialPollResult pollResult = messageHandler.responseMessageForLatestRevision(responseBody);

        assertThat(pollResult.getMaterialData()).isNull();
        assertSCMRevision(pollResult.getLatestRevision(), "r1", "some-user", "2011-07-14T19:43:37.100Z", "comment", List.of(new ModifiedFile("f1", ModifiedAction.added), new ModifiedFile("f2", ModifiedAction.modified), new ModifiedFile("f3", ModifiedAction.deleted)));
    }

    @Test
    public void shouldBuildSCMDataFromLatestRevisionResponse() {
        String responseBodyWithSCMData = "{\"revision\":{\"revision\":\"r1\",\"timestamp\":\"2011-07-14T19:43:37.100Z\"},\"scm-data\":{\"key-one\":\"value-one\"}}";
        MaterialPollResult pollResult = messageHandler.responseMessageForLatestRevision(responseBodyWithSCMData);

        Map<String, String> scmData = new HashMap<>();
        scmData.put("key-one", "value-one");
        assertThat(pollResult.getMaterialData()).isEqualTo(scmData);
        assertThat(pollResult.getRevisions().get(0).getRevision()).isEqualTo("r1");
    }

    @Test
    public void shouldBuildRequestBodyForLatestRevisionsSinceRequest() throws Exception {
        Date timestamp = new SimpleDateFormat(DATE_FORMAT).parse("2011-07-13T19:43:37.100Z");
        Map<String, String> data = new LinkedHashMap<>();
        data.put("dataKeyOne", "data-value-one");
        data.put("dataKeyTwo", "data-value-two");
        SCMRevision previouslyKnownRevision = new SCMRevision("abc.rpm", timestamp, "someuser", "comment", data, null);
        String requestBody = messageHandler.requestMessageForLatestRevisionsSince(scmPropertyConfiguration, materialData, "flyweight", previouslyKnownRevision);

        String expectedValue = "{\"scm-configuration\":{\"key-one\":{\"value\":\"value-one\"},\"key-two\":{\"value\":\"value-two\"}},\"scm-data\":{\"key-one\":\"value-one\"},\"flyweight-folder\":\"flyweight\"," +
                "\"previous-revision\":{\"revision\":\"abc.rpm\",\"timestamp\":\"2011-07-13T19:43:37.100Z\",\"data\":{\"dataKeyOne\":\"data-value-one\",\"dataKeyTwo\":\"data-value-two\"}}}";
        assertThat(requestBody).isEqualTo(expectedValue);
    }

    @Test
    public void shouldBuildSCMRevisionsFromLatestRevisionsSinceResponse() throws Exception {
        String r1 = "{\"revision\":\"r1\",\"timestamp\":\"2011-07-14T19:43:37.100Z\",\"user\":\"some-user\",\"revisionComment\":\"comment\",\"data\":{\"dataKeyTwo\":\"data-value-two\",\"dataKeyOne\":\"data-value-one\"}," +
                "\"modifiedFiles\":[{\"fileName\":\"f1\",\"action\":\"added\"},{\"fileName\":\"f2\",\"action\":\"modified\"},{\"fileName\":\"f3\",\"action\":\"deleted\"}]}";
        String r2 = "{\"revision\":\"r2\",\"timestamp\":\"2011-07-14T19:43:37.101Z\",\"user\":\"new-user\",\"revisionComment\":\"comment\",\"data\":{\"dataKeyTwo\":\"data-value-two\",\"dataKeyOne\":\"data-value-one\"}," +
                "\"modifiedFiles\":[{\"fileName\":\"f1\",\"action\":\"added\"}]}";
        String responseBody = "{\"revisions\":[" + r1 + "," + r2 + "]}";
        MaterialPollResult pollResult = messageHandler.responseMessageForLatestRevisionsSince(responseBody);

        assertThat(pollResult.getMaterialData()).isNull();
        List<SCMRevision> scmRevisions = pollResult.getRevisions();
        assertThat(scmRevisions.size()).isEqualTo(2);
        assertSCMRevision(scmRevisions.get(0), "r1", "some-user", "2011-07-14T19:43:37.100Z", "comment", List.of(new ModifiedFile("f1", ModifiedAction.added), new ModifiedFile("f2", ModifiedAction.modified), new ModifiedFile("f3", ModifiedAction.deleted)));
        assertSCMRevision(scmRevisions.get(1), "r2", "new-user", "2011-07-14T19:43:37.101Z", "comment", List.of(new ModifiedFile("f1", ModifiedAction.added)));
    }

    @Test
    public void shouldBuildSCMDataFromLatestRevisionsSinceResponse() {
        String responseBodyWithSCMData = "{\"revisions\":[],\"scm-data\":{\"key-one\":\"value-one\"}}";
        MaterialPollResult pollResult = messageHandler.responseMessageForLatestRevisionsSince(responseBodyWithSCMData);

        Map<String, String> scmData = new HashMap<>();
        scmData.put("key-one", "value-one");
        assertThat(pollResult.getMaterialData()).isEqualTo(scmData);
        assertThat(pollResult.getRevisions().isEmpty()).isTrue();
    }

    @Test
    public void shouldBuildNullSCMRevisionFromLatestRevisionsSinceWhenEmptyResponse() {
        MaterialPollResult pollResult = messageHandler.responseMessageForLatestRevisionsSince("");
        assertThat(pollResult.getRevisions()).isNull();
        assertThat(pollResult.getMaterialData()).isNull();
        pollResult = messageHandler.responseMessageForLatestRevisionsSince(null);
        assertThat(pollResult.getRevisions()).isNull();
        assertThat(pollResult.getMaterialData()).isNull();
    }

    @Test
    public void shouldBuildRequestBodyForCheckoutRequest() throws Exception {
        Date timestamp = new SimpleDateFormat(DATE_FORMAT).parse("2011-07-13T19:43:37.100Z");
        Map<String, String> data = new LinkedHashMap<>();
        data.put("dataKeyOne", "data-value-one");
        data.put("dataKeyTwo", "data-value-two");
        SCMRevision revision = new SCMRevision("abc.rpm", timestamp, "someuser", "comment", data, null);
        String requestBody = messageHandler.requestMessageForCheckout(scmPropertyConfiguration, "destination", revision);

        String expectedValue = "{\"scm-configuration\":{\"key-one\":{\"value\":\"value-one\"},\"key-two\":{\"value\":\"value-two\"}},\"destination-folder\":\"destination\"," +
                "\"revision\":{\"revision\":\"abc.rpm\",\"timestamp\":\"2011-07-13T19:43:37.100Z\",\"data\":{\"dataKeyOne\":\"data-value-one\",\"dataKeyTwo\":\"data-value-two\"}}}";
        assertThat(requestBody).isEqualTo(expectedValue);
    }

    @Test
    public void shouldBuildResultFromCheckoutResponse() {
        String responseBody = "{\"status\":\"failure\",messages=[\"message-one\",\"message-two\"]}";
        Result result = messageHandler.responseMessageForCheckout(responseBody);

        assertFailureResult(result, List.of("message-one", "message-two"));
    }

    @Test
    public void shouldValidateIncorrectJsonResponseForSCMConfiguration() {
        assertThat(errorMessageForSCMConfiguration("")).isEqualTo("Unable to de-serialize json response. Empty response body");
        assertThat(errorMessageForSCMConfiguration(null)).isEqualTo("Unable to de-serialize json response. Empty response body");
        assertThat(errorMessageForSCMConfiguration("[{\"key-one\":\"value\"},{\"key-two\":\"value\"}]")).isEqualTo("Unable to de-serialize json response. SCM configuration should be returned as a map");
        assertThat(errorMessageForSCMConfiguration("{\"\":{}}")).isEqualTo("Unable to de-serialize json response. SCM configuration key cannot be empty");
        assertThat(errorMessageForSCMConfiguration("{\"key\":[{}]}")).isEqualTo("Unable to de-serialize json response. SCM configuration properties for key 'key' should be represented as a Map");

        assertThat(errorMessageForSCMConfiguration("{\"key\":{\"part-of-identity\":\"true\"}}")).isEqualTo("Unable to de-serialize json response. 'part-of-identity' property for key 'key' should be of type boolean");
        assertThat(errorMessageForSCMConfiguration("{\"key\":{\"part-of-identity\":100}}")).isEqualTo("Unable to de-serialize json response. 'part-of-identity' property for key 'key' should be of type boolean");
        assertThat(errorMessageForSCMConfiguration("{\"key\":{\"part-of-identity\":\"\"}}")).isEqualTo("Unable to de-serialize json response. 'part-of-identity' property for key 'key' should be of type boolean");

        assertThat(errorMessageForSCMConfiguration("{\"key\":{\"secure\":\"true\"}}")).isEqualTo("Unable to de-serialize json response. 'secure' property for key 'key' should be of type boolean");
        assertThat(errorMessageForSCMConfiguration("{\"key\":{\"secure\":100}}")).isEqualTo("Unable to de-serialize json response. 'secure' property for key 'key' should be of type boolean");
        assertThat(errorMessageForSCMConfiguration("{\"key\":{\"secure\":\"\"}}")).isEqualTo("Unable to de-serialize json response. 'secure' property for key 'key' should be of type boolean");

        assertThat(errorMessageForSCMConfiguration("{\"key\":{\"required\":\"true\"}}")).isEqualTo("Unable to de-serialize json response. 'required' property for key 'key' should be of type boolean");
        assertThat(errorMessageForSCMConfiguration("{\"key\":{\"required\":100}}")).isEqualTo("Unable to de-serialize json response. 'required' property for key 'key' should be of type boolean");
        assertThat(errorMessageForSCMConfiguration("{\"key\":{\"required\":\"\"}}")).isEqualTo("Unable to de-serialize json response. 'required' property for key 'key' should be of type boolean");

        assertThat(errorMessageForSCMConfiguration("{\"key\":{\"display-name\":true}}")).isEqualTo("Unable to de-serialize json response. 'display-name' property for key 'key' should be of type string");
        assertThat(errorMessageForSCMConfiguration("{\"key\":{\"display-name\":100}}")).isEqualTo("Unable to de-serialize json response. 'display-name' property for key 'key' should be of type string");

        assertThat(errorMessageForSCMConfiguration("{\"key\":{\"display-order\":true}}")).isEqualTo("Unable to de-serialize json response. 'display-order' property for key 'key' should be of type integer");
        assertThat(errorMessageForSCMConfiguration("{\"key\":{\"display-order\":10.0}}")).isEqualTo("Unable to de-serialize json response. 'display-order' property for key 'key' should be of type integer");
        assertThat(errorMessageForSCMConfiguration("{\"key\":{\"display-order\":\"\"}}")).isEqualTo("Unable to de-serialize json response. 'display-order' property for key 'key' should be of type integer");
    }

    @Test
    public void shouldValidateIncorrectJsonResponseForSCMView() {
        assertThat(errorMessageForSCMView("{\"template\":\"<html>junk</html>\"}")).isEqualTo("Unable to de-serialize json response. Error: SCM View's 'displayValue' is a required field.");
        assertThat(errorMessageForSCMView("{\"displayValue\":\"MySCMPlugin\"}")).isEqualTo("Unable to de-serialize json response. Error: SCM View's 'template' is a required field.");
        assertThat(errorMessageForSCMView("{\"displayValue\":null, \"template\":\"<html>junk</html>\"}")).isEqualTo("Unable to de-serialize json response. Error: SCM View's 'displayValue' is a required field.");
        assertThat(errorMessageForSCMView("{\"displayValue\":true, \"template\":null}")).isEqualTo("Unable to de-serialize json response. Error: SCM View's 'displayValue' should be of type string.");
        assertThat(errorMessageForSCMView("{\"displayValue\":\"MySCMPlugin\", \"template\":true}")).isEqualTo("Unable to de-serialize json response. Error: SCM View's 'template' should be of type string.");
    }

    @Test
    public void shouldValidateIncorrectJsonForSCMRevisions() {
        assertThat(errorMessageForSCMRevisions("{\"revisions\":{}}")).isEqualTo("Unable to de-serialize json response. 'revisions' should be of type list of map");
        assertThat(errorMessageForSCMRevisions("{\"revisions\":[\"crap\"]}")).isEqualTo("Unable to de-serialize json response. SCM revision should be of type map");
    }

    @Test
    public void shouldValidateIncorrectJsonForSCMRevision() {
        assertThat(errorMessageForSCMRevision("")).isEqualTo("Unable to de-serialize json response. SCM revision cannot be empty");
        assertThat(errorMessageForSCMRevision("{\"revision\":[]}")).isEqualTo("Unable to de-serialize json response. SCM revision should be of type map");
        assertThat(errorMessageForSCMRevision("{\"crap\":{}}")).isEqualTo("Unable to de-serialize json response. SCM revision cannot be empty");
    }

    @Test
    public void shouldValidateIncorrectJsonForEachRevision() {
        assertThat(errorMessageForEachRevision("{\"revision\":{}}")).isEqualTo("SCM revision should be of type string");
        assertThat(errorMessageForEachRevision("{\"revision\":\"\"}")).isEqualTo("SCM revision's 'revision' is a required field");

        assertThat(errorMessageForEachRevision("{\"revision\":\"abc.rpm\",\"timestamp\":{}}")).isEqualTo("SCM revision timestamp should be of type string with format yyyy-MM-dd'T'HH:mm:ss.SSS'Z' and cannot be empty");
        assertThat(errorMessageForEachRevision("{\"revision\":\"abc.rpm\",\"timestamp\":\"\"}")).isEqualTo("SCM revision timestamp should be of type string with format yyyy-MM-dd'T'HH:mm:ss.SSS'Z' and cannot be empty");
        assertThat(errorMessageForEachRevision("{\"revision\":\"abc.rpm\",\"timestamp\":\"12-01-2014\"}")).isEqualTo("SCM revision timestamp should be of type string with format yyyy-MM-dd'T'HH:mm:ss.SSS'Z' and cannot be empty");

        assertThat(errorMessageForEachRevision("{\"revision\":\"abc.rpm\",\"timestamp\":\"2011-07-14T19:43:37.100Z\",\"revisionComment\":{}}")).isEqualTo("SCM revision comment should be of type string");

        assertThat(errorMessageForEachRevision("{\"revision\":\"abc.rpm\",\"timestamp\":\"2011-07-14T19:43:37.100Z\",\"user\":{}}")).isEqualTo("SCM revision user should be of type string");

        assertThat(errorMessageForEachRevision("{\"revision\":\"abc.rpm\",\"timestamp\":\"2011-07-14T19:43:37.100Z\",\"modifiedFiles\":{}}")).isEqualTo("SCM revision 'modifiedFiles' should be of type list of map");

        assertThat(errorMessageForEachRevision("{\"revision\":\"abc.rpm\",\"timestamp\":\"2011-07-14T19:43:37.100Z\",\"modifiedFiles\":[\"crap\"]}")).isEqualTo("SCM revision 'modified file' should be of type map");

        assertThat(errorMessageForEachRevision("{\"revision\":\"abc.rpm\",\"timestamp\":\"2011-07-14T19:43:37.100Z\",\"modifiedFiles\":[{\"fileName\":{}}]}")).isEqualTo("modified file 'fileName' should be of type string");
        assertThat(errorMessageForEachRevision("{\"revision\":\"abc.rpm\",\"timestamp\":\"2011-07-14T19:43:37.100Z\",\"modifiedFiles\":[{\"fileName\":\"\"}]}")).isEqualTo("modified file 'fileName' is a required field");

        assertThat(errorMessageForEachRevision("{\"revision\":\"abc.rpm\",\"timestamp\":\"2011-07-14T19:43:37.100Z\",\"modifiedFiles\":[{\"fileName\":\"f1\",\"action\":{}}]}")).isEqualTo("modified file 'action' should be of type string");
        assertThat(errorMessageForEachRevision("{\"revision\":\"abc.rpm\",\"timestamp\":\"2011-07-14T19:43:37.100Z\",\"modifiedFiles\":[{\"fileName\":\"f1\",\"action\":\"crap\"}]}")).isEqualTo("modified file 'action' can only be added, modified, deleted");
    }

    @Test
    public void shouldValidateIncorrectJsonForSCMData() {
        assertThat(errorMessageForSCMData("{\"scm-data\":[]}")).isEqualTo("Unable to de-serialize json response. SCM data should be of type map");
    }

    private void assertSCMRevision(SCMRevision scmRevision, String revision, String user, String timestamp, String comment, List<ModifiedFile> modifiedFiles) throws ParseException {
        assertThat(scmRevision.getRevision()).isEqualTo(revision);
        assertThat(scmRevision.getUser()).isEqualTo(user);
        assertThat(scmRevision.getTimestamp()).isEqualTo(new SimpleDateFormat(DATE_FORMAT).parse(timestamp));
        assertThat(scmRevision.getRevisionComment()).isEqualTo(comment);
        assertThat(scmRevision.getData().size()).isEqualTo(2);
        assertThat(scmRevision.getDataFor("dataKeyOne")).isEqualTo("data-value-one");
        assertThat(scmRevision.getDataFor("dataKeyTwo")).isEqualTo("data-value-two");
        assertThat(scmRevision.getModifiedFiles()).isEqualTo(modifiedFiles);
    }

    private void assertSuccessResult(Result result, List<String> messages) {
        assertThat(result.isSuccessful()).isTrue();
        assertThat(result.getMessages()).isEqualTo(messages);
    }

    private void assertFailureResult(Result result, List<String> messages) {
        assertThat(result.isSuccessful()).isFalse();
        assertThat(result.getMessages()).isEqualTo(messages);
    }

    private void assertValidationError(ValidationError validationError, String expectedKey, String expectedMessage) {
        assertThat(validationError.getKey()).isEqualTo(expectedKey);
        assertThat(validationError.getMessage()).isEqualTo(expectedMessage);
    }

    private void assertPropertyConfiguration(SCMProperty property, String key, String value, boolean partOfIdentity, boolean required, boolean secure, String displayName, int displayOrder) {
        assertThat(property.getKey()).isEqualTo(key);
        assertThat(property.getValue()).isEqualTo(value);
        assertThat(property.getOption(Property.PART_OF_IDENTITY)).isEqualTo(partOfIdentity);
        assertThat(property.getOption(Property.REQUIRED)).isEqualTo(required);
        assertThat(property.getOption(Property.SECURE)).isEqualTo(secure);
        assertThat(property.getOption(Property.DISPLAY_NAME)).isEqualTo(displayName);
        assertThat(property.getOption(Property.DISPLAY_ORDER)).isEqualTo(displayOrder);
    }

    private String errorMessageForSCMConfiguration(String message) {
        try {
            messageHandler.responseMessageForSCMConfiguration(message);
            fail("should have thrown exception");
        } catch (Exception e) {
            return e.getMessage();
        }
        return null;
    }

    private String errorMessageForSCMView(String message) {
        try {
            messageHandler.responseMessageForSCMView(message);
            fail("should have thrown exception");
        } catch (Exception e) {
            return e.getMessage();
        }
        return null;
    }

    private String errorMessageForSCMRevisions(String message) {
        try {
            @SuppressWarnings("unchecked") Map<String, Object> revisionsMap = (Map<String, Object>) new GsonBuilder().create().fromJson(message, Object.class);
            messageHandler.toSCMRevisions(revisionsMap);
            fail("should have thrown exception");
        } catch (Exception e) {
            return e.getMessage();
        }
        return null;
    }

    private String errorMessageForSCMRevision(String message) {
        try {
            @SuppressWarnings("unchecked") Map<String, Object> revisionMap = (Map<String, Object>) new GsonBuilder().create().fromJson(message, Object.class);
            messageHandler.toSCMRevision(revisionMap);
            fail("should have thrown exception");
        } catch (Exception e) {
            return e.getMessage();
        }
        return null;
    }

    private String errorMessageForEachRevision(String message) {
        try {
            @SuppressWarnings("unchecked") Map<String, Object> revisionMap = (Map<String, Object>) new GsonBuilder().create().fromJson(message, Object.class);
            messageHandler.getScmRevisionFromMap(revisionMap);
            fail("should have thrown exception");
        } catch (Exception e) {
            return e.getMessage();
        }
        return null;
    }

    private String errorMessageForSCMData(String message) {
        try {
            @SuppressWarnings("unchecked") Map<String, Object> dataMap = (Map<String, Object>) new GsonBuilder().create().fromJson(message, Object.class);
            messageHandler.toMaterialDataMap(dataMap);
            fail("should have thrown exception");
        } catch (Exception e) {
            return e.getMessage();
        }
        return null;
    }
}
