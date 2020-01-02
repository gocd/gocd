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

import com.thoughtworks.go.config.builder.ConfigurationPropertyBuilder;
import com.thoughtworks.go.config.policy.Policy;
import com.thoughtworks.go.config.validation.NameTypeValidator;
import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.domain.config.Configuration;
import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.plugin.access.authorization.AuthorizationMetadataStore;
import com.thoughtworks.go.plugin.domain.authorization.AuthorizationPluginInfo;

import java.util.List;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

@ConfigTag("pluginRole")
@ConfigCollection(value = ConfigurationProperty.class)
public class PluginRoleConfig extends Configuration implements Role {

    private final ConfigErrors configErrors = new ConfigErrors();

    @ConfigAttribute(value = "name", optional = false)
    protected CaseInsensitiveString name;

    @ConfigAttribute(value = "authConfigId", optional = false)
    private String authConfigId;

    @ConfigSubtag
    private Policy policy = new Policy();

    public PluginRoleConfig() {
    }

    public PluginRoleConfig(String name, String authConfigId, ConfigurationProperty... properties) {
        super(properties);
        this.name = new CaseInsensitiveString(name);
        this.authConfigId = authConfigId;
    }

    public String getAuthConfigId() {
        return authConfigId;
    }

    public void setAuthConfigId(String authConfigId) {
        this.authConfigId = authConfigId;
    }

    @Override
    public void validate(ValidationContext validationContext) {
        Role.super.validate(validationContext);

        if (!new NameTypeValidator().isNameValid(authConfigId)) {
            configErrors.add("authConfigId", NameTypeValidator.errorMessage("plugin role authConfigId", authConfigId));
        }

        if (isNotBlank(authConfigId)) {
            SecurityAuthConfig securityAuthConfig = validationContext.getServerSecurityConfig().securityAuthConfigs().find(authConfigId);

            if (securityAuthConfig == null) {
                addError("authConfigId", String.format("No such security auth configuration present for id: `%s`", getAuthConfigId()));
            }
        }
    }

    @Override
    public void addUser(RoleUser user) {
        throw new UnsupportedOperationException("PluginRoleConfig does not support adding users, should be added through PluginRoleService");
    }

    @Override
    public void removeUser(RoleUser roleUser) {
        throw new UnsupportedOperationException("PluginRoleConfig does not support removing users, should be removed through PluginRoleService");
    }

    @Override
    public ConfigErrors errors() {
        return configErrors;
    }

    @Override
    public void addError(String fieldName, String message) {
        configErrors.add(fieldName, message);
    }

    @Override
    public CaseInsensitiveString getName() {
        return this.name;
    }

    @Override
    public void setName(CaseInsensitiveString name) {
        this.name = name;
    }

    @Override
    public List<RoleUser> getUsers() {
        return PluginRoleUsersStore.instance().usersInRole(this);
    }

    @Override
    public Policy getPolicy() {
        return policy;
    }

    @Override
    public void setPolicy(Policy policy) {
        this.policy = policy;
    }

    @Override
    public boolean hasErrors() {
        return super.hasErrors() || !configErrors.isEmpty();
    }

    public void addConfigurations(List<ConfigurationProperty> configurations) {
        ConfigurationPropertyBuilder builder = new ConfigurationPropertyBuilder();
        for (ConfigurationProperty property : configurations) {
            add(builder.create(property.getConfigKeyName(),
                    property.getConfigValue(),
                    null,
                    false));
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PluginRoleConfig)) return false;
        if (!super.equals(o)) return false;

        PluginRoleConfig that = (PluginRoleConfig) o;

        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        return authConfigId != null ? authConfigId.equals(that.authConfigId) : that.authConfigId == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (authConfigId != null ? authConfigId.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "PluginRoleConfig{" +
                "name=" + name +
                ", authConfigId='" + authConfigId + '\'' +
                '}';
    }

    @Override
    public void encryptSecureProperties(CruiseConfig preprocessedConfig) {
        if (authConfigId != null) {
            SecurityAuthConfig authConfig = preprocessedConfig.server().security().securityAuthConfigs().find(this.authConfigId);
            encryptSecureConfigurations(authConfig);
        }
    }

    private void encryptSecureConfigurations(SecurityAuthConfig authConfig) {
        if (authConfig != null && hasPluginInfo(authConfig)) {
            for (ConfigurationProperty configuration : this) {
                configuration.handleSecureValueConfiguration(isSecure(configuration.getConfigKeyName(), authConfig));
            }
        }
    }

    private boolean isSecure(String configKeyName, SecurityAuthConfig authConfig) {
        AuthorizationPluginInfo pluginInfo = getPluginInfo(authConfig);

        return pluginInfo != null
                && pluginInfo.getRoleSettings() != null
                && pluginInfo.getRoleSettings().getConfiguration(configKeyName) != null
                && pluginInfo.getRoleSettings().getConfiguration(configKeyName).isSecure();
    }

    private boolean hasPluginInfo(SecurityAuthConfig authConfig) {
        return getPluginInfo(authConfig) != null;
    }

    private AuthorizationPluginInfo getPluginInfo(SecurityAuthConfig authConfig) {
        return AuthorizationMetadataStore.instance().getPluginInfo(authConfig.getPluginId());
    }
}
