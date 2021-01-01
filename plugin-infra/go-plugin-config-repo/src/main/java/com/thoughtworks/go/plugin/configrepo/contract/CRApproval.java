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
package com.thoughtworks.go.plugin.configrepo.contract;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Collection;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class CRApproval extends CRBase {
    @SerializedName("type")
    @Expose
    private CRApprovalCondition type;
    @SerializedName("users")
    @Expose
    private Collection<String> users = new ArrayList<>();
    @SerializedName("roles")
    @Expose
    private Collection<String> roles = new ArrayList<>();

    @SerializedName("allow_only_on_success")
    @Expose
    private boolean allowOnlyOnSuccess = false;

    public CRApproval() {
    }

    public CRApproval(CRApprovalCondition type) {
        this.type = type;
    }

    public void addAuthorizedUser(String user) {
        this.users.add(user);
    }

    public void addAuthorizedRole(String role) {
        this.roles.add(role);
    }

    @Override
    public void getErrors(ErrorCollection errors, String parentLocation) {
        String location = this.getLocation(parentLocation);
        errors.checkMissing(location, "type", type);
    }

    @Override
    public String getLocation(String parent) {
        String myLocation = getLocation() == null ? parent : getLocation();
        return String.format("%s; Approval", myLocation);
    }

    public void setApprovalCondition(CRApprovalCondition condition) {
        type = condition;
    }
}
