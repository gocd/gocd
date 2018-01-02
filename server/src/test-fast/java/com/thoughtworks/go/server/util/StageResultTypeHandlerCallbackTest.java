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

import java.sql.SQLException;

import com.ibatis.sqlmap.client.extensions.ParameterSetter;
import com.ibatis.sqlmap.client.extensions.ResultGetter;
import com.thoughtworks.go.domain.StageResult;
import com.thoughtworks.go.server.dao.handlers.StageResultTypeHandlerCallback;
import static org.hamcrest.core.Is.is;
import org.jmock.Expectations;
import static org.jmock.Expectations.equal;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import static org.junit.Assert.assertThat;
import org.junit.Test;

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
        Mockery context = new Mockery() {
            {
                setImposteriser(ClassImposteriser.INSTANCE);
            }
        };
        resultGetter = context.mock(ResultGetter.class);
        context.checking(new Expectations() {
            {
                one(resultGetter).getString();
                will(returnValue(str));
            }
        });
        StageResult result = (StageResult) callback.getResult(resultGetter);
        assertThat(result, is(equal(value)));

        final ParameterSetter parameterSetter = context.mock(ParameterSetter.class);
        context.checking(new Expectations() {
            {
                one(parameterSetter).setString(str);
            }
        });
        callback.setParameter(parameterSetter, value);
    }

}
