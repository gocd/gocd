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

package com.thoughtworks.go.domain.packagerepository;

import java.util.Collections;
import java.util.HashMap;

import com.thoughtworks.go.config.ConfigCollection;
import com.thoughtworks.go.config.ConfigTag;
import com.thoughtworks.go.config.Validatable;
import com.thoughtworks.go.config.ValidationContext;
import com.thoughtworks.go.domain.BaseCollection;
import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.util.ListUtil;

@ConfigTag("packages")
@ConfigCollection(value = PackageDefinition.class)
public class Packages extends BaseCollection<PackageDefinition> implements Validatable {
    public Packages() {
    }

    public Packages(PackageDefinition... packageDefinitions) {
        Collections.addAll(this, packageDefinitions);
    }

    public PackageDefinition find(final String id) {
        return ListUtil.find(this, new ListUtil.Condition() {
            @Override
            public <T> boolean isMet(T packageDefinition) {
                return ((PackageDefinition) packageDefinition).getId().equals(id);
            }
        });
    }

    @Override
    public void validate(ValidationContext validationContext) {
        HashMap<String, PackageDefinition> nameMap = new HashMap<>();
        for(PackageDefinition packageDefinition : this){
            packageDefinition.validateNameUniqueness(nameMap);
        }
    }

    @Override
    public ConfigErrors errors() {
        return new ConfigErrors();
    }

    @Override
    public void addError(String fieldName, String message) {
    }
}
