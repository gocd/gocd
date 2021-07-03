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
package com.thoughtworks.go.plugin.configrepo.contract.tasks;

import com.google.gson.JsonObject;
import com.thoughtworks.go.plugin.configrepo.contract.AbstractCRTest;
import com.thoughtworks.go.plugin.configrepo.contract.CRConfigurationProperty;
import com.thoughtworks.go.plugin.configrepo.contract.CRPluginConfiguration;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class CRPluggableTaskTest extends AbstractCRTest<CRPluggableTask> {

    private final CRPluggableTask curl;
    private final CRPluggableTask example;
    private final CRPluggableTask invalidNoPlugin;
    private final CRPluggableTask invalidDuplicatedKeys;

    public CRPluggableTaskTest() {
        CRPluginConfiguration crPluginConfiguration = new CRPluginConfiguration("curl.task.plugin", "1");
        List<CRConfigurationProperty> properties = Arrays.asList(
                new CRConfigurationProperty("Url", "http://www.google.com"),
                new CRConfigurationProperty("SecureConnection", "no"),
                new CRConfigurationProperty("RequestType", "no")
        );

        curl = new CRPluggableTask(null, null, crPluginConfiguration, properties);
        CRPluginConfiguration examplePluginConfiguration = new CRPluginConfiguration("example.task.plugin", "1");
        example = new CRPluggableTask(null, null, examplePluginConfiguration, null);

        List<CRConfigurationProperty> invalidProperties = Arrays.asList(
                new CRConfigurationProperty("Url", "http://www.google.com"),
                new CRConfigurationProperty("Url", "http://www.gg.com")
        );
        invalidNoPlugin = new CRPluggableTask();
        invalidDuplicatedKeys = new CRPluggableTask(null, null, crPluginConfiguration, invalidProperties);
    }

    @Override
    public void addGoodExamples(Map<String, CRPluggableTask> examples) {
        examples.put("curl", curl);
        examples.put("example", example);
    }

    @Override
    public void addBadExamples(Map<String, CRPluggableTask> examples) {
        examples.put("invalidNoPlugin", invalidNoPlugin);
        examples.put("invalidDuplicatedKeys", invalidDuplicatedKeys);
    }


    @Test
    public void shouldAppendTypeFieldWhenSerializingTask() {
        CRTask value = curl;
        JsonObject jsonObject = (JsonObject) gson.toJsonTree(value);
        assertThat(jsonObject.get("type").getAsString(), is(CRPluggableTask.TYPE_NAME));
    }

    @Test
    public void shouldHandlePolymorphismWhenDeserializingTask() {
        CRTask value = curl;
        String json = gson.toJson(value);

        CRPluggableTask deserializedValue = (CRPluggableTask) gson.fromJson(json, CRTask.class);
        assertThat("Deserialized value should equal to value before serialization",
                deserializedValue, is(value));
    }
}
