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
import com.thoughtworks.go.domain.JobResult;
import com.thoughtworks.go.server.dao.handlers.BuildResultTypeHandlerCallback;
import static org.hamcrest.core.Is.is;

import org.jmock.Expectations;
import static org.jmock.Expectations.equal;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import static org.junit.Assert.assertThat;
import org.junit.Before;
import org.junit.Test;

public class BuildResultTypeHandlerCallbackTest {
    private Mockery context = new Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private ResultGetter resultGetter;
    private BuildResultTypeHandlerCallback callback = new BuildResultTypeHandlerCallback();

    @Before
    public void setUp() {
        resultGetter = context.mock(ResultGetter.class);
    }

    @Test
    public void shouldReturnScheduledWhenGivenStringScheduled() throws SQLException {
        context.checking(new Expectations() {
            {
                one(resultGetter).getString();
                will(returnValue(JobResult.Passed.toString()));
            }
        });
        JobResult result = (JobResult) callback.getResult(resultGetter);
        assertThat(result, is(equal(JobResult.Passed)));
    }

    @Test
    public void shouldReturnScheduledStringWhenGivenScheduled() throws SQLException {
        final ParameterSetter parameterSetter = context.mock(ParameterSetter.class);
        context.checking(new Expectations() {
            {
                one(parameterSetter).setString(JobResult.Failed.toString());
            }
        });
        callback.setParameter(parameterSetter, JobResult.Failed);
    }
}
