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
package com.thoughtworks.go.config.rules;

import com.thoughtworks.go.config.Validatable;
import com.thoughtworks.go.config.ValidationContext;
import com.thoughtworks.go.domain.BaseCollection;
import com.thoughtworks.go.domain.ConfigErrors;

import java.util.*;

public abstract class RuleAwarePluginProfiles<T extends RuleAwarePluginProfile> extends BaseCollection<T> implements Validatable {
    private final ConfigErrors errors = new ConfigErrors();

    public RuleAwarePluginProfiles() {
    }

    public RuleAwarePluginProfiles(T... profiles) {
        super(Arrays.asList(profiles));
    }

    public RuleAwarePluginProfiles(List<T> profiles) {
        super(profiles);
    }

    public T find(String profileId) {
        for (T profile : this) {
            if (profile.getId().equals(profileId)) {
                return profile;
            }
        }
        return null;
    }

    public List<T> findByPluginId(String pluginId) {
        List<T> list = new ArrayList<>();
        for (T profile : this) {
            if (profile.getPluginId().equals(pluginId)) {
                list.add(profile);
            }
        }
        return list;
    }

    public T findByPluginIdAndProfileId(String pluginId, String profileId) {
        for (T profile : this) {
            if (profile.getPluginId().equals(pluginId) && profile.getId().equals(profileId)) {
                return profile;
            }
        }

        return null;
    }

    @Override
    public void validate(ValidationContext validationContext) {
        validateIdUniqueness();
    }

    private void validateIdUniqueness() {
        Map<String, RuleAwarePluginProfile> profiles = new HashMap<>();
        for (RuleAwarePluginProfile pluginProfile : this) {
            pluginProfile.validateIdUniqueness(profiles);
        }
    }

    @Override
    public ConfigErrors errors() {
        return errors;
    }

    @Override
    public void addError(String fieldName, String message) {
        errors().add(fieldName, message);
    }

}
