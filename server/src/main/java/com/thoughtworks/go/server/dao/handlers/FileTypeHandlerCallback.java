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

import org.apache.commons.io.FilenameUtils;
import org.apache.ibatis.type.JdbcType;

import java.io.File;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class FileTypeHandlerCallback extends StringColumnBasedTypeHandler<File> {
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, File parameter, JdbcType jdbcType) throws SQLException {
        ps.setString(i, FilenameUtils.separatorsToUnix(parameter.getPath()));
    }

    @Override
    protected File valueOf(String text) {
        if (text == null) {
            return null;
        }
        return new File(FilenameUtils.separatorsToUnix(text));
    }
}
