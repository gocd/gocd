/*
 * Copyright 2020 ThoughtWorks, Inc.
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
package com.thoughtworks.go.plugin.access.configrepo.v2;

import com.thoughtworks.go.plugin.access.configrepo.ConfigRepoMigrator;
import com.thoughtworks.go.plugin.configrepo.codec.GsonCodec;
import com.thoughtworks.go.plugin.domain.configrepo.Capabilities;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

public class JsonMessageHandler2_0Test {
    private final JsonMessageHandler2_0 handler;

    public JsonMessageHandler2_0Test() {
        ConfigRepoMigrator configRepoMigrator = mock(ConfigRepoMigrator.class);
        handler = new JsonMessageHandler2_0(new GsonCodec(), configRepoMigrator);
    }

    @Test
    public void shouldReturnAllFalseCapabilities() {
        assertEquals(new Capabilities(true, true, false, false), handler.getCapabilitiesFromResponse("{\"supports_pipeline_export\":\"true\",\"supports_parse_content\":\"true\"}"));
    }
}
