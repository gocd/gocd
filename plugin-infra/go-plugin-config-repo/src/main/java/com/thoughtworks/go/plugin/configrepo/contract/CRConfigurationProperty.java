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
package com.thoughtworks.go.plugin.configrepo.contract;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class CRConfigurationProperty extends CRBase {
    @SerializedName("key")
    @Expose
    private String key;
    @SerializedName("value")
    @Expose
    private String value;
    @SerializedName("encrypted_value")
    @Expose
    private String encryptedValue;

    public CRConfigurationProperty() {
    }

    public CRConfigurationProperty(String key, String value, String encryptedValue) {
        this.key = key;
        this.value = value;
        this.encryptedValue = encryptedValue;
    }

    public CRConfigurationProperty(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public CRConfigurationProperty(String key) {
        this.key = key;
    }


    public boolean hasEncryptedValue() {
        return StringUtils.isNotBlank(encryptedValue);
    }

    public boolean hasPlainTextValue() {
        return StringUtils.isNotBlank(value);
    }

    @Override
    public void getErrors(ErrorCollection errors, String parentLocation) {
        String location = this.getLocation(parentLocation);
        errors.checkMissing(location, "key", key);
        this.validateValues(errors, location);
    }

    @Override
    public String getLocation(String parent) {
        String myLocation = getLocation() == null ? parent : getLocation();
        String key = this.key == null ? "unknown key" : this.key;
        return String.format("%s; Configuration property (%s)", myLocation, key);
    }

    private void validateValues(ErrorCollection errors, String location) {
        if (this.hasEncryptedValue() && this.hasPlainTextValue()) {
            errors.addError(location, "Configuration property must have 'value' or 'encrypted_value' set. Please only one");
        }
    }
}
