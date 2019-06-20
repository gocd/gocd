/*
 * Copyright 2019 ThoughtWorks, Inc.
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

package com.thoughtworks.go.server.domain;

import com.thoughtworks.go.config.ResourceConfigs;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.Assert.*;
import static org.hamcrest.Matchers.is;

public class AgentTest {
    @Test
    void shouldAddResourcesToExistingResources() {
        Agent agent = new Agent("uuid", "cookie", "host", "127.0.0.1");
        agent.setResources(new ResourceConfigs("resource1"));

        agent.addResources(Arrays.asList("resource2", "resource3"));

        assertThat(agent.getResources().size(), is(3));
        assertThat(agent.getResources().resourceNames(), is(Arrays.asList("resource1", "resource2", "resource3")));
    }

    @Test
    void shouldAddResourcesToIfThereAreNoExistingResources() {
        Agent agent = new Agent("uuid", "cookie", "host", "127.0.0.1");

        agent.addResources(Arrays.asList("resource2", "resource3"));

        assertThat(agent.getResources().size(), is(2));
        assertThat(agent.getResources().resourceNames(), is(Arrays.asList("resource2", "resource3")));
    }

    @Test
    void shouldRemoveResourcesFromExistingResources() {
        Agent agent = new Agent("uuid", "cookie", "host", "127.0.0.1");
        agent.setResources(new ResourceConfigs("resource1,resource2,resource3"));

        agent.removeResources(Arrays.asList("resource2"));

        assertThat(agent.getResources().size(), is(2));
        assertThat(agent.getResources().resourceNames(), is(Arrays.asList("resource1", "resource3")));
    }

    @Test
    void shouldNotRemoveResourcesIfDoNotExist() {
        Agent agent = new Agent("uuid", "cookie", "host", "127.0.0.1");

        agent.removeResources(Arrays.asList("resource2"));

        assertTrue(agent.getResources().resourceNames().isEmpty());
    }

    @Test
    void shouldAddEnvironmentsToExistingEnvironments() {
        Agent agent = new Agent("uuid", "cookie", "host", "127.0.0.1");
        agent.setEnvironments("env1,env2");

        agent.addEnvironments(Arrays.asList("env2", "env3"));

        assertThat(agent.getEnvironments(), is("env1,env2,env3"));
    }

    @Test
    void shouldAddEnvironmentsIfNoExistingEnvironments() {
        Agent agent = new Agent("uuid", "cookie", "host", "127.0.0.1");

        agent.addEnvironments(Arrays.asList("env2", "env3"));

        assertThat(agent.getEnvironments(), is("env2,env3"));
    }

    @Test
    void shouldRemoveEnvironmentsFromExistingEnvironments() {
        Agent agent = new Agent("uuid", "cookie", "host", "127.0.0.1");
        agent.setEnvironments("env1,env2");

        agent.removeEnvironments(Arrays.asList("env1", "env3"));

        assertThat(agent.getEnvironments(), is("env2"));
    }

    @Test
    void shouldNotRemoveEnvironmentsIfEnvironmentsDoNotExist() {
        Agent agent = new Agent("uuid", "cookie", "host", "127.0.0.1");

        agent.removeEnvironments(Arrays.asList("env1", "env3"));

        assertNull(agent.getEnvironments());
    }
}
