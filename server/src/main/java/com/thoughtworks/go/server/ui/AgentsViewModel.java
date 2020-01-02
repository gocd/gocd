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
package com.thoughtworks.go.server.ui;

import com.thoughtworks.go.domain.AgentStatus;
import com.thoughtworks.go.domain.BaseCollection;
import org.apache.commons.collections4.CollectionUtils;

import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.stream.Stream;

import static com.thoughtworks.go.domain.AgentStatus.Disabled;
import static com.thoughtworks.go.domain.AgentStatus.Pending;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.collections4.CollectionUtils.exists;
import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * @understands collection of agents view model
 */
@SuppressWarnings("deprecation")
@Deprecated
public class AgentsViewModel extends BaseCollection<AgentViewModel> {

    public AgentsViewModel() {
    }

    public AgentsViewModel(AgentViewModel... agentViewModels) {
        super(agentViewModels);
    }

    public void sortBy(Comparator<AgentViewModel> comparator, SortOrder direction) {
        this.sort(direction.comparator(comparator));
    }

    public int disabledCount() {
        return count(Disabled);
    }

    public int pendingCount() {
        return count(Pending);
    }

    public int enabledCount() {
        return Math.toIntExact(this.stream().filter(agentViewModel -> agentViewModel.getStatus().isEnabled()).count());
    }

    public void filter(String filterCriteria) {
        if (isBlank(filterCriteria)) {
            return;
        }

        final Map<String, String> criterionMap = getFilterCriterionMap(filterCriteria);
        if (criterionMap.isEmpty()) {
            return;
        }

        CollectionUtils.filter(this,
                agent -> criterionMap.entrySet().stream().anyMatch(entry -> AgentFilters.valueOf(entry.getKey().toUpperCase()).matches(agent, entry.getValue())));
    }

    private Map<String, String> getFilterCriterionMap(String filterCriteria) {
        return Stream.of(filterCriteria.split(","))
                .map(filterCriterion -> filterCriterion.split(":"))
                .filter(keyValPair -> keyValPair.length == 2)
                .filter(keyValPair -> agentFiltersHas(keyValPair[0].trim()))
                .collect(toMap(keyValPair -> keyValPair[0].trim(), keyValPair -> keyValPair[1].trim()));
    }

    private boolean agentFiltersHas(final String enumKey) {
        return CollectionUtils.exists(asList(AgentFilters.values()), agentFilters -> agentFilters.name().equals(enumKey.toUpperCase()));
    }

    private int count(AgentStatus status) {
        long countMatchingStatus = this.stream().filter(agentViewModel -> agentViewModel.getStatus().equals(status)).count();
        return Math.toIntExact(countMatchingStatus);
    }
}


@SuppressWarnings("deprecation")
enum AgentFilters {
    RESOURCE {
        @Override public boolean matches(AgentViewModel agent, final String searchCriteria) {
            return matchesFilter(agent.getResources(), searchCriteria);
        }
    },
    STATUS {
        @Override public boolean matches(AgentViewModel agent, String searchCriteria) {
            return matchesFilter(agent.getStatusForDisplay(), searchCriteria);
        }
    },
    NAME {
        @Override public boolean matches(AgentViewModel agent, String searchCriteria) {
            return matchesFilter(agent.getHostname(), searchCriteria);
        }
    },
    IP {
        @Override public boolean matches(AgentViewModel agent, String searchCriteria) {
            return matchesFilter(agent.getIpAddress(), searchCriteria);
        }
    },
    OS {
        @Override public boolean matches(AgentViewModel agent, String searchCriteria) {
            return matchesFilter(agent.getOperatingSystem(), searchCriteria);
        }
    },
    ENVIRONMENT {
        @Override public boolean matches(AgentViewModel agent, final String searchCriteria) {
            return matchesFilter(agent.getEnvironments(), searchCriteria);
        }
    };

    static boolean matchesFilter(Collection collection, final String searchCriteria) {
        final SearchCriteria criteria = new SearchCriteria(searchCriteria);
        return exists(collection, o -> criteria.matches((String) o));
    }

    static boolean matchesFilter(String agentValue, String searchCriteria) {
        return new SearchCriteria(searchCriteria).matches(agentValue);
    }

    public abstract boolean matches(AgentViewModel agent, String searchCriteria);
}

