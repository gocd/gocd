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

import com.thoughtworks.go.plugin.configrepo.contract.CRConfigurationProperty;
import com.thoughtworks.go.plugin.configrepo.contract.CRPluginConfiguration;
import com.thoughtworks.go.plugin.configrepo.contract.ErrorCollection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;


public class CRPluggableScmMaterial extends CRMaterial implements SourceCodeMaterial {
    public static final String TYPE_NAME = "plugin";

    private String scm_id;
    protected String destination;
    private CRFilter filter;

    private CRPluginConfiguration plugin_configuration;
    private Collection<CRConfigurationProperty> configuration = new ArrayList<>();

    public CRPluggableScmMaterial(){
        type = TYPE_NAME;
    }
    public CRPluggableScmMaterial(String materialName,String scmId,String folder,String... filters)
    {
        super(TYPE_NAME,materialName);
        this.scm_id = scmId;
        this.destination = folder;
        this.filter = new CRFilter(Arrays.asList(filters),false/*not supported in 16.6*/);
    }
    public CRPluggableScmMaterial(String name, String scmId, String directory, List<String> filter) {
        super(TYPE_NAME,name);
        this.scm_id = scmId;
        this.destination = directory;
        this.filter = new CRFilter(filter,false);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        CRPluggableScmMaterial that = (CRPluggableScmMaterial)o;
        if(that == null)
            return  false;

        if(!super.equals(that))
            return false;

        if (scm_id != null ? !scm_id.equals(that.scm_id) : that.scm_id != null) {
            return false;
        }
        if (destination != null ? !destination.equals(that.destination) : that.destination != null) {
            return false;
        }
        if (filter != null ? !filter.equals(that.filter) : that.filter != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = (this.getName() != null ? this.getName().hashCode() : 0);
        result = 31 * result + (scm_id != null ? scm_id.hashCode() : 0);
        result = 31 * result + (destination != null ? destination.hashCode() : 0);
        result = 31 * result + (filter != null ? filter.hashCode() : 0);
        return result;
    }

    @Override
    public String typeName() {
        return TYPE_NAME;
    }

    public String getScmId() {
        return scm_id;
    }

    public void setScmId(String scmId) {
        this.scm_id = scmId;
    }

    public List<String> getFilterList() {
        if(filter == null)
            return null;
        return filter.getList();
    }

    public void setFilterIgnore(List<String> filter) {
        this.filter.setIgnore(filter);
    }

    public String getDirectory() {
        return destination;
    }

    @Override
    public void getErrors(ErrorCollection errors, String parentLocation) {
        String location = getLocation(parentLocation);
        if (getScmId() == null && plugin_configuration == null) {
            errors.addError(location, "Either the scm_id or the plugin_configuration must be set");
        }
    }

    @Override
    public String getLocation(String parent) {
        String myLocation = getLocation() == null ? parent : getLocation();
        String name = getName() == null ? "" : getName();
        String url = getScmId() != null ? getScmId() : "unknown";
        return String.format("%s; Pluggable SCM material %s ID: %s",myLocation,name,url);
    }

    @Override
    public String getDestination() {
        return destination;
    }

    public CRPluginConfiguration getPluginConfiguration() {
        return plugin_configuration;
    }

    public void setPluginConfiguration(CRPluginConfiguration pluginConfiguration) {
        this.plugin_configuration = pluginConfiguration;
    }

    public Collection<CRConfigurationProperty> getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Collection<CRConfigurationProperty> configuration) {
        this.configuration = configuration;
    }
}
