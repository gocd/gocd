/*
 * Copyright 2017 ThoughtWorks, Inc.
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

package com.thoughtworks.go.plugin.access.configrepo.contract;

import com.thoughtworks.go.config.validation.NameTypeValidator;
import com.thoughtworks.go.plugin.access.configrepo.ErrorCollection;
import com.thoughtworks.go.util.StringUtil;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import java.util.HashSet;

public class CRParameter extends CRBase {
    private String name;
    private String value;

    public CRParameter() {}

    public CRParameter(String name) {
        this.name = name;
    }

    public CRParameter(String name, String value) {
        this.name = name;
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CRParameter that = (CRParameter) o;

        if (name != null ? !name.equals(that.name) : that.name != null) {
            return false;
        }
        return value != null ? value.equals(that.value) : that.value == null;
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + (value != null ? value.hashCode() : 0);
        return result;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String getLocation(String parent) {
        String myLocation = getLocation() == null ? parent : getLocation();
        String key = this.name == null ? "unknown name" : this.name;
        return String.format("%s; Parameter (%s)", myLocation, key);
    }

    public String validateNameUniqueness(HashSet<String> keys) {
        if(keys.contains(this.getName()))
            return String.format("Param name '%s' is not unique.",this.getName());
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
