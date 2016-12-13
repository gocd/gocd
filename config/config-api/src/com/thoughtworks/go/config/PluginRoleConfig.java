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

package com.thoughtworks.go.config;

import com.thoughtworks.go.config.validation.NameTypeValidator;
import com.thoughtworks.go.domain.config.ConfigurationProperty;

import java.util.Collection;

@ConfigTag("pluginRole")
@ConfigCollection(value = ConfigurationProperty.class)
public class PluginRoleConfig extends AbstractRole {

    @ConfigAttribute(value = "pluginId", optional = false)
    private String pluginId;

    @ConfigAttribute(value = "authConfigId", optional = false)
    private String authConfigId;

    private Users users = new Users();

    public PluginRoleConfig() {
    }

    public PluginRoleConfig(String name, String pluginId, String authConfigId) {
        this.name = new CaseInsensitiveString(name);
        this.pluginId = pluginId;
        this.authConfigId = authConfigId;
    }

    public String getPluginId() {
        return pluginId;
    }

    public String getAuthConfigId() {
        return authConfigId;
    }

    @Override
    public void validate(ValidationContext validationContext) {
        super.validate(validationContext);
        if (!new NameTypeValidator().isNameValid(pluginId)) {
            configErrors.add("pluginId", NameTypeValidator.errorMessage("foo pluginId", pluginId));
        }
        if (!new NameTypeValidator().isNameValid(authConfigId)) {
            configErrors.add("authConfigId", NameTypeValidator.errorMessage("foo authConfigId", authConfigId));
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        PluginRoleConfig that = (PluginRoleConfig) o;

        if (pluginId != null ? !pluginId.equals(that.pluginId) : that.pluginId != null) return false;
        return authConfigId != null ? authConfigId.equals(that.authConfigId) : that.authConfigId == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (pluginId != null ? pluginId.hashCode() : 0);
        result = 31 * result + (authConfigId != null ? authConfigId.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "PluginRoleConfig{" +
                "name=" + name +
                ", pluginId='" + pluginId + '\'' +
                ", authConfigId='" + authConfigId + '\'' +
                '}';
    }

    @Override
    public Collection<RoleUser> doGetUsers() {
        return users;
    }

    @Override
    public void doSetUsers(Collection<RoleUser> users) {
        this.users = Users.users(users);
    }

}
