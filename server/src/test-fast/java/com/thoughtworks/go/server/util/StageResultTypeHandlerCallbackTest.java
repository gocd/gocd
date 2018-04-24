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
import com.thoughtworks.go.domain.StageResult;
import com.thoughtworks.go.server.dao.handlers.StageResultTypeHandlerCallback;
import org.junit.Test;

import java.sql.SQLException;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class StageResultTypeHandlerCallbackTest {
    private StageResultTypeHandlerCallback callback = new StageResultTypeHandlerCallback();

    @Test
    public void shouldReturnScheduledWhenGivenStringScheduled() throws SQLException {
        assertMaps("Passed", StageResult.Passed);
        assertMaps("Failed", StageResult.Failed);
        assertMaps("Cancelled", StageResult.Cancelled);
        assertMaps("Unknown", StageResult.Unknown);
    }

    private void assertMaps(final String str, StageResult value) throws SQLException {
        final ResultGetter resultGetter;
        resultGetter = mock(ResultGetter.class);
        when(resultGetter.getString()).thenReturn(str);
        StageResult result = (StageResult) callback.getResult(resultGetter);
        assertThat(result, is(value));

        final ParameterSetter parameterSetter = mock(ParameterSetter.class);
        callback.setParameter(parameterSetter, value);
        verify(parameterSetter).setString(str);
    }

}
