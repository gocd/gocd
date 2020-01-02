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
package com.thoughtworks.go.plugin.configrepo.contract.material;


import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.thoughtworks.go.plugin.configrepo.contract.ErrorCollection;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public abstract class CRScmMaterial extends CRMaterial implements SourceCodeMaterial {
    @SerializedName("filter")
    @Expose
    protected CRFilter filter;
    @SerializedName("destination")
    @Expose
    protected String destination;
    @SerializedName("auto_update")
    @Expose
    protected boolean autoUpdate = true;
    @SerializedName("username")
    @Expose
    protected String username;
    @SerializedName("password")
    @Expose
    protected String password;
    @SerializedName("encrypted_password")
    @Expose
    protected String encryptedPassword;

    public CRScmMaterial() {
    }

    public CRScmMaterial(String type, String materialName, String folder, boolean autoUpdate, boolean whitelist, String username, List<String> filter) {
        super(type, materialName);
        this.destination = folder;
        this.filter = new CRFilter(filter, whitelist);
        this.autoUpdate = autoUpdate;
        this.username = username;
    }

    public List<String> getFilterList() {
        if (filter == null)
            return null;
        return filter.getList();
    }

    public boolean isWhitelist() {
        if (this.filter != null)
            return this.filter.isWhitelist();
        return false;
    }

    @Override
    public void getErrors(ErrorCollection errors, String parentLocation) {
        validatePassword(errors, parentLocation);
    }

    protected void getCommonErrors(ErrorCollection errors, String parentLocation) {
        String location = getLocation(parentLocation);
        if (this.filter != null)
            this.filter.getErrors(errors, location);
    }

    public void setWhitelistNoCheck(String... filters) { //for tests
        this.filter.setWhitelistNoCheck(Arrays.asList(filters));
    }

    public boolean hasEncryptedPassword() {
        return StringUtils.isNotBlank(encryptedPassword);
    }

    public boolean hasPlainTextPassword() {
        return StringUtils.isNotBlank(password);
    }

    protected void validatePassword(ErrorCollection errors, String location) {
        if (this.hasEncryptedPassword() && this.hasPlainTextPassword()) {
            errors.addError(location, String.format("%s material has both plain-text and encrypted passwords set. Please set only one password.", typeName()));
        }
    }
}
