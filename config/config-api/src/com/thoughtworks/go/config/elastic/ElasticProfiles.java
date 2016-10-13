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

package com.thoughtworks.go.config.elastic;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.domain.ConfigErrors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@ConfigTag("profiles")
@ConfigCollection(ElasticProfile.class)
public class ElasticProfiles extends ArrayList<ElasticProfile> implements Validatable {
    private final ConfigErrors errors = new ConfigErrors();

    public ElasticProfiles() {
    }

    public ElasticProfiles(ElasticProfile... profiles) {
        super(Arrays.asList(profiles));
    }

    public ElasticProfile find(String profileId) {
        for (ElasticProfile profile : this) {
            if (profile.getId().equals(profileId)) {
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
        Map<String, ElasticProfile> profiles = new HashMap<>();
        for (ElasticProfile elasticProfile : this) {
            elasticProfile.validateIdUniquness(profiles);
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
