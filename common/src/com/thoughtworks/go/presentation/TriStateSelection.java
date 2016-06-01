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

package com.thoughtworks.go.presentation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.thoughtworks.go.config.AdminsConfig;
import com.thoughtworks.go.config.AgentConfig;
import com.thoughtworks.go.config.Agents;
import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.EnvironmentConfig;
import com.thoughtworks.go.config.Resource;
import com.thoughtworks.go.config.Role;
import com.thoughtworks.go.config.UserRoleMatcher;
import com.thoughtworks.go.domain.config.Admin;
import com.thoughtworks.go.util.comparator.AlphaAsciiComparator;

/**
 * @understands What values are selected on multiple agents
 */
public class TriStateSelection implements Comparable<TriStateSelection> {
    private String value;
    private Action action;
    private boolean enabled;

    private AlphaAsciiComparator comparator = new AlphaAsciiComparator();

    public TriStateSelection(String value, Action action) {
        this(value, action, true);
    }

    public TriStateSelection(String value, String action) {
        this(value, Action.valueOf(action));
    }

    public TriStateSelection(String value, Action action, boolean enabled) {
        this.value = value;
        this.action = action;
        this.enabled = enabled;
    }

    public String getValue() {
        return value;
    }

    public Action getAction() {
        return action;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public static List<TriStateSelection> forAgentsResources(Set<Resource> resources, Agents agents) {
        return convert(resources, agents, new Assigner<Resource, AgentConfig>() {
            public boolean shouldAssociate(AgentConfig agent, Resource resource) {
                return agent.getResources().contains(resource);
            }

            public String identifier(Resource resource) {
                return resource.getName();
            }

            public boolean shouldEnable(AgentConfig agent, Resource resource) {
                return true;
            }
        });
    }

    public static List<TriStateSelection> forRoles(Set<Role> allRoles, List<String> users) {
        return convert(allRoles, users, new Assigner<Role, String>() {
            public boolean shouldAssociate(String user, Role role) {
                return role.hasMember(new CaseInsensitiveString(user));
            }

            public String identifier(Role role) {
                return CaseInsensitiveString.str(role.getName());
            }

            public boolean shouldEnable(String user, Role role) {
                return true;
            }
        });
    }

    public static TriStateSelection forSystemAdmin(final AdminsConfig adminsConfig, final Set<Role> allRoles, final UserRoleMatcher userRoleMatcher, List<String> users) {
        return convert(new HashSet<>(Arrays.asList(Admin.GO_SYSTEM_ADMIN)), users, new Assigner<String, String>() {
            public boolean shouldAssociate(String userName, String ignore) {
                return adminsConfig.hasUser(new CaseInsensitiveString(userName), userRoleMatcher);
            }

            public String identifier(String ignore) {
                return Admin.GO_SYSTEM_ADMIN;
            }

            public boolean shouldEnable(String userName, String ignore) {
                List<Role> roles = new ArrayList<>();
                for (Role role : allRoles) {
                    if (role.hasMember(new CaseInsensitiveString(userName))) {
                        roles.add(role);
                    }
                }
                return ! adminsConfig.isAdminRole(roles);
            }
        }).get(0);
    }

    public static List<TriStateSelection> forAgentsEnvironmens(Set<EnvironmentConfig> environments, Agents agents) {
        return convert(environments, agents, new Assigner<EnvironmentConfig, AgentConfig>() {
            public boolean shouldAssociate(AgentConfig agent, EnvironmentConfig environment) {
                return environment.hasAgent(agent.getUuid());
            }

            public String identifier(EnvironmentConfig environment) {
                return CaseInsensitiveString.str(environment.name());
            }

            public boolean shouldEnable(AgentConfig agent, EnvironmentConfig environment) {
                return true;
            }
        });
    }

    static <T,V> List<TriStateSelection> convert(Set<T> assignables, List<V> assignees, Assigner<T,V> associator) {
        ArrayList<TriStateSelection> selections = new ArrayList<>();
        for (T t : assignables) {
            int count = 0;
            boolean enabled = true;
            for (V assignee : assignees) {
                if (associator.shouldAssociate(assignee, t)) {
                    count++;
                }
                enabled = enabled && associator.shouldEnable(assignee, t);
            }
            Action action = Action.remove;
            if (count > 0) {
                if (count == assignees.size()) {
                    action = Action.add;
                } else {
                    action = Action.nochange;
                }
            }
            selections.add(new TriStateSelection(associator.identifier(t), action, enabled));
        }
        Collections.sort(selections);
        return selections;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        TriStateSelection that = (TriStateSelection) o;

        if (action != null ? !action.equals(that.action) : that.action != null) {
            return false;
        }
        if (value != null ? !value.equals(that.value) : that.value != null) {
            return false;
        }

        return enabled == that.enabled;
    }

    @Override
    public int hashCode() {
        int result = value != null ? value.hashCode() : 0;
        result = 31 * result + (action != null ? action.hashCode() : 0);
        return result;
    }

    @Override public String toString() {
        return "TriStateSelection{" +
                "value='" + value + '\'' +
                ", action='" + action + '\'' +
                ", enabled='" + enabled + '\'' +
                '}';
    }

    public int compareTo(TriStateSelection other) {
        return comparator.compare(this.value,other.value);
    }

    static interface Assigner<T,V> {
        boolean shouldAssociate(V v, T t);
        String identifier(T t);
        boolean shouldEnable(V v, T t);
    }

    public static enum Action {
        add, remove, nochange
    }

}
