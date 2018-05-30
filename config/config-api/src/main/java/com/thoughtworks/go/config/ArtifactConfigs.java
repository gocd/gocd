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

package com.thoughtworks.go.config;

import com.thoughtworks.go.domain.BaseCollection;
import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.domain.config.Configuration;
import com.thoughtworks.go.domain.config.ConfigurationValue;
import com.thoughtworks.go.domain.config.SecureKeyInfoProvider;
import com.thoughtworks.go.plugin.access.artifact.ArtifactMetadataStore;
import com.thoughtworks.go.plugin.domain.artifact.ArtifactPluginInfo;
import com.thoughtworks.go.plugin.domain.common.PluginConfiguration;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@ConfigTag("artifacts")
@ConfigCollection(ArtifactConfig.class)
public class ArtifactConfigs extends BaseCollection<ArtifactConfig> implements Validatable, ParamsAttributeAware {
    private final ConfigErrors configErrors = new ConfigErrors();

    public ArtifactConfigs() {
    }

    public ArtifactConfigs(List<ArtifactConfig> artifactConfigConfigs) {
        super(artifactConfigConfigs);
    }

    public ArtifactConfigs(ArtifactConfig... artifactConfigConfigs) {
        super(artifactConfigConfigs);
    }

    public boolean validateTree(ValidationContext validationContext) {
        validate(validationContext);
        boolean isValid = errors().isEmpty();

        for (ArtifactConfig artifactConfig : this) {
            isValid = artifactConfig.validateTree(validationContext) && isValid;
        }
        return isValid;
    }

    public PluggableArtifactConfig findByArtifactId(String artifactId) {
        for (PluggableArtifactConfig artifact : getPluggableArtifactConfigs()) {
            if (artifact.getId().equals(artifactId)) {
                return artifact;
            }
        }
        return null;
    }

    public List<PluggableArtifactConfig> findByStoreId(String storeId) {
        final ArrayList<PluggableArtifactConfig> pluggableArtifactConfigs = new ArrayList<>();
        for (PluggableArtifactConfig artifact : getPluggableArtifactConfigs()) {
            if (artifact.getStoreId().equals(storeId)) {
                pluggableArtifactConfigs.add(artifact);
            }
        }
        return pluggableArtifactConfigs;
    }

    public void validate(ValidationContext validationContext) {
        List<ArtifactConfig> plans = new ArrayList<>();
        for (ArtifactConfig artifactConfig : this) {
            artifactConfig.validateUniqueness(plans);
        }
    }

    public ConfigErrors errors() {
        return configErrors;
    }

    public void addError(String fieldName, String message) {
        configErrors.add(fieldName, message);
    }

    public void setConfigAttributes(Object attributes) {
        clear();
        if (attributes == null) {
            return;
        }
        List<Map> attrList = (List<Map>) attributes;
        for (Map attrMap : attrList) {
            String type = (String) attrMap.get("artifactTypeValue");
            if (TestArtifactConfig.TEST_PLAN_DISPLAY_NAME.equals(type) || BuildArtifactConfig.ARTIFACT_PLAN_DISPLAY_NAME.equals(type)) {
                String source = (String) attrMap.get(BuiltinArtifactConfig.SRC);
                String destination = (String) attrMap.get(BuiltinArtifactConfig.DEST);
                if (source.trim().isEmpty() && destination.trim().isEmpty()) {
                    continue;
                }
                if (TestArtifactConfig.TEST_PLAN_DISPLAY_NAME.equals(type)) {
                    this.add(new TestArtifactConfig(source, destination));
                } else {
                    this.add(new BuildArtifactConfig(source, destination));
                }

            }
            else {
                String artifactId = (String) attrMap.get(PluggableArtifactConfig.ID);
                String storeId = (String) attrMap.get(PluggableArtifactConfig.STORE_ID);
                String pluginId = (String) attrMap.get("pluginId");

                PluggableArtifactConfig pluggableArtifactConfig = new PluggableArtifactConfig(artifactId, storeId);

                // TODO: set artifact store
                if (StringUtils.isNotBlank(pluginId)) {
                    setPluginConfigurationAttributes(attrMap, pluginId, pluggableArtifactConfig);
                }

                this.add(pluggableArtifactConfig);

            }

        }
    }

    protected void setPluginConfigurationAttributes(Map attributes, String pluginId, PluggableArtifactConfig pluggableArtifactConfig) {
        ArtifactPluginInfo pluginInfo = getArtifactPluginInfo(pluginId);
        if (pluginInfo != null && pluginInfo.getArtifactConfigSettings() != null) {
            for (PluginConfiguration pluginConfiguration : pluginInfo.getArtifactConfigSettings().getConfigurations()) {
                String key = pluginConfiguration.getKey();
                if (attributes.containsKey(key)) {
                    Configuration configuration = pluggableArtifactConfig.getConfiguration();
                    if (configuration.getProperty(key) == null) {
                        configuration.addNewConfiguration(pluginConfiguration.getKey(), pluginConfiguration.isSecure());
                    }
                    configuration.getProperty(key).setConfigurationValue(new ConfigurationValue((String) attributes.get(key)));
                    configuration.getProperty(key).handleSecureValueConfiguration(pluginConfiguration.isSecure());
                }
            }
        } else {
            throw new RuntimeException("metadata unavailable for plugin: " + pluginId);
        }
    }

    private ArtifactPluginInfo getArtifactPluginInfo(String pluginId) {
        ArtifactMetadataStore artifactMetadataStore = ArtifactMetadataStore.instance();
        return artifactMetadataStore.getPluginInfo(pluginId);
    }

    public List<BuiltinArtifactConfig> getBuiltInArtifactConfigs() {
        final List<BuiltinArtifactConfig> artifactConfigs = new ArrayList<>();
        for (ArtifactConfig artifactConfig : this) {
            if (artifactConfig instanceof BuiltinArtifactConfig) {
                artifactConfigs.add((BuiltinArtifactConfig) artifactConfig);
            }
        }
        return artifactConfigs;
    }

    public List<PluggableArtifactConfig> getPluggableArtifactConfigs() {
        final List<PluggableArtifactConfig> artifactConfigs = new ArrayList<>();
        for (ArtifactConfig artifactConfig : this) {
            if (artifactConfig instanceof PluggableArtifactConfig) {
                artifactConfigs.add((PluggableArtifactConfig) artifactConfig);
            }
        }
        return artifactConfigs;
    }
}
