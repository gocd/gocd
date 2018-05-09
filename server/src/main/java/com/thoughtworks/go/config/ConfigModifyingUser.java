/*
 * Copyright 2016 ThoughtWorks, Inc.
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
 */

package com.thoughtworks.go.config;

import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.util.UserHelper;

/**
 * @understands who modified config for a particular update command
 */
public class ConfigModifyingUser {
    private final String userName;

    public ConfigModifyingUser() {
        this(CaseInsensitiveString.str(UserHelper.getUserName().getUsername()));
    }

    public ConfigModifyingUser(String userName) {
        this.userName = userName;
    }

    public ConfigModifyingUser(Username username) {
        this(username.getUsername().toString());
    }

    public String getUserName() {
        return userName;
    }
}
