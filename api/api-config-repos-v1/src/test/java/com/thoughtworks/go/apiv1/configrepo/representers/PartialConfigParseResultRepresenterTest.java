/*
 * Copyright 2018 ThoughtWorks, Inc.
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

package com.thoughtworks.go.apiv1.configrepo.representers;

import com.thoughtworks.go.config.PartialConfigParseResult;
import com.thoughtworks.go.config.remote.PartialConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PartialConfigParseResultRepresenterTest {
    @Test
    void shouldSerializeSuccess() {
        final PartialConfigParseResult result = new PartialConfigParseResult("1234", new PartialConfig());
        assertEquals("{\"revision\":\"1234\",\"success\":true}", PartialConfigParseResultRepresenter.toJSON(result));
    }

    @Test
    void shouldSerializeFailure() {
        final PartialConfigParseResult result = new PartialConfigParseResult("1234", new RuntimeException("Boom!"));
        assertEquals("{\"revision\":\"1234\",\"success\":false,\"error\":\"Boom!\"}", PartialConfigParseResultRepresenter.toJSON(result));
    }

    @Test
    void shouldSerializeNull() {
        final PartialConfigParseResult result = null;
        assertEquals("{\"revision\":null}", PartialConfigParseResultRepresenter.toJSON(result));
    }
}