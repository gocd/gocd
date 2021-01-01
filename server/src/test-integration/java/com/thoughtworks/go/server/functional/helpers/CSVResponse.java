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
package com.thoughtworks.go.server.functional.helpers;

import org.apache.commons.lang3.StringUtils;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CSVResponse {
    private String tableContent;
    private int csvReponseStatus;
    private String contentType;
    private List<String> allRows = new ArrayList<>();
    private List<List> allColumns = new ArrayList<>();

    public CSVResponse(MockHttpServletResponse response) throws UnsupportedEncodingException {
        this(response.getContentAsString(), response.getStatus(), response.getContentType());
    }

    public CSVResponse(String tableContent) {
        this(tableContent, -1, "you did not specify the content type");
    }

    public CSVResponse(String tableContent, int csvReponseStatus, String contentType) {
        this.tableContent = tableContent;
        this.csvReponseStatus = csvReponseStatus;
        this.contentType = contentType;
        initTable();
    }

    private void initTable() {
        String[] rows = tableContent.split("\n");
        for (int i = 0; i < rows.length; i++) {
            this.allRows.add(rows[i]);
            String[] columns = rows[i].split(",");
            for (int j = 0; j < columns.length; j++) {
                if (allColumns.size() <= j) {
                    allColumns.add(new ArrayList());
                }
                List oneColumn = allColumns.get(j);
                oneColumn.add(columns[j]);
            }
        }
    }

    public boolean isCSV() {
        return "text/csv".equals(contentType);
    }

    public boolean statusEquals(int status) {
        return this.csvReponseStatus == status;
    }

    public boolean containsColumn(String... columns) {
        List<String> targetColumn = null;
        for (List column : this.allColumns) {
            targetColumn = Arrays.asList(columns);
            if (StringUtils.contains(column.toString(), targetColumn.toString())) {
                return true;
            }
        }
        return false;
    }

    public boolean containsRow(String... rows) {
        List<String> targetRows = null;
        for (String row : this.allRows) {
            targetRows = Arrays.asList(rows);
            List<String> storedRows = Arrays.asList(row.split(","));
            if (StringUtils.contains(storedRows.toString(), targetRows.toString())) {
                return true;
            }
        }
        throw new RuntimeException("Failed to find " + targetRows + " in all allColumns " + this.allRows);

    }

}
