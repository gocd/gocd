/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.util;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.thoughtworks.go.util.ListUtil.join;

public class Csv {
    private final List<CsvRow> data = new ArrayList<>();

    public static Csv fromString(String csvContent) {
        Csv csv = new Csv();
        String[] lines = csvContent.split("\n");
        if (lines.length > 1) {
            String header = lines[0];
            String[] columns = header.split(",");

            for (int i = 1; i < lines.length; i++) {
                String line = lines[i];
                CsvRow row = csv.newRow();
                String[] strings = line.split(",");
                for (int j = 0; j < strings.length; j++) {
                    String string = strings[j];
                    row.put(columns[j], string);
                }
            }
        }
        return csv;
    }

    public CsvRow newRow() {
        CsvRow newRow = new CsvRow();
        data.add(newRow);
        return newRow;
    }

    public String toString() {
        Set<String> allFields = fields();
        StringBuilder sb = new StringBuilder();
        sb.append(join(allFields, ",")).append("\n");
        for (CsvRow row : data) {
            sb.append(row.toString(allFields)).append("\n");
        }
        return sb.toString();
    }

    private Set<String> fields() {
        Set<String> fields = new LinkedHashSet<>();
        for (CsvRow row : data) {
            fields.addAll(row.fields());
        }
        return fields;
    }

    public int rowCount() {
        return data.size();
    }

    /**
     * Test if this Csv contains specified row.
     * Note: Provided row may only contain part of the columns.
     * @param row Each row is represented as a map, with column name as key, and column value as value
     * @return
     */
    public boolean containsRow(Map<String, String> row) {
        for (CsvRow csvRow : data) {
            if (csvRow.contains(row)) {
                return true;
            }
        }
        return false;
    }
}
