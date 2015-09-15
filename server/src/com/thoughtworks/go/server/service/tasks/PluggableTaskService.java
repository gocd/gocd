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

package com.thoughtworks.go.server.service.tasks;

import com.thoughtworks.go.config.pluggabletask.PluggableTask;
import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.i18n.Localizer;
import com.thoughtworks.go.plugin.access.packagematerial.PackageConfiguration;
import com.thoughtworks.go.plugin.access.packagematerial.PackageConfigurations;
import com.thoughtworks.go.plugin.access.pluggabletask.PluggableTaskConfigStore;
import com.thoughtworks.go.plugin.access.pluggabletask.TaskExtension;
import com.thoughtworks.go.plugin.access.pluggabletask.TaskPreference;
import com.thoughtworks.go.plugin.api.config.Property;
import com.thoughtworks.go.plugin.api.response.validation.ValidationError;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.plugin.api.task.Task;
import com.thoughtworks.go.plugin.api.task.TaskConfig;
import com.thoughtworks.go.plugin.api.task.TaskConfigProperty;
import com.thoughtworks.go.plugin.infra.ActionWithReturn;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static org.apache.commons.lang.StringUtils.isEmpty;

@Service
public class PluggableTaskService {

    private TaskExtension taskExtension;
    private Localizer localizer;

    @Autowired
    public PluggableTaskService(TaskExtension taskExtension, Localizer localizer) {
        this.taskExtension = taskExtension;
        this.localizer = localizer;
    }

    public boolean validate(final PluggableTask modifiedTask) {
        final TaskConfig configuration = new TaskConfig();
        for (ConfigurationProperty configurationProperty : modifiedTask.getConfiguration()) {
            configuration.add(new TaskConfigProperty(configurationProperty.getConfigurationKey().getName(), configurationProperty.getValue()));
        }

        final String pluginId = modifiedTask.getPluginConfiguration().getId();
        ValidationResult validationResult = taskExtension.validate(pluginId, configuration);

        final TaskPreference preference = PluggableTaskConfigStore.store().preferenceFor(pluginId);
        if (PluggableTaskConfigStore.store().hasPreferenceFor(pluginId)) {
            for (ConfigurationProperty configurationProperty : modifiedTask.getConfiguration()) {
                String key = configurationProperty.getConfigurationKey().getName();
                final Property property = preference.getConfig().get(key);
                if (property != null) {
                    final Boolean required = property.getOption(Property.REQUIRED);
                    if (required && StringUtil.isBlank(configurationProperty.getConfigValue()))
                        validationResult.addError(new ValidationError(property.getKey(), localizer.localize("MANDATORY_CONFIGURATION_FIELD")));
                }
            }
        }
        for (ValidationError validationError : validationResult.getErrors()) {
            modifiedTask.getConfiguration().getProperty(validationError.getKey()).addError(validationError.getKey(), validationError.getMessage());
        }

        return validationResult.isSuccessful();
    }
}
