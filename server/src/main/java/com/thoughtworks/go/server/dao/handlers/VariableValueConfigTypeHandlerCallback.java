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

import com.thoughtworks.go.config.VariableValueConfig;
import org.apache.ibatis.type.JdbcType;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * @understands converting a {@link com.thoughtworks.go.config.VariableValueConfig } to a string and back
 */
public class VariableValueConfigTypeHandlerCallback extends StringColumnBasedTypeHandler<VariableValueConfig> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, VariableValueConfig parameter, JdbcType jdbcType) throws SQLException {
        ps.setString(i, parameter.getValue());
    }

    @Override
    protected VariableValueConfig valueOf(String s) {
        return new VariableValueConfig(s);
    }
}
