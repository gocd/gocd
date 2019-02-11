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
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.plugin.infra.plugininfo.PluginRegistry;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.SetUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

@Component
public class PluginExtensionsAndVersionValidatorImpl implements PluginExtensionsAndVersionValidator {
    private final ExtensionsRegistry extensionsRegistry;
    private static final String UNSUPPORTED_VERSION_ERROR_MESSAGE = "Expected %s extension version(s) %s by plugin is unsupported. GoCD Supported versions are %s.";
    private static final String UNSUPPORTED_EXTENSION_ERROR_MESSAGE = "Extension(s) %s used by the plugin is not supported. GoCD Supported extensions are %s.";
    private final PluginRegistry pluginRegistry;

    @Autowired
    public PluginExtensionsAndVersionValidatorImpl(ExtensionsRegistry extensionsRegistry, PluginRegistry pluginRegistry) {
        this.extensionsRegistry = extensionsRegistry;
        this.pluginRegistry = pluginRegistry;
    }

    @Override
    public ValidationResult validate(GoPluginDescriptor descriptor) {
        ValidationResult validationResult = new ValidationResult(descriptor.id());
        final Set<String> gocdSupportedExtensions = extensionsRegistry.allRegisteredExtensions();

        final Map<String, List<String>> extensionsInfoFromPlugin = pluginRegistry.getExtensionsInfo(descriptor.id());

        final Set<String> difference = SetUtils.difference(extensionsInfoFromPlugin.keySet(), gocdSupportedExtensions).toSet();

        if (difference.size() > 0) {
            validationResult.addError(format(UNSUPPORTED_EXTENSION_ERROR_MESSAGE, difference, gocdSupportedExtensions));
            return validationResult;
        }


        for (String extensionType : extensionsInfoFromPlugin.keySet()) {
            final List<Double> gocdSupportedExtensionVersions = extensionsRegistry.gocdSupportedExtensionVersions(extensionType).stream()
                    .map(Double::parseDouble).collect(toList());

            final List<String> requiredExtensionVersionsByPlugin = extensionsInfoFromPlugin.get(extensionType);
            validateExtensionVersions(validationResult, extensionType, gocdSupportedExtensionVersions, requiredExtensionVersionsByPlugin);
        }

        return validationResult;
    }

    private void validateExtensionVersions(ValidationResult validationResult, String extensionType, List<Double> gocdSupportedExtensionVersions, List<String> requiredExtensionVersionsByPlugin) {
        final List<Double> requiredExtensionVersions = requiredExtensionVersionsByPlugin.stream()
                .map(parseToDouble())
                .collect(toList());

        final List<Double> intersection = ListUtils.intersection(gocdSupportedExtensionVersions, requiredExtensionVersions);

        if (intersection.isEmpty()) {
            validationResult.addError(format(UNSUPPORTED_VERSION_ERROR_MESSAGE, extensionType, requiredExtensionVersionsByPlugin, gocdSupportedExtensionVersions));
        }
    }

    private Function<String, Double> parseToDouble() {
        return versionToParse -> {
            try {
                return Double.parseDouble(versionToParse);
            } catch (NumberFormatException e) {
                return -1.0;
            }
        };
    }
}
