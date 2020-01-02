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
package com.thoughtworks.go.presentation;

import java.util.*;

import com.thoughtworks.go.config.Agent;
import com.thoughtworks.go.config.Agents;
import com.thoughtworks.go.config.ResourceConfig;
import com.thoughtworks.go.config.ResourceConfigs;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import org.hamcrest.Matchers;
import static org.junit.Assert.assertThat;
import org.junit.Before;
import org.junit.Test;

public class TriStateSelectionTest {
    private Set<ResourceConfig> resourceConfigs;
    private Agents agents;

    @Before
    public void before() {
        resourceConfigs = new HashSet<>();
        resourceConfigs.add(new ResourceConfig("one"));
        resourceConfigs.add(new ResourceConfig("two"));

        agents = new Agents();
    }

    @Test
    public void shouldHaveActionRemoveIfThereAreNoAgents() {
        List<TriStateSelection> selections = TriStateSelection.forAgentsResources(resourceConfigs, agents);
        assertThat(selections, hasItem(new TriStateSelection("one", TriStateSelection.Action.remove)));
        assertThat(selections, hasItem(new TriStateSelection("two", TriStateSelection.Action.remove)));
        assertThat(selections.size(), is(2));
    }

    @Test
    public void shouldHaveActionAddIfAllAgentsHaveThatResource() {
        resourceConfigs.add(new ResourceConfig("all"));
        agents.add(new Agent("uuid1", "host1", "127.0.0.1", singletonList("all")));
        agents.add(new Agent("uuid2", "host2", "127.0.0.2", singletonList("all")));

        List<TriStateSelection> selections = TriStateSelection.forAgentsResources(resourceConfigs, agents);
        assertThat(selections, hasItem(new TriStateSelection("all", TriStateSelection.Action.add)));
    }

    @Test
    public void shouldBeNoChangeIfAllAgentsHaveThatResource() {
        resourceConfigs.add(new ResourceConfig("some"));
        agents.add(new Agent("uuid1", "host1", "127.0.0.1", singletonList("some")));
        agents.add(new Agent("uuid2", "host2", "127.0.0.2", emptyList()));

        List<TriStateSelection> selections = TriStateSelection.forAgentsResources(resourceConfigs, agents);
        assertThat(selections, hasItem(new TriStateSelection("some", TriStateSelection.Action.nochange)));
    }

    @Test
    public void shouldHaveActionRemoveIfNoAgentsHaveResource() {
        resourceConfigs.add(new ResourceConfig("none"));
        agents.add(new Agent("uuid1", "host1", "127.0.0.1", singletonList("one")));
        agents.add(new Agent("uuid2", "host2", "127.0.0.2", singletonList("two")));

        List<TriStateSelection> selections = TriStateSelection.forAgentsResources(resourceConfigs, agents);
        assertThat(selections, hasItem(new TriStateSelection("none", TriStateSelection.Action.remove)));
    }

    @Test
    public void shouldListResourceSelectionInAlhpaOrder() {
        HashSet<ResourceConfig> resourceConfigs = new HashSet<>();
        resourceConfigs.add(new ResourceConfig("B02"));
        resourceConfigs.add(new ResourceConfig("b01"));
        resourceConfigs.add(new ResourceConfig("a01"));
        List<TriStateSelection> selections = TriStateSelection.forAgentsResources(resourceConfigs, agents);

        assertThat(selections.get(0), Matchers.is(new TriStateSelection("a01", TriStateSelection.Action.remove)));
        assertThat(selections.get(1), Matchers.is(new TriStateSelection("b01", TriStateSelection.Action.remove)));
        assertThat(selections.get(2), Matchers.is(new TriStateSelection("B02", TriStateSelection.Action.remove)));
    }

    @Test
    public void shouldDisableWhenDisableVoted() {
        final boolean[] associate = new boolean[1];

        final TriStateSelection.Assigner<String, String> disableWhenEql = new TriStateSelection.Assigner<String, String>() {
            @Override
            public boolean shouldAssociate(String a, String b) {
                return associate[0];
            }

            @Override
            public String identifier(String s) {
                return s;
            }

            @Override
            public boolean shouldEnable(String a, String b) {
                return !a.equals(b);
            }
        };

        final HashSet<String> assignables = new HashSet<>(Arrays.asList("quux", "baz"));

        associate[0] = true;
        List<TriStateSelection> selections = TriStateSelection.convert(assignables, Arrays.asList("foo", "bar"), disableWhenEql);
        assertThat(selections, hasItem(new TriStateSelection("quux", TriStateSelection.Action.add)));
        assertThat(selections, hasItem(new TriStateSelection("baz", TriStateSelection.Action.add)));

        associate[0] = false;
        selections = TriStateSelection.convert(assignables, Arrays.asList("foo", "bar"), disableWhenEql);
        assertThat(selections, hasItem(new TriStateSelection("quux", TriStateSelection.Action.remove)));
        assertThat(selections, hasItem(new TriStateSelection("baz", TriStateSelection.Action.remove)));

        associate[0] = true;
        selections = TriStateSelection.convert(assignables, Arrays.asList("quux", "bar"), disableWhenEql);
        assertThat(selections, hasItem(new TriStateSelection("quux", TriStateSelection.Action.add, false)));
        assertThat(selections, hasItem(new TriStateSelection("baz", TriStateSelection.Action.add, true)));

        associate[0] = false;
        selections = TriStateSelection.convert(assignables, Arrays.asList("bar", "baz"), disableWhenEql);
        assertThat(selections, hasItem(new TriStateSelection("quux", TriStateSelection.Action.remove, true)));
        assertThat(selections, hasItem(new TriStateSelection("baz", TriStateSelection.Action.remove, false)));
    }
}
