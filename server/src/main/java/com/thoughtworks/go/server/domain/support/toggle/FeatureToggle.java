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
package com.thoughtworks.go.server.domain.support.toggle;

import com.google.gson.annotations.Expose;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class FeatureToggle {
    @Expose
    private final String key;
    @Expose
    private String description;
    @Expose
    private boolean value;

    private boolean hasBeenChangedFromDefault = false;

    public FeatureToggle(String key, String description, boolean value) {
        this.key = key;
        this.description = description;
        this.value = value;
    }

    public FeatureToggle(FeatureToggle other) {
        this(other.key, other.description, other.value);
        this.hasBeenChangedFromDefault = other.hasBeenChangedFromDefault;
    }

    public boolean hasSameKeyAs(String otherKey) {
        return key.equalsIgnoreCase(otherKey);
    }

    public FeatureToggle withValueHasBeenChangedFlag(boolean hasBeenChangedFromDefault) {
        FeatureToggle toggle = new FeatureToggle(this);
        toggle.hasBeenChangedFromDefault = hasBeenChangedFromDefault;
        return toggle;
    }

    public FeatureToggle withDescription(String description) {
        FeatureToggle toggle = new FeatureToggle(this);
        toggle.description = description;
        return toggle;
    }

    public FeatureToggle withValue(boolean newValue) {
        FeatureToggle toggle = new FeatureToggle(this);
        toggle.value = newValue;
        return toggle;
    }

    public boolean hasSameValueAs(FeatureToggle other) {
        return value == other.value;
    }

    public boolean isOn() {
        return value;
    }

    public String key() {
        return key;
    }

    public String description() {
        return description;
    }

    public boolean hasBeenChangedFromDefault() {
        return hasBeenChangedFromDefault;
    }

    @Override
    public boolean equals(Object o) {
        return EqualsBuilder.reflectionEquals(this, o);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
