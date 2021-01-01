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
package com.thoughtworks.go.server.ui;


import com.thoughtworks.go.domain.AgentInstance;
import com.thoughtworks.go.helper.AgentInstanceMother;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Fail.fail;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;

@SuppressWarnings("deprecation")
class AgentsViewModelTest {

    @Nested
    class Sort{
        @Test
        void shouldSortByStatusAsc() {
            AgentsViewModel instances = agentsViewModel();
            instances.sortBy(AgentViewModel.STATUS_COMPARATOR, SortOrder.ASC);
            for (int i = 1; i < instances.size(); i++) {
                assertThat(instances.get(i - 1).getStatus().compareTo(instances.get(i).getStatus()), is(org.hamcrest.Matchers.lessThan(0)));
            }
        }

        @Test
        void shouldSortByStatusDesc() {
            AgentsViewModel instances = agentsViewModel();
            instances.sortBy(AgentViewModel.STATUS_COMPARATOR, SortOrder.DESC);

            for (int i = 1; i < instances.size(); i++) {
                assertThat(instances.get(i - 1).getStatus().compareTo(instances.get(i).getStatus()), is(greaterThan(0)));
            }
        }
    }

    @Nested
    class Counting{
        @Test
        void shouldReturnTheCorrectCountForAgentStatuses() {
            AgentsViewModel agents = agentsViewModel();
            assertThat(agents.disabledCount(), is(1));
            assertThat(agents.enabledCount(), is(2));
            assertThat(agents.pendingCount(), is(1));
        }
    }

    @Nested
    class Filter{

        @Nested
        class PositiveCases{
            @Test
            void shouldFilterByResources() {
                AgentsViewModel agents = agentsViewModel();
                agents.filter("resource:Foo");
                assertThat(agents.size(), is(2));
            }

            @Test
            void shouldFilterOnlyBySingleResource() {
                AgentsViewModel agents = agentsViewModel();
                agents.filter("resource:bar | fooooo");
                assertThat(agents.size(), is(0));
            }

            @Test
            void shouldFilterByAgentName() {
                AgentsViewModel agents = agentsViewModel();
                agents.filter(String.format("name:%s", AgentInstanceMother.disabled().getHostname()));
                assertThat(agents.size(), is(1));
                agents = agentsViewModel();
                agents.filter("name:CCeDev");
                assertThat(agents.size(), is(4));
            }

            @Test
            void shouldFilterByIpAddress() {
                AgentsViewModel agents = agentsViewModel();
                agents.filter("ip:10.");
                assertThat(agents.size(), is(4));
                agents.filter(String.format("ip:%s", AgentInstanceMother.disabled().getIpAddress()));
                assertThat(agents.size(), is(1));
            }

            @Test
            void shouldFilterByOS() {
                AgentsViewModel agents = agentsViewModel();
                agents.filter("os:Macos");
                assertThat(agents.size(), is(1));
            }

            @Test
            void shouldFilterByEnvironmentNames() {
                AgentsViewModel agents = agentsViewModel();
                agents.filter("environment:Uat");
                assertThat(agents.size(), is(2));
                agents = agentsViewModel();
                agents.filter("environment:de");
                assertThat(agents.size(), is(1));
            }

            @Test
            void shouldFilterByResourcesOrStatus() {
                AgentsViewModel agents = agentsViewModel();
                agents.filter("resource:foo, status: Pending");
                assertThat(agents.size(), is(3));
            }

            @Test
            void shouldFilterByStatus() {
                AgentsViewModel agents = agentsViewModel();
                agents.filter("status:building");
                assertThat(agents.size(), is(1));
                assertThat(agents.get(0).isBuilding(), is(true));

                agents = agentsViewModel();
                agents.filter("status:Building");
                assertThat(agents.size(), is(1));
                assertThat(agents.get(0).isBuilding(), is(true));
            }

            @Test
            void shouldFilterWithExactMatch() {
                AgentsViewModel agents = agentsViewModel();
                agents.filter("resource:\"Foo\", baz");
                assertThat(agents.size(), is(1));
                assertThat(agents.get(0).getHostname().equals("CCeDev01"), is(true));
            }
        }

        @Nested
        class NegativeCases{
            @Test
            void shouldHandleUnclosedDoubleQuotes() {
                AgentsViewModel agents = agentsViewModel();
                agents.filter("resource:\"");
                assertThat(agents.size(), is(0));
            }

            @Test
            void shouldNotThrowUpAndShouldReturnAllAgentsIfInvalidFilterCriteriaIsPassed() {
                AgentsViewModel agentsViewModel = agentsViewModel();
                try {
                    agentsViewModel.filter("foo:bar");
                    assertThat(agentsViewModel.size(), is(4));
                } catch (Exception e) {
                    fail("should not fail.");
                }
            }

            @Test
            void shouldNotThrowUpIfInvalidFilterFormatIsPassed() {
                AgentsViewModel agentsViewModel = agentsViewModel();
                try {
                    agentsViewModel.filter("some_invalid_format");
                } catch (Exception e) {
                    fail("should not fail");
                }
            }
        }

    }

    private AgentsViewModel agentsViewModel() {
        AgentsViewModel agents = new AgentsViewModel();
        AgentInstance idleAgentInstance = AgentInstanceMother.idle(new Date(), "CCeDev01");
        AgentInstanceMother.updateOS(idleAgentInstance, "macos");
        idleAgentInstance.getAgent().addResources(asList("foo","bar"));
        agents.add(new AgentViewModel(idleAgentInstance, "uat"));

        AgentInstance buildingAgentInstance = AgentInstanceMother.building();
        buildingAgentInstance.getAgent().addResource("goofooboo");
        agents.add(new AgentViewModel(buildingAgentInstance, "dev", "uat"));
        agents.add(new AgentViewModel(AgentInstanceMother.pending()));
        agents.add(new AgentViewModel(AgentInstanceMother.disabled(), "prod"));
        return agents;
    }
}
