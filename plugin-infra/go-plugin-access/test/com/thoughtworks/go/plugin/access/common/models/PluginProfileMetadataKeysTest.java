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

package com.thoughtworks.go.plugin.access.common.models;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
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

}