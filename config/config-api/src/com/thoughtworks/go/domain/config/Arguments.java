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

package com.thoughtworks.go.domain.config;

import java.util.ArrayList;

import com.thoughtworks.go.config.Argument;
import com.thoughtworks.go.config.ConfigCollection;
import com.thoughtworks.go.config.Validatable;
import com.thoughtworks.go.config.ValidationContext;
import com.thoughtworks.go.domain.BaseCollection;
import com.thoughtworks.go.domain.ConfigErrors;

@ConfigCollection(Argument.class)
public class Arguments extends BaseCollection<Argument> implements Validatable {
    private final ConfigErrors configErrors = new ConfigErrors();

    public Arguments() {
    }

    public Arguments(Argument... args) {
        super(args);
    }

    public void validate(ValidationContext validationContext) {
    }

    public ConfigErrors errors() {
        return configErrors;
    }

    public void addError(String fieldName, String message) {
        configErrors.add(fieldName, message);
    }

    public String[] toStringArray() {
        ArrayList<String> list = new ArrayList<>(this.size());
        for (Argument arg : this) {
            list.add(arg.getValue());
        }
        return list.toArray(new String[list.size()]);
    }
}
