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

import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.osgi.framework.Bundle;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class GoPluginBundleDescriptor {
    private Bundle bundle;
    private String bundleSymbolicName;
    private String bundleClassPath;
    private String bundleActivator;

    private List<GoPluginDescriptor> pluginDescriptors;

    public GoPluginBundleDescriptor(GoPluginDescriptor... pluginDescriptors) {
        this.pluginDescriptors = Arrays.asList(pluginDescriptors);
        for (GoPluginDescriptor goPluginDescriptor : pluginDescriptors) {
            goPluginDescriptor.setBundleDescriptor(this);
        }
    }

    public List<GoPluginDescriptor> descriptors() {
        return pluginDescriptors;
    }

    public boolean isBundledPlugin() {
        return pluginDescriptors.get(0).isBundledPlugin();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        GoPluginBundleDescriptor that = (GoPluginBundleDescriptor) o;

        return new EqualsBuilder()
                .append(bundle, that.bundle)
                .append(bundleSymbolicName, that.bundleSymbolicName)
                .append(bundleClassPath, that.bundleClassPath)
                .append(bundleActivator, that.bundleActivator)
                .append(pluginDescriptors, that.pluginDescriptors)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(bundle)
                .append(bundleSymbolicName)
                .append(bundleClassPath)
                .append(bundleActivator)
                .append(pluginDescriptors)
                .toHashCode();
    }

    @Override
    public String toString() {
        return "GoPluginBundleDescriptor{" +
                "bundle=" + bundle +
                ", bundleSymbolicName='" + bundleSymbolicName + '\'' +
                ", bundleClassPath='" + bundleClassPath + '\'' +
                ", bundleActivator='" + bundleActivator + '\'' +
                ", pluginDescriptors=" + pluginDescriptors +
                '}';
    }

    public GoPluginBundleDescriptor markAsInvalid(List<String> messages, Exception e) {
        for (GoPluginDescriptor pluginDescriptor : pluginDescriptors) {
            pluginDescriptor.markAsInvalidWithoutUpdatingBundleDescriptor(messages, e);
        }
        return this;
    }

    public String fileName() {
        return pluginDescriptors.get(0).fileName();
    }

    public boolean isInvalid() {
        return IterableUtils.matchesAny(pluginDescriptors, GoPluginDescriptor::isInvalid);
    }

    public File bundleLocation() {
        return pluginDescriptors.get(0).bundleLocation();
    }

    public GoPluginBundleDescriptor setBundle(Bundle bundle) {
        this.bundle = bundle;
        return this;
    }

    public Bundle bundle() {
        return bundle;
    }

    public List<String> getMessages() {
        return descriptors().stream().flatMap(descriptor -> descriptor.getStatus().getMessages().stream()).collect(Collectors.toList());
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

    public List<String> pluginIDs() {
        return descriptors().stream().map(GoPluginDescriptor::id).collect(Collectors.toList());
    }

    /* TODO: Temporary symbolic name for bundle. Get rid of it. */
    public String generateSymbolicName() {
        return StringUtils.join(pluginIDs(), "--");
    }
}
