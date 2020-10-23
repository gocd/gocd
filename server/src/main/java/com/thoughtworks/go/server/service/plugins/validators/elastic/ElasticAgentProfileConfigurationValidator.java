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
package com.thoughtworks.go.server.service.plugins.validators.elastic;

import com.thoughtworks.go.config.elastic.ElasticProfile;
import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.plugin.access.elastic.ElasticAgentExtension;
import com.thoughtworks.go.plugin.api.response.validation.ValidationError;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;

public class ElasticAgentProfileConfigurationValidator {
    private final ElasticAgentExtension elasticAgentExtension;

    public ElasticAgentProfileConfigurationValidator(ElasticAgentExtension elasticAgentExtension) {
        this.elasticAgentExtension = elasticAgentExtension;
    }

    public void validate(ElasticProfile elasticAgentProfile, String pluginId) {
        try {
            ValidationResult result = elasticAgentExtension.validate(pluginId, elasticAgentProfile.getConfigurationAsMap(true, true));

            if (!result.isSuccessful()) {
                for (ValidationError error : result.getErrors()) {
                    ConfigurationProperty property = elasticAgentProfile.getProperty(error.getKey());

                    if (property == null) {
                        elasticAgentProfile.addNewConfiguration(error.getKey(), false);
                        property = elasticAgentProfile.getProperty(error.getKey());
                    }
                    property.addError(error.getKey(), error.getMessage());
                }
            }
        } catch (RecordNotFoundException e) {
            elasticAgentProfile.addError("pluginId", String.format("Unable to validate Elastic Agent Profile configuration, missing plugin: %s", pluginId));
        }
    }
}
