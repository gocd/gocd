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
package com.thoughtworks.go.plugin.access.configrepo.contract;

import com.thoughtworks.go.plugin.access.configrepo.ErrorCollection;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Map;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNull;
import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class CRArtifactTest extends CRBaseTest<CRArtifact> {

    private final CRArtifact artifact;
    private final CRArtifact invalidNoSource;

    private CRArtifact validArtifactWithNoConfiguration;
    private CRArtifact validArtifactWithConfiguration;
    private CRArtifact invalidArtifactWithNoId;
    private CRArtifact invalidArtifactWithNoStoreId;
    private CRArtifact invalidArtifactWithInvalidConfiguration;

    public CRArtifactTest()
    {
        artifact = new CRArtifact("src","dest",CRArtifactType.build);
        invalidNoSource = new CRArtifact(null,"dest",CRArtifactType.test);

        validArtifactWithNoConfiguration = new CRArtifact("id", "storeId");
        validArtifactWithConfiguration = new CRArtifact("id", "storeId", new CRConfigurationProperty("foo", "bar"));

        invalidArtifactWithNoId = new CRArtifact(null, "storeId");
        invalidArtifactWithNoStoreId = new CRArtifact("id", null);
        invalidArtifactWithInvalidConfiguration = new CRArtifact("id", "storeId", new CRConfigurationProperty("foo", "bar", "baz"));
    }

    @Override
    public void addGoodExamples(Map<String, CRArtifact> examples) {
        examples.put("artifact",artifact);
        examples.put("validArtifactWithNoConfiguration", validArtifactWithNoConfiguration);
        examples.put("validArtifactWithConfiguration", validArtifactWithConfiguration);
    }

    @Override
    public void addBadExamples(Map<String, CRArtifact> examples) {
        examples.put("invalidNoSource",invalidNoSource);
        examples.put("invalidArtifactWithNoId", invalidArtifactWithNoId);
        examples.put("invalidArtifactWithNoStoreId", invalidArtifactWithNoStoreId);
        examples.put("invalidArtifactWithInvalidConfiguration", invalidArtifactWithInvalidConfiguration);
    }


    @Test
    public void shouldDeserializeFromAPILikeObject()
    {
        String json = "{\n" +
                "      \"source\": \"test\",\n" +
                "      \"destination\": \"res1\",\n" +
                "      \"type\": \"test\"\n" +
                "    }";
        CRArtifact deserializedValue = gson.fromJson(json,CRArtifact.class);

        assertThat(deserializedValue.getSource(),is("test"));
        assertThat(deserializedValue.getDestination(),is("res1"));
        assertThat(deserializedValue.getType(),is(CRArtifactType.test));

        ErrorCollection errors = deserializedValue.getErrors();
        assertTrue(errors.isEmpty());
    }

    @Test
    public void shouldHandleBadArtifactTypeWhenDeserializing()
    {
        String json = "{\n" +
                "      \"source\": \"test\",\n" +
                "      \"destination\": \"res1\",\n" +
                "      \"type\": \"bla\"\n" +
                "    }";
        CRArtifact deserializedValue = gson.fromJson(json,CRArtifact.class);

        assertThat(deserializedValue.getSource(),is("test"));
        assertThat(deserializedValue.getDestination(),is("res1"));
        assertNull(deserializedValue.getType());

        ErrorCollection errors = deserializedValue.getErrors();
        assertFalse(errors.isEmpty());
    }

    @Test
    public void shouldDeserializeWhenConfigurationIsNull() {
        String json = "{\n" +
                "              \"id\" : \"id\",\n" +
                "              \"store_id\" : \"s3\",\n" +
                "              \"type\": \"plugin\"\n" +
                "            }";

        CRArtifact crPluggableArtifact = gson.fromJson(json, CRArtifact.class);

        Assert.assertThat(crPluggableArtifact.getId(), Matchers.is("id"));
        assertThat(crPluggableArtifact.getType(), is(CRArtifactType.plugin));
        Assert.assertThat(crPluggableArtifact.getStoreId(), Matchers.is("s3"));
        Assert.assertNull(crPluggableArtifact.getConfiguration());
    }

    @Test
    public void shouldDeserializePluggableArtifacts() {
        String json = "{\n" +
                "              \"id\" : \"id\",\n" +
                "              \"store_id\" : \"s3\",\n" +
                "              \"type\": \"plugin\",\n" +
                "              \"configuration\": [{\"key\":\"image\", \"value\": \"gocd-agent\"}]" +
                "            }";

        CRArtifact crPluggableArtifact = gson.fromJson(json, CRArtifact.class);

        Assert.assertThat(crPluggableArtifact.getId(), Matchers.is("id"));
        Assert.assertThat(crPluggableArtifact.getStoreId(), Matchers.is("s3"));
        Assert.assertThat(crPluggableArtifact.getConfiguration(), is(Arrays.asList(new CRConfigurationProperty("image", "gocd-agent"))));
    }
}