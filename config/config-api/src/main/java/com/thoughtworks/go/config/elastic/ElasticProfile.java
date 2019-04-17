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
import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.plugin.access.elastic.ElasticAgentMetadataStore;
import com.thoughtworks.go.plugin.domain.elastic.ElasticAgentPluginInfo;

import java.util.Collection;
import java.util.Objects;

import static java.lang.String.format;

@ConfigTag("profile")
@ConfigCollection(value = ConfigurationProperty.class)
public class ElasticProfile extends PluginProfile {

    @ConfigAttribute(value = "clusterProfileId", allowNull = true)
    protected String clusterProfileId;

    public ElasticProfile() {
        super();
    }

    public ElasticProfile(String id, String pluginId, ConfigurationProperty... props) {
        super(id, pluginId, props);
    }

    public ElasticProfile(String id, String pluginId, String clusterProfileId, ConfigurationProperty... props) {
        super(id, pluginId, props);
        this.clusterProfileId = clusterProfileId;
    }

    public ElasticProfile(String id, String pluginId, Collection<ConfigurationProperty> configProperties) {
        this(id, pluginId, configProperties.toArray(new ConfigurationProperty[0]));
    }

    public ElasticProfile(String id, String pluginId, String clusterProfileId, Collection<ConfigurationProperty> configProperties) {
        this(id, pluginId, clusterProfileId, configProperties.toArray(new ConfigurationProperty[0]));
    }

    @Override
    protected String getObjectDescription() {
        return "Elastic agent profile";
    }

    public String getClusterProfileId() {
        return clusterProfileId;
    }

    @Override
    protected boolean isSecure(String key) {
        ElasticAgentPluginInfo pluginInfo = this.metadataStore().getPluginInfo(getPluginId());

        if (pluginInfo == null
                || pluginInfo.getElasticAgentProfileSettings() == null
                || pluginInfo.getElasticAgentProfileSettings().getConfiguration(key) == null) {
            return false;
        }

        return pluginInfo.getElasticAgentProfileSettings().getConfiguration(key).isSecure();
    }

    @Override
    protected boolean hasPluginInfo() {
        return this.metadataStore().getPluginInfo(getPluginId()) != null;
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
        return Objects.equals(clusterProfileId, that.clusterProfileId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), clusterProfileId);
    }

    @Override
    public String toString() {
        return getObjectDescription() + "{" +
                "id='" + id + '\'' +
                ", pluginId='" + pluginId + '\'' +
                ", clusterProfileId='" + clusterProfileId + '\'' +
                ", properties='" + super.getConfigurationAsMap(false) + '\'' +
                '}';
    }

    @Override
    public void validateTree(ValidationContext validationContext) {
        super.validateTree(validationContext);
    }

    @Override
    public void validate(ValidationContext validationContext) {
        super.validate(validationContext);
        if (this.errors().isEmpty() && !super.hasErrors()) {
            ClusterProfiles clusterProfiles = validationContext.getClusterProfiles();
            ClusterProfile associatedClusterProfile = clusterProfiles.find(this.clusterProfileId);
            if (associatedClusterProfile == null) {
                this.errors().add("clusterProfileId", String.format("No Cluster Profile exists with the specified cluster_profile_id '%s'.", this.clusterProfileId));
                return;
            }

            if (!associatedClusterProfile.getPluginId().equals(this.getPluginId())) {
                String errorMsg = format("Referenced Cluster Profile and Elastic Agent Profile should belong to same plugin. " +
                                "Specified cluster profile '%s' belongs to '%s' plugin, whereas, elastic agent profile belongs to '%s' plugin.",
                        this.getClusterProfileId(),
                        associatedClusterProfile.getPluginId(),
                        this.getPluginId());

                this.errors().add("clusterProfileId", errorMsg);
                return;
            }
        }
    }
}
