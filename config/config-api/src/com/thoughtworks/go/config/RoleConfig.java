/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
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
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.config;
    
import com.thoughtworks.go.domain.ConfigErrors;

import java.util.Collection;

@ConfigTag("role")
public class RoleConfig implements Role {

    private final ConfigErrors configErrors = new ConfigErrors();

    @ConfigAttribute(value = "name", optional = false)
    protected CaseInsensitiveString name;

    @ConfigSubtag
    private Users users = new Users();

    public RoleConfig() {
        this(null, new Users());
    }

    public RoleConfig(CaseInsensitiveString name, RoleUser... users) {
        this(name, Users.users(users));
    }

    public RoleConfig(CaseInsensitiveString name, Users users) {
        this.name = name;
        doSetUsers(new Users());
        for (RoleUser user : users) {
            addUser(user);
        }
    }

    @Override
    public Collection<RoleUser> doGetUsers() {
        return users;
    }

    @Override
    public void doSetUsers(Collection<RoleUser> users) {
        this.users = new Users();

        for (RoleUser user : users) {
            addUser(user);
        }
    }

    public ConfigErrors errors() {
        return configErrors;
    }

    public void addError(String fieldName, String message) {
        configErrors.add(fieldName, message);
    }

    @Override
    public CaseInsensitiveString getName() {
        return name;
    }

    @Override
    public void setName(CaseInsensitiveString name) {
        this.name = name;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RoleConfig that = (RoleConfig) o;

        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        return users != null ? users.equals(that.users) : that.users == null;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (users != null ? users.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "RoleConfig{" +
                "name=" + name +
                ", users=" + users +
                '}';
    }
}
