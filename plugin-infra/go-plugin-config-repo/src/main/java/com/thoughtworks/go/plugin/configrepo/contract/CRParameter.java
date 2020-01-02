/*
 * Copyright 2020 ThoughtWorks, Inc.
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
package com.thoughtworks.go.plugin.configrepo.contract;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.thoughtworks.go.config.validation.NameTypeValidator;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class CRParameter extends CRBase {
    @SerializedName("name")
    @Expose
    private String name;
    @SerializedName("value")
    @Expose
    private String value;

    public CRParameter() {
    }

    public CRParameter(String name, String value) {
        this.name = name;
        this.value = value;
    }

    @Override
    public String getLocation(String parent) {
        String myLocation = getLocation() == null ? parent : getLocation();
        String key = this.name == null ? "unknown name" : this.name;
        return String.format("%s; Parameter (%s)", myLocation, key);
    }

    public String validateNameUniqueness(HashSet<String> keys) {
        if (keys.contains(this.getName()))
            return String.format("Param name '%s' is not unique.", this.getName());
        else
            keys.add(this.getName());
        return null;
    }

    @Override
    public void getErrors(ErrorCollection errors, String parentLocation) {
        String location = this.getLocation(parentLocation);
        if (new NameTypeValidator().isNameInvalid(name)) {
            errors.addError(location, NameTypeValidator.errorMessage("parameter", name));
        }
    }
}
