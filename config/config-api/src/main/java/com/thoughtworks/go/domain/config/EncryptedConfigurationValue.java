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
package com.thoughtworks.go.domain.config;

import java.io.Serializable;
import java.util.Map;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.thoughtworks.go.config.ConfigTag;
import com.thoughtworks.go.config.ConfigValue;
import com.thoughtworks.go.config.Validatable;
import com.thoughtworks.go.config.ValidationContext;
import com.thoughtworks.go.domain.ConfigErrors;

@ConfigTag("encryptedValue")
public class EncryptedConfigurationValue implements Serializable, Validatable {

    @Expose
    @SerializedName("value")
    @ConfigValue
    private String value;

    public static final String VALUE = "value";

    private ConfigErrors errors = new ConfigErrors();

    public EncryptedConfigurationValue(String value) {
        this.value = value;
    }

    public EncryptedConfigurationValue() {
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        EncryptedConfigurationValue that = (EncryptedConfigurationValue) o;

        if (value != null ? !value.equals(that.value) : that.value != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return value != null ? value.hashCode() : 0;
    }

    public void setConfigAttributes(Object attributes) {
        Map attributesMap = (Map) attributes;
        if (attributesMap.containsKey(VALUE)) {
            value = (String) attributesMap.get(VALUE);
        }
    }

    @Override
    public void validate(ValidationContext validationContext) {

    }

    @Override
    public ConfigErrors errors() {
        return errors;
    }

    @Override
    public void addError(String fieldName, String message) {
        errors.add(fieldName, message);
    }

    @Override
    public String toString() {
        return "EncryptedConfigurationValue{" +
                "value='" + value + '\'' +
                '}';
    }
}
