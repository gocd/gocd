/*
 * Copyright 2021 ThoughtWorks, Inc.
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
package com.thoughtworks.go.plugin.configrepo.contract;

import org.junit.Test;

import java.util.Arrays;
import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

public class CRPluggableArtifactTest extends AbstractCRTest<CRPluggableArtifact> {
    private CRPluggableArtifact validArtifactWithNoConfiguration;
    private CRPluggableArtifact validArtifactWithConfiguration;
    private CRPluggableArtifact invalidArtifactWithNoId;
    private CRPluggableArtifact invalidArtifactWithNoStoreId;
    private CRPluggableArtifact invalidArtifactWithInvalidConfiguration;

    public CRPluggableArtifactTest() {
        validArtifactWithNoConfiguration = new CRPluggableArtifact("id", "storeId", null);
        validArtifactWithConfiguration = new CRPluggableArtifact("id", "storeId", Arrays.asList(new CRConfigurationProperty("foo", "bar")));

        invalidArtifactWithNoId = new CRPluggableArtifact(null, "storeId", null);
        invalidArtifactWithNoStoreId = new CRPluggableArtifact("id", null, null);
        invalidArtifactWithInvalidConfiguration = new CRPluggableArtifact("id", "storeId", Arrays.asList(new CRConfigurationProperty("foo", "bar", "baz")));
    }

    @Override
    public void addGoodExamples(Map<String, CRPluggableArtifact> examples) {
        examples.put("validArtifactWithNoConfiguration", validArtifactWithNoConfiguration);
        examples.put("validArtifactWithConfiguration", validArtifactWithConfiguration);
    }

    @Override
    public void addBadExamples(Map<String, CRPluggableArtifact> examples) {
        examples.put("invalidArtifactWithNoId", invalidArtifactWithNoId);
        examples.put("invalidArtifactWithNoStoreId", invalidArtifactWithNoStoreId);
        examples.put("invalidArtifactWithInvalidConfiguration", invalidArtifactWithInvalidConfiguration);
    }

    @Test
    public void shouldDeserializeWhenConfigurationIsNull() {
        String json = "{\n" +
                "           \"id\" : \"id\",\n" +
                "           \"store_id\" : \"s3\",\n" +
                "           \"type\": \"external\"\n" +
                "            }";

        CRPluggableArtifact crPluggableArtifact = gson.fromJson(json, CRPluggableArtifact.class);

        assertThat(crPluggableArtifact.getId(), is("id"));
        assertThat(crPluggableArtifact.getStoreId(), is("s3"));
        assertThat(crPluggableArtifact.getType(), is(CRArtifactType.external));
        assertNull(crPluggableArtifact.getConfiguration());
    }


    @Test
    public void shouldCheckForTypeWhileDeserializing() {
        String json = "{\n" +
                "           \"id\" : \"id\",\n" +
                "           \"store_id\" : \"s3\"\n" +
                "            }";

        CRPluggableArtifact crPluggableArtifact = gson.fromJson(json, CRPluggableArtifact.class);

        assertThat(crPluggableArtifact.getId(), is("id"));
        assertThat(crPluggableArtifact.getStoreId(), is("s3"));
        assertNull(crPluggableArtifact.getType());
        assertNull(crPluggableArtifact.getConfiguration());

        assertFalse(crPluggableArtifact.getErrors().isEmpty());
    }

    @Test
    public void shouldDeserializePluggableArtifacts() {
        String json = "{\n" +
                "              \"id\" : \"id\",\n" +
                "              \"store_id\" : \"s3\",\n" +
                "              \"type\": \"external\",\n" +
                "              \"configuration\": [{\"key\":\"image\", \"value\": \"gocd-agent\"}]" +
                "            }";

        CRPluggableArtifact crPluggableArtifact = gson.fromJson(json, CRPluggableArtifact.class);

        assertThat(crPluggableArtifact.getId(), is("id"));
        assertThat(crPluggableArtifact.getStoreId(), is("s3"));
        assertThat(crPluggableArtifact.getConfiguration(), is(Arrays.asList(new CRConfigurationProperty("image", "gocd-agent"))));
    }
}
