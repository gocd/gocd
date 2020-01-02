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
package com.thoughtworks.go.config.elastic;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.validation.NameTypeValidator;
import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.domain.config.Configuration;
import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.plugin.access.elastic.ElasticAgentMetadataStore;
import com.thoughtworks.go.plugin.domain.elastic.ElasticAgentPluginInfo;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.apache.commons.lang3.StringUtils.isBlank;

@ConfigTag("agentProfile")
@ConfigCollection(value = ConfigurationProperty.class)
public class ElasticProfile extends Configuration implements Validatable {
    public static final String ID = "id";
    public static final String CLUSTER_PROFILE_ID = "clusterProfileId";
    private final ConfigErrors errors = new ConfigErrors();

    @ConfigAttribute(value = "id", optional = false)
    protected String id;

    @ConfigAttribute(value = "clusterProfileId", optional = false)
    protected String clusterProfileId;

    public ElasticProfile() {
        super();
    }

    public ElasticProfile(String id, String clusterProfileId, ConfigurationProperty... props) {
        super(props);
        this.id = id;
        this.clusterProfileId = clusterProfileId;
    }

    public ElasticProfile(String id, String clusterProfileId, Collection<ConfigurationProperty> configProperties) {
        this(id, clusterProfileId, configProperties.toArray(new ConfigurationProperty[0]));
    }

    protected String getObjectDescription() {
        return "Elastic agent profile";
    }

    public String getId() {
        return id;
    }

    public String getClusterProfileId() {
        return clusterProfileId;
    }

    protected boolean isSecure(String key, ClusterProfile clusterProfile) {
        ElasticAgentPluginInfo pluginInfo = this.metadataStore().getPluginInfo(clusterProfile.getPluginId());

        if (pluginInfo == null
                || pluginInfo.getElasticAgentProfileSettings() == null
                || pluginInfo.getElasticAgentProfileSettings().getConfiguration(key) == null) {
            return false;
        }

        return pluginInfo.getElasticAgentProfileSettings().getConfiguration(key).isSecure();
    }

    protected boolean hasPluginInfo(ClusterProfile clusterProfile) {
        return this.metadataStore().getPluginInfo(clusterProfile.getPluginId()) != null;
    }

    private ElasticAgentMetadataStore metadataStore() {
        return ElasticAgentMetadataStore.instance();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ElasticProfile that = (ElasticProfile) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(clusterProfileId, that.clusterProfileId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), id, clusterProfileId);
    }

    @Override
    public String toString() {
        return "ElasticProfile{" +
                "id='" + id + '\'' +
                ", clusterProfileId='" + clusterProfileId + '\'' +
                super.toString() +
                '}';
    }

    @Override
    public void validate(ValidationContext validationContext) {
        validateUniqueness(getObjectDescription() + " " + (isBlank(id) ? "(noname)" : "'" + id + "'"));

        if (isBlank(id)) {
            errors().add(ID, getObjectDescription() + " cannot have a blank id.");
        }

        if (new NameTypeValidator().isNameInvalid(id)) {
            errors().add(ID, String.format("Invalid id '%s'. %s", id, NameTypeValidator.ERROR_MESSAGE));
        }

        if (errors().isEmpty() && !super.hasErrors()) {
            ClusterProfiles clusterProfiles = validationContext.getClusterProfiles();
            ClusterProfile associatedClusterProfile = clusterProfiles.find(this.clusterProfileId);
            if (associatedClusterProfile == null) {
                errors().add("clusterProfileId", String.format("No Cluster Profile exists with the specified cluster_profile_id '%s'.", this.clusterProfileId));
            }
        }
    }

    public void addConfigurations(List<ConfigurationProperty> props) {
        this.addAll(props);
    }

    public List<ConfigErrors> getAllErrors() {
        return ErrorCollector.getAllErrors(this);
    }

    @Override
    public ConfigErrors errors() {
        return errors;
    }

    @Override
    public boolean hasErrors() {
        return !errors().isEmpty() || super.hasErrors();
    }

    public void validateTree(ConfigSaveValidationContext configSaveValidationContext) {
        super.validateTree();
        validate(configSaveValidationContext);
    }

    @Override
    public void addError(String fieldName, String message) {
        errors().add(fieldName, message);
    }

    void validateIdUniqueness(Map<String, ElasticProfile> profiles) {
        ElasticProfile profileWithSameId = profiles.get(id);
        if (profileWithSameId == null) {
            profiles.put(id, this);
        } else {
            profileWithSameId.addError(ID, String.format(getObjectDescription() + " id '%s' is not unique", id));
            errors().add(ID, String.format(getObjectDescription() + " id '%s' is not unique", id));
        }
    }

    public void encryptSecureProperties(CruiseConfig preprocessed) {
        if (clusterProfileId != null) {
            ClusterProfile clusterProfile = preprocessed.getElasticConfig().getClusterProfiles().find(clusterProfileId);
            encryptSecureConfigurations(clusterProfile);
        }
    }

    private void encryptSecureConfigurations(ClusterProfile clusterProfile) {
        if (clusterProfile != null && hasPluginInfo(clusterProfile)) {
            for (ConfigurationProperty configuration : this) {
                configuration.handleSecureValueConfiguration(isSecure(configuration.getConfigKeyName(), clusterProfile));
            }
        }
    }
}
