/*
 * Copyright 2015 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.thoughtworks.go.domain.config;

import java.io.Serializable;
import java.util.Map;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.thoughtworks.go.config.ConfigAttribute;
import com.thoughtworks.go.config.ConfigTag;
import com.thoughtworks.go.config.Validatable;
import com.thoughtworks.go.config.ValidationContext;
import com.thoughtworks.go.domain.ConfigErrors;

@ConfigTag("pluginConfiguration")
public class PluginConfiguration implements Serializable, Validatable {

    @Expose
    @SerializedName("id")
    @ConfigAttribute(value = "id", allowNull = false)
    private String id;
    @Expose
    @SerializedName("version")
    @ConfigAttribute(value = "version", allowNull = false)
    private String version;

    public static final String ID = "id";

    private ConfigErrors errors = new ConfigErrors();

    public PluginConfiguration() {
    }

    public PluginConfiguration(String id, String version) {
        this.id = id;
        this.version = version;
    }

    public String getId() {
        return id;
    }

    public String getVersion() {
        return version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        PluginConfiguration that = (PluginConfiguration) o;

        if (id != null ? !id.equals(that.id) : that.id != null) {
            return false;
        }
        if (version != null ? !version.equals(that.version) : that.version != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (version != null ? version.hashCode() : 0);
        return result;
    }

    public void setConfigAttributes(Object attributes) {
        Map attributesMap = (Map) attributes;
        if (attributesMap.containsKey(ID)) {
            id = (String) attributesMap.get(ID);
        }
    }

    @Override
    public void validate(ValidationContext validationContext) {
    }

    @Override
    public ConfigErrors errors() {
        return errors;
    }

    @Override
    public void addError(String fieldName, String message) {
        errors.add(fieldName, message);
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setId(String id) {
        this.id = id;
    }
}
