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
package com.thoughtworks.go.config;

import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.plugin.access.authorization.AuthorizationMetadataStore;
import com.thoughtworks.go.plugin.domain.authorization.AuthorizationPluginInfo;

import java.util.Collection;

@ConfigTag("authConfig")
@ConfigCollection(value = ConfigurationProperty.class)
public class SecurityAuthConfig extends PluginProfile {
    @ConfigAttribute(value = "allowOnlyKnownUsersToLogin")
    protected boolean allowOnlyKnownUsersToLogin = false;

    public SecurityAuthConfig() {
        super();
    }

    public SecurityAuthConfig(String id, String pluginId, ConfigurationProperty... props) {
        super(id, pluginId, props);
    }

    public SecurityAuthConfig(String id, String pluginId, Collection<ConfigurationProperty> configProperties) {
        this(id, pluginId, configProperties.toArray(new ConfigurationProperty[0]));
    }

    @Override
    protected String getObjectDescription() {
        return "Security authorization configuration";
    }

    @Override
    protected boolean isSecure(String key) {
        AuthorizationPluginInfo pluginInfo = this.metadataStore().getPluginInfo(getPluginId());

        if (pluginInfo == null
                || pluginInfo.getAuthConfigSettings() == null
                || pluginInfo.getAuthConfigSettings().getConfiguration(key) == null) {
            return false;
        }

        return pluginInfo.getAuthConfigSettings().getConfiguration(key).isSecure();
    }

    @Override
    protected boolean hasPluginInfo() {
        return this.metadataStore().getPluginInfo(getPluginId()) != null;
    }

    public boolean hasRole(PluginRoleConfig role) {
        return role.getAuthConfigId().equals(id);
    }

    public Boolean isOnlyKnownUserAllowedToLogin() {
        return allowOnlyKnownUsersToLogin;
    }

    public void setAllowOnlyKnownUsersToLogin(boolean allowOnlyKnownUsersToLogin) {
        this.allowOnlyKnownUsersToLogin = allowOnlyKnownUsersToLogin;
    }

    private AuthorizationMetadataStore metadataStore() {
        return AuthorizationMetadataStore.instance();
    }
}
