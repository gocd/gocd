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
package com.thoughtworks.go.plugins;

import java.util.Dictionary;

import com.thoughtworks.go.config.registry.PluginNamespace;
import org.apache.commons.lang3.StringUtils;
import org.jdom2.Namespace;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;

/**
 * @understands: GoPluginManifest
 */
public class GoPluginManifest {
    private final Dictionary<String, String> headers;

    public GoPluginManifest(Bundle bundle) {
        headers = bundle.getHeaders();
    }

    public String getPluginNamespacePrefix() {
        String value = headers.get(PluginNamespace.XSD_NAMESPACE_PREFIX);
        if (StringUtils.isBlank(value)) {
            throw new RuntimeException(String.format("Value for header %s is null or empty", value));
        }
        return value;
    }

    public String getPluginNamespaceUri() {
        String value = headers.get(PluginNamespace.XSD_NAMESPACE_URI);
        if (StringUtils.isBlank(value)) {
            throw new RuntimeException(String.format("Value for header %s is null or empty", value));
        }
        return value;
    }

    public Namespace getPluginNamespace() {
        return Namespace.getNamespace(getPluginNamespacePrefix(), getPluginNamespaceUri());
    }

    public String getBundleName() {
        return headers.get(Constants.BUNDLE_NAME);
    }
}
