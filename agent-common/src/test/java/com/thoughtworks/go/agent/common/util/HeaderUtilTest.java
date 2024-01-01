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
package com.thoughtworks.go.agent.common.util;

import org.apache.http.message.BasicHeader;
import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class HeaderUtilTest {
    @Test
    public void shouldGetExtraPropertiesFromHeader() {
        assertExtraPropertiesWithoutBase64(null, new HashMap<>());
        assertExtraPropertiesWithoutBase64("", new HashMap<>());
        assertExtraProperties("", new HashMap<>());

        assertExtraProperties("Key1=Value1 key2=value2",
            Map.of("Key1", "Value1", "key2", "value2"));

        assertExtraProperties("  Key1=Value1    key2=value2  ",
            Map.of("Key1", "Value1", "key2", "value2"));

        assertExtraProperties("Key1=Value1 key2=value2 key2=value3",
            Map.of("Key1", "Value1", "key2", "value2"));

        assertExtraProperties("Key1%20WithSpace=Value1%20WithSpace key2=value2",
            Map.of("Key1 WithSpace", "Value1 WithSpace", "key2", "value2"));
    }

    @Test
    public void shouldNotFailIfExtraPropertiesAreNotFormattedProperly() {
        assertExtraProperties("abc", new HashMap<>());
    }

    private void assertExtraProperties(String actualHeaderValueBeforeBase64, Map<String, String> expectedProperties) {
        String headerValueInBase64 = Base64.getEncoder().encodeToString(actualHeaderValueBeforeBase64.getBytes(UTF_8));
        assertExtraPropertiesWithoutBase64(headerValueInBase64, expectedProperties);
    }

    private void assertExtraPropertiesWithoutBase64(String actualHeaderValue, Map<String, String> expectedProperties) {
        Map<String, String> actualResult = HeaderUtil.parseExtraProperties(new BasicHeader("some-key", actualHeaderValue));
        assertThat(actualResult, is(expectedProperties));
    }

}