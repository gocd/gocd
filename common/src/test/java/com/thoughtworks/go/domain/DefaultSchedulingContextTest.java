/*
 * Copyright 2020 ThoughtWorks, Inc.
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

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.helper.StageConfigMother;
import com.thoughtworks.go.util.ReflectionUtil;
import org.junit.jupiter.api.Test;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;

public class DefaultSchedulingContextTest {
    @Test
    void shouldFindNoAgentsIfNoneExist() {
        DefaultSchedulingContext context = new DefaultSchedulingContext("approved", new Agents());
        assertThat(context.findAgentsMatching(new ResourceConfigs())).isEqualTo(new Agents());
    }

    @Test
    void shouldFindAllAgentsIfNoResourcesAreSpecified() {
        Agent linux = agent("uuid1", "linux");
        Agent windows = agent("uuid2", "windows");
        Agents matchingAgents = new Agents(linux, windows);
        DefaultSchedulingContext context = new DefaultSchedulingContext("approved", matchingAgents);
        assertThat(context.findAgentsMatching(new ResourceConfigs())).isEqualTo(matchingAgents);
    }

    @Test
    void shouldOnlyFindAgentsThatMatchResourcesSpecified() {
        Agent linux = agent("uuid1", "linux");
        Agent windows = agent("uuid2", "windows");
        Agents matchingAgents = new Agents(linux, windows);
        DefaultSchedulingContext context = new DefaultSchedulingContext("approved", matchingAgents);
        assertThat(context.findAgentsMatching(resources("linux"))).isEqualTo(new Agents(linux));
    }

    @Test
    void shouldFindNoAgentsIfNoneMatch() {
        Agent linux = agent("uuid1", "linux");
        Agent windows = agent("uuid2", "windows");
        Agents matchingAgents = new Agents(linux, windows);
        DefaultSchedulingContext context = new DefaultSchedulingContext("approved", matchingAgents);
        assertThat(context.findAgentsMatching(resources("macosx"))).isEqualTo(new Agents());
    }

    @Test
    void shouldNotMatchDeniedAgents() {
        Agent linux = agent("uuid1", "linux");
        Agent windows = agent("uuid2", "windows");
        windows.disable();
        Agents matchingAgents = new Agents(linux, windows);
        DefaultSchedulingContext context = new DefaultSchedulingContext("approved", matchingAgents);
        assertThat(context.findAgentsMatching(resources())).isEqualTo(new Agents(linux));
    }

    @Test
    void shouldSetEnvironmentVariablesOnSchedulingContext() {
        EnvironmentVariablesConfig existing = new EnvironmentVariablesConfig();
        existing.add("firstVar", "firstVal");
        existing.add("overriddenVar", "originalVal");

        SchedulingContext schedulingContext = new DefaultSchedulingContext();
        schedulingContext = schedulingContext.overrideEnvironmentVariables(existing);

        EnvironmentVariablesConfig stageLevel = new EnvironmentVariablesConfig();
        stageLevel.add("stageVar", "stageVal");
        stageLevel.add("overriddenVar", "overriddenVal");

        StageConfig config = StageConfigMother.custom("test", Approval.automaticApproval());
        config.setVariables(stageLevel);

        ReflectionUtil.setField(schedulingContext, "rerun", true);
        SchedulingContext context = schedulingContext.overrideEnvironmentVariables(config.getVariables());

        assertThat(context.isRerun()).isTrue();
        EnvironmentVariablesConfig environmentVariablesUsed = context.getEnvironmentVariablesConfig();
        assertThat(environmentVariablesUsed.size()).isEqualTo(3);
        assertThat(environmentVariablesUsed).contains(new EnvironmentVariableConfig("firstVar", "firstVal"));
        assertThat(environmentVariablesUsed).contains(new EnvironmentVariableConfig("overriddenVar", "overriddenVal"));
        assertThat(environmentVariablesUsed).contains(new EnvironmentVariableConfig("stageVar", "stageVal"));
    }

    @Test
    void shouldCreatePermittedAgentContextCorrectly() {
        Agent linux = agent("uuid1", "linux");
        Agent windows = agent("uuid2", "windows");
        windows.disable();
        Agents matchingAgents = new Agents(linux, windows);

        EnvironmentVariablesConfig existing = new EnvironmentVariablesConfig();
        existing.add("firstVar", "firstVal");
        existing.add("overriddenVar", "originalVal");

        SchedulingContext schedulingContext = new DefaultSchedulingContext("approver", matchingAgents);
        schedulingContext = schedulingContext.overrideEnvironmentVariables(existing);

        EnvironmentVariablesConfig stageLevel = new EnvironmentVariablesConfig();
        stageLevel.add("stageVar", "stageVal");
        stageLevel.add("overriddenVar", "overriddenVal");

        StageConfig config = StageConfigMother.custom("test", Approval.automaticApproval());
        config.setVariables(stageLevel);

        SchedulingContext context = schedulingContext.overrideEnvironmentVariables(config.getVariables());
        ReflectionUtil.setField(context, "rerun", true);
        SchedulingContext permittedAgentContext = context.permittedAgent("uuid1");

        Agents agents = (Agents) ReflectionUtil.getField(permittedAgentContext, "agents");
        assertThat(agents.size()).isEqualTo(1);
        assertThat(agents.get(0).getAgentIdentifier().getUuid()).isEqualTo("uuid1");
        assertThat(permittedAgentContext.isRerun()).isTrue();
        assertThat(permittedAgentContext.getApprovedBy()).isEqualTo("approver");
        EnvironmentVariablesConfig environmentVariablesUsed = permittedAgentContext.getEnvironmentVariablesConfig();
        assertThat(environmentVariablesUsed.size()).isEqualTo(3);
        assertThat(environmentVariablesUsed).contains(new EnvironmentVariableConfig("firstVar", "firstVal"));
        assertThat(environmentVariablesUsed).contains(new EnvironmentVariableConfig("overriddenVar", "overriddenVal"));
        assertThat(environmentVariablesUsed).contains(new EnvironmentVariableConfig("stageVar", "stageVal"));
    }

    @Test
    void shouldCreateRerunSchedulingContextCorrectly() {
        Agent linux = agent("uuid1", "linux");
        Agent windows = agent("uuid2", "windows");
        windows.disable();
        Agents matchingAgents = new Agents(linux, windows);

        EnvironmentVariablesConfig existing = new EnvironmentVariablesConfig();
        existing.add("firstVar", "firstVal");
        existing.add("overriddenVar", "originalVal");

        SchedulingContext schedulingContext = new DefaultSchedulingContext("approver", matchingAgents);
        schedulingContext = schedulingContext.overrideEnvironmentVariables(existing);

        EnvironmentVariablesConfig stageLevel = new EnvironmentVariablesConfig();
        stageLevel.add("stageVar", "stageVal");
        stageLevel.add("overriddenVar", "overriddenVal");

        StageConfig config = StageConfigMother.custom("test", Approval.automaticApproval());
        config.setVariables(stageLevel);

        SchedulingContext context = schedulingContext.overrideEnvironmentVariables(config.getVariables());
        SchedulingContext rerunContext = context.rerunContext();

        assertThat(rerunContext.isRerun()).isTrue();
        assertThat(rerunContext.getApprovedBy()).isEqualTo("approver");
        Agents agents = (Agents) ReflectionUtil.getField(rerunContext, "agents");
        assertThat(agents).isEqualTo(matchingAgents);
        EnvironmentVariablesConfig environmentVariablesUsed = rerunContext.getEnvironmentVariablesConfig();
        assertThat(environmentVariablesUsed.size()).isEqualTo(3);
        assertThat(environmentVariablesUsed).contains(new EnvironmentVariableConfig("firstVar", "firstVal"));
        assertThat(environmentVariablesUsed).contains(new EnvironmentVariableConfig("overriddenVar", "overriddenVal"));
        assertThat(environmentVariablesUsed).contains(new EnvironmentVariableConfig("stageVar", "stageVal"));
    }

    @Test
    void shouldReturnElasticProfileIdSetAtPipelineConfigLevel() {
        DefaultSchedulingContext schedulingContext = new DefaultSchedulingContext("admin", new Agents(), emptyMap(), emptyMap(), null, null);

        assertThat(schedulingContext.getElasticProfileIdAtPipelineConfig()).isNull();

        schedulingContext = new DefaultSchedulingContext("admin", new Agents(), emptyMap(), emptyMap(), "some-profile-id", null);

        assertThat(schedulingContext.getElasticProfileIdAtPipelineConfig()).isEqualTo("some-profile-id");
    }

    @Test
    void shouldReturnElasticProfileIdSetAtStageConfigLevel() {
        DefaultSchedulingContext schedulingContext = new DefaultSchedulingContext("admin", new Agents(), emptyMap(), emptyMap(), null, null);

        assertThat(schedulingContext.getElasticProfileIdAtStageConfig()).isNull();

        schedulingContext = new DefaultSchedulingContext("admin", new Agents(), emptyMap(), emptyMap(), null, "some-profile-id");

        assertThat(schedulingContext.getElasticProfileIdAtStageConfig()).isEqualTo("some-profile-id");
    }

    private Agent agent(String uuid, String... names) {
        return new Agent(uuid, "localhost", "127.0.0.1", names == null ? null : asList(names));
    }

    public static ResourceConfigs resources(String... names) {
        ResourceConfigs resourceConfigs = new ResourceConfigs();
        for (String name : names) {
            resourceConfigs.add(new ResourceConfig(name));
        }
        return resourceConfigs;
    }
}
