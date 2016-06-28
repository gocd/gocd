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

package com.thoughtworks.go.server.domain.support.toggle;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FeatureToggles {
    private List<FeatureToggle> toggles;

    public FeatureToggles() {
        this(new ArrayList<FeatureToggle>());
    }

    public FeatureToggles(FeatureToggle... toggles) {
        this(Arrays.asList(toggles));
    }

    public FeatureToggles(List<FeatureToggle> toggles) {
        this.toggles = toggles;
    }

    public List<FeatureToggle> all() {
        return toggles;
    }

    public FeatureToggle find(String key) {
        for (FeatureToggle toggle : toggles) {
            if (toggle.hasSameKeyAs(key)) {
                return toggle;
            }
        }
        return null;
    }

    public FeatureToggles overrideWithTogglesIn(FeatureToggles overridingToggles) {
        List<FeatureToggle> mergedToggles = new ArrayList<>();

        for (FeatureToggle availableToggle : toggles) {
            FeatureToggle toggleToAdd = availableToggle;

            FeatureToggle overriddenToggle = overridingToggles.find(availableToggle.key());
            if (overriddenToggle != null) {
                toggleToAdd = overriddenToggle.withValueHasBeenChangedFlag(!overriddenToggle.hasSameValueAs(availableToggle));
            }

            if (toggleToAdd.description() == null) {
                toggleToAdd = toggleToAdd.withDescription(availableToggle.description());
            }

            mergedToggles.add(toggleToAdd);
        }

        return new FeatureToggles(mergedToggles);
    }

    public FeatureToggles changeToggleValue(String key, boolean newValue) {
        List<FeatureToggle> newTogglesList = new ArrayList<>();
        boolean toggleHasBeenFound = false;

        for (FeatureToggle existingToggle : toggles) {
            FeatureToggle toggleToAdd = existingToggle;
            if (existingToggle.hasSameKeyAs(key)) {
                toggleToAdd = new FeatureToggle(existingToggle).withValue(newValue);
                toggleHasBeenFound = true;
            }

            newTogglesList.add(toggleToAdd);
        }

        if (!toggleHasBeenFound) {
            newTogglesList.add(new FeatureToggle(key, null, newValue));
        }

        return new FeatureToggles(newTogglesList);
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
