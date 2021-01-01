/*
 * Copyright 2021 ThoughtWorks, Inc.
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
package com.thoughtworks.go.domain;

import com.thoughtworks.go.config.ValidationContext;
import org.junit.Test;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.HashMap;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;


public class KillAllChildProcessTaskTest {
    private ValidationContext validationContext;

    public long getSystemTime() {
        ThreadMXBean bean = ManagementFactory.getThreadMXBean();
        return bean.isCurrentThreadCpuTimeSupported() ?
                (bean.getCurrentThreadCpuTime() - bean.getCurrentThreadUserTime()) : 0L;
    }

    @Test
    public void shouldReturnDefaultsForCancelTaskAndGetConditions() throws Exception {
        KillAllChildProcessTask processTask = new KillAllChildProcessTask();
        Task actual = processTask.cancelTask();
        assertThat(actual, is(instanceOf(NullTask.class)));
        assertThat(processTask.getConditions().size(), is(0));
    }

    @Test
    public void shouldNotAllowSettingOfConfigAttributes() throws Exception {
        KillAllChildProcessTask processTask = new KillAllChildProcessTask();
        try {
            processTask.setConfigAttributes(new HashMap());
            fail("should have failed, as configuration of kill-all task is not allowed");
        } catch (UnsupportedOperationException e) {
            assertThat(e.getMessage(), is("Not a configurable task"));
        }
    }

    @Test
    public void validateShouldReturnNoErrors() throws Exception {
        KillAllChildProcessTask processTask = new KillAllChildProcessTask();
        processTask.validate(validationContext);
        assertThat(processTask.errors().isEmpty(), is(true));
    }

    @Test
    public void shouldKnowItsType() {
        assertThat(new KillAllChildProcessTask().getTaskType(), is("killallchildprocess"));
    }

    @Test
    public void shouldReturnEmptyPropertiesForDisplay() {
        assertThat(new KillAllChildProcessTask().getPropertiesForDisplay().isEmpty(), is(true));
    }
}
