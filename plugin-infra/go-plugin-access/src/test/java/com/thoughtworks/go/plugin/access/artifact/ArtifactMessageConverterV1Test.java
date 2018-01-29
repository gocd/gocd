/*
 * Copyright 2017 ThoughtWorks, Inc.
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

package com.thoughtworks.go.plugin.access.artifact;

import com.thoughtworks.go.config.ArtifactStore;
import com.thoughtworks.go.config.FetchPluggableArtifactTask;
import com.thoughtworks.go.config.PluggableArtifactConfig;
import com.thoughtworks.go.domain.ArtifactPlan;
import com.thoughtworks.go.plugin.access.artifact.model.PublishArtifactResponse;
import com.thoughtworks.go.plugin.api.response.validation.ValidationError;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.plugin.domain.artifact.Capabilities;
import org.hamcrest.MatcherAssert;
import org.json.JSONException;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import java.util.Collections;
import java.util.Map;

import static com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother.create;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasEntry;
import static org.junit.Assert.assertNotNull;

public class ArtifactMessageConverterV1Test {

    @Test
    public void publishArtifactMessage_shouldSerializeToJson() throws JSONException {
        final ArtifactMessageConverterV1 converter = new ArtifactMessageConverterV1();
        final ArtifactStore artifactStore = new ArtifactStore("s3-store", "pluginId", create("Foo", false, "Bar"));
        final ArtifactPlan artifactPlan = new ArtifactPlan(new PluggableArtifactConfig("installers", "s3-store", create("Baz", true, "Car")));

        final String publishArtifactMessage = converter.publishArtifactMessage(artifactPlan, artifactStore, "/temp");

        final String expectedStr = "{\n" +
                "  \"artifact_plan\": {\n" +
                "    \"configuration\": {\n" +
                "      \"Baz\": \"Car\"\n" +
                "    },\n" +
                "    \"id\": \"installers\",\n" +
                "    \"storeId\": \"s3-store\"\n" +
                "  },\n" +
                "  \"artifact_store\": {\n" +
                "    \"configuration\": {\n" +
                "      \"Foo\": \"Bar\"\n" +
                "    },\n" +
                "    \"id\": \"s3-store\"\n" +
                "  },\n" +
                "  \"agent_working_directory\": \"/temp\"\n" +
                "}";

        JSONAssert.assertEquals(expectedStr, publishArtifactMessage, true);
    }

    @Test
    public void publishArtifactResponse_shouldDeserializeFromJson() {
        final ArtifactMessageConverterV1 converter = new ArtifactMessageConverterV1();

        final PublishArtifactResponse response = converter.publishArtifactResponse("{\n" +
                "  \"metadata\": {\n" +
                "    \"artifact-version\": \"10.12.0\"\n" +
                "  }\n" +
                "}");


        MatcherAssert.assertThat(response.getMetadata().size(), is(1));
        MatcherAssert.assertThat(response.getMetadata(), hasEntry("artifact-version", "10.12.0"));
    }

    @Test
    public void validateConfigurationRequestBody_shouldSerializeConfigurationToJson() throws JSONException {
        final ArtifactMessageConverterV1 converter = new ArtifactMessageConverterV1();

        final String requestBody = converter.validateConfigurationRequestBody(Collections.singletonMap("Foo", "Bar"));

        JSONAssert.assertEquals("{\"Foo\":\"Bar\"}", requestBody, true);
    }

    @Test
    public void getConfigurationValidationResultFromResponseBody_shouldDeserializeJsonToValidationResult() {
        final ArtifactMessageConverterV1 converter = new ArtifactMessageConverterV1();
        String responseBody = "[{\"message\":\"Url must not be blank.\",\"key\":\"Url\"},{\"message\":\"SearchBase must not be blank.\",\"key\":\"SearchBase\"}]";

        ValidationResult validationResult = converter.getConfigurationValidationResultFromResponseBody(responseBody);

        assertThat(validationResult.isSuccessful(), is(false));
        assertThat(validationResult.getErrors(), containsInAnyOrder(
                new ValidationError("Url", "Url must not be blank."),
                new ValidationError("SearchBase", "SearchBase must not be blank.")
        ));
    }

    @Test
    public void fetchArtifactMessage_shouldSerializeToJson() throws JSONException {
        final ArtifactMessageConverterV1 converter = new ArtifactMessageConverterV1();
        final ArtifactStore artifactStore = new ArtifactStore("s3-store", "pluginId", create("Foo", false, "Bar"));
        final Map<String, Object> metadata = Collections.singletonMap("Version", "10.12.0");
        final FetchPluggableArtifactTask pluggableArtifactTask = new FetchPluggableArtifactTask(null, null, "artifactId", create("Filename", false, "build/libs/foo.jar"));

        final String fetchArtifactMessage = converter.fetchArtifactMessage(artifactStore, pluggableArtifactTask.getConfiguration(), metadata, "/temp");

        final String expectedStr = "{\n" +
                "  \"artifact_metadata\": {\n" +
                "    \"Version\": \"10.12.0\"\n" +
                "  },\n" +
                "  \"store_configuration\": {\n" +
                "    \"Foo\": \"Bar\"\n" +
                "  },\n" +
                "  \"fetch_artifact_configuration\": {\n" +
                "      \"Filename\": \"build/libs/foo.jar\"\n" +
                "    },\n" +
                "  \"agent_working_directory\": \"/temp\"\n" +
                "}";

        JSONAssert.assertEquals(expectedStr, fetchArtifactMessage, true);
    }

    @Test
    public void shouldDeserializeImageFromJson() throws Exception {
        com.thoughtworks.go.plugin.domain.common.Image image = new ArtifactMessageConverterV1().getImageResponseFromBody("{\"content_type\":\"foo\", \"data\":\"bar\"}");
        assertThat(image.getContentType(), is("foo"));
        assertThat(image.getData(), is("bar"));
    }

    @Test
    public void shouldDeserializeCapabilities() {
        final Capabilities capabilities = new ArtifactMessageConverterV1().getCapabilitiesFromResponseBody("{}");
        assertNotNull(capabilities);
    }
}