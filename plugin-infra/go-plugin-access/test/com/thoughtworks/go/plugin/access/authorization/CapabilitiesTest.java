/*
 * Copyright 2016 ThoughtWorks, Inc.
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

package com.thoughtworks.go.plugin.access.authorization;

import com.thoughtworks.go.plugin.access.authorization.models.Capabilities;
import com.thoughtworks.go.plugin.access.authorization.models.SupportedAuthType;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class CapabilitiesTest {

    @Test
    public void shouldDeserializeFromJSON() throws Exception {
        String json = "" +
                "{\n" +
                "  \"supported_auth_type\": \"web\",\n" +
                "  \"can_search\": true,\n" +
                "  \"can_verify_connection\": true,\n" +
                "  \"can_authorize\": true\n" +
                "}";

        Capabilities capabilities = Capabilities.fromJSON(json);

        assertThat(capabilities.getSupportedAuthType(), is(SupportedAuthType.Web));
        assertThat(capabilities.canSearch(), is(true));
        assertThat(capabilities.canAuthorize(), is(true));
        assertThat(capabilities.canVerifyConnection(), is(true));
    }

}