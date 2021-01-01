/*
 * Copyright 2021 ThoughtWorks, Inc.
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

import com.thoughtworks.go.plugin.access.common.settings.GoPluginExtension;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.lang.String.format;

@Component
public class ExtensionsRegistry {
    private final Map<String, GoPluginExtension> registry = new HashMap<>();

    public void registerExtension(GoPluginExtension extension) {
        registry.put(extension.extensionName(), extension);
    }

    public Set<String> allRegisteredExtensions() {
        return registry.keySet();
    }

    public List<String> gocdSupportedExtensionVersions(String extensionType) {
        if (!registry.containsKey(extensionType)) {
            throw new UnsupportedExtensionException(format("Requested extension '%s' is not supported by GoCD. Supported extensions are %s.", extensionType, allRegisteredExtensions()));
        }

        return registry.get(extensionType).goSupportedVersions();
    }
}
