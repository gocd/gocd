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
package com.thoughtworks.go.api;


import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum ApiVersion {
    v1(),
    v2(),
    v3(),
    v4(),
    v5(),
    v6(),
    v7(),
    v8(),
    v9(),
    v10(),
    v11();

    public static final String LATEST_VERSION_MIMETYPE = "application/vnd.go.cd+json";

    private static final Set<String> VALID_HEADERS = Set.copyOf(
        Stream.concat(
                Stream.of(LATEST_VERSION_MIMETYPE),
                Arrays.stream(ApiVersion.values()).map(ApiVersion::mimeType))
            .collect(Collectors.toSet())
    );

    private static final Map<String, ApiVersion> HEADER_TO_VERSION_MAP = Map.copyOf(
        Arrays.stream(ApiVersion.values())
            .collect(Collectors.toMap(v -> v.mimeType, Function.identity()))
    );

    private final String mimeType;

    ApiVersion() {
        this.mimeType = "application/vnd.go.cd." + this.name() + "+json";
    }

    public String mimeType() {
        return this.mimeType;
    }

    public static ApiVersion parse(String mimeType) {
        return HEADER_TO_VERSION_MAP.get(mimeType);
    }


    public static boolean isValid(String mimeType) {
        return VALID_HEADERS.contains(mimeType);
    }
}
