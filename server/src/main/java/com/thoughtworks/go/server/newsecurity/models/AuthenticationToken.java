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
package com.thoughtworks.go.server.newsecurity.models;

import com.thoughtworks.go.server.security.userdetail.GoUserPrinciple;
import com.thoughtworks.go.util.Clock;
import com.thoughtworks.go.util.SystemEnvironment;

public class AuthenticationToken<T extends Credentials> {
    private final GoUserPrinciple user;
    private final T credentials;
    private final String authConfigId;
    private final long authenticatedAt;
    private final String pluginId;
    private boolean invalidated;

    public AuthenticationToken(GoUserPrinciple user,
                               T credentials,
                               String pluginId,
                               long authenticatedAt,
                               String authConfigId) {
        this.user = user;
        this.credentials = credentials;
        this.authConfigId = authConfigId;
        this.invalidated = false;
        this.pluginId = pluginId;
        this.authenticatedAt = authenticatedAt;
    }

    public GoUserPrinciple getUser() {
        return user;
    }

    public T getCredentials() {
        return credentials;
    }

    public boolean isAuthenticated(Clock clock, SystemEnvironment systemEnvironment) {
        return !(invalidated || isExpired(clock, systemEnvironment));
    }

    public String getPluginId() {
        return pluginId;
    }

    public String getAuthConfigId() {
        return authConfigId;
    }

    public void invalidate() {
        this.invalidated = true;
    }

    public boolean isAnonymousToken() {
        return credentials instanceof AnonymousCredential;
    }

    public boolean isUsernamePasswordToken() {
        return credentials instanceof UsernamePassword;
    }

    public boolean isAccessTokenCredentials() {
        return credentials instanceof AccessTokenCredential;
    }

    private boolean isExpired(Clock clock, SystemEnvironment systemEnvironment) {
        return systemEnvironment.isReAuthenticationEnabled() &&
                (clock.currentTimeMillis() - authenticatedAt) > systemEnvironment.getReAuthenticationTimeInterval();
    }

    @Override
    public String toString() {
        return "AuthenticationToken{" +
                "user=" + user +
                ", authConfigId='" + authConfigId + '\'' +
                ", authenticatedAt=" + authenticatedAt +
                ", pluginId='" + pluginId + '\'' +
                ", invalidated=" + invalidated +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AuthenticationToken)) return false;

        AuthenticationToken<?> that = (AuthenticationToken<?>) o;

        if (authenticatedAt != that.authenticatedAt) return false;
        if (invalidated != that.invalidated) return false;
        if (user != null ? !user.equals(that.user) : that.user != null) return false;
        if (credentials != null ? !credentials.equals(that.credentials) : that.credentials != null) return false;
        if (authConfigId != null ? !authConfigId.equals(that.authConfigId) : that.authConfigId != null) return false;
        return pluginId != null ? pluginId.equals(that.pluginId) : that.pluginId == null;
    }

    @Override
    public int hashCode() {
        int result = user != null ? user.hashCode() : 0;
        result = 31 * result + (credentials != null ? credentials.hashCode() : 0);
        result = 31 * result + (authConfigId != null ? authConfigId.hashCode() : 0);
        result = 31 * result + (int) (authenticatedAt ^ (authenticatedAt >>> 32));
        result = 31 * result + (invalidated ? 1 : 0);
        result = 31 * result + (pluginId != null ? pluginId.hashCode() : 0);
        return result;
    }
}
