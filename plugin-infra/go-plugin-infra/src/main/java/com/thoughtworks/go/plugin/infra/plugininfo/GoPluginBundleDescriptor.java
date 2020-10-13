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
package com.thoughtworks.go.plugin.infra.plugininfo;

import lombok.*;
import lombok.experimental.Accessors;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.lang3.StringUtils;
import org.osgi.framework.Bundle;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

@NoArgsConstructor
@ToString
@EqualsAndHashCode
@Accessors(fluent = true)
@Getter
@XmlRootElement(name = "gocd-bundle")
public class GoPluginBundleDescriptor {
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @Getter
    private Bundle bundle;

    @Getter
    @Setter
    @XmlAttribute
    private String version = "1";

    @XmlElementWrapper(name = "plugins")
    @XmlElement(name = "plugin")
    private List<GoPluginDescriptor> pluginDescriptors;

    public GoPluginBundleDescriptor(GoPluginDescriptor... pluginDescriptors) {
        this.pluginDescriptors = List.of(pluginDescriptors);
        this.pluginDescriptors.forEach(descriptor -> {
            descriptor.setBundleDescriptor(this);
            descriptor.version(this.version);
        });
    }

    public List<GoPluginDescriptor> descriptors() {
        return pluginDescriptors;
    }

    public boolean isBundledPlugin() {
        return first().isBundledPlugin();
    }

    public GoPluginBundleDescriptor markAsInvalid(List<String> messages, Exception e) {
        pluginDescriptors.forEach(pluginDescriptor -> pluginDescriptor.markAsInvalidWithoutUpdatingBundleDescriptor(messages, e));
        return this;
    }

    public String bundleJARFileLocation() {
        return first().pluginJarFileLocation();
    }

    public boolean isInvalid() {
        return IterableUtils.matchesAny(pluginDescriptors, GoPluginDescriptor::isInvalid);
    }

    public File bundleLocation() {
        return first().bundleLocation();
    }

    public GoPluginBundleDescriptor setBundle(Bundle bundle) {
        this.bundle = bundle;
        return this;
    }

    public List<String> getMessages() {
        return descriptors().stream().flatMap(descriptor -> descriptor.getStatus().getMessages().stream()).collect(Collectors.toList());
    }

    public String bundleSymbolicName() {
        return StringUtils.join(pluginIDs(), "--");
    }

    public List<String> pluginIDs() {
        return descriptors().stream().map(GoPluginDescriptor::id).collect(Collectors.toList());
    }

    private GoPluginDescriptor first() {
        return this.pluginDescriptors.get(0);
    }
}
