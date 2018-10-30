/*
 * Copyright 2017 ThoughtWorks, Inc.
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
package com.thoughtworks.go.plugin.access.configrepo.contract;

import com.thoughtworks.go.plugin.access.configrepo.ErrorCollection;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.Collection;

public class CRApproval extends CRBase {
    private CRApprovalCondition type;
    private Collection<String> users = new ArrayList<>();
    private Collection<String> roles = new ArrayList<>();

    public CRApproval()
    {
    }
    public CRApproval(CRApprovalCondition type)
    {
        this.type = type;
    }
    public CRApproval(CRApprovalCondition type, Collection<String> authorizedRoles, Collection<String> authorizedUsers) {
        this.type = type;
        this.users = authorizedUsers;
        this.roles = authorizedRoles;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        CRApproval that = (CRApproval)o;
        if(that == null)
            return  false;

        if (type != null ? !type.equals(that.type) : that.type != null) {
            return false;
        }

        if (users != null ? !CollectionUtils.isEqualCollection(this.users, that.users) : that.users != null) {
            return false;
        }

        if (roles != null ? !CollectionUtils.isEqualCollection(this.roles, that.roles) : that.roles != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = (type != null ? type.hashCode() : 0);
        result = 31 * result + (users != null ? users.size() : 0);
        result = 31 * result + (roles != null ? roles.size() : 0);
        return result;
    }

    public CRApprovalCondition getType()
    {
        return type;
    }

    public Collection<String> getAuthorizedUsers() {
        return users;
    }

    public void setAuthorizedUsers(Collection<String> authorizedUsers) {
        this.users = authorizedUsers;
    }

    public Collection<String> getAuthorizedRoles() {
        return roles;
    }

    public void setAuthorizedRoles(Collection<String> authorizedRoles) {
        this.roles = authorizedRoles;
    }

    public void addAuthorizedUser(String user) {
        this.users.add(user);
    }

    public void addAuthorizedRole(String role) {
        this.roles.add(role);
    }

    @Override
    public void getErrors(ErrorCollection errors, String parentLocation) {
        String location =this.getLocation(parentLocation);
        errors.checkMissing(location,"type",type);
    }

    @Override
    public String getLocation(String parent) {
        String myLocation = getLocation() == null ? parent : getLocation();
        return String.format("%s; Approval",myLocation);
    }

    public void setApprovalCondition(CRApprovalCondition condition) {
        type = condition;
    }
}
