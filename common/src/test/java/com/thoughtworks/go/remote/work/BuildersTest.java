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
package com.thoughtworks.go.remote.work;

import com.thoughtworks.go.domain.RunIfConfigs;
import com.thoughtworks.go.domain.StubGoPublisher;
import com.thoughtworks.go.domain.builder.Builder;
import com.thoughtworks.go.domain.builder.CommandBuilder;
import com.thoughtworks.go.domain.builder.StubBuilder;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Collections;

import static com.thoughtworks.go.config.RunIfConfig.ANY;
import static com.thoughtworks.go.config.RunIfConfig.FAILED;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class BuildersTest {

    @Test
    public void shouldNotBuildIfTheJobIsCanceled() throws Exception {
        StubGoPublisher goPublisher = new StubGoPublisher();
        EnvironmentVariableContext environmentVariableContext = new EnvironmentVariableContext();

        CommandBuilder builder = new CommandBuilder("echo", "hello", new File("."), new RunIfConfigs(FAILED),
                new com.thoughtworks.go.domain.builder.StubBuilder(),
                "");

        Builders builders = new Builders(Collections.singletonList(builder), goPublisher, null, null, null);
        builders.setIsCancelled(true);
        builders.build(environmentVariableContext, "utf-8");

        assertThat(goPublisher.getMessage(), is(""));
    }


    @Test
    public void shouldNotSetAsCurrentBuilderIfNotRun() throws Exception {
        EnvironmentVariableContext environmentVariableContext = new EnvironmentVariableContext();
        Builder builder = new CommandBuilder("echo", "", new File("."), new RunIfConfigs(FAILED), null, "");
        Builders builders = new Builders(Collections.singletonList(builder), null, null, null, null);

        builders.setIsCancelled(true);
        builders.build(environmentVariableContext, "utf-8");

        Builders expected = new Builders(Collections.singletonList(builder), null, null, null, null);
        expected.setIsCancelled(true);

        assertThat(builders, is(expected));
    }

    @Test
    public void shouldNotCancelAnythingIfAllBuildersHaveRun() throws Exception {
        EnvironmentVariableContext environmentVariableContext = new EnvironmentVariableContext();
        Builder builder = new StubBuilder(new RunIfConfigs(ANY));
        Builders builders = new Builders(Collections.singletonList(builder), new StubGoPublisher(), null, null, null);
        builders.build(environmentVariableContext, "utf-8");
        builders.cancel(environmentVariableContext, "utf-8");
    }
}
