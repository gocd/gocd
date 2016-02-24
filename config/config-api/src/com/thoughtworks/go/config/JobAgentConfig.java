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

package com.thoughtworks.go.config;

import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.domain.config.Configuration;
import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.util.StringUtil;

import java.util.Collection;

import static java.lang.String.format;

@ConfigTag("agentConfig")
@ConfigCollection(value = ConfigurationProperty.class)
public class JobAgentConfig extends Configuration implements Validatable {

    public static final String PLUGIN_ID = "pluginId";
    private final ConfigErrors errors = new ConfigErrors();

    @ConfigAttribute(value = "pluginId", allowNull = false)
    private String pluginId;

    public JobAgentConfig() {

    }

    JobAgentConfig(String pluginId, ConfigurationProperty... configurationProperties) {
        super(configurationProperties);
        this.pluginId = pluginId;
    }

    public JobAgentConfig(String pluginId, Collection<ConfigurationProperty> configProperties) {
        this(pluginId, configProperties.toArray(new ConfigurationProperty[0]));
    }

    @Override
    public void validate(ValidationContext validationContext) {
        super.validateUniqueness(format("agent config of job '%s::%s::%s'", validationContext.getPipeline().name(), validationContext.getStage().name(), validationContext.getJob().name()));
        if (StringUtil.isBlank(pluginId)) {
            addError(PLUGIN_ID,
                    format("Agent config on job '%s::%s::%s' cannot have a blank plugin id.",
                            validationContext.getPipeline().name(),
                            validationContext.getStage().name(),
                            validationContext.getJob().name())
            );
        }
    }

    @Override
    public ConfigErrors errors() {
        return errors;
    }

    @Override
    public void addError(String fieldName, String message) {
        errors().add(fieldName, message);
    }

    public String getPluginId() {
        return pluginId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        JobAgentConfig that = (JobAgentConfig) o;

        if (errors != null ? !errors.equals(that.errors) : that.errors != null) return false;
        return pluginId != null ? pluginId.equals(that.pluginId) : that.pluginId == null;

    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (errors != null ? errors.hashCode() : 0);
        result = 31 * result + (pluginId != null ? pluginId.hashCode() : 0);
        return result;
    }
}
