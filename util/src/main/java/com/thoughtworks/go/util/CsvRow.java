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
package com.thoughtworks.go.util;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

public class CsvRow {
    private LinkedHashMap<String, String> rowData = new LinkedHashMap<>();

    public CsvRow put(String name, String value) {
        rowData.put(name, value);
        return this;
    }

    public Set<String> fields() {
        return rowData.keySet();
    }

    public String get(String key) {
        return (String) ObjectUtils.defaultIfNull(rowData.get(key), "");
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        Set<String> fields = fields();
        sb.append(StringUtils.join(fields, ",")).append("\n");
        sb.append(toString(fields)).append("\n");
        return sb.toString();
    }

    public String toString(Set<String> keys) {
        ArrayList<String> stringData = new ArrayList<>();
        for (String field : keys) {
            stringData.add(this.get(field));
        }
        return StringUtils.join(stringData, ",");
    }

    public boolean contains(Map<String, String> row) {
        for (String column : row.keySet()) {
            if (!row.get(column).equals(rowData.get(column))) {
                return false;
            }
        }
        return true;
    }
}
