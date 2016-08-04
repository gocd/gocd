/*
 * Copyright 2016 ThoughtWorks, Inc.
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
import com.thoughtworks.go.util.StringUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;

import java.util.*;

/**
 * @understands collection of agents view model
 */
@Deprecated
public class AgentsViewModel extends BaseCollection<AgentViewModel> {

    private static final String RESOURCE = "resource";
    private static final String STATUS = "status";

    public AgentsViewModel() {
    }

    public AgentsViewModel(AgentViewModel... agentViewModels) {
        super(agentViewModels);
    }

    public void sortBy(Comparator<AgentViewModel> comparator, SortOrder direction) {
        Collections.sort(this, direction.comparator(comparator));
    }

    public int disabledCount() {
        return count(AgentStatus.Disabled);
    }

    public int pendingCount() {
        return count(AgentStatus.Pending);
    }

    public int enabledCount() {
        int count = 0;
        for (AgentViewModel agentViewModel : this) {
            if (agentViewModel.getStatus().isEnabled()) {
                count++;
            }
        }
        return count;
    }

    public void filter(String filterCriteria) {

        if (StringUtil.isBlank(filterCriteria)) {
            return;
        }
        final Map<String, String> filters = filters(filterCriteria);

        if (filters.isEmpty()) {
            return;
        }

        CollectionUtils.filter(this, new Predicate() {
            public boolean evaluate(Object o) {
                boolean finalResult = false;
                AgentViewModel agent = (AgentViewModel) o;
                for (Map.Entry<String, String> entry : filters.entrySet()) {
                    AgentFilters filter = AgentFilters.valueOf(entry.getKey().toUpperCase());
                    finalResult = finalResult || filter.matches(agent, entry.getValue());
                }
                return finalResult;
            }

        });
    }

    private Map<String, String> filters(String filterCriteria) {
        String[] filters = filterCriteria.split(",");
        Map<String, String> filterMap = new HashMap<>();
        for (String filter : filters) {
            String[] keyValue = filter.split(":");
            if (keyValue.length == 2 && agentFiltersHas(keyValue[0].trim())) {
                filterMap.put(keyValue[0].trim(), keyValue[1].trim());
            }
        }
        return filterMap;
    }

    private boolean agentFiltersHas(final String enumKey) {
        return CollectionUtils.exists(Arrays.asList(AgentFilters.values()), new Predicate() {
            public boolean evaluate(Object o) {
                AgentFilters filter = (AgentFilters) o;
                return filter.name().equals(enumKey.toUpperCase());
            }
        });
    }

    private int count(AgentStatus status) {
        int count = 0;
        for (AgentViewModel agentViewModel : this) {
            if (agentViewModel.getStatus().equals(status)) {
                count++;
            }
        }
        return count;
    }

    public Boolean hasAgentsThatNeedUpgrade() {
        for (AgentViewModel agentViewModel : this) {
            if(agentViewModel.needsUpgrade())
                return true;
        }
        return false;
    }
}

enum AgentFilters {
    RESOURCE {
        @Override public boolean matches(AgentViewModel agent, final String searchCriteria) {
            return this.matchesFilter(agent.getResources(), searchCriteria);
        }
    },
    STATUS {
        @Override public boolean matches(AgentViewModel agent, String searchCriteria) {
            return this.matchesFilter(agent.getStatusForDisplay(), searchCriteria);
        }
    },
    NAME {
        @Override public boolean matches(AgentViewModel agent, String searchCriteria) {
            return this.matchesFilter(agent.getHostname(), searchCriteria);
        }
    },
    IP {
        @Override public boolean matches(AgentViewModel agent, String searchCriteria) {
            return this.matchesFilter(agent.getIpAddress(), searchCriteria);
        }
    },
    OS {
        @Override public boolean matches(AgentViewModel agent, String searchCriteria) {
            return this.matchesFilter(agent.getOperatingSystem(), searchCriteria);
        }
    },
    ENVIRONMENT {
        @Override public boolean matches(AgentViewModel agent, final String searchCriteria) {
            return this.matchesFilter(agent.getEnvironments(), searchCriteria);
        }
    };

    static boolean matchesFilter(Collection collection, final String searchCriteria) {
        final SearchCriteria criteria = new SearchCriteria(searchCriteria);
        return CollectionUtils.exists(collection, new Predicate() {
            public boolean evaluate(Object o) {
                return criteria.matches((String) o);
            }
        });
    }

    static boolean matchesFilter(String agentValue, String searchCriteria) {
        return new SearchCriteria(searchCriteria).matches(agentValue);
    }

    public abstract boolean matches(AgentViewModel agent, String searchCriteria);
}

