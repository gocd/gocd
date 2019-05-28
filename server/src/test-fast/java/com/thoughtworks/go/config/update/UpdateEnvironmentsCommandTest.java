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
package com.thoughtworks.go.config.update;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.EnvironmentsConfig;
import com.thoughtworks.go.helper.GoConfigMother;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class UpdateEnvironmentsCommandTest {

    @Test
    public void shouldAddAgentToListOfEnvironment() throws Exception {
        String agentUuid = "uuid";
        UpdateEnvironmentsCommand command = new UpdateEnvironmentsCommand(agentUuid, "foo, bar, baz");
        CruiseConfig cruiseConfig = new GoConfigMother().defaultCruiseConfig();
        cruiseConfig.addEnvironment("foo");
        cruiseConfig.addEnvironment("bar");
        cruiseConfig.addEnvironment("baz");

        command.update(cruiseConfig);

        EnvironmentsConfig environments = cruiseConfig.getEnvironments();
        assertThat(environments.named(new CaseInsensitiveString("foo")).getAgents().getUuids().contains(agentUuid), is(true));
        assertThat(environments.named(new CaseInsensitiveString("bar")).getAgents().getUuids().contains(agentUuid), is(true));
        assertThat(environments.named(new CaseInsensitiveString("baz")).getAgents().getUuids().contains(agentUuid), is(true));
    }

    @Test
    public void shouldNotThrowUpIfEnvironmentNameIsInvalid() throws Exception {
        String agentUuid = "uuid";
        UpdateEnvironmentsCommand command = new UpdateEnvironmentsCommand(agentUuid, "foo, bar, monkey");
        CruiseConfig cruiseConfig = new GoConfigMother().defaultCruiseConfig();
        cruiseConfig.addEnvironment("foo");
        cruiseConfig.addEnvironment("bar");

        command.update(cruiseConfig);

        EnvironmentsConfig environments = cruiseConfig.getEnvironments();
        assertThat(environments.named(new CaseInsensitiveString("foo")).getAgents().getUuids().contains(agentUuid), is(true));
        assertThat(environments.named(new CaseInsensitiveString("bar")).getAgents().getUuids().contains(agentUuid), is(true));
    }
}
