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

package com.thoughtworks.go.plugin.domain.authentication;

import com.thoughtworks.go.plugin.api.info.PluginDescriptor;
import com.thoughtworks.go.plugin.domain.common.PluggableInstanceSettings;
import com.thoughtworks.go.plugin.domain.common.PluginConstants;
import com.thoughtworks.go.plugin.domain.common.PluginInfo;

@Deprecated
public class AuthenticationPluginInfo extends PluginInfo {

    private final String displayName;
    private final String displayImageURL;
    private final boolean supportsPasswordBasedAuthentication;
    private final boolean supportsWebBasedAuthentication;

    public AuthenticationPluginInfo(PluginDescriptor descriptor, String displayName, String displayImageURL, boolean supportsPasswordBasedAuthentication, boolean supportsWebBasedAuthentication, PluggableInstanceSettings pluginSettings) {
        super(descriptor, PluginConstants.AUTHENTICATION_EXTENSION, pluginSettings);
        this.displayName = displayName;
        this.displayImageURL = displayImageURL;
        this.supportsPasswordBasedAuthentication = supportsPasswordBasedAuthentication;
        this.supportsWebBasedAuthentication = supportsWebBasedAuthentication;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDisplayImageURL() {
        return displayImageURL;
    }

    public boolean isSupportsPasswordBasedAuthentication() {
        return supportsPasswordBasedAuthentication;
    }

    public boolean isSupportsWebBasedAuthentication() {
        return supportsWebBasedAuthentication;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        AuthenticationPluginInfo that = (AuthenticationPluginInfo) o;

        if (supportsPasswordBasedAuthentication != that.supportsPasswordBasedAuthentication) return false;
        if (supportsWebBasedAuthentication != that.supportsWebBasedAuthentication) return false;
        if (displayName != null ? !displayName.equals(that.displayName) : that.displayName != null) return false;
        return displayImageURL != null ? displayImageURL.equals(that.displayImageURL) : that.displayImageURL == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (displayName != null ? displayName.hashCode() : 0);
        result = 31 * result + (displayImageURL != null ? displayImageURL.hashCode() : 0);
        result = 31 * result + (supportsPasswordBasedAuthentication ? 1 : 0);
        result = 31 * result + (supportsWebBasedAuthentication ? 1 : 0);
        return result;
    }
}
