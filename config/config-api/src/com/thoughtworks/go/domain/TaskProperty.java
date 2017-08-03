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

package com.thoughtworks.go.domain;

public final class TaskProperty {
    private final String name;
    private final String value;
    private final String cssClass;

    public TaskProperty(String name, String value) {
        this(name, value, defaultCssClass(name));
    }

    private static String defaultCssClass(String name) {
       return name.toLowerCase().replaceAll(":$", "");
    }

    @Override public String toString() {
        return String.format("TaskProperty{name='%s', value='%s', cssClass='%s'}", name, value, cssClass);
    }

    public TaskProperty(String name, String value, String cssClass) {
        this.name = name;
        this.value = value;
        this.cssClass = defaultCssClass(cssClass);
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        TaskProperty that = (TaskProperty) o;

        if (cssClass != null ? !cssClass.equals(that.cssClass) : that.cssClass != null) {
            return false;
        }
        if (name != null ? !name.equals(that.name) : that.name != null) {
            return false;
        }
        if (value != null ? !value.equals(that.value) : that.value != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (value != null ? value.hashCode() : 0);
        result = 31 * result + (cssClass != null ? cssClass.hashCode() : 0);
        return result;
    }

    public String getValue() {
        return value;
    }

    public String getCssClass() {
        return cssClass;
    }
}
