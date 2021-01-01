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

package com.thoughtworks.go.server.util;

import static com.thoughtworks.go.util.CachedDigestUtils.sha512_256Hex;
import static org.apache.commons.lang3.StringUtils.join;

public interface DigestMixin {
    String SEP_CHAR = "/";

    static String digestHex(String datum) {
        return sha512_256Hex(datum);
    }

    static String digestMany(String... data) {
        return digestHex(join(data, SEP_CHAR));
    }

    default String digest(String datum) {
        return digestHex(datum);
    }

    default String digest(String... data) {
        return digestMany(data);
    }
}
