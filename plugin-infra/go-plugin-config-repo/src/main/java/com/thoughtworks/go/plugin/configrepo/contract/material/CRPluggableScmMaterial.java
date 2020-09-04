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
import com.thoughtworks.go.plugin.configrepo.contract.CRConfigurationProperty;
import com.thoughtworks.go.plugin.configrepo.contract.CRPluginConfiguration;
import com.thoughtworks.go.plugin.configrepo.contract.ErrorCollection;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class CRPluggableScmMaterial extends CRMaterial implements SourceCodeMaterial {
    public static final String TYPE_NAME = "plugin";

    @SerializedName("scm_id")
    @Expose
    private String scmId;
    @SerializedName("destination")
    @Expose
    protected String destination;
    @SerializedName("filter")
    @Expose
    private CRFilter filter;

    @SerializedName("plugin_configuration")
    @Expose
    private CRPluginConfiguration pluginConfiguration;
    @SerializedName("configuration")
    @Expose
    private Collection<CRConfigurationProperty> configuration = new ArrayList<>();

    public CRPluggableScmMaterial() {
        this(null, null, null, null, false);
    }

    public CRPluggableScmMaterial(String name, String scmId, String directory, List<String> filter, boolean isFilterInverted) {
        super(TYPE_NAME, name);
        this.scmId = scmId;
        this.destination = directory;
        this.filter = new CRFilter(filter, isFilterInverted);
    }

    @Override
    public String typeName() {
        return TYPE_NAME;
    }

    public List<String> getFilterList() {
        if (filter == null)
            return null;
        return filter.getList();
    }

    public void setFilterIgnore(List<String> filter) {
        this.filter.setIgnore(filter);
    }

    public boolean isWhitelist() {
        if (this.filter != null)
            return this.filter.isIncluded();
        return false;
    }

    @Override
    public void getErrors(ErrorCollection errors, String parentLocation) {
        String location = getLocation(parentLocation);
        if (getScmId() == null && pluginConfiguration == null) {
            errors.addError(location, "Either the scm_id or the plugin_configuration must be set");
        }
    }

    @Override
    public String getLocation(String parent) {
        String myLocation = getLocation() == null ? parent : getLocation();
        String name = getName() == null ? "" : getName();
        String url = getScmId() != null ? getScmId() : "unknown";
        return String.format("%s; Pluggable SCM material %s ID: %s", myLocation, name, url);
    }
}
