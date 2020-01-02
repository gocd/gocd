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

import com.thoughtworks.go.CurrentGoCDVersion;
import com.thoughtworks.go.plugin.api.info.PluginDescriptor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.osgi.framework.Version;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static com.thoughtworks.go.plugin.infra.plugininfo.PluginStatus.State.ACTIVE;
import static com.thoughtworks.go.plugin.infra.plugininfo.PluginStatus.State.INVALID;

@ToString
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class GoPluginDescriptor implements PluginDescriptor {
    @EqualsAndHashCode.Include
    private String id;
    @EqualsAndHashCode.Include
    private String version = "1";
    @EqualsAndHashCode.Include
    private About about;

    /*
     * Absolute path to plugin jar e.g. $GO_SERVER_DIR/plugins/bundled/foo.jar
     */
    private String pluginJarFileLocation;
    /*
     * Path to bundle directory in plugin work folder. e.g. $PLUGIN_WORK_DIR/foo.jar/
     */
    private File bundleLocation;
    private boolean isBundledPlugin;

    /*
     * GoPluginBundleDescriptor.toString() uses toString of GoPluginDescriptor
     * Hence this needs to be excluded from the @ToString to avoid recursive call
     */
    @ToString.Exclude
    private GoPluginBundleDescriptor bundleDescriptor;

    @Builder.Default
    private PluginStatus status = new PluginStatus(ACTIVE);
    @Builder.Default
    private final List<String> extensionClasses = new ArrayList<>();

    @Override
    public String id() {
        return id;
    }

    @Override
    public String version() {
        return version;
    }

    @Override
    public About about() {
        return about;
    }

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

    public String pluginFileLocation() {
        return pluginJarFileLocation;
    }

    public String fileName() {
        return bundleLocation.getName();
    }

    public File bundleLocation() {
        return bundleLocation;
    }

    public boolean isBundledPlugin() {
        return isBundledPlugin;
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
    public static class About implements PluginDescriptor.About {
        private String name;
        private String version;
        private String targetGoVersion;
        private String description;
        private Vendor vendor;

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
    public static class Vendor implements PluginDescriptor.Vendor {
        private String name;
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
}
