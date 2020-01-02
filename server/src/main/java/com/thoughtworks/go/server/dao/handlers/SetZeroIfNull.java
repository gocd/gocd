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
package com.thoughtworks.go.server.dao.handlers;

import org.apache.ibatis.type.LongTypeHandler;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SetZeroIfNull extends LongTypeHandler {

    @Override
    public Long getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return valueOf(super.getNullableResult(cs, columnIndex));
    }

    @Override
    public Long getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return valueOf(super.getNullableResult(rs, columnName));
    }

    @Override
    public Long getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return valueOf(super.getNullableResult(rs, columnIndex));
    }

    private Long valueOf(Long l) {
        if (l == null) {
            return 0L;
        } else {
            return l;
        }
    }
}
