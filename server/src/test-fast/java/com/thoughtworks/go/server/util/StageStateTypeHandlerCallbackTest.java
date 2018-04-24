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

package com.thoughtworks.go.server.util;

import com.ibatis.sqlmap.client.extensions.ParameterSetter;
import com.ibatis.sqlmap.client.extensions.ResultGetter;
import com.thoughtworks.go.domain.StageState;
import com.thoughtworks.go.server.dao.handlers.StageStateTypeHandlerCallback;
import org.junit.Test;

import java.sql.SQLException;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class StageStateTypeHandlerCallbackTest {
    private StageStateTypeHandlerCallback callback = new StageStateTypeHandlerCallback();

    @Test
    public void shouldReturnScheduledWhenGivenStringScheduled() throws SQLException {
        assertMaps("Passed", StageState.Passed);
        assertMaps("Failed", StageState.Failed);
        assertMaps("Cancelled", StageState.Cancelled);
        assertMaps("Unknown", StageState.Unknown);
        assertMaps("Building", StageState.Building);
        assertMaps("Failing", StageState.Failing);
    }

    private void assertMaps(final String str, StageState value) throws SQLException {
        final ResultGetter resultGetter;
        resultGetter = mock(ResultGetter.class);
        when(resultGetter.getString()).thenReturn(str);
        StageState result = (StageState) callback.getResult(resultGetter);
        assertThat(result, is(value));

        final ParameterSetter parameterSetter = mock(ParameterSetter.class);
        callback.setParameter(parameterSetter, value);
        verify(parameterSetter).setString(str);
    }

}
