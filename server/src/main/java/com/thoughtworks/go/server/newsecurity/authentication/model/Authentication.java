/*
 * Copyright 2018 ThoughtWorks, Inc.
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

package com.thoughtworks.go.server.newsecurity.authentication.model;

import org.springframework.security.core.userdetails.UserDetails;

public class Authentication {
    private UserDetails user;
    private Object credentials;
    private String pluginId;
    private String authConfigId;

    public Authentication(UserDetails user, Object credentials, String pluginId, String authConfigId) {
        this.user = user;
        this.credentials = credentials;
        this.pluginId = pluginId;
        this.authConfigId = authConfigId;
    }

    public UserDetails getUser() {
        return user;
    }

    public Object getCredentials() {
        return credentials;
    }

    public String getPluginId() {
        return pluginId;
    }

    public String getAuthConfigId() {
        return authConfigId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Authentication)) return false;

        Authentication that = (Authentication) o;

        if (user != null ? !user.equals(that.user) : that.user != null) return false;
        if (credentials != null ? !credentials.equals(that.credentials) : that.credentials != null) return false;
        if (pluginId != null ? !pluginId.equals(that.pluginId) : that.pluginId != null) return false;
        return authConfigId != null ? authConfigId.equals(that.authConfigId) : that.authConfigId == null;
    }

    @Override
    public int hashCode() {
        int result = user != null ? user.hashCode() : 0;
        result = 31 * result + (credentials != null ? credentials.hashCode() : 0);
        result = 31 * result + (pluginId != null ? pluginId.hashCode() : 0);
        result = 31 * result + (authConfigId != null ? authConfigId.hashCode() : 0);
        return result;
    }
}
