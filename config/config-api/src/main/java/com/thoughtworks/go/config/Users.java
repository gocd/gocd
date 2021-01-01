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
package com.thoughtworks.go.config;

import com.thoughtworks.go.domain.BaseCollection;
import com.thoughtworks.go.domain.ConfigErrors;

import java.util.Collection;

@ConfigTag("users")
@ConfigCollection(RoleUser.class)
public class Users extends BaseCollection<RoleUser> implements Validatable {
    private final ConfigErrors configErrors = new ConfigErrors();

    static Users users(RoleUser... users) {
        Users usersList = new Users();
        for (RoleUser user : users) {
            usersList.add(user);
        }
        return usersList;
    }


    static Users users(Collection<RoleUser> users) {
        Users usersList = new Users();
        for (RoleUser user : users) {
            usersList.add(user);
        }
        return usersList;
    }

    @Override
    public void validate(ValidationContext validationContext) {
    }

    @Override
    public ConfigErrors errors() {
        return configErrors;
    }

    @Override
    public void addError(String fieldName, String message) {
        configErrors.add(fieldName, message);
    }

}
