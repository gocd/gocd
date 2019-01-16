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
package com.thoughtworks.go.server.database.migration;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.sql.*;
import java.util.HashMap;
import java.util.List;

public class M002 {
    private static final Type FILTERS_TYPE = new TypeToken<HashMap<String, List<Filter>>>() {}.getType();
    private static final String ID = "id";
    private static final String FILTERS = "filters";
    private static final int SCHEMA = 2;
    private static final Gson GSON = new Gson();

    static Migration ensureFilterStateIsNotNull() {
        return (cxn) -> {
            if (!required(cxn)) return;

            try (Statement s = cxn.createStatement()) {
                try (ResultSet rs = s.executeQuery("SELECT id, filters FROM PIPELINESELECTIONS WHERE version = 1")) {
                    while (rs.next()) {
                        perform(cxn, rs.getLong(ID), rs.getString(FILTERS));
                    }
                }
            }
        };
    }

    static void perform(Connection cxn, long id, String filters) throws SQLException {
        try (PreparedStatement ps = cxn.prepareStatement("UPDATE PIPELINESELECTIONS SET version = ?, filters = ? WHERE id = ?")) {
            ps.setInt(1, SCHEMA);
            ps.setString(2, addStateIfNull(filters));
            ps.setLong(3, id);
            ps.executeUpdate();
        }
    }

    static String addStateIfNull(String rawFilters) {
        HashMap<String, List<Filter>> filters = GSON.fromJson(rawFilters, FILTERS_TYPE);

        for (Filter filter : filters.get("filters")) {
            filter.setStateIfNull();
        }

        return GSON.toJson(filters);
    }

    static boolean required(Connection cxn) throws SQLException {
        try (Statement s = cxn.createStatement()) {
            final ResultSet rs = s.executeQuery("SELECT COUNT(*) as remaining FROM PIPELINESELECTIONS WHERE version = 1");
            rs.next();

            return rs.getInt("remaining") > 0;
        }
    }

    private static class Filter {
        String name;
        String type;
        String[] pipelines;
        String[] state;

        public void setStateIfNull() {
            if (state == null) {
                state = new String[]{};
            }
        }
    }
}
