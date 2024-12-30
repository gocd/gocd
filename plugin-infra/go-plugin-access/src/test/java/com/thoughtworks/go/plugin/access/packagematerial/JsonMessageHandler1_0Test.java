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
package com.thoughtworks.go.plugin.access.packagematerial;

import com.thoughtworks.go.plugin.api.config.Property;
import com.thoughtworks.go.plugin.api.material.packagerepository.PackageConfiguration;
import com.thoughtworks.go.plugin.api.material.packagerepository.PackageMaterialProperty;
import com.thoughtworks.go.plugin.api.material.packagerepository.PackageRevision;
import com.thoughtworks.go.plugin.api.material.packagerepository.RepositoryConfiguration;
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
    private RepositoryConfiguration repositoryConfiguration;
    private com.thoughtworks.go.plugin.api.material.packagerepository.PackageConfiguration packageConfiguration;

    @BeforeEach
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
    public void shouldBuildRepositoryConfigurationFromResponseBody() {
        String responseBody = "{" +
                "\"key-one\":{}," +
                "\"key-two\":{\"default-value\":\"two\",\"part-of-identity\":true,\"secure\":true,\"required\":true,\"display-name\":\"display-two\",\"display-order\":\"1\"}," +
                "\"key-three\":{\"default-value\":\"three\",\"part-of-identity\":false,\"secure\":false,\"required\":false,\"display-name\":\"display-three\",\"display-order\":\"2\"}" +
                "}";

        RepositoryConfiguration repositoryConfiguration = messageHandler.responseMessageForRepositoryConfiguration(responseBody);
        assertPropertyConfiguration((PackageMaterialProperty) repositoryConfiguration.get("key-one"), "key-one", "", true, true, false, "", 0);
        assertPropertyConfiguration((PackageMaterialProperty) repositoryConfiguration.get("key-two"), "key-two", "two", true, true, true, "display-two", 1);
        assertPropertyConfiguration((PackageMaterialProperty) repositoryConfiguration.get("key-three"), "key-three", "three", false, false, false, "display-three", 2);
    }

    @Test
    public void shouldBuildPackageConfigurationFromResponseBody() {
        String responseBody = "{" +
                "\"key-one\":{}," +
                "\"key-two\":{\"default-value\":\"two\",\"part-of-identity\":true,\"secure\":true,\"required\":true,\"display-name\":\"display-two\",\"display-order\":\"1\"}," +
                "\"key-three\":{\"default-value\":\"three\",\"part-of-identity\":false,\"secure\":false,\"required\":false,\"display-name\":\"display-three\",\"display-order\":\"2\"}" +
                "}";

        com.thoughtworks.go.plugin.api.material.packagerepository.PackageConfiguration packageConfiguration = messageHandler.responseMessageForPackageConfiguration(responseBody);
        assertPropertyConfiguration((PackageMaterialProperty) packageConfiguration.get("key-one"), "key-one", "", true, true, false, "", 0);
        assertPropertyConfiguration((PackageMaterialProperty) packageConfiguration.get("key-two"), "key-two", "two", true, true, true, "display-two", 1);
        assertPropertyConfiguration((PackageMaterialProperty) packageConfiguration.get("key-three"), "key-three", "three", false, false, false, "display-three", 2);
    }

    @Test
    public void shouldBuildRequestBodyForCheckRepositoryConfigurationValidRequest() {
        String requestMessage = messageHandler.requestMessageForIsRepositoryConfigurationValid(repositoryConfiguration);
        assertThat(requestMessage).isEqualTo("{\"repository-configuration\":{\"key-one\":{\"value\":\"value-one\"},\"key-two\":{\"value\":\"value-two\"}}}");
    }

    @Test
    public void shouldBuildValidationResultFromCheckRepositoryConfigurationValidResponse() {
        String responseBody = "[{\"key\":\"key-one\",\"message\":\"incorrect value\"},{\"message\":\"general error\"}]";
        ValidationResult validationResult = messageHandler.responseMessageForIsRepositoryConfigurationValid(responseBody);
        assertValidationError(validationResult.getErrors().get(0), "key-one", "incorrect value");
        assertValidationError(validationResult.getErrors().get(1), "", "general error");
    }

    @Test
    public void shouldBuildSuccessValidationResultFromCheckRepositoryConfigurationValidResponse() {
        assertThat(messageHandler.responseMessageForIsRepositoryConfigurationValid("").isSuccessful()).isTrue();
        assertThat(messageHandler.responseMessageForIsRepositoryConfigurationValid(null).isSuccessful()).isTrue();
    }

    @Test
    public void shouldBuildRequestBodyForCheckPackageConfigurationValidRequest() {
        String requestMessage = messageHandler.requestMessageForIsPackageConfigurationValid(packageConfiguration, repositoryConfiguration);
        assertThat(requestMessage).isEqualTo("{\"repository-configuration\":{\"key-one\":{\"value\":\"value-one\"},\"key-two\":{\"value\":\"value-two\"}},\"package-configuration\":{\"key-three\":{\"value\":\"value-three\"},\"key-four\":{\"value\":\"value-four\"}}}");
    }

    @Test
    public void shouldBuildValidationResultForCheckRepositoryConfigurationValidResponse() {
        String responseBody = "[{\"key\":\"key-one\",\"message\":\"incorrect value\"},{\"message\":\"general error\"}]";
        ValidationResult validationResult = messageHandler.responseMessageForIsPackageConfigurationValid(responseBody);
        assertValidationError(validationResult.getErrors().get(0), "key-one", "incorrect value");
        assertValidationError(validationResult.getErrors().get(1), "", "general error");
    }

    @Test
    public void shouldBuildRequestBodyForCheckRepositoryConnectionRequest() {
        String requestMessage = messageHandler.requestMessageForCheckConnectionToRepository(repositoryConfiguration);
        assertThat(requestMessage).isEqualTo("{\"repository-configuration\":{\"key-one\":{\"value\":\"value-one\"},\"key-two\":{\"value\":\"value-two\"}}}");
    }

    @Test
    public void shouldBuildSuccessResultFromCheckRepositoryConnectionResponse() {
        String responseBody = "{\"status\":\"success\",messages=[\"message-one\",\"message-two\"]}";
        Result result = messageHandler.responseMessageForCheckConnectionToRepository(responseBody);
        assertSuccessResult(result, List.of("message-one", "message-two"));
    }

    @Test
    public void shouldBuildFailureResultFromCheckRepositoryConnectionResponse() {
        String responseBody = "{\"status\":\"failure\",messages=[\"message-one\",\"message-two\"]}";
        Result result = messageHandler.responseMessageForCheckConnectionToRepository(responseBody);
        assertFailureResult(result, List.of("message-one", "message-two"));
    }

    @Test
    public void shouldHandleNullMessagesForCheckRepositoryConnectionResponse() {
        assertSuccessResult(messageHandler.responseMessageForCheckConnectionToRepository("{\"status\":\"success\"}"), new ArrayList<>());
        assertFailureResult(messageHandler.responseMessageForCheckConnectionToRepository("{\"status\":\"failure\"}"), new ArrayList<>());
    }

    @Test
    public void shouldBuildRequestBodyForCheckPackageConnectionRequest() {
        String requestMessage = messageHandler.requestMessageForCheckConnectionToPackage(packageConfiguration, repositoryConfiguration);
        assertThat(requestMessage).isEqualTo("{\"repository-configuration\":{\"key-one\":{\"value\":\"value-one\"},\"key-two\":{\"value\":\"value-two\"}},\"package-configuration\":{\"key-three\":{\"value\":\"value-three\"},\"key-four\":{\"value\":\"value-four\"}}}");
    }

    @Test
    public void shouldBuildSuccessResultFromCheckPackageConnectionResponse() {
        String responseBody = "{\"status\":\"success\",messages=[\"message-one\",\"message-two\"]}";
        Result result = messageHandler.responseMessageForCheckConnectionToPackage(responseBody);
        assertSuccessResult(result, List.of("message-one", "message-two"));
    }

    @Test
    public void shouldBuildFailureResultFromCheckPackageConnectionResponse() {
        String responseBody = "{\"status\":\"failure\",messages=[\"message-one\",\"message-two\"]}";
        Result result = messageHandler.responseMessageForCheckConnectionToPackage(responseBody);
        assertFailureResult(result, List.of("message-one", "message-two"));
    }

    @Test
    public void shouldHandleNullMessagesForCheckPackageConnectionResponse() {
        assertSuccessResult(messageHandler.responseMessageForCheckConnectionToPackage("{\"status\":\"success\"}"), new ArrayList<>());
        assertFailureResult(messageHandler.responseMessageForCheckConnectionToPackage("{\"status\":\"failure\"}"), new ArrayList<>());
    }

    @Test
    public void shouldBuildRequestBodyForLatestRevisionRequest() {
        String requestBody = messageHandler.requestMessageForLatestRevision(packageConfiguration, repositoryConfiguration);
        assertThat(requestBody).isEqualTo("{\"repository-configuration\":{\"key-one\":{\"value\":\"value-one\"},\"key-two\":{\"value\":\"value-two\"}},\"package-configuration\":{\"key-three\":{\"value\":\"value-three\"},\"key-four\":{\"value\":\"value-four\"}}}");
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
        assertThat(getErrorMessageFromLatestRevision("")).isEqualTo("Empty response body");
        assertThat(getErrorMessageFromLatestRevision("{}")).isEqualTo("Empty response body");
        assertThat(getErrorMessageFromLatestRevision(null)).isEqualTo("Empty response body");
    }

    @Test
    public void shouldBuildRequestBodyForLatestRevisionSinceRequest() throws Exception {
        Date timestamp = new SimpleDateFormat(DATE_FORMAT).parse("2011-07-13T19:43:37.100Z");
        Map<String, String> data = new LinkedHashMap<>();
        data.put("dataKeyOne", "data-value-one");
        data.put("dataKeyTwo", "data-value-two");
        PackageRevision previouslyKnownRevision = new PackageRevision("abc.rpm", timestamp, "someuser", "comment", null, data);
        String requestBody = messageHandler.requestMessageForLatestRevisionSince(packageConfiguration, repositoryConfiguration, previouslyKnownRevision);
        String expectedValue = "{\"repository-configuration\":{\"key-one\":{\"value\":\"value-one\"},\"key-two\":{\"value\":\"value-two\"}}," +
                "\"package-configuration\":{\"key-three\":{\"value\":\"value-three\"},\"key-four\":{\"value\":\"value-four\"}}," +
                "\"previous-revision\":{\"revision\":\"abc.rpm\",\"timestamp\":\"2011-07-13T19:43:37.100Z\",\"data\":{\"dataKeyOne\":\"data-value-one\",\"dataKeyTwo\":\"data-value-two\"}}}";
        assertThat(requestBody).isEqualTo(expectedValue);
    }

    @Test
    public void shouldBuildPackageRevisionFromLatestRevisionSinceResponse() throws Exception {
        String responseBody = "{\"revision\":\"abc.rpm\",\"timestamp\":\"2011-07-14T19:43:37.100Z\",\"user\":\"some-user\",\"revisionComment\":\"comment\"," +
                "\"trackbackUrl\":\"http:\\\\localhost:9999\",\"data\":{\"dataKeyOne\":\"data-value-one\",\"dataKeyTwo\":\"data-value-two\"}}";
        PackageRevision packageRevision = messageHandler.responseMessageForLatestRevisionSince(responseBody);
        assertPackageRevision(packageRevision, "abc.rpm", "some-user", "2011-07-14T19:43:37.100Z", "comment", "http:\\localhost:9999");
    }

    @Test
    public void shouldBuildNullPackageRevisionFromLatestRevisionSinceWhenEmptyResponse() {
        assertThat(messageHandler.responseMessageForLatestRevisionSince("")).isNull();
        assertThat(messageHandler.responseMessageForLatestRevisionSince(null)).isNull();
        assertThat(messageHandler.responseMessageForLatestRevisionSince("{}")).isNull();
    }

    @Test
    public void shouldValidateIncorrectJsonResponseForRepositoryConfiguration() {
        assertThat(errorMessageForRepositoryConfiguration("")).isEqualTo("Unable to de-serialize json response. Empty response body");
        assertThat(errorMessageForRepositoryConfiguration(null)).isEqualTo("Unable to de-serialize json response. Empty response body");
        assertThat(errorMessageForRepositoryConfiguration("[{\"key-one\":\"value\"},{\"key-two\":\"value\"}]")).isEqualTo("Unable to de-serialize json response. Repository configuration should be returned as a map");
        assertThat(errorMessageForRepositoryConfiguration("{\"\":{}}")).isEqualTo("Unable to de-serialize json response. Repository configuration key cannot be empty");
        assertThat(errorMessageForRepositoryConfiguration("{\"key\":[{}]}")).isEqualTo("Unable to de-serialize json response. Repository configuration properties for key 'key' should be represented as a Map");

        assertThat(errorMessageForRepositoryConfiguration("{\"key\":{\"part-of-identity\":\"true\"}}")).isEqualTo("Unable to de-serialize json response. 'part-of-identity' property for key 'key' should be of type boolean");
        assertThat(errorMessageForRepositoryConfiguration("{\"key\":{\"part-of-identity\":100}}")).isEqualTo("Unable to de-serialize json response. 'part-of-identity' property for key 'key' should be of type boolean");
        assertThat(errorMessageForRepositoryConfiguration("{\"key\":{\"part-of-identity\":\"\"}}")).isEqualTo("Unable to de-serialize json response. 'part-of-identity' property for key 'key' should be of type boolean");

        assertThat(errorMessageForRepositoryConfiguration("{\"key\":{\"secure\":\"true\"}}")).isEqualTo("Unable to de-serialize json response. 'secure' property for key 'key' should be of type boolean");
        assertThat(errorMessageForRepositoryConfiguration("{\"key\":{\"secure\":100}}")).isEqualTo("Unable to de-serialize json response. 'secure' property for key 'key' should be of type boolean");
        assertThat(errorMessageForRepositoryConfiguration("{\"key\":{\"secure\":\"\"}}")).isEqualTo("Unable to de-serialize json response. 'secure' property for key 'key' should be of type boolean");

        assertThat(errorMessageForRepositoryConfiguration("{\"key\":{\"required\":\"true\"}}")).isEqualTo("Unable to de-serialize json response. 'required' property for key 'key' should be of type boolean");
        assertThat(errorMessageForRepositoryConfiguration("{\"key\":{\"required\":100}}")).isEqualTo("Unable to de-serialize json response. 'required' property for key 'key' should be of type boolean");
        assertThat(errorMessageForRepositoryConfiguration("{\"key\":{\"required\":\"\"}}")).isEqualTo("Unable to de-serialize json response. 'required' property for key 'key' should be of type boolean");

        assertThat(errorMessageForRepositoryConfiguration("{\"key\":{\"display-name\":true}}")).isEqualTo("Unable to de-serialize json response. 'display-name' property for key 'key' should be of type string");
        assertThat(errorMessageForRepositoryConfiguration("{\"key\":{\"display-name\":100}}")).isEqualTo("Unable to de-serialize json response. 'display-name' property for key 'key' should be of type string");

        assertThat(errorMessageForRepositoryConfiguration("{\"key\":{\"display-order\":true}}")).isEqualTo("Unable to de-serialize json response. 'display-order' property for key 'key' should be of type integer");
        assertThat(errorMessageForRepositoryConfiguration("{\"key\":{\"display-order\":10.0}}")).isEqualTo("Unable to de-serialize json response. 'display-order' property for key 'key' should be of type integer");
        assertThat(errorMessageForRepositoryConfiguration("{\"key\":{\"display-order\":\"\"}}")).isEqualTo("Unable to de-serialize json response. 'display-order' property for key 'key' should be of type integer");
    }

    @Test
    public void shouldValidateIncorrectJsonResponseForPackageConfiguration() {
        assertThat(errorMessageForPackageConfiguration("")).isEqualTo("Unable to de-serialize json response. Empty response body");
        assertThat(errorMessageForPackageConfiguration(null)).isEqualTo("Unable to de-serialize json response. Empty response body");
        assertThat(errorMessageForPackageConfiguration("[{\"key-one\":\"value\"},{\"key-two\":\"value\"}]")).isEqualTo("Unable to de-serialize json response. Package configuration should be returned as a map");
        assertThat(errorMessageForPackageConfiguration("{\"\":{}}")).isEqualTo("Unable to de-serialize json response. Package configuration key cannot be empty");
        assertThat(errorMessageForPackageConfiguration("{\"key\":[{}]}")).isEqualTo("Unable to de-serialize json response. Package configuration properties for key 'key' should be represented as a Map");

        assertThat(errorMessageForRepositoryConfiguration("{\"key\":{\"part-of-identity\":\"true\"}}")).isEqualTo("Unable to de-serialize json response. 'part-of-identity' property for key 'key' should be of type boolean");
        assertThat(errorMessageForRepositoryConfiguration("{\"key\":{\"part-of-identity\":100}}")).isEqualTo("Unable to de-serialize json response. 'part-of-identity' property for key 'key' should be of type boolean");
        assertThat(errorMessageForRepositoryConfiguration("{\"key\":{\"part-of-identity\":\"\"}}")).isEqualTo("Unable to de-serialize json response. 'part-of-identity' property for key 'key' should be of type boolean");

        assertThat(errorMessageForRepositoryConfiguration("{\"key\":{\"secure\":\"true\"}}")).isEqualTo("Unable to de-serialize json response. 'secure' property for key 'key' should be of type boolean");
        assertThat(errorMessageForRepositoryConfiguration("{\"key\":{\"secure\":100}}")).isEqualTo("Unable to de-serialize json response. 'secure' property for key 'key' should be of type boolean");
        assertThat(errorMessageForRepositoryConfiguration("{\"key\":{\"secure\":\"\"}}")).isEqualTo("Unable to de-serialize json response. 'secure' property for key 'key' should be of type boolean");

        assertThat(errorMessageForRepositoryConfiguration("{\"key\":{\"required\":\"true\"}}")).isEqualTo("Unable to de-serialize json response. 'required' property for key 'key' should be of type boolean");
        assertThat(errorMessageForRepositoryConfiguration("{\"key\":{\"required\":100}}")).isEqualTo("Unable to de-serialize json response. 'required' property for key 'key' should be of type boolean");
        assertThat(errorMessageForRepositoryConfiguration("{\"key\":{\"required\":\"\"}}")).isEqualTo("Unable to de-serialize json response. 'required' property for key 'key' should be of type boolean");

        assertThat(errorMessageForRepositoryConfiguration("{\"key\":{\"display-name\":true}}")).isEqualTo("Unable to de-serialize json response. 'display-name' property for key 'key' should be of type string");
        assertThat(errorMessageForRepositoryConfiguration("{\"key\":{\"display-name\":100}}")).isEqualTo("Unable to de-serialize json response. 'display-name' property for key 'key' should be of type string");

        assertThat(errorMessageForRepositoryConfiguration("{\"key\":{\"display-order\":true}}")).isEqualTo("Unable to de-serialize json response. 'display-order' property for key 'key' should be of type integer");
        assertThat(errorMessageForRepositoryConfiguration("{\"key\":{\"display-order\":10.0}}")).isEqualTo("Unable to de-serialize json response. 'display-order' property for key 'key' should be of type integer");
        assertThat(errorMessageForRepositoryConfiguration("{\"key\":{\"display-order\":\"\"}}")).isEqualTo("Unable to de-serialize json response. 'display-order' property for key 'key' should be of type integer");

    }

    @Test
    public void shouldValidateIncorrectJsonForPackageRevision() {
        assertThat(errorMessageForPackageRevision("[{\"revision\":\"abc.rpm\"}]")).isEqualTo("Unable to de-serialize json response. Package revision should be returned as a map");
        assertThat(errorMessageForPackageRevision("{\"revision\":{}}")).isEqualTo("Unable to de-serialize json response. Package revision should be of type string");
        assertThat(errorMessageForPackageRevision("{\"revisionComment\":{}}")).isEqualTo("Unable to de-serialize json response. Package revision comment should be of type string");
        assertThat(errorMessageForPackageRevision("{\"user\":{}}")).isEqualTo("Unable to de-serialize json response. Package revision user should be of type string");
        assertThat(errorMessageForPackageRevision("{\"timestamp\":{}}")).isEqualTo("Unable to de-serialize json response. Package revision timestamp should be of type string with format yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        assertThat(errorMessageForPackageRevision("{\"timestamp\":\"12-01-2014\"}")).isEqualTo("Unable to de-serialize json response. Package revision timestamp should be of type string with format yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    }

    private void assertPackageRevision(PackageRevision packageRevision, String revision, String user, String timestamp, String comment, String trackbackUrl) throws ParseException {
        assertThat(packageRevision.getRevision()).isEqualTo(revision);
        assertThat(packageRevision.getUser()).isEqualTo(user);
        assertThat(packageRevision.getTimestamp()).isEqualTo(new SimpleDateFormat(DATE_FORMAT).parse(timestamp));
        assertThat(packageRevision.getRevisionComment()).isEqualTo(comment);
        assertThat(packageRevision.getTrackbackUrl()).isEqualTo(trackbackUrl);
        assertThat(packageRevision.getData().size()).isEqualTo(2);
        assertThat(packageRevision.getDataFor("dataKeyOne")).isEqualTo("data-value-one");
        assertThat(packageRevision.getDataFor("dataKeyTwo")).isEqualTo("data-value-two");
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

    private void assertPropertyConfiguration(PackageMaterialProperty property, String key, String value, boolean partOfIdentity, boolean required, boolean secure, String displayName, int displayOrder) {
        assertThat(property.getKey()).isEqualTo(key);
        assertThat(property.getValue()).isEqualTo(value);
        assertThat(property.getOption(Property.PART_OF_IDENTITY)).isEqualTo(partOfIdentity);
        assertThat(property.getOption(Property.REQUIRED)).isEqualTo(required);
        assertThat(property.getOption(Property.SECURE)).isEqualTo(secure);
        assertThat(property.getOption(Property.DISPLAY_NAME)).isEqualTo(displayName);
        assertThat(property.getOption(Property.DISPLAY_ORDER)).isEqualTo(displayOrder);
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
