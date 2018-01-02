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

package com.thoughtworks.go.server.dao.handlers;

import java.sql.SQLException;

import com.ibatis.sqlmap.client.extensions.ParameterSetter;
import com.ibatis.sqlmap.client.extensions.ResultGetter;
import com.ibatis.sqlmap.client.extensions.TypeHandlerCallback;
import com.thoughtworks.go.domain.StageState;

public class StageStateTypeHandlerCallback implements TypeHandlerCallback {
    public void setParameter(ParameterSetter setter, Object parameter) throws SQLException {
        setter.setString(parameter.toString());
    }

    public Object getResult(ResultGetter getter) throws SQLException {
        return valueOf(getter.getString());
    }

    public Object valueOf(String s) {
        return StageState.valueOf(s);
    }
}