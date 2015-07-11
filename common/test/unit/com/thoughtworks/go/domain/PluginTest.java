/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/
package com.thoughtworks.go.domain;

import com.google.gson.GsonBuilder;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class PluginTest {
    private Plugin plugin;

    @Before
    public void setUp() throws Exception {
        Map<String, String> configuration = new HashMap<String, String>();
        configuration.put("k1", "v1");
        configuration.put("k2", "v2");
        plugin = new Plugin("plugin-id", new GsonBuilder().create().toJson(configuration));
    }

    @Test
    public void shouldGetAllConfigurationKeys() {
        assertEquals(new HashSet<String>(asList("k1", "k2")), plugin.getAllConfigurationKeys());
    }

    @Test
    public void shouldGetValueForConfigurationKey() throws Exception {
        assertThat(plugin.getConfigurationValue("k1"), is("v1"));
    }
}
