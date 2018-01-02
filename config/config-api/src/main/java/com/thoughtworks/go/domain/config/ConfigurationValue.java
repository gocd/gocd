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

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.thoughtworks.go.config.ConfigTag;
import com.thoughtworks.go.config.ConfigValue;
import com.thoughtworks.go.config.Validatable;
import com.thoughtworks.go.config.ValidationContext;
import com.thoughtworks.go.domain.ConfigErrors;

import java.io.Serializable;
import java.util.Map;

@ConfigTag("value")
public class ConfigurationValue implements Serializable, Validatable {

    @Expose
    @SerializedName("value")
    @ConfigValue
    private String value;
    private ConfigErrors errors = new ConfigErrors();

    public static final String VALUE = "value";

    public ConfigurationValue(String value) {
        this.value = value;
    }

    public ConfigurationValue(boolean value) {
        this(String.valueOf(value));
    }

    public ConfigurationValue(Number value) {
        this(String.valueOf(value));
    }

    public ConfigurationValue() {
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

        ConfigurationValue that = (ConfigurationValue) o;

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
        return "ConfigurationValue{" +
                "value='" + value + '\'' +
                '}';
    }
}
