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
public class CRP4Material extends CRScmMaterial {

    public static final String TYPE_NAME = "p4";

    public static CRP4Material withEncryptedPassword(String name, String folder, boolean autoUpdate, boolean whitelist, List<String> filter, String serverAndPort, String userName, String encryptedPassword, boolean useTickets, String view) {
        CRP4Material crp4Material = new CRP4Material(name, folder, autoUpdate, serverAndPort, view, userName, null, useTickets, whitelist, filter);
        crp4Material.setEncryptedPassword(encryptedPassword);
        return crp4Material;
    }

    @SerializedName("port")
    @Expose
    private String port;
    @SerializedName("username")
    @Expose
    private String username;
    @SerializedName("password")
    @Expose
    private String password;
    @SerializedName("encrypted_password")
    @Expose
    private String encryptedPassword;
    @SerializedName("use_tickets")
    @Expose
    private boolean useTickets;
    @SerializedName("view")
    @Expose
    private String view;

    public CRP4Material(String materialName, String folder, boolean autoUpdate, String serverAndPort, String view, String userName, String password, boolean useTickets, boolean whitelist, List<String> filters) {
        super(TYPE_NAME, materialName, folder, autoUpdate, whitelist, filters);
        this.port = serverAndPort;
        this.username = userName;
        this.password = password;
        this.useTickets = useTickets;
        this.view = view;
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
        String location = getLocation(parentLocation);
        getCommonErrors(errors, location);
        errors.checkMissing(location, "port", port);
        errors.checkMissing(location, "view", view);
        validatePassword(errors, parentLocation);
    }

    @Override
    public String getLocation(String parent) {
        String myLocation = getLocation() == null ? parent : getLocation();
        String name = getName() == null ? "" : getName();
        String port = getPort() != null ? getPort() : "unknown";
        return String.format("%s; Perforce material %s Port: %s", myLocation, name, port);
    }
}
