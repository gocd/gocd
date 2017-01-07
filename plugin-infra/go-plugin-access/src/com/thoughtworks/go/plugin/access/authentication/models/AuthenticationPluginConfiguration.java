/*
 * Copyright 2016 ThoughtWorks, Inc.
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

package com.thoughtworks.go.plugin.access.authentication.models;

public class AuthenticationPluginConfiguration {
    private String displayName;
    private String displayImageURL;
    private boolean supportsWebBasedAuthentication;
    private boolean supportsPasswordBasedAuthentication;

    public AuthenticationPluginConfiguration(String displayName, String displayImageURL, boolean supportsWebBasedAuthentication, boolean supportsPasswordBasedAuthentication) {
        this.displayName = displayName;
        this.displayImageURL = displayImageURL;
        this.supportsWebBasedAuthentication = supportsWebBasedAuthentication;
        this.supportsPasswordBasedAuthentication = supportsPasswordBasedAuthentication;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayImageURL() {
        return displayImageURL;
    }

    public void setDisplayImageURL(String displayImageURL) {
        this.displayImageURL = displayImageURL;
    }

    public boolean supportsWebBasedAuthentication() {
        return supportsWebBasedAuthentication;
    }

    public void setSupportsWebBasedAuthentication(boolean supportsWebBasedAuthentication) {
        this.supportsWebBasedAuthentication = supportsWebBasedAuthentication;
    }

    public boolean supportsPasswordBasedAuthentication() {
        return supportsPasswordBasedAuthentication;
    }

    public void setSupportsPasswordBasedAuthentication(boolean supportsPasswordBasedAuthentication) {
        this.supportsPasswordBasedAuthentication = supportsPasswordBasedAuthentication;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AuthenticationPluginConfiguration that = (AuthenticationPluginConfiguration) o;

        if (supportsPasswordBasedAuthentication != that.supportsPasswordBasedAuthentication) return false;
        if (supportsWebBasedAuthentication != that.supportsWebBasedAuthentication) return false;
        if (displayImageURL != null ? !displayImageURL.equals(that.displayImageURL) : that.displayImageURL != null)
            return false;
        if (displayName != null ? !displayName.equals(that.displayName) : that.displayName != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = displayName != null ? displayName.hashCode() : 0;
        result = 31 * result + (displayImageURL != null ? displayImageURL.hashCode() : 0);
        result = 31 * result + (supportsWebBasedAuthentication ? 1 : 0);
        result = 31 * result + (supportsPasswordBasedAuthentication ? 1 : 0);
        return result;
    }
}
