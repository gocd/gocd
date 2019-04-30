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

import java.util.Arrays;
import java.util.List;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class CRTfsMaterial extends CRScmMaterial {

    public static CRTfsMaterial withEncryptedPassword(String name, String directory, boolean autoUpdate,
                                                      boolean whitelist, List<String> filter, String url, String domain, String username,
                                                      String encrypted_password, String project) {
        return new CRTfsMaterial(name, directory, autoUpdate, url, username, null, encrypted_password, project, domain, whitelist, filter);
    }

    public static CRTfsMaterial withPlainPassword(String name, String directory, boolean autoUpdate,
                                                  boolean whitelist, List<String> filter, String url, String domain, String username,
                                                  String password, String project) {
        return new CRTfsMaterial(name, directory, autoUpdate, url, username, password, null, project, domain, whitelist, filter);
    }

    public static final String TYPE_NAME = "tfs";

    @SerializedName("url")
    @Expose
    private String url;
    @SerializedName("username")
    @Expose
    private String username;
    @SerializedName("domain")
    @Expose
    private String domain;
    @SerializedName("password")
    @Expose
    private String password;
    @SerializedName("encrypted_password")
    @Expose
    private String encryptedPassword;
    @SerializedName("project")
    @Expose
    private String project;

    public CRTfsMaterial(String materialName, String folder, boolean autoUpdate, String url, String userName, String password, String encryptedPassword, String projectPath, String domain, boolean whitelist, List<String> filters) {
        super(TYPE_NAME, materialName, folder, autoUpdate, whitelist, filters);
        this.url = url;
        this.username = userName;
        this.password = password;
        this.encryptedPassword = encryptedPassword;
        this.project = projectPath;
        this.domain = domain;
    }

    @Override
    public String typeName() {
        return TYPE_NAME;
    }

    public boolean hasEncryptedPassword() {
        return StringUtils.isNotBlank(encryptedPassword);
    }

    public boolean hasPlainTextPassword() {
        return StringUtils.isNotBlank(password);
    }

    private void validatePassword(ErrorCollection errors, String location) {
        if (this.hasEncryptedPassword() && this.hasPlainTextPassword()) {
            errors.addError(location, "Tfs material has both plain-text and encrypted passwords set. Please set only one password.");
        }
    }

    @Override
    public void getErrors(ErrorCollection errors, String parentLocation) {
        String location = this.getLocation(parentLocation);
        getCommonErrors(errors, location);
        errors.checkMissing(location, "url", url);
        errors.checkMissing(location, "username", username);
        errors.checkMissing(location, "project", project);
    }

    @Override
    public String getLocation(String parent) {
        String myLocation = getLocation() == null ? parent : getLocation();
        String name = getName() == null ? "" : getName();
        String url = getUrl() != null ? getUrl() : "unknown";
        return String.format("%s; Tfs material %s URL: %s", myLocation, name, url);
    }
}
