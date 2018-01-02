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

package com.thoughtworks.go.util;

import java.util.List;

import com.thoughtworks.go.config.AntTask;
import com.thoughtworks.go.config.ExecTask;
import com.thoughtworks.go.config.FetchTask;
import com.thoughtworks.go.config.NantTask;
import com.thoughtworks.go.config.RakeTask;
import com.thoughtworks.go.config.registry.ConfigElementImplementationRegistry;
import com.thoughtworks.go.config.registry.NoPluginsInstalled;
import com.thoughtworks.go.domain.Task;
import org.junit.Test;

import static org.junit.Assert.assertThat;
import static org.hamcrest.CoreMatchers.hasItem;

public class ConfigUtilTest {
    @Test
    public void shouldGetAllTasks() {
        ConfigElementImplementationRegistry registry = new ConfigElementImplementationRegistry(new NoPluginsInstalled());
        registry.registerImplementer(Task.class, AntTask.class, ExecTask.class, NantTask.class, RakeTask.class, FetchTask.class);

        List<String> tasks = ConfigUtil.allTasks(registry);
        assertThat(tasks, hasItem("ant"));
        assertThat(tasks, hasItem("exec"));
        assertThat(tasks, hasItem("nant"));
        assertThat(tasks, hasItem("rake"));
        assertThat(tasks, hasItem("fetchartifact"));
    }
}
