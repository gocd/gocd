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
package com.thoughtworks.go.server.database.migration;

import com.google.gson.Gson;
import org.apache.commons.lang3.StringUtils;

import java.sql.*;

class M001 {
    private static final String ID = "id";
    private static final String SELECTIONS = "selections";
    private static final String TO_BE_EXCLUDED = "isblacklist";
    private static final int SCHEMA = 1;
    private static final Gson GSON = new Gson();

    static Migration convertPipelineSelectionsToFilters() {
        return (cxn) -> {
            if (!required(cxn)) return;

            try (Statement s = cxn.createStatement()) {
                final ResultSet rs = s.executeQuery("SELECT id, selections, isblacklist FROM pipelineselections WHERE version = 0");
                while (rs.next()) {
                    perform(cxn, rs.getLong(ID), rs.getString(SELECTIONS), rs.getBoolean(TO_BE_EXCLUDED));
                }
            }
        };
    }

    static void perform(Connection cxn, long id, String selections, boolean isBlacklist) throws SQLException {
        try (PreparedStatement ps = cxn.prepareStatement("UPDATE pipelineselections SET selections = NULL, version = ?, filters = ? WHERE id = ?")) {
            ps.setInt(1, SCHEMA);
            ps.setString(2, asJson(selections, isBlacklist));
            ps.setLong(3, id);
            ps.executeUpdate();
        }
    }

    static String asJson(String selections, boolean isBlacklist) {
        return GSON.toJson(new FilterSet(selections, isBlacklist));
    }

    static boolean required(Connection cxn) throws SQLException {
        try (Statement s = cxn.createStatement()) {
            try (ResultSet rs = s.executeQuery("SELECT COUNT(*) as remaining FROM PIPELINESELECTIONS WHERE version = 0")) {
                rs.next();
                return rs.getInt("remaining") > 0;
            }
        }
    }

    private static class FilterSet {
        Filter[] filters;

        public FilterSet(String selections, boolean isToBeExcluded) {
            this.filters = new Filter[]{new Filter(selections, isToBeExcluded)};
        }
    }

    private static class Filter {
        String name;
        String type;
        String[] pipelines;

        public Filter(String selections, boolean isToBeExcluded) {
            name = "Default";
            pipelines = StringUtils.isBlank(selections) ? new String[]{} : StringUtils.split(selections, ",");
            type = isToBeExcluded ? "blacklist" : "whitelist";
        }
    }
}
