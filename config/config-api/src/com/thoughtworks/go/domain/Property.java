/*
 * Copyright 2015 ThoughtWorks, Inc.
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

package com.thoughtworks.go.domain;

import com.thoughtworks.go.util.json.JsonAware;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.thoughtworks.go.util.ObjectUtil.nullSafeEquals;

public class Property implements JsonAware, Serializable {
    private String key;
    private String value;

    public Property() {
    }

    public Property(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    void setValue(String value) {
        this.value = value;
    }


    public String toString() {
        return "Name=" + key + " Value=" + value;
    }

    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (that == null) {
            return false;
        }
        if (this.getClass() != that.getClass()) {
            return false;
        }

        return equals((Property) that);
    }

    private boolean equals(Property that) {
        if (!nullSafeEquals(this.key, that.key)) {
            return false;
        }
        if (!nullSafeEquals(this.value, that.value)) {
            return false;
        }
        return true;
    }

    public int hashCode() {
        int result;
        result = key.hashCode();
        result = 31 * result + value.hashCode();
        return result;
    }

    public Map<String, Object> toJson() {
        Map<String, Object> jsonMap = new LinkedHashMap<>();
        jsonMap.put(key, value);
        return jsonMap;
    }
}
