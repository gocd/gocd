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
package com.thoughtworks.go.server.dao.handlers;

import com.thoughtworks.go.domain.JobState;
import org.apache.ibatis.type.EnumTypeHandler;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class BuildStateTypeHandlerCallback extends EnumTypeHandler<JobState> {
    public BuildStateTypeHandlerCallback(Class<JobState> type) {
        super(type);
    }

    @Override
    public JobState getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return valueOf(super.getNullableResult(rs, columnIndex));
    }

    @Override
    public JobState getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return valueOf(super.getNullableResult(rs, columnName));
    }

    @Override
    public JobState getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return valueOf(super.getNullableResult(cs, columnIndex));
    }

    private JobState valueOf(JobState nullableResult) {
        return nullableResult == null ? JobState.Unknown : nullableResult;
    }

}
