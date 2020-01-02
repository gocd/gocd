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
package com.thoughtworks.go.plugin.domain.common;

import java.util.LinkedHashMap;
import java.util.Map;

public class Metadata {
    private final boolean required;
    private final boolean secure;

    public Metadata(boolean required, boolean secure) {
        this.required = required;
        this.secure = secure;
    }

    public boolean isRequired() {
        return required;
    }

    public boolean isSecure() {
        return secure;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();

        map.put("secure", isSecure());
        map.put("required", isRequired());

        return map;
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Metadata metadata = (Metadata) o;

        if (required != metadata.required) return false;
        return secure == metadata.secure;

    }

    @Override
    public int hashCode() {
        int result = (required ? 1 : 0);
        result = 31 * result + (secure ? 1 : 0);
        return result;
    }
}
