/*
 * Copyright 2019 ThoughtWorks, Inc.
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
import com.thoughtworks.go.domain.config.EncryptedConfigurationValue;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@ConfigTag("artifacts")
@ConfigCollection(ArtifactTypeConfig.class)
public class ArtifactTypeConfigs extends BaseCollection<ArtifactTypeConfig> implements Validatable, ParamsAttributeAware {
    private final ConfigErrors configErrors = new ConfigErrors();

    public ArtifactTypeConfigs() {
    }

    public ArtifactTypeConfigs(List<ArtifactTypeConfig> artifactConfigConfigs) {
        super(artifactConfigConfigs);
    }

    public ArtifactTypeConfigs(ArtifactTypeConfig... artifactConfigConfigs) {
        super(artifactConfigConfigs);
    }

    public boolean validateTree(ValidationContext validationContext) {
        validate(validationContext);
        boolean isValid = errors().isEmpty();

        for (ArtifactTypeConfig artifactTypeConfig : this) {
            isValid = artifactTypeConfig.validateTree(validationContext) && isValid;
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

    @Override
    public void validate(ValidationContext validationContext) {
        List<ArtifactTypeConfig> plans = new ArrayList<>();
        for (ArtifactTypeConfig artifactTypeConfig : this) {
            artifactTypeConfig.validateUniqueness(plans);
        }
    }

    @Override
    public ConfigErrors errors() {
        return configErrors;
    }

    @Override
    public void addError(String fieldName, String message) {
        configErrors.add(fieldName, message);
    }

    @Override
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

            } else {
                String artifactId = (String) attrMap.get(PluggableArtifactConfig.ID);
                String storeId = (String) attrMap.get(PluggableArtifactConfig.STORE_ID);
                String pluginId = (String) attrMap.get("pluginId");
                Map<String, Object> userSpecifiedConfiguration = (Map<String, Object>) attrMap.get("configuration");
                PluggableArtifactConfig pluggableArtifactConfig = new PluggableArtifactConfig(artifactId, storeId);
                this.add(pluggableArtifactConfig);
                if (userSpecifiedConfiguration == null) {
                    return;
                }

                if (StringUtils.isBlank(pluginId)) {
                    Configuration configuration = pluggableArtifactConfig.getConfiguration();

                    for (String key : userSpecifiedConfiguration.keySet()) {
                        Map<String, String> configurationMetadata = (Map<String, String>) userSpecifiedConfiguration.get(key);
                        if (configurationMetadata != null) {
                            boolean isSecure = Boolean.parseBoolean(configurationMetadata.get("isSecure"));
                            if (configuration.getProperty(key) == null) {
                                configuration.addNewConfiguration(key, isSecure);
                            }
                            if (isSecure) {
                                configuration.getProperty(key).setEncryptedValue(new EncryptedConfigurationValue(configurationMetadata.get("value")));
                            } else {
                                configuration.getProperty(key).setConfigurationValue(new ConfigurationValue(configurationMetadata.get("value")));
                            }
                        }
                    }
                } else {
                    for (Map.Entry<String, Object> configuration : userSpecifiedConfiguration.entrySet()) {
                        pluggableArtifactConfig.getConfiguration().addNewConfigurationWithValue(configuration.getKey(), String.valueOf(configuration.getValue()), false);
                    }
                }
            }
        }
    }

    public List<BuiltinArtifactConfig> getBuiltInArtifactConfigs() {
        final List<BuiltinArtifactConfig> artifactConfigs = new ArrayList<>();
        for (ArtifactTypeConfig artifactTypeConfig : this) {
            if (artifactTypeConfig instanceof BuiltinArtifactConfig) {
                artifactConfigs.add((BuiltinArtifactConfig) artifactTypeConfig);
            }
        }
        return artifactConfigs;
    }

    public List<PluggableArtifactConfig> getPluggableArtifactConfigs() {
        final List<PluggableArtifactConfig> artifactConfigs = new ArrayList<>();
        for (ArtifactTypeConfig artifactTypeConfig : this) {
            if (artifactTypeConfig instanceof PluggableArtifactConfig) {
                artifactConfigs.add((PluggableArtifactConfig) artifactTypeConfig);
            }
        }
        return artifactConfigs;
    }
}
