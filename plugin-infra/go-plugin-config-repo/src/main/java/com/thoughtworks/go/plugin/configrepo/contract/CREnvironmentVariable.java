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
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.HashSet;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class CREnvironmentVariable extends CRBase {
    @SerializedName("name")
    @Expose
    private String name;
    @SerializedName("value")
    @Expose
    private String value;
    @SerializedName("encrypted_value")
    @Expose
    private String encryptedValue;

    public CREnvironmentVariable() {
    }

    public CREnvironmentVariable(String name) {
        this.name = name;
    }


    public CREnvironmentVariable(String key, String value) {
        this.name = key;
        this.value = value;
    }

    public CREnvironmentVariable(String name, String value, String encryptedValue) {
        this.name = name;
        this.value = value;
        this.encryptedValue = encryptedValue;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
    }

    public String validateNameUniqueness(HashSet<String> keys) {
        if (keys.contains(this.getName()))
            return String.format("Environment variable %s defined more than once", this.getName());
        else
            keys.add(this.getName());
        return null;
    }

    @Override
    public void getErrors(ErrorCollection errors, String parentLocation) {
        String location = this.getLocation(parentLocation);
        errors.checkMissing(location, "name", name);
        this.validateValue(errors, location);
    }

    @Override
    public String getLocation(String parent) {
        String myLocation = getLocation() == null ? parent : getLocation();
        String key = this.name == null ? "unknown name" : this.name;
        return String.format("%s; Environment variable (%s)", myLocation, key);
    }

    private void validateValue(ErrorCollection errors, String location) {
        if (StringUtils.isBlank(value) && StringUtils.isBlank(encryptedValue))
            errors.addError(location, "Environment variable value not set");
        if (!StringUtils.isBlank(value) && !StringUtils.isBlank(encryptedValue))
            errors.addError(location, "Environment variable value and encrypted_value is set. Only one field can be assigned.");
    }

    public boolean hasEncryptedValue() {
        return !StringUtils.isBlank(encryptedValue);
    }

    public boolean hasValue() {
        return !StringUtils.isBlank(value);
    }
}
