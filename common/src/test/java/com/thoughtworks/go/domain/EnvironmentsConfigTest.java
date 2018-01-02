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

import com.thoughtworks.go.config.BasicEnvironmentConfig;
import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.EnvironmentConfig;
import com.thoughtworks.go.config.EnvironmentsConfig;
import com.thoughtworks.go.config.exceptions.NoSuchEnvironmentException;
import com.thoughtworks.go.config.merge.MergeEnvironmentConfig;
import com.thoughtworks.go.config.remote.FileConfigOrigin;
import com.thoughtworks.go.config.remote.RepoConfigOrigin;
import org.hamcrest.Matchers;
import org.hamcrest.core.Is;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class EnvironmentsConfigTest {
    private EnvironmentsConfig configs;
    private BasicEnvironmentConfig env;

    @Before public void setUp() throws Exception {
        configs = new EnvironmentsConfig();
        env = new BasicEnvironmentConfig(new CaseInsensitiveString("uat"));
        env.addPipeline(new CaseInsensitiveString("deployment"));
        env.addAgent("agent-one");
        configs.add(env);
    }

    @Test
    public void shouldFindEnvironmentGivenPipelineName() throws Exception {
        assertThat(configs.findEnvironmentForPipeline(new CaseInsensitiveString("deployment")), Is.<EnvironmentConfig>is(env));
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
        assertThat(configs.named(new CaseInsensitiveString("uat")), Is.<EnvironmentConfig>is(env));
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
        BasicEnvironmentConfig env2 = new BasicEnvironmentConfig(new CaseInsensitiveString("prod"));
        env2.addPipeline(new CaseInsensitiveString("test"));
        env2.addAgent("agent-one");
        env2.addAgent("agent-two");
        configs.add(env2);

        BasicEnvironmentConfig env3 = new BasicEnvironmentConfig(new CaseInsensitiveString("dev"));
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

    @Test
    public void shouldGetLocalPartsWhenOriginIsNull()
    {
        assertThat(configs.getLocal().size(), is(1));
        assertThat(configs.getLocal().get(0), Is.<EnvironmentConfig>is(env));
    }
    @Test
    public void shouldGetLocalPartsWhenOriginIsFile()
    {
        env.setOrigins(new FileConfigOrigin());
        assertThat(configs.getLocal().size(), is(1));
        assertThat(configs.getLocal().get(0), Is.<EnvironmentConfig>is(env));
    }
    @Test
    public void shouldGetLocalPartsWhenOriginIsRepo()
    {
        env.setOrigins(new RepoConfigOrigin());
        assertThat(configs.getLocal().size(), is(0));
    }

    @Test
    public void shouldGetLocalPartsWhenOriginIsMixed()
    {
        env.setOrigins(new FileConfigOrigin());

        BasicEnvironmentConfig prodLocalPart = new BasicEnvironmentConfig(new CaseInsensitiveString("PROD"));
        prodLocalPart.addAgent("1235");
        prodLocalPart.setOrigins(new FileConfigOrigin());
        BasicEnvironmentConfig prodRemotePart = new BasicEnvironmentConfig(new CaseInsensitiveString("PROD"));
        prodRemotePart.setOrigins(new RepoConfigOrigin());
        MergeEnvironmentConfig pairEnvironmentConfig = new MergeEnvironmentConfig(prodLocalPart, prodRemotePart);

        configs.add(pairEnvironmentConfig);

        assertThat(configs.getLocal().size(), is(2));
        assertThat(configs.getLocal(), hasItem(env));
        assertThat(configs.getLocal(), hasItem(prodLocalPart));
    }
}