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
package com.thoughtworks.go.plugin.configrepo.contract.tasks;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.thoughtworks.go.plugin.configrepo.contract.CRConfigurationProperty;
import com.thoughtworks.go.plugin.configrepo.contract.CRPluginConfiguration;
import com.thoughtworks.go.plugin.configrepo.contract.ErrorCollection;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class CRPluggableTask extends CRTask {
    public static final String TYPE_NAME = "plugin";

    @SerializedName("plugin_configuration")
    @Expose
    private CRPluginConfiguration pluginConfiguration;
    @SerializedName("configuration")
    @Expose
    private Collection<CRConfigurationProperty> configuration;

    public CRPluggableTask() {
        super(TYPE_NAME);
    }

    public CRPluggableTask(CRPluginConfiguration pluginConfiguration,
                           CRConfigurationProperty... properties) {
        super(TYPE_NAME);
        this.pluginConfiguration = pluginConfiguration;
        this.configuration = Arrays.asList(properties);
    }

    public CRPluggableTask(CRPluginConfiguration pluginConfiguration,
                           List<CRConfigurationProperty> properties) {
        super(TYPE_NAME);
        this.pluginConfiguration = pluginConfiguration;
        this.configuration = properties;
    }

    public CRPluggableTask(String id, String version,
                           CRConfigurationProperty... properties) {
        super(TYPE_NAME);
        this.pluginConfiguration = new CRPluginConfiguration(id, version);
        this.configuration = Arrays.asList(properties);
    }

    public CRPluggableTask(CRRunIf runIf, CRTask onCancel,
                           CRPluginConfiguration pluginConfiguration, Collection<CRConfigurationProperty> configuration) {
        super(runIf, onCancel);
        this.pluginConfiguration = pluginConfiguration;
        this.configuration = configuration;
    }

    @Override
    public void getErrors(ErrorCollection errors, String parentLocation) {
        String location = getLocation(parentLocation);
        errors.checkMissing(location, "plugin_configuration", pluginConfiguration);

        if (this.pluginConfiguration != null)
            this.pluginConfiguration.getErrors(errors, location);

        if (this.configuration != null) {
            for (CRConfigurationProperty p : configuration) {
                p.getErrors(errors, location);
            }
        }
        validateKeyUniqueness(errors, location);
    }

    @Override
    public String getLocation(String parent) {
        String myLocation = getLocation() == null ? parent : getLocation();
        return String.format("%s; pluggable task", myLocation);
    }

    private void validateKeyUniqueness(ErrorCollection errors, String location) {
        if (this.configuration == null)
            return;
        HashSet<String> keys = new HashSet<>();
        for (CRConfigurationProperty property1 : this.configuration) {
            String key = property1.getKey();
            if (keys.contains(key))
                errors.addError(location, String.format(
                        "Configuration property %s is defined more than once", property1));
            else
                keys.add(key);
        }
    }
}
