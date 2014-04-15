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

package com.thoughtworks.go.config.server.security.ldap;

import java.util.Arrays;

import com.thoughtworks.go.config.ConfigCollection;
import com.thoughtworks.go.config.ConfigTag;
import com.thoughtworks.go.config.Validatable;
import com.thoughtworks.go.config.ValidationContext;
import com.thoughtworks.go.domain.BaseCollection;
import com.thoughtworks.go.domain.ConfigErrors;

@ConfigTag("bases")
@ConfigCollection(value = BaseConfig.class, minimum = 1)
public class BasesConfig extends BaseCollection<BaseConfig> implements Validatable {
    private static final String SEARCH_BASES = "basesConfig";

    private ConfigErrors errors = new ConfigErrors();

    public BasesConfig() {}

    public BasesConfig(BaseConfig... baseConfig) {
        this.addAll(Arrays.asList(baseConfig));
    }

    @Override
    public void validate(ValidationContext validationContext) {
    }

    public void validateBases() {
        if (isEmpty()) {
            addError(SEARCH_BASES, "Search Base is required for LDAP");
        }
    }

    @Override
    public ConfigErrors errors() {
        return errors;
    }

    @Override
    public void addError(String fieldName, String message) {
        errors.add(fieldName, message);
    }
}
