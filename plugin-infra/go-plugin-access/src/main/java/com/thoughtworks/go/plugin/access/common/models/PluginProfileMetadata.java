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
package com.thoughtworks.go.plugin.access.common.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.thoughtworks.go.plugin.domain.common.Metadata;

public class PluginProfileMetadata {

    @Expose
    @SerializedName("required")
    private final boolean required;

    @Expose
    @SerializedName("secure")
    private final boolean secure;

    public PluginProfileMetadata(boolean required, boolean secure) {
        this.required = required;
        this.secure = secure;
    }

    public boolean isRequired() {
        return required;
    }

    public boolean isSecure() {
        return secure;
    }

    public Metadata toMetadata() {
        return new Metadata(isRequired(), isSecure());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PluginProfileMetadata)) return false;

        PluginProfileMetadata that = (PluginProfileMetadata) o;

        if (required != that.required) return false;
        return secure == that.secure;
    }

    @Override
    public int hashCode() {
        int result = (required ? 1 : 0);
        result = 31 * result + (secure ? 1 : 0);
        return result;
    }
}
