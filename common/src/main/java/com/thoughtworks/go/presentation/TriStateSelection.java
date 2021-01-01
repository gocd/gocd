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
package com.thoughtworks.go.presentation;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.domain.config.Admin;
import com.thoughtworks.go.util.comparator.AlphaAsciiComparator;

import java.util.*;

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

    public static List<TriStateSelection> forAgentsResources(Set<ResourceConfig> resourceConfigs, Agents agents) {
        return convert(resourceConfigs, agents, new Assigner<ResourceConfig, Agent>() {
            @Override
            public boolean shouldAssociate(Agent agent, ResourceConfig resourceConfig) {
                return agent.getResourcesAsList().contains(resourceConfig.getName());
            }

            @Override
            public String identifier(ResourceConfig resourceConfig) {
                return resourceConfig.getName();
            }

            @Override
            public boolean shouldEnable(Agent agent, ResourceConfig resourceConfig) {
                return true;
            }
        });
    }

    public static List<TriStateSelection> forRoles(Set<Role> allRoles, List<String> users) {
        return convert(allRoles, users, new Assigner<Role, String>() {
            @Override
            public boolean shouldAssociate(String user, Role role) {
                return role.hasMember(new CaseInsensitiveString(user));
            }

            @Override
            public String identifier(Role role) {
                return CaseInsensitiveString.str(role.getName());
            }

            @Override
            public boolean shouldEnable(String user, Role role) {
                return true;
            }
        });
    }

    public static TriStateSelection forSystemAdmin(final AdminsConfig adminsConfig, final Set<Role> allRoles, final UserRoleMatcher userRoleMatcher, List<String> users) {
        return convert(new HashSet<>(Arrays.asList(Admin.GO_SYSTEM_ADMIN)), users, new Assigner<String, String>() {
            @Override
            public boolean shouldAssociate(String userName, String ignore) {
                return adminsConfig.hasUser(new CaseInsensitiveString(userName), userRoleMatcher);
            }

            @Override
            public String identifier(String ignore) {
                return Admin.GO_SYSTEM_ADMIN;
            }

            @Override
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

    @Override
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
