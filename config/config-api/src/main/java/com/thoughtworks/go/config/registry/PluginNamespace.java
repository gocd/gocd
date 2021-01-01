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
package com.thoughtworks.go.config.registry;

import java.net.URL;

import com.thoughtworks.go.plugins.GoPluginManifest;
import com.thoughtworks.go.util.GoConstants;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/**
* @understands the XML namespace that a plugin provides for a given set of config tags
*/
public class PluginNamespace {
    public static final String XSD_NAMESPACE_PREFIX = GoConstants.GO_PLUGIN_MANIFEST_HEADER_PREFIX + "Xsd-Prefix";
    public static final String XSD_NAMESPACE_URI = GoConstants.GO_PLUGIN_MANIFEST_HEADER_PREFIX + "Xsd-Uri";
    final String prefix;
    final String uri;
    final URL xsdResource;

    public PluginNamespace(BundleContext bundleContext, URL xsdResource) {
        if (bundleContext == null) {
            throw new IllegalArgumentException(String.format("context for xsd-resource %s is null", xsdResource));
        }
        Bundle bundle = bundleContext.getBundle();
        if (bundle == null) {
            throw new IllegalArgumentException(String.format("bundle for xsd-resource %s is null", xsdResource));
        }
        GoPluginManifest manifest = new GoPluginManifest(bundle);
        this.prefix = manifest.getPluginNamespacePrefix();
        this.uri = manifest.getPluginNamespaceUri();
        this.xsdResource = xsdResource;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        PluginNamespace that = (PluginNamespace) o;

        if (prefix != null ? !prefix.equals(that.prefix) : that.prefix != null) {
            return false;
        }
        if (uri != null ? !uri.equals(that.uri) : that.uri != null) {
            return false;
        }
        if (xsdResource != null ? !xsdResource.equals(that.xsdResource) : that.xsdResource != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = prefix != null ? prefix.hashCode() : 0;
        result = 31 * result + (uri != null ? uri.hashCode() : 0);
        result = 31 * result + (xsdResource != null ? xsdResource.hashCode() : 0);
        return result;
    }

    @Override public String toString() {
        return "PluginNamespace{" +
                "prefix='" + prefix + '\'' +
                ", uri='" + uri + '\'' +
                ", xsdResource=" + xsdResource +
                '}';
    }
}
