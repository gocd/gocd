/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.plugin.infra.plugininfo;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.thoughtworks.go.util.OperatingSystem;
import com.thoughtworks.go.plugin.api.info.PluginDescriptor;
import org.osgi.framework.Bundle;

import static com.thoughtworks.go.plugin.infra.plugininfo.PluginStatus.State.ACTIVE;
import static com.thoughtworks.go.plugin.infra.plugininfo.PluginStatus.State.INVALID;

public class GoPluginDescriptor implements PluginDescriptor {

    private String id;
    private String version = "1";

    private About about;
    private PluginStatus status = new PluginStatus(ACTIVE);

    private String pluginJarFileLocation;
    private File bundleLocation;
    private boolean bundledPlugin;
    private String bundleSymbolicName;
    private String bundleClassPath;
    private String bundleActivator;
    private Bundle bundle;

    public GoPluginDescriptor(String id, String version, About about, String pluginJarFileLocation, File bundleLocation, boolean isBundledPlugin) {
        this(id, pluginJarFileLocation, bundleLocation, isBundledPlugin);
        this.version = version;
        this.about = about;
    }

    public static GoPluginDescriptor usingId(String id, String pluginJarFileLocation, File bundleLocation, boolean isBundledPlugin) {
        return new GoPluginDescriptor(id, pluginJarFileLocation, bundleLocation, isBundledPlugin);
    }

    private GoPluginDescriptor(String id, String pluginJarFileLocation, File bundleLocation, boolean isBundledPlugin) {
        this.id = id;
        this.pluginJarFileLocation = pluginJarFileLocation;
        this.bundleLocation = bundleLocation;
        this.bundledPlugin = isBundledPlugin;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public String version() {
        return version;
    }

    public About about() {
        return about;
    }

    public GoPluginDescriptor markAsInvalid(List<String> messages, Exception rootCause) {
        status = new PluginStatus(INVALID).setMessages(messages, rootCause);
        return this;
    }

    public boolean isInvalid() {
        return status.isInvalid();
    }

    public PluginStatus getStatus() {
        return status;
    }

    public void setStatus(PluginStatus status) {
        this.status = status;
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

    public String bundleSymbolicName() {
        return bundleSymbolicName;
    }

    public String bundleClassPath() {
        return bundleClassPath;
    }

    public String bundleActivator() {
        return bundleActivator;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        GoPluginDescriptor that = (GoPluginDescriptor) o;

        if (about != null ? !about.equals(that.about) : that.about != null) {
            return false;
        }
        if (id != null ? !id.equals(that.id) : that.id != null) {
            return false;
        }
        if (version != null ? !version.equals(that.version) : that.version != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (version != null ? version.hashCode() : 0);
        result = 31 * result + (about != null ? about.hashCode() : 0);
        return result;
    }

    // TODO: Think about equals(), hashCode() and toString() of these classes.


    @Override
    public String toString() {
        return "GoPluginDescriptor{" +
                "id='" + id + '\'' +
                ", version='" + version + '\'' +
                ", about=" + about +
                ", status=" + status +
                ", pluginJarFileLocation='" + pluginJarFileLocation + '\'' +
                ", bundleLocation=" + bundleLocation +
                ", bundledPlugin=" + bundledPlugin +
                ", bundleSymbolicName='" + bundleSymbolicName + '\'' +
                ", bundleClassPath='" + bundleClassPath + '\'' +
                ", bundleActivator='" + bundleActivator + '\'' +
                ", bundle=" + bundle +
                '}';
    }

    public void updateBundleInformation(String symbolicName, String classPath, String bundleActivator) {
        this.bundleSymbolicName = symbolicName;
        this.bundleClassPath = classPath;
        this.bundleActivator = bundleActivator;
    }

    public GoPluginDescriptor setBundle(Bundle bundle) {
        this.bundle = bundle;
        return this;
    }

    public Bundle bundle() {
        return bundle;
    }

    public boolean isBundledPlugin() {
        return bundledPlugin;
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

    public static class About implements PluginDescriptor.About {
        private String name;
        private String version;
        private String targetGoVersion;
        private String description;
        private Vendor vendor;
        private final List<String> targetOperatingSystems = new ArrayList<>();

        public About(String name, String version, String targetGoVersion, String description, Vendor vendor, List<String> targetOperatingSystems) {
            this.name = name;
            this.version = version;
            this.targetGoVersion = targetGoVersion;
            this.description = description;
            this.vendor = vendor;

            if (targetOperatingSystems != null) {
                this.targetOperatingSystems.addAll(targetOperatingSystems);
            }
        }

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

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            About about = (About) o;

            if (description != null ? !description.equals(about.description) : about.description != null) {
                return false;
            }
            if (name != null ? !name.equals(about.name) : about.name != null) {
                return false;
            }
            if (targetGoVersion != null ? !targetGoVersion.equals(about.targetGoVersion) : about.targetGoVersion != null) {
                return false;
            }
            if (targetOperatingSystems != null ? !targetOperatingSystems.equals(about.targetOperatingSystems) : about.targetOperatingSystems != null) {
                return false;
            }
            if (vendor != null ? !vendor.equals(about.vendor) : about.vendor != null) {
                return false;
            }
            if (version != null ? !version.equals(about.version) : about.version != null) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            int result = name != null ? name.hashCode() : 0;
            result = 31 * result + (version != null ? version.hashCode() : 0);
            result = 31 * result + (targetGoVersion != null ? targetGoVersion.hashCode() : 0);
            result = 31 * result + (description != null ? description.hashCode() : 0);
            result = 31 * result + (vendor != null ? vendor.hashCode() : 0);
            result = 31 * result + (targetOperatingSystems != null ? targetOperatingSystems.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return "About{" +
                    "name='" + name + '\'' +
                    ", version='" + version + '\'' +
                    ", targetGoVersion='" + targetGoVersion + '\'' +
                    ", description='" + description + '\'' +
                    ", vendor=" + vendor +
                    ", targetOperatingSystems=" + targetOperatingSystems +
                    '}';
        }
    }

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

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            Vendor vendor = (Vendor) o;

            if (name != null ? !name.equals(vendor.name) : vendor.name != null) {
                return false;
            }
            if (url != null ? !url.equals(vendor.url) : vendor.url != null) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            int result = name != null ? name.hashCode() : 0;
            result = 31 * result + (url != null ? url.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return "Vendor{" +
                    "name='" + name + '\'' +
                    ", url='" + url + '\'' +
                    '}';
        }
    }
}
