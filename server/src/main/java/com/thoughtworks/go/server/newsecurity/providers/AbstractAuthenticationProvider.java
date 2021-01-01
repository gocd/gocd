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
package com.thoughtworks.go.server.newsecurity.providers;

import com.thoughtworks.go.plugin.domain.authorization.User;
import com.thoughtworks.go.server.newsecurity.models.AuthenticationToken;
import com.thoughtworks.go.server.newsecurity.models.Credentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

public abstract class AbstractAuthenticationProvider<T extends Credentials> {
    protected final Logger LOGGER = LoggerFactory.getLogger(getClass());

    protected com.thoughtworks.go.domain.User toDomainUser(User user) {
        return new com.thoughtworks.go.domain.User(user.getUsername(), user.getDisplayName(), user.getEmailId());
    }

    protected User ensureDisplayNamePresent(User user) {
        if (user == null) {
            return null;
        }

        if (isNotBlank(user.getDisplayName())) {
            return user;
        }

        return new User(user.getUsername(), user.getUsername(), user.getEmailId());
    }

    public abstract AuthenticationToken<T> reauthenticate(AuthenticationToken<T> authenticationToken);

    public abstract AuthenticationToken<T> authenticate(T credentials, String pluginId);
}
