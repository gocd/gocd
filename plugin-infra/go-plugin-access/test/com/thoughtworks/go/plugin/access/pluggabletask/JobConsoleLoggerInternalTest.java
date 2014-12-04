package com.thoughtworks.go.plugin.access.pluggabletask;

import com.thoughtworks.go.plugin.api.task.JobConsoleLogger;
import com.thoughtworks.go.plugin.api.task.TaskExecutionContext;
import com.thoughtworks.go.util.ReflectionUtil;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

public class JobConsoleLoggerInternalTest {
    @Test
    public void shouldSetAndUnsetContext() {
        final TaskExecutionContext context = mock(TaskExecutionContext.class);
        JobConsoleLoggerInternal.setContext(context);
        assertThat((TaskExecutionContext) ReflectionUtil.getStaticField(JobConsoleLogger.class, "context"), is(context));

        JobConsoleLoggerInternal.unsetContext();
        assertThat(ReflectionUtil.getStaticField(JobConsoleLogger.class, "context"), is(nullValue()));
    }
}
