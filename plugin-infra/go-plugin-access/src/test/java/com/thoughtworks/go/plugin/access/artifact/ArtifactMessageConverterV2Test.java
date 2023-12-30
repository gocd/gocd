/*
 * Copyright 2024 Thoughtworks, Inc.
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
import org.assertj.core.api.Assertions;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother.create;
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ArtifactMessageConverterV2Test {

    @Test
    public void publishArtifactMessage_shouldSerializeToJson() {
        final ArtifactMessageConverterV2 converter = new ArtifactMessageConverterV2();
        final ArtifactStore artifactStore = new ArtifactStore("s3-store", "pluginId", create("Foo", false, "Bar"));
        final ArtifactPlan artifactPlan = new ArtifactPlan(new PluggableArtifactConfig("installers", "s3-store", create("Baz", true, "Car")));
        final Map<String, String> environmentVariables = new HashMap<>();
        environmentVariables.put("foo", "bar");
        final String publishArtifactMessage = converter.publishArtifactMessage(artifactPlan, artifactStore, "/temp", environmentVariables);

        final String expectedStr = "{" +
                "  \"artifact_plan\": {" +
                "    \"configuration\": {" +
                "      \"Baz\": \"Car\"" +
                "    }," +
                "    \"id\": \"installers\"," +
                "    \"storeId\": \"s3-store\"" +
                "  }," +
                "  \"artifact_store\": {" +
                "    \"configuration\": {" +
                "      \"Foo\": \"Bar\"" +
                "    }," +
                "    \"id\": \"s3-store\"" +
                "  }," +
                "  \"agent_working_directory\": \"/temp\"," +
                "  \"environment_variables\": {" +
                "       \"foo\": \"bar\"" +
                "   }" +
                "}";

        assertThatJson(expectedStr).isEqualTo(publishArtifactMessage);
    }

    @Test
    public void publishArtifactResponse_shouldDeserializeFromJson() {
        final ArtifactMessageConverterV2 converter = new ArtifactMessageConverterV2();

        final PublishArtifactResponse response = converter.publishArtifactResponse("""
                {
                  "metadata": {
                    "artifact-version": "10.12.0"
                  }
                }""");


        MatcherAssert.assertThat(response.getMetadata().size(), is(1));
        MatcherAssert.assertThat(response.getMetadata(), hasEntry("artifact-version", "10.12.0"));
    }

    @Test
    public void validateConfigurationRequestBody_shouldSerializeConfigurationToJson() {
        final ArtifactMessageConverterV2 converter = new ArtifactMessageConverterV2();

        final String requestBody = converter.validateConfigurationRequestBody(Map.of("Foo", "Bar"));

        assertThatJson("{\"Foo\":\"Bar\"}").isEqualTo(requestBody);
    }

    @Test
    public void getConfigurationValidationResultFromResponseBody_shouldDeserializeJsonToValidationResult() {
        final ArtifactMessageConverterV2 converter = new ArtifactMessageConverterV2();
        String responseBody = "[{\"message\":\"Url must not be blank.\",\"key\":\"Url\"},{\"message\":\"SearchBase must not be blank.\",\"key\":\"SearchBase\"}]";

        ValidationResult validationResult = converter.getConfigurationValidationResultFromResponseBody(responseBody);

        assertThat(validationResult.isSuccessful(), is(false));
        assertThat(validationResult.getErrors(), containsInAnyOrder(
                new ValidationError("Url", "Url must not be blank."),
                new ValidationError("SearchBase", "SearchBase must not be blank.")
        ));
    }

    @Test
    public void fetchArtifactMessage_shouldSerializeToJson() {
        final ArtifactMessageConverterV2 converter = new ArtifactMessageConverterV2();
        final ArtifactStore artifactStore = new ArtifactStore("s3-store", "pluginId", create("Foo", false, "Bar"));
        final Map<String, Object> metadata = Map.of("Version", "10.12.0");
        final FetchPluggableArtifactTask pluggableArtifactTask = new FetchPluggableArtifactTask(null, null, "artifactId", create("Filename", false, "build/libs/foo.jar"));

        final String fetchArtifactMessage = converter.fetchArtifactMessage(artifactStore, pluggableArtifactTask.getConfiguration(), metadata, "/temp");

        final String expectedStr = """
                {
                  "artifact_metadata": {
                    "Version": "10.12.0"
                  },
                  "store_configuration": {
                    "Foo": "Bar"
                  },
                  "fetch_artifact_configuration": {
                      "Filename": "build/libs/foo.jar"
                    },
                  "agent_working_directory": "/temp"
                }""";

        assertThatJson(expectedStr).isEqualTo(fetchArtifactMessage);
    }

    @Test
    public void fetchArtifactMessage_shouldDeserializeAndAssumeEmpty() {
        Assertions.assertThat(new ArtifactMessageConverterV2().getFetchArtifactEnvironmentVariablesFromResponseBody("")).isEmpty();
    }

    @Test
    public void shouldDeserializeImageFromJson() throws Exception {
        com.thoughtworks.go.plugin.domain.common.Image image = new ArtifactMessageConverterV2().getImageResponseFromBody("{\"content_type\":\"foo\", \"data\":\"bar\"}");
        assertThat(image.getContentType(), is("foo"));
        assertThat(image.getData(), is("bar"));
    }

    @Test
    public void shouldDeserializeCapabilities() {
        final Capabilities capabilities = new ArtifactMessageConverterV2().getCapabilitiesFromResponseBody("{}");
        assertNotNull(capabilities);
    }
}
