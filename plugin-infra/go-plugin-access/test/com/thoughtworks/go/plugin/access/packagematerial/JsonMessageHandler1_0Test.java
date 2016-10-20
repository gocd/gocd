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

package com.thoughtworks.go.plugin.access.packagematerial;

import com.thoughtworks.go.plugin.api.config.Property;
import com.thoughtworks.go.plugin.api.material.packagerepository.PackageConfiguration;
import com.thoughtworks.go.plugin.api.material.packagerepository.PackageMaterialProperty;
import com.thoughtworks.go.plugin.api.material.packagerepository.PackageRevision;
import com.thoughtworks.go.plugin.api.material.packagerepository.RepositoryConfiguration;
import com.thoughtworks.go.plugin.api.response.Result;
import com.thoughtworks.go.plugin.api.response.validation.ValidationError;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import org.junit.Before;
import org.junit.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class JsonMessageHandler1_0Test {
    public static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
    private JsonMessageHandler1_0 messageHandler;
    private RepositoryConfiguration repositoryConfiguration;
    private com.thoughtworks.go.plugin.api.material.packagerepository.PackageConfiguration packageConfiguration;

    @Before
    public void setUp() throws Exception {
        messageHandler = new JsonMessageHandler1_0();
        repositoryConfiguration = new RepositoryConfiguration();
        repositoryConfiguration.add(new PackageMaterialProperty("key-one", "value-one"));
        repositoryConfiguration.add(new PackageMaterialProperty("key-two", "value-two"));

        packageConfiguration = new PackageConfiguration();
        packageConfiguration.add(new PackageMaterialProperty("key-three", "value-three"));
        packageConfiguration.add(new PackageMaterialProperty("key-four", "value-four"));
    }

    @Test
    public void shouldBuildRepositoryConfigurationFromResponseBody() throws Exception {
        String responseBody = "{" +
                "\"key-one\":{}," +
                "\"key-two\":{\"default-value\":\"two\",\"part-of-identity\":true,\"secure\":true,\"required\":true,\"display-name\":\"display-two\",\"display-order\":\"1\"}," +
                "\"key-three\":{\"default-value\":\"three\",\"part-of-identity\":false,\"secure\":false,\"required\":false,\"display-name\":\"display-three\",\"display-order\":\"2\"}" +
                "}";

        RepositoryConfiguration repositoryConfiguration = messageHandler.responseMessageForRepositoryConfiguration(responseBody);
        assertPropertyConfiguration((PackageMaterialProperty) repositoryConfiguration.get("key-one"), "key-one", null, true, true, false, "", 0);
        assertPropertyConfiguration((PackageMaterialProperty) repositoryConfiguration.get("key-two"), "key-two", "two", true, true, true, "display-two", 1);
        assertPropertyConfiguration((PackageMaterialProperty) repositoryConfiguration.get("key-three"), "key-three", "three", false, false, false, "display-three", 2);
    }

    @Test
    public void shouldBuildPackageConfigurationFromResponseBody() throws Exception {
        String responseBody = "{" +
                "\"key-one\":{}," +
                "\"key-two\":{\"default-value\":\"two\",\"part-of-identity\":true,\"secure\":true,\"required\":true,\"display-name\":\"display-two\",\"display-order\":\"1\"}," +
                "\"key-three\":{\"default-value\":\"three\",\"part-of-identity\":false,\"secure\":false,\"required\":false,\"display-name\":\"display-three\",\"display-order\":\"2\"}" +
                "}";

        com.thoughtworks.go.plugin.api.material.packagerepository.PackageConfiguration packageConfiguration = messageHandler.responseMessageForPackageConfiguration(responseBody);
        assertPropertyConfiguration((PackageMaterialProperty) packageConfiguration.get("key-one"), "key-one", null, true, true, false, "", 0);
        assertPropertyConfiguration((PackageMaterialProperty) packageConfiguration.get("key-two"), "key-two", "two", true, true, true, "display-two", 1);
        assertPropertyConfiguration((PackageMaterialProperty) packageConfiguration.get("key-three"), "key-three", "three", false, false, false, "display-three", 2);
    }

    @Test
    public void shouldBuildRequestBodyForCheckRepositoryConfigurationValidRequest() throws Exception {
        String requestMessage = messageHandler.requestMessageForIsRepositoryConfigurationValid(repositoryConfiguration);
        assertThat(requestMessage, is("{\"repository-configuration\":{\"key-one\":{\"value\":\"value-one\"},\"key-two\":{\"value\":\"value-two\"}}}"));
    }

    @Test
    public void shouldBuildValidationResultFromCheckRepositoryConfigurationValidResponse() throws Exception {
        String responseBody = "[{\"key\":\"key-one\",\"message\":\"incorrect value\"},{\"message\":\"general error\"}]";
        ValidationResult validationResult = messageHandler.responseMessageForIsRepositoryConfigurationValid(responseBody);
        assertValidationError(validationResult.getErrors().get(0), "key-one", "incorrect value");
        assertValidationError(validationResult.getErrors().get(1), "", "general error");
    }

    @Test
    public void shouldBuildSuccessValidationResultFromCheckRepositoryConfigurationValidResponse() throws Exception {
        assertThat(messageHandler.responseMessageForIsRepositoryConfigurationValid("").isSuccessful(), is(true));
        assertThat(messageHandler.responseMessageForIsRepositoryConfigurationValid(null).isSuccessful(), is(true));
    }

    @Test
    public void shouldBuildRequestBodyForCheckPackageConfigurationValidRequest() throws Exception {
        String requestMessage = messageHandler.requestMessageForIsPackageConfigurationValid(packageConfiguration, repositoryConfiguration);
        assertThat(requestMessage, is("{\"repository-configuration\":{\"key-one\":{\"value\":\"value-one\"},\"key-two\":{\"value\":\"value-two\"}},\"package-configuration\":{\"key-three\":{\"value\":\"value-three\"},\"key-four\":{\"value\":\"value-four\"}}}"));
    }

    @Test
    public void shouldBuildValidationResultForCheckRepositoryConfigurationValidResponse() throws Exception {
        String responseBody = "[{\"key\":\"key-one\",\"message\":\"incorrect value\"},{\"message\":\"general error\"}]";
        ValidationResult validationResult = messageHandler.responseMessageForIsPackageConfigurationValid(responseBody);
        assertValidationError(validationResult.getErrors().get(0), "key-one", "incorrect value");
        assertValidationError(validationResult.getErrors().get(1), "", "general error");
    }

    @Test
    public void shouldBuildRequestBodyForCheckRepositoryConnectionRequest() throws Exception {
        String requestMessage = messageHandler.requestMessageForCheckConnectionToRepository(repositoryConfiguration);
        assertThat(requestMessage, is("{\"repository-configuration\":{\"key-one\":{\"value\":\"value-one\"},\"key-two\":{\"value\":\"value-two\"}}}"));
    }

    @Test
    public void shouldBuildSuccessResultFromCheckRepositoryConnectionResponse() throws Exception {
        String responseBody = "{\"status\":\"success\",messages=[\"message-one\",\"message-two\"]}";
        Result result = messageHandler.responseMessageForCheckConnectionToRepository(responseBody);
        assertSuccessResult(result, Arrays.asList("message-one", "message-two"));
    }

    @Test
    public void shouldBuildFailureResultFromCheckRepositoryConnectionResponse() throws Exception {
        String responseBody = "{\"status\":\"failure\",messages=[\"message-one\",\"message-two\"]}";
        Result result = messageHandler.responseMessageForCheckConnectionToRepository(responseBody);
        assertFailureResult(result, Arrays.asList("message-one", "message-two"));
    }

    @Test
    public void shouldHandleNullMessagesForCheckRepositoryConnectionResponse() throws Exception {
        assertSuccessResult(messageHandler.responseMessageForCheckConnectionToRepository("{\"status\":\"success\"}"), new ArrayList<String>());
        assertFailureResult(messageHandler.responseMessageForCheckConnectionToRepository("{\"status\":\"failure\"}"), new ArrayList<String>());
    }

    @Test
    public void shouldBuildRequestBodyForCheckPackageConnectionRequest() throws Exception {
        String requestMessage = messageHandler.requestMessageForCheckConnectionToPackage(packageConfiguration, repositoryConfiguration);
        assertThat(requestMessage, is("{\"repository-configuration\":{\"key-one\":{\"value\":\"value-one\"},\"key-two\":{\"value\":\"value-two\"}},\"package-configuration\":{\"key-three\":{\"value\":\"value-three\"},\"key-four\":{\"value\":\"value-four\"}}}"));
    }

    @Test
    public void shouldBuildSuccessResultFromCheckPackageConnectionResponse() throws Exception {
        String responseBody = "{\"status\":\"success\",messages=[\"message-one\",\"message-two\"]}";
        Result result = messageHandler.responseMessageForCheckConnectionToPackage(responseBody);
        assertSuccessResult(result, Arrays.asList("message-one", "message-two"));
    }

    @Test
    public void shouldBuildFailureResultFromCheckPackageConnectionResponse() throws Exception {
        String responseBody = "{\"status\":\"failure\",messages=[\"message-one\",\"message-two\"]}";
        Result result = messageHandler.responseMessageForCheckConnectionToPackage(responseBody);
        assertFailureResult(result, Arrays.asList("message-one", "message-two"));
    }

    @Test
    public void shouldHandleNullMessagesForCheckPackageConnectionResponse() throws Exception {
        assertSuccessResult(messageHandler.responseMessageForCheckConnectionToPackage("{\"status\":\"success\"}"), new ArrayList<String>());
        assertFailureResult(messageHandler.responseMessageForCheckConnectionToPackage("{\"status\":\"failure\"}"), new ArrayList<String>());
    }

    @Test
    public void shouldBuildRequestBodyForLatestRevisionRequest() throws Exception {
        String requestBody = messageHandler.requestMessageForLatestRevision(packageConfiguration, repositoryConfiguration);
        assertThat(requestBody, is("{\"repository-configuration\":{\"key-one\":{\"value\":\"value-one\"},\"key-two\":{\"value\":\"value-two\"}},\"package-configuration\":{\"key-three\":{\"value\":\"value-three\"},\"key-four\":{\"value\":\"value-four\"}}}"));
    }

    @Test
    public void shouldBuildPackageRevisionFromLatestRevisionResponse() throws Exception {
        String responseBody = "{\"revision\":\"abc.rpm\",\"timestamp\":\"2011-07-14T19:43:37.100Z\",\"user\":\"some-user\",\"revisionComment\":\"comment\"," +
                "\"trackbackUrl\":\"http:\\\\localhost:9999\",\"data\":{\"dataKeyOne\":\"data-value-one\",\"dataKeyTwo\":\"data-value-two\"}}";
        PackageRevision packageRevision = messageHandler.responseMessageForLatestRevision(responseBody);
        assertPackageRevision(packageRevision, "abc.rpm", "some-user", "2011-07-14T19:43:37.100Z", "comment", "http:\\localhost:9999");
    }

    @Test
    public void shouldThrowExceptionWhenAttemptingToGetLatestRevisionFromEmptyResponse(){
        assertThat(getErrorMessageFromLatestRevision(""), is("Empty response body"));
        assertThat(getErrorMessageFromLatestRevision("{}"), is("Empty response body"));
        assertThat(getErrorMessageFromLatestRevision(null), is("Empty response body"));
    }

    @Test
    public void shouldBuildRequestBodyForLatestRevisionSinceRequest() throws Exception {
        Date timestamp = new SimpleDateFormat(DATE_FORMAT).parse("2011-07-13T19:43:37.100Z");
        Map data = new LinkedHashMap();
        data.put("dataKeyOne", "data-value-one");
        data.put("dataKeyTwo", "data-value-two");
        PackageRevision previouslyKnownRevision = new PackageRevision("abc.rpm", timestamp, "someuser", "comment", null, data);
        String requestBody = messageHandler.requestMessageForLatestRevisionSince(packageConfiguration, repositoryConfiguration, previouslyKnownRevision);
        String expectedValue = "{\"repository-configuration\":{\"key-one\":{\"value\":\"value-one\"},\"key-two\":{\"value\":\"value-two\"}}," +
                "\"package-configuration\":{\"key-three\":{\"value\":\"value-three\"},\"key-four\":{\"value\":\"value-four\"}}," +
                "\"previous-revision\":{\"revision\":\"abc.rpm\",\"timestamp\":\"2011-07-13T19:43:37.100Z\",\"data\":{\"dataKeyOne\":\"data-value-one\",\"dataKeyTwo\":\"data-value-two\"}}}";
        assertThat(requestBody, is(expectedValue));
    }

    @Test
    public void shouldBuildPackageRevisionFromLatestRevisionSinceResponse() throws Exception {
        String responseBody = "{\"revision\":\"abc.rpm\",\"timestamp\":\"2011-07-14T19:43:37.100Z\",\"user\":\"some-user\",\"revisionComment\":\"comment\"," +
                "\"trackbackUrl\":\"http:\\\\localhost:9999\",\"data\":{\"dataKeyOne\":\"data-value-one\",\"dataKeyTwo\":\"data-value-two\"}}";
        PackageRevision packageRevision = messageHandler.responseMessageForLatestRevisionSince(responseBody);
        assertPackageRevision(packageRevision, "abc.rpm", "some-user", "2011-07-14T19:43:37.100Z", "comment", "http:\\localhost:9999");
    }

    @Test
    public void shouldBuildNullPackageRevisionFromLatestRevisionSinceWhenEmptyResponse() throws Exception {
        assertThat(messageHandler.responseMessageForLatestRevisionSince(""), nullValue());
        assertThat(messageHandler.responseMessageForLatestRevisionSince(null), nullValue());
        assertThat(messageHandler.responseMessageForLatestRevisionSince("{}"), nullValue());
    }

    @Test
    public void shouldValidateIncorrectJsonResponseForRepositoryConfiguration() {
        assertThat(errorMessageForRepositoryConfiguration(""), is("Unable to de-serialize json response. Empty response body"));
        assertThat(errorMessageForRepositoryConfiguration(null), is("Unable to de-serialize json response. Empty response body"));
        assertThat(errorMessageForRepositoryConfiguration("[{\"key-one\":\"value\"},{\"key-two\":\"value\"}]"), is("Unable to de-serialize json response. Repository configuration should be returned as a map"));
        assertThat(errorMessageForRepositoryConfiguration("{\"\":{}}"), is("Unable to de-serialize json response. Repository configuration key cannot be empty"));
        assertThat(errorMessageForRepositoryConfiguration("{\"key\":[{}]}"), is("Unable to de-serialize json response. Repository configuration properties for key 'key' should be represented as a Map"));

        assertThat(errorMessageForRepositoryConfiguration("{\"key\":{\"part-of-identity\":\"true\"}}"), is("Unable to de-serialize json response. 'part-of-identity' property for key 'key' should be of type boolean"));
        assertThat(errorMessageForRepositoryConfiguration("{\"key\":{\"part-of-identity\":100}}"), is("Unable to de-serialize json response. 'part-of-identity' property for key 'key' should be of type boolean"));
        assertThat(errorMessageForRepositoryConfiguration("{\"key\":{\"part-of-identity\":\"\"}}"), is("Unable to de-serialize json response. 'part-of-identity' property for key 'key' should be of type boolean"));

        assertThat(errorMessageForRepositoryConfiguration("{\"key\":{\"secure\":\"true\"}}"), is("Unable to de-serialize json response. 'secure' property for key 'key' should be of type boolean"));
        assertThat(errorMessageForRepositoryConfiguration("{\"key\":{\"secure\":100}}"), is("Unable to de-serialize json response. 'secure' property for key 'key' should be of type boolean"));
        assertThat(errorMessageForRepositoryConfiguration("{\"key\":{\"secure\":\"\"}}"), is("Unable to de-serialize json response. 'secure' property for key 'key' should be of type boolean"));

        assertThat(errorMessageForRepositoryConfiguration("{\"key\":{\"required\":\"true\"}}"), is("Unable to de-serialize json response. 'required' property for key 'key' should be of type boolean"));
        assertThat(errorMessageForRepositoryConfiguration("{\"key\":{\"required\":100}}"), is("Unable to de-serialize json response. 'required' property for key 'key' should be of type boolean"));
        assertThat(errorMessageForRepositoryConfiguration("{\"key\":{\"required\":\"\"}}"), is("Unable to de-serialize json response. 'required' property for key 'key' should be of type boolean"));

        assertThat(errorMessageForRepositoryConfiguration("{\"key\":{\"display-name\":true}}"), is("Unable to de-serialize json response. 'display-name' property for key 'key' should be of type string"));
        assertThat(errorMessageForRepositoryConfiguration("{\"key\":{\"display-name\":100}}"), is("Unable to de-serialize json response. 'display-name' property for key 'key' should be of type string"));

        assertThat(errorMessageForRepositoryConfiguration("{\"key\":{\"display-order\":true}}"), is("Unable to de-serialize json response. 'display-order' property for key 'key' should be of type integer"));
        assertThat(errorMessageForRepositoryConfiguration("{\"key\":{\"display-order\":10.0}}"), is("Unable to de-serialize json response. 'display-order' property for key 'key' should be of type integer"));
        assertThat(errorMessageForRepositoryConfiguration("{\"key\":{\"display-order\":\"\"}}"), is("Unable to de-serialize json response. 'display-order' property for key 'key' should be of type integer"));
    }

    @Test
    public void shouldValidateIncorrectJsonResponseForPackageConfiguration() {
        assertThat(errorMessageForPackageConfiguration(""), is("Unable to de-serialize json response. Empty response body"));
        assertThat(errorMessageForPackageConfiguration(null), is("Unable to de-serialize json response. Empty response body"));
        assertThat(errorMessageForPackageConfiguration("[{\"key-one\":\"value\"},{\"key-two\":\"value\"}]"), is("Unable to de-serialize json response. Package configuration should be returned as a map"));
        assertThat(errorMessageForPackageConfiguration("{\"\":{}}"), is("Unable to de-serialize json response. Package configuration key cannot be empty"));
        assertThat(errorMessageForPackageConfiguration("{\"key\":[{}]}"), is("Unable to de-serialize json response. Package configuration properties for key 'key' should be represented as a Map"));

        assertThat(errorMessageForRepositoryConfiguration("{\"key\":{\"part-of-identity\":\"true\"}}"), is("Unable to de-serialize json response. 'part-of-identity' property for key 'key' should be of type boolean"));
        assertThat(errorMessageForRepositoryConfiguration("{\"key\":{\"part-of-identity\":100}}"), is("Unable to de-serialize json response. 'part-of-identity' property for key 'key' should be of type boolean"));
        assertThat(errorMessageForRepositoryConfiguration("{\"key\":{\"part-of-identity\":\"\"}}"), is("Unable to de-serialize json response. 'part-of-identity' property for key 'key' should be of type boolean"));

        assertThat(errorMessageForRepositoryConfiguration("{\"key\":{\"secure\":\"true\"}}"), is("Unable to de-serialize json response. 'secure' property for key 'key' should be of type boolean"));
        assertThat(errorMessageForRepositoryConfiguration("{\"key\":{\"secure\":100}}"), is("Unable to de-serialize json response. 'secure' property for key 'key' should be of type boolean"));
        assertThat(errorMessageForRepositoryConfiguration("{\"key\":{\"secure\":\"\"}}"), is("Unable to de-serialize json response. 'secure' property for key 'key' should be of type boolean"));

        assertThat(errorMessageForRepositoryConfiguration("{\"key\":{\"required\":\"true\"}}"), is("Unable to de-serialize json response. 'required' property for key 'key' should be of type boolean"));
        assertThat(errorMessageForRepositoryConfiguration("{\"key\":{\"required\":100}}"), is("Unable to de-serialize json response. 'required' property for key 'key' should be of type boolean"));
        assertThat(errorMessageForRepositoryConfiguration("{\"key\":{\"required\":\"\"}}"), is("Unable to de-serialize json response. 'required' property for key 'key' should be of type boolean"));

        assertThat(errorMessageForRepositoryConfiguration("{\"key\":{\"display-name\":true}}"), is("Unable to de-serialize json response. 'display-name' property for key 'key' should be of type string"));
        assertThat(errorMessageForRepositoryConfiguration("{\"key\":{\"display-name\":100}}"), is("Unable to de-serialize json response. 'display-name' property for key 'key' should be of type string"));

        assertThat(errorMessageForRepositoryConfiguration("{\"key\":{\"display-order\":true}}"), is("Unable to de-serialize json response. 'display-order' property for key 'key' should be of type integer"));
        assertThat(errorMessageForRepositoryConfiguration("{\"key\":{\"display-order\":10.0}}"), is("Unable to de-serialize json response. 'display-order' property for key 'key' should be of type integer"));
        assertThat(errorMessageForRepositoryConfiguration("{\"key\":{\"display-order\":\"\"}}"), is("Unable to de-serialize json response. 'display-order' property for key 'key' should be of type integer"));

    }

    @Test
    public void shouldValidateIncorrectJsonForPackageRevision() {
        assertThat(errorMessageForPackageRevision("[{\"revision\":\"abc.rpm\"}]"), is("Unable to de-serialize json response. Package revision should be returned as a map"));
        assertThat(errorMessageForPackageRevision("{\"revision\":{}}"), is("Unable to de-serialize json response. Package revision should be of type string"));
        assertThat(errorMessageForPackageRevision("{\"revisionComment\":{}}"), is("Unable to de-serialize json response. Package revision comment should be of type string"));
        assertThat(errorMessageForPackageRevision("{\"user\":{}}"), is("Unable to de-serialize json response. Package revision user should be of type string"));
        assertThat(errorMessageForPackageRevision("{\"timestamp\":{}}"), is("Unable to de-serialize json response. Package revision timestamp should be of type string with format yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"));
        assertThat(errorMessageForPackageRevision("{\"timestamp\":\"12-01-2014\"}"), is("Unable to de-serialize json response. Package revision timestamp should be of type string with format yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"));
    }

    private void assertPackageRevision(PackageRevision packageRevision, String revision, String user, String timestamp, String comment, String trackbackUrl) throws ParseException {
        assertThat(packageRevision.getRevision(), is(revision));
        assertThat(packageRevision.getUser(), is(user));
        assertThat(packageRevision.getTimestamp(), is(new SimpleDateFormat(DATE_FORMAT).parse(timestamp)));
        assertThat(packageRevision.getRevisionComment(), is(comment));
        assertThat(packageRevision.getTrackbackUrl(), is(trackbackUrl));
        assertThat(packageRevision.getData().size(), is(2));
        assertThat(packageRevision.getDataFor("dataKeyOne"), is("data-value-one"));
        assertThat(packageRevision.getDataFor("dataKeyTwo"), is("data-value-two"));
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

    private void assertPropertyConfiguration(PackageMaterialProperty property, String key, String value, boolean partOfIdentity, boolean required, boolean secure, String displayName, int displayOrder) {
        assertThat(property.getKey(), is(key));
        assertThat(property.getValue(), is(value));
        assertThat(property.getOption(Property.PART_OF_IDENTITY), is(partOfIdentity));
        assertThat(property.getOption(Property.REQUIRED), is(required));
        assertThat(property.getOption(Property.SECURE), is(secure));
        assertThat(property.getOption(Property.DISPLAY_NAME), is(displayName));
        assertThat(property.getOption(Property.DISPLAY_ORDER), is(displayOrder));
    }

    private String errorMessageForRepositoryConfiguration(String message) {
        try {
            messageHandler.responseMessageForRepositoryConfiguration(message);
            fail("should have thrown exception");
        } catch (Exception e) {
            return e.getMessage();
        }
        return null;
    }

    private String errorMessageForPackageConfiguration(String message) {
        try {
            messageHandler.responseMessageForPackageConfiguration(message);
            fail("should have thrown exception");
        } catch (Exception e) {
            return e.getMessage();
        }
        return null;
    }

    private String errorMessageForPackageRevision(String message) {
        try {
            messageHandler.toPackageRevision(message);
            fail("should have thrown exception");
        } catch (Exception e) {
            return e.getMessage();
        }
        return null;
    }

    private String getErrorMessageFromLatestRevision(String responseBody) {
        try{
            messageHandler.responseMessageForLatestRevision(responseBody);
            fail("Should throw exception");
        } catch( RuntimeException e){
            return e.getMessage();
        } return null;
    }
}