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

package com.thoughtworks.go.config;

import com.thoughtworks.go.domain.ConfigErrors;
import org.apache.commons.lang.StringUtils;

import static com.thoughtworks.go.util.StringUtil.nullToBlank;

@ConfigTag("passwordFile")
public class PasswordFileConfig implements Validatable {
    @ConfigAttribute(value = "path") private String path = "";
    private final ConfigErrors configErrors = new ConfigErrors();

    public PasswordFileConfig() {
    }

    public PasswordFileConfig(String path) {
        this.path = nullToBlank(path);
    }

    public String path() {
        return path;
    }

    public void validate(ValidationContext validationContext) {
        if (isEnabled() && !validationContext.systemEnvironment().inbuiltLdapPasswordAuthEnabled()) {
            configErrors.add("base", "'passwordFile' tag has been deprecated in favour of bundled PasswordFile plugin. Use that instead.");
        }
    }

    public ConfigErrors errors() {
        return configErrors;
    }

    public void addError(String fieldName, String message) {
        configErrors.add(fieldName, message);
    }

    public boolean equals(Object other) {
        if (this == other) { return true; }
        if (other == null) { return false; }
        if (this.getClass() != other.getClass()) { return false; }
        return equals((PasswordFileConfig) other);
    }

    private boolean equals(PasswordFileConfig other) {
        if (!path.equals(other.path)) { return false; }
        return true;
    }

    public int hashCode() {
        return (path != null ? path.hashCode() : 0);
    }

    public boolean isEnabled() {
        return !StringUtils.isBlank(path);
    }
}
