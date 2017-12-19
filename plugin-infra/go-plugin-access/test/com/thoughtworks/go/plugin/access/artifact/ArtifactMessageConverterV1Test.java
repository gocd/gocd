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
import com.thoughtworks.go.config.ArtifactStores;
import com.thoughtworks.go.config.PluggableArtifactConfig;
import com.thoughtworks.go.domain.ArtifactPlan;
import org.json.JSONException;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother.create;
import static org.hamcrest.Matchers.hasEntry;
import static org.junit.Assert.assertThat;

public class ArtifactMessageConverterV1Test {

    @Test
    public void publishArtifactMessage_shouldSerializeToJson() throws JSONException {
        final ArtifactMessageConverterV1 converter = new ArtifactMessageConverterV1();
        final ArtifactStores artifactStores = new ArtifactStores(new ArtifactStore("s3-store", "pluginId", create("Foo", false, "Bar")));
        final List<ArtifactPlan> artifactPlans = Arrays.asList(new ArtifactPlan(new PluggableArtifactConfig("installers", "s3-store", create("Baz", true, "Car"))));

        final String publishArtifactMessage = converter.publishArtifactMessage(artifactStores, artifactPlans);
        System.out.println(publishArtifactMessage);

        final String expectedStr = "{\n" +
                "  \"artifactPlans\": [\n" +
                "    {\n" +
                "      \"configuration\": {\n" +
                "        \"Baz\": \"Car\"\n" +
                "      },\n" +
                "      \"id\": \"installers\",\n" +
                "      \"storeId\": \"s3-store\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"artifactStores\": [\n" +
                "    {\n" +
                "      \"configuration\": {\n" +
                "        \"Foo\": \"Bar\"\n" +
                "      },\n" +
                "      \"id\": \"s3-store\"\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        JSONAssert.assertEquals(expectedStr, publishArtifactMessage, true);
    }

    @Test
    public void publishArtifactResponse_shouldDeserializeFromJson() {
        final ArtifactMessageConverterV1 converter = new ArtifactMessageConverterV1();

        final Map<String, Object> metadata = converter.publishArtifactResponse("{\"Foo\":\"Bar\"}");

        assertThat(metadata, hasEntry("Foo", "Bar"));
    }
}