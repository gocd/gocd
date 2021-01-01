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
package com.thoughtworks.go.plugin.domain.artifact;

import com.thoughtworks.go.plugin.api.info.PluginDescriptor;
import com.thoughtworks.go.plugin.domain.common.Image;
import com.thoughtworks.go.plugin.domain.common.PluggableInstanceSettings;
import com.thoughtworks.go.plugin.domain.common.PluginConstants;
import com.thoughtworks.go.plugin.domain.common.PluginInfo;

public class ArtifactPluginInfo extends PluginInfo {
    private final PluggableInstanceSettings storeConfigSettings;
    private final PluggableInstanceSettings artifactConfigSettings;
    private final PluggableInstanceSettings fetchArtifactSettings;
    private final Capabilities capabilities;

    public ArtifactPluginInfo(PluginDescriptor descriptor, PluggableInstanceSettings storeConfigSettings, PluggableInstanceSettings publishArtifactSettings, PluggableInstanceSettings fetchArtifactSettings, Image image, Capabilities capabilities) {
        super(descriptor, PluginConstants.ARTIFACT_EXTENSION, null, image);
        this.storeConfigSettings = storeConfigSettings;
        this.artifactConfigSettings = publishArtifactSettings;
        this.fetchArtifactSettings = fetchArtifactSettings;
        this.capabilities = capabilities;
    }

    public PluggableInstanceSettings getStoreConfigSettings() {
        return storeConfigSettings;
    }

    public PluggableInstanceSettings getArtifactConfigSettings() {
        return artifactConfigSettings;
    }

    public PluggableInstanceSettings getFetchArtifactSettings() {
        return fetchArtifactSettings;
    }

    public Capabilities getCapabilities() {
        return capabilities;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ArtifactPluginInfo)) return false;
        if (!super.equals(o)) return false;

        ArtifactPluginInfo that = (ArtifactPluginInfo) o;

        if (storeConfigSettings != null ? !storeConfigSettings.equals(that.storeConfigSettings) : that.storeConfigSettings != null)
            return false;
        if (artifactConfigSettings != null ? !artifactConfigSettings.equals(that.artifactConfigSettings) : that.artifactConfigSettings != null)
            return false;
        if (fetchArtifactSettings != null ? !fetchArtifactSettings.equals(that.fetchArtifactSettings) : that.fetchArtifactSettings != null)
            return false;
        return capabilities != null ? capabilities.equals(that.capabilities) : that.capabilities == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (storeConfigSettings != null ? storeConfigSettings.hashCode() : 0);
        result = 31 * result + (artifactConfigSettings != null ? artifactConfigSettings.hashCode() : 0);
        result = 31 * result + (fetchArtifactSettings != null ? fetchArtifactSettings.hashCode() : 0);
        result = 31 * result + (capabilities != null ? capabilities.hashCode() : 0);
        return result;
    }
}
