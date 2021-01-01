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
package com.thoughtworks.go.plugin.access.configrepo.v1.messages;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.thoughtworks.go.plugin.configrepo.contract.CRConfigurationProperty;

import java.util.ArrayList;
import java.util.Collection;

public class ParseDirectoryMessage {
    @Expose
    @SerializedName("directory")
    private String directory;
    @Expose
    @SerializedName("configurations")
    private Collection<CRConfigurationProperty> configurations;

    public ParseDirectoryMessage(String destinationFolder) {
        this.directory = destinationFolder;
        this.configurations = new ArrayList<>();
    }
    public void addConfiguration(String name,String value,String encryptedValue)
    {
        configurations.add(new CRConfigurationProperty(name,value,encryptedValue));
    }

    public String getDirectory() {
        return directory;
    }

    public void setDirectory(String directory) {
        this.directory = directory;
    }

    public Collection<CRConfigurationProperty> getConfigurations() {
        return configurations;
    }

    public void setConfigurations(Collection<CRConfigurationProperty> configurations) {
        this.configurations = configurations;
    }
}
