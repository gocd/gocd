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

package com.thoughtworks.go.util;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;

import static com.thoughtworks.go.util.UriEncodingUtil.*;
import static org.assertj.core.api.Assertions.assertThat;

class UriEncodingUtilTest {
    private static final String KITCHEN_SINK = "_%_?_=_/_(_)_[_]_:_&";

    @Test
    void shouldEncodePartParanoid() {
        assertThat(encodePartParanoid(KITCHEN_SINK)).isEqualTo("_%25_%3F_%3D_%2F_%28_%29_%5B_%5D_%3A_%26");
    }

    @Test
    void shouldEncodeQueryParam() {
        assertThat(encodeQueryParam(KITCHEN_SINK)).isEqualTo("_%25_?_%3D_/_(_)_%5B_%5D_:_%26");
    }

    @Test
    void shouldEncodeQueryParams() {
        assertThat(encodeQueryParams(new LinkedHashMap<>() {{ put(KITCHEN_SINK, KITCHEN_SINK); put("hello", "world"); }}))
            .isEqualTo("_%25_%3F_%3D_%2F_%28_%29_%5B_%5D_%3A_%26=_%25_%3F_%3D_%2F_%28_%29_%5B_%5D_%3A_%26&hello=world");
    }
    
    @Test
    void shouldEncodePathSegments() {
        assertThat(encodePathSegment("_%_/_")).isEqualTo("_%25_%2F_");
    }

    @Nested
    class PathPartial {
        @Test
        void shouldEncodePath() {
            assertThat(encodePathPartial("_%_")).isEqualTo("_%25_");
        }

        @Test
        void shouldEncodeAllPartsInUrlIgnoringSlash() {
            assertThat(encodePathPartial("_%_/_%_")).isEqualTo("_%25_/_%25_");
        }

        @Test
        void shouldKeepPrecedingSlash() {
            assertThat(encodePathPartial("/_%_/_%_")).isEqualTo("/_%25_/_%25_");
        }

        @Test
        void shouldKeepTrailingSlash() {
            assertThat(encodePathPartial("_%_/_%_/")).isEqualTo("_%25_/_%25_/");
        }

        @Test
        void shouldAssumeQueryStuffIsPartOfPath() {
            assertThat(encodePathPartial("_?_=_")).isEqualTo("_%3F_=_");
        }
    }
}