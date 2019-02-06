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

package com.thoughtworks.go.plugin.access;

import com.thoughtworks.go.plugin.infra.PluginExtensionsAndVersionValidator;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PluginExtensionsAndVersionValidatorImpl implements PluginExtensionsAndVersionValidator {
    private final ExtensionsRegistry extensionsRegistry;
    private static final String ERROR_MESSAGE = "Expected %s extension version by plugin is %s. GoCD Supported versions are %s.";
    private PluginManager pluginManager;

    @Autowired
    public PluginExtensionsAndVersionValidatorImpl(ExtensionsRegistry extensionsRegistry) {
        this.extensionsRegistry = extensionsRegistry;
        pluginManager = extensionsRegistry.getPluginManager();
    }

    @Override
    public ValidationResult validate(GoPluginDescriptor descriptor) {
        ValidationResult validationResult = new ValidationResult(descriptor.id());
        for (String extensionType : extensionsRegistry.allRegisteredExtensions()) {
            if (pluginManager.isPluginOfType(extensionType, descriptor.id())) {
                validatePluginRequestedExtensionVersionWithGoCDSupportedVersion(descriptor, validationResult, extensionType);
            }
        }

        return validationResult;
    }

    private void validatePluginRequestedExtensionVersionWithGoCDSupportedVersion(GoPluginDescriptor descriptor, ValidationResult validationResult, String extensionType) {
        if (!extensionsRegistry.supportsExtensionVersion(descriptor.id(), extensionType)) {
            try {
                final List<String> gocdSupportedExtensionVersions = extensionsRegistry.gocdSupportedExtensionVersions(extensionType);
                final List<String> requiredExtensionVersionsByPlugin = pluginManager.getRequiredExtensionVersionsByPlugin(descriptor.id(), extensionType);
                validationResult.addError(String.format(ERROR_MESSAGE, extensionType, requiredExtensionVersionsByPlugin, gocdSupportedExtensionVersions));
            } catch (UnsupportedExtensionException e) {
                validationResult.addError(e.getMessage());
            }
        }
    }
}
