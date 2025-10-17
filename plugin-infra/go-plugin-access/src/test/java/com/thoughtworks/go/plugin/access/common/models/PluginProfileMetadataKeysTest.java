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
package com.thoughtworks.go.plugin.access.common.models;

import com.thoughtworks.go.plugin.domain.common.Metadata;
import com.thoughtworks.go.plugin.domain.common.PluginConfiguration;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class PluginProfileMetadataKeysTest {

    @Test
    public void shouldUnmarshallProfileMetadata() {
        PluginProfileMetadataKeys metadata = PluginProfileMetadataKeys.fromJSON("""
                [{
                  "key": "foo",
                  "metadata": {
                    "secure": true,
                    "required": false
                  }
                }, {
                  "key": "bar"
                }]""");
        assertThat(metadata.size()).isEqualTo(2);
        PluginProfileMetadataKey foo = metadata.get("foo");
        assertThat(foo.getMetadata().isRequired()).isFalse();
        assertThat(foo.getMetadata().isSecure()).isTrue();

        PluginProfileMetadataKey bar = metadata.get("bar");
        assertThat(bar.getMetadata().isRequired()).isFalse();
        assertThat(bar.getMetadata().isSecure()).isFalse();
    }

    @Test
    public void shouldGetPluginConfigurations() {
        PluginProfileMetadataKeys metadata = PluginProfileMetadataKeys.fromJSON("""
                [{
                  "key": "username",
                  "metadata": {
                    "secure": true,
                    "required": false
                  }
                }, {
                  "key": "password",
                  "metadata": {
                    "secure": true,
                    "required": true
                  }
                }]""");

        List<PluginConfiguration> pluginConfigurations = metadata.toPluginConfigurations();

        assertThat(pluginConfigurations).contains(
                new PluginConfiguration("username", new Metadata(false, true)),
                new PluginConfiguration("password", new Metadata(true, true)));
    }

    @Test
    public void shouldGetPluginConfigurationsWithMetadataDefaultedToFalseInAbsenceOfPluginMetadata() {
        PluginProfileMetadataKeys metadata = PluginProfileMetadataKeys.fromJSON("""
                [{
                  "key": "username"
                }, {
                  "key": "password",
                  "metadata": {
                    "secure": true,
                    "required": true
                  }
                }]""");

        List<PluginConfiguration> pluginConfigurations = metadata.toPluginConfigurations();

        assertThat(pluginConfigurations).contains(
                new PluginConfiguration("username", new Metadata(false, false)),
                new PluginConfiguration("password", new Metadata(true, true)));
    }
}
