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
package com.thoughtworks.go.plugin.infra.plugininfo;

import com.thoughtworks.go.CurrentGoCDVersion;
import com.thoughtworks.go.plugin.api.info.PluginDescriptor;
import lombok.*;
import lombok.experimental.Accessors;
import org.osgi.framework.Version;

import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static com.thoughtworks.go.plugin.infra.plugininfo.PluginStatus.State.ACTIVE;
import static com.thoughtworks.go.plugin.infra.plugininfo.PluginStatus.State.INVALID;

@XmlRootElement(name = "go-plugin")
@XmlAccessorType(XmlAccessType.NONE)
@NoArgsConstructor
@AllArgsConstructor
@Accessors(fluent = true)
@ToString
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class GoPluginDescriptor implements PluginDescriptor {
    @XmlAttribute
    @EqualsAndHashCode.Include
    @Getter
    private String id;

    @XmlAttribute
    @EqualsAndHashCode.Include
    @Builder.Default
    @Getter
    @Setter
    private String version = "1";

    @XmlElement
    @EqualsAndHashCode.Include
    @Getter
    private About about;

    /*
     * Absolute path to plugin jar e.g. $GO_SERVER_DIR/plugins/bundled/foo.jar
     */
    @Getter
    @Setter
    private String pluginJarFileLocation;

    /*
     * Path to bundle directory in plugin work folder. e.g. $PLUGIN_WORK_DIR/foo.jar/
     */
    @Getter
    @Setter
    private File bundleLocation;

    @Getter
    @Setter
    private boolean isBundledPlugin;

    /*
     * GoPluginBundleDescriptor.toString() uses toString of GoPluginDescriptor
     * Hence this needs to be excluded from the @ToString to avoid recursive call
     */
    @ToString.Exclude
    private GoPluginBundleDescriptor bundleDescriptor;

    @Builder.Default
    private PluginStatus status = new PluginStatus(ACTIVE);

    @XmlElementWrapper(name = "extensions")
    @XmlElement(name = "extension")
    @XmlJavaTypeAdapter(value = ExtensionAdapter.class)
    @Builder.Default
    private final List<String> extensionClasses = new ArrayList<>();

    public GoPluginDescriptor markAsInvalid(List<String> messages, Exception rootCause) {
        bundleDescriptor().markAsInvalid(messages, rootCause);
        return this;
    }

    public boolean isInvalid() {
        return status.isInvalid();
    }

    public PluginStatus getStatus() {
        return status;
    }

    void markAsInvalidWithoutUpdatingBundleDescriptor(List<String> messages, Exception rootCause) {
        status = new PluginStatus(INVALID).setMessages(messages, rootCause);
    }

    public String fileName() {
        return bundleLocation.getName();
    }

    public boolean isCurrentOSValidForThisPlugin(String currentOS) {
        if (about == null || about.targetOperatingSystems.isEmpty()) {
            return true;
        }

        for (String targetOperatingSystem : about.targetOperatingSystems) {
            if (targetOperatingSystem.equalsIgnoreCase(currentOS)) {
                return true;
            }
        }
        return false;
    }

    public boolean isCurrentGocdVersionValidForThisPlugin() {
        if (about == null || about.targetGoVersion() == null) {
            return true;
        }

        Version targetGoVersion = new Version(about.targetGoVersion());
        Version currentGoVersion = new Version(CurrentGoCDVersion.getInstance().goVersion());

        return targetGoVersion.compareTo(currentGoVersion) <= 0;
    }

    public GoPluginBundleDescriptor bundleDescriptor() {
        return bundleDescriptor;
    }

    public void setBundleDescriptor(GoPluginBundleDescriptor bundleDescriptor) {
        this.bundleDescriptor = bundleDescriptor;
    }

    List<String> extensionClasses() {
        return extensionClasses;
    }

    public GoPluginDescriptor addExtensionClasses(List<String> extensionClasses) {
        this.extensionClasses.addAll(extensionClasses);
        return this;
    }

    @ToString
    @EqualsAndHashCode
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class About implements PluginDescriptor.About {
        @XmlElement
        private String name;
        @XmlElement
        private String version;
        @XmlElement(name = "target-go-version")
        private String targetGoVersion;
        @XmlElement
        private String description;
        @XmlElement
        private Vendor vendor;

        @XmlElementWrapper(name = "target-os")
        @XmlElement(name = "value")
        @Builder.Default
        private final List<String> targetOperatingSystems = new ArrayList<>();

        @Override
        public String name() {
            return name;
        }

        @Override
        public String version() {
            return version;
        }

        @Override
        public String targetGoVersion() {
            return targetGoVersion;
        }

        @Override
        public String description() {
            return description;
        }

        @Override
        public Vendor vendor() {
            return vendor;
        }

        @Override
        public List<String> targetOperatingSystems() {
            return targetOperatingSystems;
        }
    }

    @ToString
    @EqualsAndHashCode
    @NoArgsConstructor
    public static class Vendor implements PluginDescriptor.Vendor {
        @XmlElement
        private String name;
        @XmlElement
        private String url;

        public Vendor(String name, String url) {
            this.name = name;
            this.url = url;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public String url() {
            return url;
        }
    }

    /**
     * Only used for serialization
     */
    @NoArgsConstructor
    private static class Extension {
        @XmlAttribute(name = "class")
        String className;
    }

    private static class ExtensionAdapter extends XmlAdapter<Extension, String> {
        @Override
        public String unmarshal(Extension v) {
            return v.className;
        }

        /**
         * Not used, but included for completeness
         */
        @Override
        public Extension marshal(String v) {
            Extension extension = new Extension();
            extension.className = v;
            return extension;
        }
    }
}
