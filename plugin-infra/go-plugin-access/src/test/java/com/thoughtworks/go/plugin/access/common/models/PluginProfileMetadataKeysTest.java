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
package com.thoughtworks.go.plugin.access.common.models;

import com.thoughtworks.go.plugin.domain.common.Metadata;
import com.thoughtworks.go.plugin.domain.common.PluginConfiguration;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;

public class PluginProfileMetadataKeysTest {

    @Test
    public void shouldUnJSONizeProfileMetadata() throws Exception {
        PluginProfileMetadataKeys metadata = PluginProfileMetadataKeys.fromJSON("[{\n" +
                "  \"key\": \"foo\",\n" +
                "  \"metadata\": {\n" +
                "    \"secure\": true,\n" +
                "    \"required\": false\n" +
                "  }\n" +
                "}, {\n" +
                "  \"key\": \"bar\"\n" +
                "}]");
        assertThat(metadata.size(), is(2));
        PluginProfileMetadataKey foo = metadata.get("foo");
        assertThat(foo.getMetadata().isRequired(), is(false));
        assertThat(foo.getMetadata().isSecure(), is(true));

        PluginProfileMetadataKey bar = metadata.get("bar");
        assertThat(bar.getMetadata().isRequired(), is(false));
        assertThat(bar.getMetadata().isSecure(), is(false));
    }

    @Test
    public void shouldGetPluginConfigurations() throws Exception {
        PluginProfileMetadataKeys metadata = PluginProfileMetadataKeys.fromJSON("[{\n" +
                "  \"key\": \"username\",\n" +
                "  \"metadata\": {\n" +
                "    \"secure\": true,\n" +
                "    \"required\": false\n" +
                "  }\n" +
                "}, {\n" +
                "  \"key\": \"password\",\n" +
                "  \"metadata\": {\n" +
                "    \"secure\": true,\n" +
                "    \"required\": true\n" +
                "  }\n" +
                "}]");

        List<PluginConfiguration> pluginConfigurations = metadata.toPluginConfigurations();

        assertThat(pluginConfigurations, containsInAnyOrder(
                new PluginConfiguration("username", new Metadata(false, true)),
                new PluginConfiguration("password", new Metadata(true, true))));
    }

    @Test
    public void shouldGetPluginConfigurationsWithMetadataDefaultedToFalseInAbsenceOfPluginMetadata() throws Exception {
        PluginProfileMetadataKeys metadata = PluginProfileMetadataKeys.fromJSON("[{\n" +
                "  \"key\": \"username\"\n" +
                "}, {\n" +
                "  \"key\": \"password\",\n" +
                "  \"metadata\": {\n" +
                "    \"secure\": true,\n" +
                "    \"required\": true\n" +
                "  }\n" +
                "}]");

        List<PluginConfiguration> pluginConfigurations = metadata.toPluginConfigurations();

        assertThat(pluginConfigurations, containsInAnyOrder(
                new PluginConfiguration("username", new Metadata(false, false)),
                new PluginConfiguration("password", new Metadata(true, true))));
    }
}
