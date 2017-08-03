/*
 * Copyright 2017 ThoughtWorks, Inc.
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

package com.thoughtworks.go.server.service.builders;

import com.thoughtworks.go.domain.KillAllChildProcessTask;
import com.thoughtworks.go.domain.builder.Builder;
import com.thoughtworks.go.util.ProcessWrapper;
import com.thoughtworks.go.util.command.CommandLine;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;
import com.thoughtworks.go.util.command.ProcessOutputStreamConsumer;
import com.thoughtworks.go.work.DefaultGoPublisher;
import com.thoughtworks.studios.shine.io.StringOutputStream;
import org.junit.Before;
import org.junit.Test;

import java.io.ObjectOutputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

public class KillAllChildProcessTaskBuilderTest {
    private BuilderFactory builderFactory;

    @Before
    public void setUp() throws Exception {
        builderFactory = mock(BuilderFactory.class);
    }

    @Test(timeout = 11*60*1000)//11 minutes
    public void shouldKillAllChildProcessOnbuild() throws Exception {
        ProcessWrapper processWrapper = CommandLine.createCommandLine("sleep").withArg(String.valueOf(10 * 60)).execute(ProcessOutputStreamConsumer.inMemoryConsumer(), new EnvironmentVariableContext(),
                null);//60 mins

        assertThat(processWrapper.isRunning(), is(true));

        DefaultGoPublisher publisher = mock(DefaultGoPublisher.class);
        EnvironmentVariableContext environmentVariableContext = mock(EnvironmentVariableContext.class);


        long before = getSystemTime();
        Builder builder = new KillAllChildProcessTaskBuilder().createBuilder(builderFactory, new KillAllChildProcessTask(), null, null);
        builder.build(publisher, environmentVariableContext, null);

        assertThat(processWrapper.waitForExit(), is(greaterThan(0)));
        assertThat(getSystemTime() - before, is(lessThan(10*60*1000*1000*1000L)));//min = 10; sec = 60*min; mills = 1000*sec; micro = 1000*mills; nano = 1000*micro;
    }

    @Test
    public void builderReturnedByThisTaskBuilderShouldBeSerializable() throws Exception {
        KillAllChildProcessTaskBuilder killAllChildProcessTaskBuilder = new KillAllChildProcessTaskBuilder();
        Builder builder = killAllChildProcessTaskBuilder.createBuilder(null, null, null, null);
        new ObjectOutputStream(new StringOutputStream()).writeObject(builder);
    }

    public long getSystemTime() {
        ThreadMXBean bean = ManagementFactory.getThreadMXBean();
        return bean.isCurrentThreadCpuTimeSupported() ?
                (bean.getCurrentThreadCpuTime() - bean.getCurrentThreadUserTime()) : 0L;
    }
}
