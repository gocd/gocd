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

import java.util.Collection;

@ConfigTag("role")
public class RoleConfig extends AbstractRole {

    @ConfigSubtag
    private Users users = new Users();

    public RoleConfig() {
        this(null, new Users());
    }

    public RoleConfig(CaseInsensitiveString name, RoleUser... users) {
        super(name, users);
    }

    public RoleConfig(CaseInsensitiveString name, Users users) {
        super(name, users);
    }

    @Override
    public Collection<RoleUser> doGetUsers() {
        return users;
    }

    @Override
    public void doSetUsers(Collection<RoleUser> users) {
        this.users = Users.users(users);
    }

    @Override
    public String toString() {
        return "RoleConfig{" +
                "name=" + name +
                ", users=" + users +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        RoleConfig that = (RoleConfig) o;

        return users != null ? users.equals(that.users) : that.users == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (users != null ? users.hashCode() : 0);
        return result;
    }


}
