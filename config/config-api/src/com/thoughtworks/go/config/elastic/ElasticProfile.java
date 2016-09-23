/*
 * Copyright 2016 ThoughtWorks, Inc.
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

package com.thoughtworks.go.config.elastic;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.domain.config.Configuration;
import com.thoughtworks.go.domain.config.ConfigurationProperty;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.apache.commons.lang.StringUtils.isBlank;

@ConfigTag("profile")
@ConfigCollection(value = ConfigurationProperty.class)
public class ElasticProfile extends Configuration implements Validatable {
    public static final String ID = "id";
    public static final String PLUGIN_ID = "pluginId";

    private final ConfigErrors errors = new ConfigErrors();

    @ConfigAttribute(value = "id", optional = false)
    private String id;

    @ConfigAttribute(value = "pluginId", allowNull = false)
    private String pluginId;

    public ElasticProfile() {
    }

    public ElasticProfile(String id, String pluginId, ConfigurationProperty... props) {
        super(props);
        this.id = id;
        this.pluginId = pluginId;
    }

    public ElasticProfile(String id, String pluginId, Collection<ConfigurationProperty> configProperties) {
        this(id, pluginId, configProperties.toArray(new ConfigurationProperty[0]));
    }

    public ElasticProfile(ElasticProfile profile) {
        this(profile.getId(), profile.getPluginId(), profile);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPluginId() {
        return pluginId;
    }

    public void setPluginId(String pluginId) {
        this.pluginId = pluginId;
    }

    @Override
    public ConfigErrors errors() {
        return errors;
    }

    @Override
    public void addError(String fieldName, String message) {
        errors().add(fieldName, message);
    }

    @Override
    public void validate(ValidationContext validationContext) {
        super.validateUniqueness("elastic profile " + (isBlank(id) ? "(noname)" : "'" + id + "'"));

        if (isBlank(id)) {
            addError(ID, "Elastic profile cannot have a blank id.");
        }

        if (isBlank(pluginId)) {
            addError(PLUGIN_ID, "Elastic profile cannot have a blank plugin id.");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        ElasticProfile that = (ElasticProfile) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        return pluginId != null ? pluginId.equals(that.pluginId) : that.pluginId == null;

    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (id != null ? id.hashCode() : 0);
        result = 31 * result + (pluginId != null ? pluginId.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "ElasticProfile{" +
                "id='" + id + '\'' +
                ", pluginId='" + pluginId + '\'' +
                ", properties='" + super.toString() + '\'' +
                '}';
    }

    void validateIdUniquness(Map<String, ElasticProfile> profiles) {
        ElasticProfile profileWithSameId = profiles.get(id);
        if (profileWithSameId == null) {
            profiles.put(id, this);
        } else {
            profileWithSameId.addError(ID, String.format("Elastic agent profile id '%s' is not unique", id));
            this.addError(ID, String.format("Elastic agent profile id '%s' is not unique", id));
        }
    }

    public List<ConfigErrors> getAllErrors() {
        return ErrorCollector.getAllErrors(this);
    }
}
