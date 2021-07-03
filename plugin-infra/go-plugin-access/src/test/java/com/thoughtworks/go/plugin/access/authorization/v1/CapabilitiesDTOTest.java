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
package com.thoughtworks.go.plugin.access.authorization.v1;

import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CapabilitiesDTOTest {

    @Test
    public void shouldDeserializeFromJSON() throws Exception {
        String json = "" +
                "{\n" +
                "  \"supported_auth_type\": \"web\",\n" +
                "  \"can_search\": true,\n" +
                "  \"can_authorize\": true\n" +
                "}";

        CapabilitiesDTO capabilities = CapabilitiesDTO.fromJSON(json);

        assertThat(capabilities.getSupportedAuthType(), is(SupportedAuthTypeDTO.Web));
        assertTrue(capabilities.canSearch());
        assertTrue(capabilities.canAuthorize());
    }
}