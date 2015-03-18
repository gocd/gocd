/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
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
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.server.service.materials;

import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.domain.scm.SCM;
import com.thoughtworks.go.i18n.Localizer;
import com.thoughtworks.go.plugin.access.scm.*;
import com.thoughtworks.go.plugin.api.config.Property;
import com.thoughtworks.go.plugin.api.response.Result;
import com.thoughtworks.go.plugin.api.response.validation.ValidationError;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PluggableScmService {
    private SCMExtension scmExtension;
    private Localizer localizer;

    @Autowired
    public PluggableScmService(SCMExtension scmExtension, Localizer localizer) {
        this.scmExtension = scmExtension;
        this.localizer = localizer;
    }

    public void validate(final SCM scmConfig) {
        final String pluginId = scmConfig.getPluginConfiguration().getId();
        final SCMPropertyConfiguration configuration = getScmPropertyConfiguration(scmConfig);
        ValidationResult validationResult = scmExtension.isSCMConfigurationValid(pluginId, configuration);

        if (SCMMetadataStore.getInstance().hasPreferenceFor(pluginId)) {
            SCMConfigurations configurationMetadata = SCMMetadataStore.getInstance().getConfigurationMetadata(pluginId);
            for (SCMConfiguration scmConfiguration : configurationMetadata.list()) {
                String key = scmConfiguration.getKey();
                boolean isRequired = SCMMetadataStore.getInstance().hasOption(pluginId, key, Property.REQUIRED);
                ConfigurationProperty property = scmConfig.getConfiguration().getProperty(key);
                String configValue = property == null ? null : property.getValue();
                if (isRequired && StringUtil.isBlank(configValue)) {
                    validationResult.addError(new ValidationError(key, localizer.localize("MANDATORY_CONFIGURATION_FIELD")));
                }
            }
        }

        for (ValidationError validationError : validationResult.getErrors()) {
            ConfigurationProperty property = scmConfig.getConfiguration().getProperty(validationError.getKey());
            if (property != null) {
                property.addError(validationError.getKey(), validationError.getMessage());
            }
        }
    }

    public Result checkConnection(final SCM scmConfig) {
        final String pluginId = scmConfig.getPluginConfiguration().getId();
        final SCMPropertyConfiguration configuration = getScmPropertyConfiguration(scmConfig);
        return scmExtension.checkConnectionToSCM(pluginId, configuration);
    }

    private SCMPropertyConfiguration getScmPropertyConfiguration(SCM scmConfig) {
        final SCMPropertyConfiguration configuration = new SCMPropertyConfiguration();
        for (ConfigurationProperty configurationProperty : scmConfig.getConfiguration()) {
            configuration.add(new SCMProperty(configurationProperty.getConfigurationKey().getName(), configurationProperty.getValue()));
        }
        return configuration;
    }
}
