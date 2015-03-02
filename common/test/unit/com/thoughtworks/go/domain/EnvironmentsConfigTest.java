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

package com.thoughtworks.go.domain;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.EnvironmentConfig;
import com.thoughtworks.go.config.EnvironmentsConfig;
import com.thoughtworks.go.config.exceptions.NoSuchEnvironmentException;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class EnvironmentsConfigTest {
    private EnvironmentsConfig configs;
    private EnvironmentConfig env;

    @Before public void setUp() throws Exception {
        configs = new EnvironmentsConfig();
        env = new EnvironmentConfig(new CaseInsensitiveString("uat"));
        env.addPipeline(new CaseInsensitiveString("deployment"));
        env.addAgent("agent-one");
        configs.add(env);
    }

    @Test
    public void shouldFindEnvironmentGivenPipelineName() throws Exception {
        assertThat(configs.findEnvironmentForPipeline(new CaseInsensitiveString("deployment")), is(env));
    }
    
    @Test public void shouldFindIfAGivenPipelineBelongsToAnyEnvironment() throws Exception {
        assertThat(configs.isPipelineAssociatedWithAnyEnvironment(new CaseInsensitiveString("deployment")), is(true));
    }

    @Test public void shouldFindOutIfAGivenPipelineDoesNotBelongsToAnyEnvironment() throws Exception {
        assertThat(configs.isPipelineAssociatedWithAnyEnvironment(new CaseInsensitiveString("unit-test")), is(false));
    }

    @Test public void shouldFindOutIfGivenAgentUUIDIsReferencedByAnyEnvironment() throws Exception {
        assertThat(configs.isAgentUnderEnvironment("agent-one"), is(true));
    }

    @Test public void shouldFindOutIfGivenAgentUUIDIsNotReferencedByAnyEnvironment() throws Exception {
        assertThat(configs.isAgentUnderEnvironment("agent-not-in-any-env"), is(false));
    }

    @Test public void shouldFindEnvironmentConfigGivenAnEnvironmentName() throws Exception {
        assertThat(configs.named(new CaseInsensitiveString("uat")), is(env));
    }

    @Test
    public void shouldUnderstandEnvironmentsForAgent() {
        assertThat(configs.environmentsForAgent("agent-one"), hasItem("uat"));
    }

    @Test public void shouldThrowExceptionIfTheEnvironmentDoesNotExist() {
        try {
            configs.named(new CaseInsensitiveString("not-exist"));
            fail("Should throw exception if the environment does not exist");
        } catch (NoSuchEnvironmentException e) {
            assertThat(e.getMessage(), Matchers.is("Environment [not-exist] does not exist."));
        }
    }

    @Test
    public void shouldRemoveAgentFromAllEnvironments() throws Exception {
        EnvironmentConfig env2 = new EnvironmentConfig(new CaseInsensitiveString("prod"));
        env2.addPipeline(new CaseInsensitiveString("test"));
        env2.addAgent("agent-one");
        env2.addAgent("agent-two");
        configs.add(env2);

        EnvironmentConfig env3 = new EnvironmentConfig(new CaseInsensitiveString("dev"));
        env3.addPipeline(new CaseInsensitiveString("build"));
        env3.addAgent("agent-two");
        env3.addAgent("agent-three");
        configs.add(env3);

        assertThat(configs.get(0).getAgents().size(), is(1));
        assertThat(configs.get(1).getAgents().size(), is(2));
        assertThat(configs.environmentsForAgent("agent-one").size(), is(2));

        configs.removeAgentFromAllEnvironments("agent-one");

        assertThat(configs.get(0).getAgents().size(), is(0));
        assertThat(configs.get(1).getAgents().size(), is(1));
        assertThat(configs.get(2).getAgents().size(), is(2));
        assertThat(configs.environmentsForAgent("agent-one").size(), is(0));
        assertThat(configs.environmentsForAgent("agent-two").size(), is(2));
        assertThat(configs.environmentsForAgent("agent-three").size(), is(1));
    }
}