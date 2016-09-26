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

package com.thoughtworks.go.remote.work;

import java.io.File;
import java.util.Arrays;

import com.thoughtworks.go.domain.BuildLogElement;
import com.thoughtworks.go.domain.GoControlLog;
import com.thoughtworks.go.domain.builder.Builder;
import com.thoughtworks.go.domain.builder.CommandBuilder;
import com.thoughtworks.go.plugin.access.pluggabletask.TaskExtension;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.command.CruiseControlException;

import static com.thoughtworks.go.config.RunIfConfig.FAILED;
import com.thoughtworks.go.domain.RunIfConfigs;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;
import com.thoughtworks.go.work.DefaultGoPublisher;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Before;
import org.junit.Test;

public class BuildersTest {
    private EnvironmentVariableContext environmentVariableContext;
    private SystemEnvironment systemEnvironment;

    @Before
    public void setUp() throws Exception {
        environmentVariableContext = new EnvironmentVariableContext();
        systemEnvironment = new SystemEnvironment();
    }

    @Test
    public void shouldNotSetAsCurrentBuilderIfNotRun() throws CruiseControlException {
        Builder builder = new CommandBuilder("echo", "", new File("."), new RunIfConfigs(FAILED), null, "");
        Builders builders = new Builders(Arrays.asList(builder), null, null, null);

        builders.setIsCancelled(true);
        builders.build(environmentVariableContext, systemEnvironment);

        Builders expected = new Builders(Arrays.asList(builder), null, null, null);
        expected.setIsCancelled(true);

        assertThat(builders, is(expected));
    }

    @Test
    public void shouldNotCancelAnythingIfAllBuildersHaveRun() throws CruiseControlException {
        Builder builder = new StubBuilder();
        Builders builders = new Builders(Arrays.asList(builder), null, new GoControlLog(), null);
        builders.build(environmentVariableContext, systemEnvironment);
        builders.cancel(environmentVariableContext, systemEnvironment);
    }

    private class StubBuilder extends Builder {
        public StubBuilder() {
            super(null, null, "");
        }

        public void build(BuildLogElement buildLogElement, DefaultGoPublisher publisher,
                          EnvironmentVariableContext environmentVariableContext, SystemEnvironment systemEnvironment, TaskExtension taskExtension) throws CruiseControlException {
        }

        public void cancel(DefaultGoPublisher publisher, EnvironmentVariableContext environmentVariableContext) {
            throw new RuntimeException("Should not be called");
        }
    }
}
