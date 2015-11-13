/*
 * Copyright 2015 ThoughtWorks, Inc.
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
 */

package com.thoughtworks.go.config;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.thoughtworks.go.domain.ConfigErrors;


@ConfigTag("tab")
public class Tab implements Validatable {
    @ConfigAttribute(value = "name", optional = false)
    private String name;
    @ConfigAttribute(value = "path", optional = false)
    private String path;
    private ConfigErrors configErrors = new ConfigErrors();

    private static final Pattern TAB_NAME_PATTERN = Pattern.compile("[a-zA-Z0-9_\\-.]+");
    private static final Pattern TAB_PATH_PATTERN = Pattern.compile("[\\S]+");
    public static final String NAME = "name";
    public static final String PATH = "path";
    private static final int NAME_MAX_LENGTH = 15;

    public Tab() {
    }

    public Tab(String name, String path) {
        this.name = name;
        this.path = path;
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String toString() {
        return "Tab[" + name + ", " + path + "]";
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Tab tab = (Tab) o;

        if (name != null ? !name.equals(tab.name) : tab.name != null) {
            return false;
        }
        if (path != null ? !path.equals(tab.path) : tab.path != null) {
            return false;
        }

        return true;
    }

    public int hashCode() {
        int result;
        result = (name != null ? name.hashCode() : 0);
        result = 31 * result + (path != null ? path.hashCode() : 0);
        return result;
    }

    public boolean validateTree(ValidationContext validationContext) {
        validate(validationContext);
        return errors().isEmpty();
    }

    public void validate(ValidationContext validationContext) {
        validateTabNamePathCorrectness();
        validateTabNameSize();
    }

    public ConfigErrors errors() {
        return configErrors;
    }

    public void addError(String fieldName, String message) {
        configErrors.add(fieldName, message);
    }

    public void validateTabNameUniqueness(ArrayList<Tab> tabs) {
        for (Tab tab : tabs) {
            if(name.equals(tab.getName())){
                this.addError(NAME, String.format("Tab name '%s' is not unique.", name));
                tab.addError(NAME, String.format("Tab name '%s' is not unique.", name));
                return;
            }
        }
        tabs.add(this);
    }

    public void validateTabNamePathCorrectness() {
        Matcher matcher = TAB_NAME_PATTERN.matcher(name);
        if (!matcher.matches()) {
            this.addError(NAME, String.format("Tab name '%s' is invalid. This must be alphanumeric and can contain underscores and periods.", name));
        }
        matcher = TAB_PATH_PATTERN.matcher(path);
        if (!matcher.matches()) {
            this.addError(PATH, String.format("Tab path '%s' is invalid. This must be a valid file path.", path));
        }
    }

    public void validateTabNameSize() {
        if (this.name.length() > NAME_MAX_LENGTH) {
            addError(NAME, String.format("Tab name should not exceed 15 characters"));
        }
    }
}
