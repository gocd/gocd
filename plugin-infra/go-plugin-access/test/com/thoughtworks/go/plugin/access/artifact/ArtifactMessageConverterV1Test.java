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
import com.thoughtworks.go.config.PluggableArtifactConfig;
import com.thoughtworks.go.domain.ArtifactPlan;
import com.thoughtworks.go.plugin.access.artifact.model.PublishArtifactResponse;
import org.hamcrest.MatcherAssert;
import org.json.JSONException;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother.create;
import static org.hamcrest.Matchers.*;

public class ArtifactMessageConverterV1Test {

    @Test
    public void publishArtifactMessage_shouldSerializeToJson() throws JSONException {
        final ArtifactMessageConverterV1 converter = new ArtifactMessageConverterV1();
        final ArtifactStore artifactStore = new ArtifactStore("s3-store", "pluginId", create("Foo", false, "Bar"));
        final List<ArtifactPlan> artifactPlans = Arrays.asList(
                new ArtifactPlan(new PluggableArtifactConfig("installers", "s3-store", create("Baz", true, "Car"))),
                new ArtifactPlan(new PluggableArtifactConfig("test-reports", "s3-store", create("junit", false, "junit.xml")))
        );

        final String publishArtifactMessage = converter.publishArtifactMessage(Collections.singletonMap(artifactStore, artifactPlans));

        final String expectedStr = "[\n" +
                "  {\n" +
                "    \"configuration\": {\n" +
                "      \"Foo\": \"Bar\"\n" +
                "    },\n" +
                "    \"id\": \"s3-store\",\n" +
                "    \"artifact_plans\": [\n" +
                "      {\n" +
                "        \"configuration\": {\n" +
                "          \"Baz\": \"Car\"\n" +
                "        },\n" +
                "        \"id\": \"installers\",\n" +
                "        \"storeId\": \"s3-store\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"configuration\": {\n" +
                "          \"junit\": \"junit.xml\"\n" +
                "        },\n" +
                "        \"id\": \"test-reports\",\n" +
                "        \"storeId\": \"s3-store\"\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "]";

        JSONAssert.assertEquals(expectedStr, publishArtifactMessage, true);
    }

    @Test
    public void publishArtifactResponse_shouldDeserializeFromJson() {
        final ArtifactMessageConverterV1 converter = new ArtifactMessageConverterV1();

        final PublishArtifactResponse response = converter.publishArtifactResponse("{\n" +
                "  \"metadata\": {\n" +
                "    \"artifact-version\": \"10.12.0\"\n" +
                "  },\n" +
                "  \"errors\": [\"Foo\",\"Bar\"]\n" +
                "}");


        MatcherAssert.assertThat(response.getMetadata().size(), is(1));
        MatcherAssert.assertThat(response.getMetadata(), hasEntry("artifact-version", "10.12.0"));

        MatcherAssert.assertThat(response.getErrors(), hasSize(2));
        MatcherAssert.assertThat(response.getErrors(), contains("Foo", "Bar"));
    }
}