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
package com.thoughtworks.go.plugin.configrepo.contract.material;


import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.thoughtworks.go.plugin.configrepo.contract.ErrorCollection;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class CRSvnMaterial extends CRScmMaterial {

    public static CRSvnMaterial withEncryptedPassword(String name, String destination, boolean autoUpdate, boolean whitelist, List<String> filter, String url, String userName, String encryptedPassword, boolean checkExternals) {
        CRSvnMaterial crSvnMaterial = new CRSvnMaterial(name, destination, autoUpdate, url, userName, null, checkExternals, whitelist, filter);
        crSvnMaterial.setEncryptedPassword(encryptedPassword);
        return crSvnMaterial;
    }

    public static final String TYPE_NAME = "svn";

    @SerializedName("url")
    @Expose
    private String url;
    @SerializedName("username")
    @Expose
    private String username;
    @SerializedName("password")
    @Expose
    private String password;
    @SerializedName("encrypted_password")
    @Expose
    private String encryptedPassword;
    @SerializedName("check_externals")
    @Expose
    private boolean checkExternals;

    public CRSvnMaterial() {
        type = TYPE_NAME;
    }

    public CRSvnMaterial(String materialName, String folder, boolean autoUpdate, String url, String userName, String password, boolean checkExternals, boolean whitelist, List<String> filters) {
        super(TYPE_NAME, materialName, folder, autoUpdate, whitelist, filters);
        this.url = url;
        this.username = userName;
        this.password = password;
        this.checkExternals = checkExternals;
    }

    public boolean hasEncryptedPassword() {
        return StringUtils.isNotBlank(encryptedPassword);
    }

    public boolean hasPlainTextPassword() {
        return StringUtils.isNotBlank(password);
    }

    @Override
    public String typeName() {
        return TYPE_NAME;
    }


    private void validatePassword(ErrorCollection errors, String location) {
        if (this.hasEncryptedPassword() && this.hasPlainTextPassword()) {
            errors.addError(location, "Svn material has both plain-text and encrypted passwords set. Please set only one password.");
        }
    }

    @Override
    public void getErrors(ErrorCollection errors, String parentLocation) {
        String location = this.getLocation(parentLocation);
        getCommonErrors(errors, location);
        errors.checkMissing(location, "url", url);
        validatePassword(errors, location);
    }

    @Override
    public String getLocation(String parent) {
        String myLocation = getLocation() == null ? parent : getLocation();
        String name = getName() == null ? "" : getName();
        String url = getUrl() != null ? getUrl() : "unknown";
        return String.format("%s; Svn material %s URL: %s", myLocation, name, url);
    }
}
