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
package com.thoughtworks.go.server.util;

import com.thoughtworks.go.domain.JobState;
import com.thoughtworks.go.server.dao.handlers.BuildStateTypeHandlerCallback;
import org.junit.Test;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class BuildStateTypeHandlerCallbackTest {

    @Test
    public void shouldReturnStateWithStringColumnName() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("foo")).thenReturn(JobState.Scheduled.toString());
        BuildStateTypeHandlerCallback callback = new BuildStateTypeHandlerCallback(JobState.class);
        JobState result = callback.getResult(rs, "foo");
        assertThat(result, is(JobState.Scheduled));
    }

    @Test
    public void shouldReturnStateWithIntegerColumnName() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString(42)).thenReturn(JobState.Scheduled.toString());
        BuildStateTypeHandlerCallback callback = new BuildStateTypeHandlerCallback(JobState.class);
        JobState result = callback.getResult(rs, 42);
        assertThat(result, is(JobState.Scheduled));
    }

    @Test
    public void shouldReturnStateWithIntegerColumnNameAndCallableStatement() throws SQLException {
        CallableStatement rs = mock(CallableStatement.class);
        when(rs.getString(42)).thenReturn(JobState.Scheduled.toString());
        BuildStateTypeHandlerCallback callback = new BuildStateTypeHandlerCallback(JobState.class);
        JobState result = callback.getResult(rs, 42);
        assertThat(result, is(JobState.Scheduled));
    }

    @Test
    public void shouldSerialize() throws SQLException {
        PreparedStatement ps = mock(PreparedStatement.class);
        BuildStateTypeHandlerCallback callback = new BuildStateTypeHandlerCallback(JobState.class);
        callback.setParameter(ps, 42, JobState.Completed, null);
        verify(ps).setString(42, "Completed");
        verifyNoMoreInteractions(ps);
    }
}
