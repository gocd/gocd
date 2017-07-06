/*
 * Copyright 2017 ThoughtWorks, Inc.
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

package com.thoughtworks.go.server.ui.plugins;

import com.thoughtworks.go.plugin.access.common.models.Image;
import com.thoughtworks.go.plugin.access.elastic.ElasticAgentPluginConstants;
import com.thoughtworks.go.plugin.api.info.PluginDescriptor;

@Deprecated
public class ElasticPluginInfo extends NewPluginInfo {
    private final PluggableInstanceSettings profileSettings;
    private final Image image;

    public ElasticPluginInfo(PluginDescriptor descriptor, PluggableInstanceSettings profileSettings, Image image) {
        super(descriptor, ElasticAgentPluginConstants.EXTENSION_NAME);
        this.profileSettings = profileSettings;
        this.image = image;
    }

    public PluggableInstanceSettings getProfileSettings() {
        return profileSettings;
    }

    public Image getImage() {
        return image;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        ElasticPluginInfo that = (ElasticPluginInfo) o;

        if (profileSettings != null ? !profileSettings.equals(that.profileSettings) : that.profileSettings != null)
            return false;
        return image != null ? image.equals(that.image) : that.image == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (profileSettings != null ? profileSettings.hashCode() : 0);
        result = 31 * result + (image != null ? image.hashCode() : 0);
        return result;
    }
}
