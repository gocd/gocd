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

package com.thoughtworks.go.plugin.access.scm;

import com.google.gson.Gson;
import com.thoughtworks.go.plugin.access.scm.revision.ModifiedAction;
import com.thoughtworks.go.plugin.access.scm.revision.ModifiedFile;
import com.thoughtworks.go.plugin.access.scm.revision.SCMRevision;
import com.thoughtworks.go.plugin.api.config.Property;
import com.thoughtworks.go.plugin.api.response.Result;
import com.thoughtworks.go.plugin.api.response.validation.ValidationError;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import org.junit.Before;
import org.junit.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.*;

public class JsonMessageHandler1_0Test {
    public static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    private JsonMessageHandler1_0 messageHandler;
    private SCMPropertyConfiguration scmPropertyConfiguration;

    @Before
    public void setUp() throws Exception {
        messageHandler = new JsonMessageHandler1_0();
        scmPropertyConfiguration = new SCMPropertyConfiguration();
        scmPropertyConfiguration.add(new SCMProperty("key-one", "value-one"));
        scmPropertyConfiguration.add(new SCMProperty("key-two", "value-two"));
    }

    @Test
    public void shouldBuildSCMConfigurationFromResponseBody() throws Exception {
        String responseBody = "{" +
                "\"key-one\":{}," +
                "\"key-two\":{\"default-value\":\"two\",\"part-of-identity\":true,\"secure\":true,\"required\":true,\"display-name\":\"display-two\",\"display-order\":\"1\"}," +
                "\"key-three\":{\"default-value\":\"three\",\"part-of-identity\":false,\"secure\":false,\"required\":false,\"display-name\":\"display-three\",\"display-order\":\"2\"}" +
                "}";
        SCMPropertyConfiguration scmConfiguration = messageHandler.responseMessageForSCMConfiguration(responseBody);

        assertPropertyConfiguration((SCMProperty) scmConfiguration.get("key-one"), "key-one", null, true, true, false, "", 0);
        assertPropertyConfiguration((SCMProperty) scmConfiguration.get("key-two"), "key-two", "two", true, true, true, "display-two", 1);
        assertPropertyConfiguration((SCMProperty) scmConfiguration.get("key-three"), "key-three", "three", false, false, false, "display-three", 2);
    }

    @Test
    public void shouldBuildRequestBodyForCheckSCMConfigurationValidRequest() throws Exception {
        String requestMessage = messageHandler.requestMessageForIsSCMConfigurationValid(scmPropertyConfiguration);

        assertThat(requestMessage, is("{\"scm-configuration\":{\"key-one\":{\"value\":\"value-one\"},\"key-two\":{\"value\":\"value-two\"}}}"));
    }

    @Test
    public void shouldBuildValidationResultFromCheckSCMConfigurationValidResponse() throws Exception {
        String responseBody = "[{\"key\":\"key-one\",\"message\":\"incorrect value\"},{\"message\":\"general error\"}]";
        ValidationResult validationResult = messageHandler.responseMessageForIsSCMConfigurationValid(responseBody);

        assertValidationError(validationResult.getErrors().get(0), "key-one", "incorrect value");
        assertValidationError(validationResult.getErrors().get(1), "", "general error");
    }

    @Test
    public void shouldBuildSuccessValidationResultFromCheckSCMConfigurationValidResponse() throws Exception {
        assertThat(messageHandler.responseMessageForIsSCMConfigurationValid("").isSuccessful(), is(true));
        assertThat(messageHandler.responseMessageForIsSCMConfigurationValid(null).isSuccessful(), is(true));
    }

    @Test
    public void shouldBuildRequestBodyForCheckSCMConnectionRequest() throws Exception {
        String requestMessage = messageHandler.requestMessageForCheckConnectionToSCM(scmPropertyConfiguration);

        assertThat(requestMessage, is("{\"scm-configuration\":{\"key-one\":{\"value\":\"value-one\"},\"key-two\":{\"value\":\"value-two\"}}}"));
    }

    @Test
    public void shouldBuildSuccessResultFromCheckSCMConnectionResponse() throws Exception {
        String responseBody = "{\"status\":\"success\",messages=[\"message-one\",\"message-two\"]}";
        Result result = messageHandler.responseMessageForCheckConnectionToSCM(responseBody);

        assertSuccessResult(result, asList("message-one", "message-two"));
    }

    @Test
    public void shouldBuildFailureResultFromCheckSCMConnectionResponse() throws Exception {
        String responseBody = "{\"status\":\"failure\",messages=[\"message-one\",\"message-two\"]}";
        Result result = messageHandler.responseMessageForCheckConnectionToSCM(responseBody);

        assertFailureResult(result, asList("message-one", "message-two"));
    }

    @Test
    public void shouldHandleNullMessagesForCheckSCMConnectionResponse() throws Exception {
        assertSuccessResult(messageHandler.responseMessageForCheckConnectionToSCM("{\"status\":\"success\"}"), new ArrayList<String>());
        assertFailureResult(messageHandler.responseMessageForCheckConnectionToSCM("{\"status\":\"failure\"}"), new ArrayList<String>());
    }

    @Test
    public void shouldBuildRequestBodyForLatestRevisionRequest() throws Exception {
        String requestBody = messageHandler.requestMessageForLatestRevision(scmPropertyConfiguration, "flyweight");

        assertThat(requestBody, is("{\"scm-configuration\":{\"key-one\":{\"value\":\"value-one\"},\"key-two\":{\"value\":\"value-two\"}},\"flyweight-folder\":\"flyweight\"}"));
    }

    @Test
    public void shouldBuildSCMRevisionFromLatestRevisionResponse() throws Exception {
        String responseBody = "{\"revision\":\"r1\",\"timestamp\":\"2011-07-14T19:43:37.100Z\",\"user\":\"some-user\",\"revisionComment\":\"comment\",\"data\":{\"dataKeyTwo\":\"data-value-two\",\"dataKeyOne\":\"data-value-one\"}," +
                "\"modifiedFiles\":[{\"fileName\":\"f1\",\"action\":\"added\"},{\"fileName\":\"f2\",\"action\":\"modified\"},{\"fileName\":\"f3\",\"action\":\"deleted\"}]}";
        SCMRevision scmRevision = messageHandler.responseMessageForLatestRevision(responseBody);

        assertSCMRevision(scmRevision, "r1", "some-user", "2011-07-14T19:43:37.100Z", "comment", asList(new ModifiedFile("f1", ModifiedAction.added), new ModifiedFile("f2", ModifiedAction.modified), new ModifiedFile("f3", ModifiedAction.deleted)));
    }

    @Test
    public void shouldBuildRequestBodyForLatestRevisionsSinceRequest() throws Exception {
        Date timestamp = new SimpleDateFormat(DATE_FORMAT).parse("2011-07-13T19:43:37.100Z");
        Map data = new LinkedHashMap();
        data.put("dataKeyOne", "data-value-one");
        data.put("dataKeyTwo", "data-value-two");
        SCMRevision previouslyKnownRevision = new SCMRevision("abc.rpm", timestamp, "someuser", "comment", data, null);
        String requestBody = messageHandler.requestMessageForLatestRevisionsSince(scmPropertyConfiguration, "flyweight", previouslyKnownRevision);

        String expectedValue = "{\"scm-configuration\":{\"key-one\":{\"value\":\"value-one\"},\"key-two\":{\"value\":\"value-two\"}},\"flyweight-folder\":\"flyweight\"," +
                "\"previous-revision\":{\"revision\":\"abc.rpm\",\"timestamp\":\"2011-07-13T19:43:37.100Z\",\"data\":{\"dataKeyOne\":\"data-value-one\",\"dataKeyTwo\":\"data-value-two\"}}}";
        assertThat(requestBody, is(expectedValue));
    }

    @Test
    public void shouldBuildSCMRevisionsFromLatestRevisionsSinceResponse() throws Exception {
        String r1 = "{\"revision\":\"r1\",\"timestamp\":\"2011-07-14T19:43:37.100Z\",\"user\":\"some-user\",\"revisionComment\":\"comment\",\"data\":{\"dataKeyTwo\":\"data-value-two\",\"dataKeyOne\":\"data-value-one\"}," +
                "\"modifiedFiles\":[{\"fileName\":\"f1\",\"action\":\"added\"},{\"fileName\":\"f2\",\"action\":\"modified\"},{\"fileName\":\"f3\",\"action\":\"deleted\"}]}";
        String r2 = "{\"revision\":\"r2\",\"timestamp\":\"2011-07-14T19:43:37.101Z\",\"user\":\"new-user\",\"revisionComment\":\"comment\",\"data\":{\"dataKeyTwo\":\"data-value-two\",\"dataKeyOne\":\"data-value-one\"}," +
                "\"modifiedFiles\":[{\"fileName\":\"f1\",\"action\":\"added\"}]}";
        String responseBody = "{\"revisions\":[" + r1 + "," + r2 + "]}";
        List<SCMRevision> scmRevisions = messageHandler.responseMessageForLatestRevisionsSince(responseBody);

        assertThat(scmRevisions.size(), is(2));
        assertSCMRevision(scmRevisions.get(0), "r1", "some-user", "2011-07-14T19:43:37.100Z", "comment", asList(new ModifiedFile("f1", ModifiedAction.added), new ModifiedFile("f2", ModifiedAction.modified), new ModifiedFile("f3", ModifiedAction.deleted)));
        assertSCMRevision(scmRevisions.get(1), "r2", "new-user", "2011-07-14T19:43:37.101Z", "comment", asList(new ModifiedFile("f1", ModifiedAction.added)));
    }

    @Test
    public void shouldBuildNullSCMRevisionFromLatestRevisionsSinceWhenEmptyResponse() throws Exception {
        assertThat(messageHandler.responseMessageForLatestRevisionsSince(""), nullValue());
        assertThat(messageHandler.responseMessageForLatestRevisionsSince(null), nullValue());
    }

    @Test
    public void shouldBuildRequestBodyForCheckoutRequest() throws Exception {
        Date timestamp = new SimpleDateFormat(DATE_FORMAT).parse("2011-07-13T19:43:37.100Z");
        Map data = new LinkedHashMap();
        data.put("dataKeyOne", "data-value-one");
        data.put("dataKeyTwo", "data-value-two");
        SCMRevision revision = new SCMRevision("abc.rpm", timestamp, "someuser", "comment", data, null);
        String requestBody = messageHandler.requestMessageForCheckout(scmPropertyConfiguration, "destination", revision);

        String expectedValue = "{\"scm-configuration\":{\"key-one\":{\"value\":\"value-one\"},\"key-two\":{\"value\":\"value-two\"}},\"destination-folder\":\"destination\"," +
                "\"revision\":{\"revision\":\"abc.rpm\",\"timestamp\":\"2011-07-13T19:43:37.100Z\",\"data\":{\"dataKeyOne\":\"data-value-one\",\"dataKeyTwo\":\"data-value-two\"}}}";
        assertThat(requestBody, is(expectedValue));
    }

    @Test
    public void shouldBuildResultFromCheckoutResponse() throws Exception {
        String responseBody = "{\"status\":\"failure\",messages=[\"message-one\",\"message-two\"]}";
        Result result = messageHandler.responseMessageForCheckout(responseBody);

        assertFailureResult(result, asList("message-one", "message-two"));
    }

    @Test
    public void shouldValidateIncorrectJsonResponseForSCMConfiguration() {
        assertThat(errorMessageForSCMConfiguration(""), is("Unable to de-serialize json response. Empty response body"));
        assertThat(errorMessageForSCMConfiguration(null), is("Unable to de-serialize json response. Empty response body"));
        assertThat(errorMessageForSCMConfiguration("[{\"key-one\":\"value\"},{\"key-two\":\"value\"}]"), is("Unable to de-serialize json response. SCM configuration should be returned as a map"));
        assertThat(errorMessageForSCMConfiguration("{\"\":{}}"), is("Unable to de-serialize json response. SCM configuration key cannot be empty"));
        assertThat(errorMessageForSCMConfiguration("{\"key\":[{}]}"), is("Unable to de-serialize json response. SCM configuration properties for key 'key' should be represented as a Map"));

        assertThat(errorMessageForSCMConfiguration("{\"key\":{\"part-of-identity\":\"true\"}}"), is("Unable to de-serialize json response. 'part-of-identity' property for key 'key' should be of type boolean"));
        assertThat(errorMessageForSCMConfiguration("{\"key\":{\"part-of-identity\":100}}"), is("Unable to de-serialize json response. 'part-of-identity' property for key 'key' should be of type boolean"));
        assertThat(errorMessageForSCMConfiguration("{\"key\":{\"part-of-identity\":\"\"}}"), is("Unable to de-serialize json response. 'part-of-identity' property for key 'key' should be of type boolean"));

        assertThat(errorMessageForSCMConfiguration("{\"key\":{\"secure\":\"true\"}}"), is("Unable to de-serialize json response. 'secure' property for key 'key' should be of type boolean"));
        assertThat(errorMessageForSCMConfiguration("{\"key\":{\"secure\":100}}"), is("Unable to de-serialize json response. 'secure' property for key 'key' should be of type boolean"));
        assertThat(errorMessageForSCMConfiguration("{\"key\":{\"secure\":\"\"}}"), is("Unable to de-serialize json response. 'secure' property for key 'key' should be of type boolean"));

        assertThat(errorMessageForSCMConfiguration("{\"key\":{\"required\":\"true\"}}"), is("Unable to de-serialize json response. 'required' property for key 'key' should be of type boolean"));
        assertThat(errorMessageForSCMConfiguration("{\"key\":{\"required\":100}}"), is("Unable to de-serialize json response. 'required' property for key 'key' should be of type boolean"));
        assertThat(errorMessageForSCMConfiguration("{\"key\":{\"required\":\"\"}}"), is("Unable to de-serialize json response. 'required' property for key 'key' should be of type boolean"));

        assertThat(errorMessageForSCMConfiguration("{\"key\":{\"display-name\":true}}"), is("Unable to de-serialize json response. 'display-name' property for key 'key' should be of type string"));
        assertThat(errorMessageForSCMConfiguration("{\"key\":{\"display-name\":100}}"), is("Unable to de-serialize json response. 'display-name' property for key 'key' should be of type string"));

        assertThat(errorMessageForSCMConfiguration("{\"key\":{\"display-order\":true}}"), is("Unable to de-serialize json response. 'display-order' property for key 'key' should be of type integer"));
        assertThat(errorMessageForSCMConfiguration("{\"key\":{\"display-order\":10.0}}"), is("Unable to de-serialize json response. 'display-order' property for key 'key' should be of type integer"));
        assertThat(errorMessageForSCMConfiguration("{\"key\":{\"display-order\":\"\"}}"), is("Unable to de-serialize json response. 'display-order' property for key 'key' should be of type integer"));
    }

    @Test
    public void shouldValidateIncorrectJsonForValidationResult() {
        assertThat(errorMessageForValidationResult("{{\"key\":\"abc\",\"message\":\"msg\"}}"), is("Unable to de-serialize json response. Validation errors should be returned as list or errors, with each error represented as a map"));
        assertThat(errorMessageForValidationResult("[[{\"key\":\"abc\",\"message\":\"msg\"}]]"), is("Unable to de-serialize json response. Each validation error should be represented as a map"));
        assertThat(errorMessageForValidationResult("[{\"key\":true,\"message\":\"msg\"}]"), is("Unable to de-serialize json response. Validation error key should be of type string"));
        assertThat(errorMessageForValidationResult("[{\"key\":\"abc\",\"message\":{}}]"), is("Unable to de-serialize json response. Validation message should be of type string"));
        assertThat(errorMessageForValidationResult("[{\"key\":\"abc\",\"message\":[]}]"), is("Unable to de-serialize json response. Validation message should be of type string"));
    }

    @Test
    public void shouldValidateIncorrectJsonForResult() {
        assertThat(errorMessageForCheckConnectionResult(""), is("Unable to de-serialize json response. Empty response body"));
        assertThat(errorMessageForCheckConnectionResult("[{\"result\":\"success\"}]"), is("Unable to de-serialize json response. Check connection result should be returned as map, with key represented as string and messages represented as list"));
        assertThat(errorMessageForCheckConnectionResult("{\"status\":true}"), is("Unable to de-serialize json response. Check connection 'status' should be of type string"));
        assertThat(errorMessageForCheckConnectionResult("{\"result\":true}"), is("Unable to de-serialize json response. Check connection 'status' is a required field"));

        assertThat(errorMessageForCheckConnectionResult("{\"status\":\"success\",\"messages\":{}}"), is("Unable to de-serialize json response. Check connection 'messages' should be of type list of string"));
        assertThat(errorMessageForCheckConnectionResult("{\"status\":\"success\",\"messages\":[{},{}]}"), is("Unable to de-serialize json response. Check connection 'message' should be of type string"));
    }

    @Test
    public void shouldValidateIncorrectJsonForSCMRevisions() {
        assertThat(errorMessageForSCMRevisions(""), is("Unable to de-serialize json response. Empty response body"));
        assertThat(errorMessageForSCMRevisions("[]"), is("Unable to de-serialize json response. SCM revisions should be returned as a map"));
        assertThat(errorMessageForSCMRevisions("{\"revisions\":{}}"), is("Unable to de-serialize json response. 'revisions' should be of type list of map"));
        assertThat(errorMessageForSCMRevisions("{\"revisions\":[\"crap\"]}"), is("Unable to de-serialize json response. SCM revision should be of type map"));
    }

    @Test
    public void shouldValidateIncorrectJsonForSCMRevision() {
        assertThat(errorMessageForSCMRevision(""), is("Unable to de-serialize json response. Empty response body"));
        assertThat(errorMessageForSCMRevision("[{\"revision\":\"abc.rpm\"}]"), is("Unable to de-serialize json response. SCM revision should be returned as a map"));
        assertThat(errorMessageForSCMRevision("{\"revision\":{}}"), is("Unable to de-serialize json response. SCM revision should be of type string"));
        assertThat(errorMessageForSCMRevision("{\"revision\":\"\"}"), is("Unable to de-serialize json response. SCM revision's 'revision' is a required field"));

        assertThat(errorMessageForSCMRevision("{\"revision\":\"abc.rpm\",\"timestamp\":{}}"), is("Unable to de-serialize json response. SCM revision timestamp should be of type string with format yyyy-MM-dd'T'HH:mm:ss.SSS'Z' and cannot be empty"));
        assertThat(errorMessageForSCMRevision("{\"revision\":\"abc.rpm\",\"timestamp\":\"\"}"), is("Unable to de-serialize json response. SCM revision timestamp should be of type string with format yyyy-MM-dd'T'HH:mm:ss.SSS'Z' and cannot be empty"));
        assertThat(errorMessageForSCMRevision("{\"revision\":\"abc.rpm\",\"timestamp\":\"12-01-2014\"}"), is("Unable to de-serialize json response. SCM revision timestamp should be of type string with format yyyy-MM-dd'T'HH:mm:ss.SSS'Z' and cannot be empty"));

        assertThat(errorMessageForSCMRevision("{\"revision\":\"abc.rpm\",\"timestamp\":\"2011-07-14T19:43:37.100Z\",\"revisionComment\":{}}"), is("Unable to de-serialize json response. SCM revision comment should be of type string"));

        assertThat(errorMessageForSCMRevision("{\"revision\":\"abc.rpm\",\"timestamp\":\"2011-07-14T19:43:37.100Z\",\"user\":{}}"), is("Unable to de-serialize json response. SCM revision user should be of type string"));

        assertThat(errorMessageForSCMRevision("{\"revision\":\"abc.rpm\",\"timestamp\":\"2011-07-14T19:43:37.100Z\",\"modifiedFiles\":{}}"), is("Unable to de-serialize json response. SCM revision 'modifiedFiles' should be of type list of map"));

        assertThat(errorMessageForSCMRevision("{\"revision\":\"abc.rpm\",\"timestamp\":\"2011-07-14T19:43:37.100Z\",\"modifiedFiles\":[\"crap\"]}"), is("Unable to de-serialize json response. SCM revision 'modified file' should be of type map"));

        assertThat(errorMessageForSCMRevision("{\"revision\":\"abc.rpm\",\"timestamp\":\"2011-07-14T19:43:37.100Z\",\"modifiedFiles\":[{\"fileName\":{}}]}"), is("Unable to de-serialize json response. modified file 'fileName' should be of type string"));
        assertThat(errorMessageForSCMRevision("{\"revision\":\"abc.rpm\",\"timestamp\":\"2011-07-14T19:43:37.100Z\",\"modifiedFiles\":[{\"fileName\":\"\"}]}"), is("Unable to de-serialize json response. modified file 'fileName' is a required field"));

        assertThat(errorMessageForSCMRevision("{\"revision\":\"abc.rpm\",\"timestamp\":\"2011-07-14T19:43:37.100Z\",\"modifiedFiles\":[{\"fileName\":\"f1\",\"action\":{}}]}"), is("Unable to de-serialize json response. modified file 'action' should be of type string"));
        assertThat(errorMessageForSCMRevision("{\"revision\":\"abc.rpm\",\"timestamp\":\"2011-07-14T19:43:37.100Z\",\"modifiedFiles\":[{\"fileName\":\"f1\",\"action\":\"crap\"}]}"), is("Unable to de-serialize json response. modified file 'action' can only be added, modified, deleted"));
    }

    private void assertSCMRevision(SCMRevision scmRevision, String revision, String user, String timestamp, String comment, List<ModifiedFile> modifiedFiles) throws ParseException {
        assertThat(scmRevision.getRevision(), is(revision));
        assertThat(scmRevision.getUser(), is(user));
        assertThat(scmRevision.getTimestamp(), is(new SimpleDateFormat(DATE_FORMAT).parse(timestamp)));
        assertThat(scmRevision.getRevisionComment(), is(comment));
        assertThat(scmRevision.getData().size(), is(2));
        assertThat(scmRevision.getDataFor("dataKeyOne"), is("data-value-one"));
        assertThat(scmRevision.getDataFor("dataKeyTwo"), is("data-value-two"));
        assertThat(scmRevision.getModifiedFiles(), is(modifiedFiles));
    }

    private void assertSuccessResult(Result result, List<String> messages) {
        assertThat(result.isSuccessful(), is(true));
        assertThat(result.getMessages(), is(messages));
    }

    private void assertFailureResult(Result result, List<String> messages) {
        assertThat(result.isSuccessful(), is(false));
        assertThat(result.getMessages(), is(messages));
    }

    private void assertValidationError(ValidationError validationError, String expectedKey, String expectedMessage) {
        assertThat(validationError.getKey(), is(expectedKey));
        assertThat(validationError.getMessage(), is(expectedMessage));
    }

    private void assertPropertyConfiguration(SCMProperty property, String key, String value, boolean partOfIdentity, boolean required, boolean secure, String displayName, int displayOrder) {
        assertThat(property.getKey(), is(key));
        assertThat(property.getValue(), is(value));
        assertThat(property.getOption(Property.PART_OF_IDENTITY), is(partOfIdentity));
        assertThat(property.getOption(Property.REQUIRED), is(required));
        assertThat(property.getOption(Property.SECURE), is(secure));
        assertThat(property.getOption(Property.DISPLAY_NAME), is(displayName));
        assertThat(property.getOption(Property.DISPLAY_ORDER), is(displayOrder));
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

    private String errorMessageForValidationResult(String message) {
        try {
            messageHandler.toValidationResult(message);
            fail("should have thrown exception");
        } catch (Exception e) {
            return e.getMessage();
        }
        return null;
    }

    private String errorMessageForCheckConnectionResult(String message) {
        try {
            messageHandler.toResult(message);
            fail("should have thrown exception");
        } catch (Exception e) {
            return e.getMessage();
        }
        return null;
    }

    private String errorMessageForSCMRevisions(String message) {
        try {
            messageHandler.toSCMRevisions(message);
            fail("should have thrown exception");
        } catch (Exception e) {
            return e.getMessage();
        }
        return null;
    }

    private String errorMessageForSCMRevision(String message) {
        try {
            messageHandler.toSCMRevision(message);
            fail("should have thrown exception");
        } catch (Exception e) {
            return e.getMessage();
        }
        return null;
    }
}