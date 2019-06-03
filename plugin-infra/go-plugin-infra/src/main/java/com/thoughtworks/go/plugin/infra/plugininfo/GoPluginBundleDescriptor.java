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

package com.thoughtworks.go.plugin.infra.plugininfo;

import com.thoughtworks.go.plugin.api.info.PluginDescriptor;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.osgi.framework.Bundle;

import java.io.File;
import java.util.List;

public class GoPluginBundleDescriptor {
    private GoPluginDescriptor pluginDescriptor;
    private Bundle bundle;
    private String bundleSymbolicName;
    private String bundleClassPath;
    private String bundleActivator;

    public GoPluginBundleDescriptor(GoPluginDescriptor pluginDescriptor) {
        this.pluginDescriptor = pluginDescriptor;
        this.pluginDescriptor.setBundleDescriptor(this);
    }

    public GoPluginDescriptor descriptor() {
        return pluginDescriptor;
    }

    public boolean isBundledPlugin() {
        return pluginDescriptor.isBundledPlugin();
    }

    public boolean isCurrentOSValidForThisPlugin(String currentOS) {
        return pluginDescriptor.isCurrentOSValidForThisPlugin(currentOS);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        GoPluginBundleDescriptor that = (GoPluginBundleDescriptor) o;

        return new EqualsBuilder()
                .append(pluginDescriptor, that.pluginDescriptor)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(pluginDescriptor)
                .toHashCode();
    }

    public PluginDescriptor.About about() {
        return pluginDescriptor.about();
    }

    public String id() {
        return pluginDescriptor.id();
    }

    public void markAsInvalid(List<String> messages, Exception o) {
        pluginDescriptor.markAsInvalid(messages, o);
    }

    public boolean isCurrentGocdVersionValidForThisPlugin() {
        return pluginDescriptor.isCurrentGocdVersionValidForThisPlugin();
    }

    public String fileName() {
        return pluginDescriptor.fileName();
    }

    public boolean isInvalid() {
        return pluginDescriptor.isInvalid();
    }

    public File bundleLocation() {
        return pluginDescriptor.bundleLocation();
    }

    public GoPluginBundleDescriptor setBundle(Bundle bundle) {
        this.bundle = bundle;
        return this;
    }

    public Bundle bundle() {
        return bundle;
    }

    public PluginStatus getStatus() {
        return pluginDescriptor.getStatus();
    }

    public void updateBundleInformation(String symbolicName, String classPath, String bundleActivator) {
        this.bundleSymbolicName = symbolicName;
        this.bundleClassPath = classPath;
        this.bundleActivator = bundleActivator;
    }

    public String bundleSymbolicName() {
        return bundleSymbolicName;
    }

    public String bundleClassPath() {
        return bundleClassPath;
    }

    public String bundleActivator() {
        return bundleActivator;
    }
}
