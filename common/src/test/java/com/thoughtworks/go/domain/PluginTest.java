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
package com.thoughtworks.go.domain;

import com.google.gson.GsonBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class PluginTest {
    private Plugin plugin;

    @BeforeEach
    public void setUp() throws Exception {
        plugin = new Plugin("plugin-id", new GsonBuilder().create().toJson(Map.of("k1", "v1", "k2", "v2")));
    }

    @Test
    public void shouldGetAllConfigurationKeys() {
        assertThat(plugin.getAllConfigurationKeys()).containsExactly("k1", "k2");
    }

    @Test
    public void shouldGetValueForConfigurationKey() {
        assertThat(plugin.getConfigurationValue("k1")).isEqualTo("v1");
    }
}
