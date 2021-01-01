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
package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.listener.PluginRoleChangeListener;
import com.thoughtworks.go.listener.ConfigChangedListener;
import com.thoughtworks.go.plugin.infra.PluginChangeListener;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class PluginRoleService implements ConfigChangedListener, PluginChangeListener {
    private final GoConfigService goConfigService;
    private final PluginRoleUsersStore pluginRoleUsersStore = PluginRoleUsersStore.instance();
    private final Set<PluginRoleChangeListener> listeners = new HashSet<>();

    @Autowired
    public PluginRoleService(GoConfigService goConfigService, PluginManager pluginManager) {
        this.goConfigService = goConfigService;
        pluginManager.addPluginChangeListener(this);
    }

    public void updatePluginRoles(String pluginId, String username, List<CaseInsensitiveString> pluginRolesName) {
        pluginRoleUsersStore.revokeAllRolesFor(username);

        Map<CaseInsensitiveString, PluginRoleConfig> pluginRoles = getPluginRoles(pluginId);
        for (CaseInsensitiveString pluginRoleName : pluginRolesName) {
            PluginRoleConfig pluginRoleConfig = pluginRoles.get(pluginRoleName);

            if (pluginRoleConfig != null) {
                pluginRoleUsersStore.assignRole(username, pluginRoleConfig);
            }
        }
    }

    public void register(PluginRoleChangeListener listener) {
        listeners.add(listener);
    }

    private void notifyListeners() {
        for (PluginRoleChangeListener listener : listeners) {
            listener.onPluginRoleChange();
        }
    }

    public void invalidateRolesFor(String pluginId) {
        List<PluginRoleConfig> pluginRoles = goConfigService.security().getPluginRoles(pluginId);

        if (!pluginRoles.isEmpty()) {
            pluginRoleUsersStore.remove(pluginRoles);
            notifyListeners();
        }
    }

    public List<RoleUser> usersForPluginRole(String roleName) {
        return pluginRoleUsersStore.usersInRole(pluginRole(roleName));
    }

    public void revokeAllRolesFor(String username) {
        pluginRoleUsersStore.revokeAllRolesFor(username);
    }

    @Override
    public void onConfigChange(CruiseConfig newCruiseConfig) {
        List<PluginRoleConfig> pluginRolesAfterConfigUpdate = newCruiseConfig.server().security().getRoles().getPluginRoleConfigs();

        pluginRoleUsersStore.removePluginRolesNotIn(pluginRolesAfterConfigUpdate);
    }

    private PluginRoleConfig pluginRole(String roleName) {
        return goConfigService.security().getRoles().findPluginRoleByName(new CaseInsensitiveString(roleName));
    }

    private Map<CaseInsensitiveString, PluginRoleConfig> getPluginRoles(String pluginId) {
        Map<CaseInsensitiveString, PluginRoleConfig> result = new HashMap<>();

        List<PluginRoleConfig> pluginRoles = goConfigService.security().getPluginRoles(pluginId);

        for (PluginRoleConfig pluginRole : pluginRoles) {
            result.put(pluginRole.getName(), pluginRole);
        }
        return result;
    }

    @Override
    public void pluginLoaded(GoPluginDescriptor pluginDescriptor) {
//        do nothing
    }

    @Override
    public void pluginUnLoaded(GoPluginDescriptor pluginDescriptor) {
        invalidateRolesFor(pluginDescriptor.id());
    }
}
