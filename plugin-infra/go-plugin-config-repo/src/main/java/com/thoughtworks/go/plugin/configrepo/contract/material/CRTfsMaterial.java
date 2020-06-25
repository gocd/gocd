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

import java.util.List;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class CRTfsMaterial extends CRScmMaterial {

    public static final String TYPE_NAME = "tfs";

    @SerializedName("url")
    @Expose
    private String url;
    @SerializedName("domain")
    @Expose
    private String domain;
    @SerializedName("project")
    @Expose
    private String project;

    public CRTfsMaterial(String materialName, String folder, boolean autoUpdate, boolean isFilterInverted, String username, List<String> filters, String url, String projectPath, String domain) {
        super(TYPE_NAME, materialName, folder, autoUpdate, isFilterInverted, username, filters);
        this.url = url;
        this.project = projectPath;
        this.domain = domain;
    }

    @Override
    public String typeName() {
        return TYPE_NAME;
    }

    @Override
    public void getErrors(ErrorCollection errors, String parentLocation) {
        String location = this.getLocation(parentLocation);

        super.getErrors(errors, parentLocation);

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
