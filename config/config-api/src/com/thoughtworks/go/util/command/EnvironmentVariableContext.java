/*************************GO-LICENSE-START*********************************
 * Copyright 2016 ThoughtWorks, Inc.
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

package com.thoughtworks.go.util.command;

import com.thoughtworks.go.util.GoConstants;

import java.io.Serializable;
import java.util.*;

import static java.lang.String.format;

/**
 * @understands a set of variables to be passed to the Agent for a job
 */
public class EnvironmentVariableContext implements Serializable {


    public static class EnvironmentVariable implements Serializable {
        private String name;
        private String value;
        private boolean secure;
        public static final String MASK_VALUE = "********";

        EnvironmentVariable() {
        }

        public EnvironmentVariable(String name, String value) {
            this.name = name;
            this.value = value;
            this.secure = false;
        }

        public EnvironmentVariable(String key, String value, boolean secure) {
            this(key, value);
            this.secure = secure;
        }


        public String name() {
            return name;
        }

        public String value() {
            return value;
        }

        public String valueForDisplay() {
            return isSecure() ? EnvironmentVariable.MASK_VALUE : value();
        }

        @Override public String toString() {
            return format("%s => %s",name,value);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            EnvironmentVariable that = (EnvironmentVariable) o;

            if (!name.equals(that.name)) {
                return false;
            }
            if (!value.equals(that.value)) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            int result = name.hashCode();
            result = 31 * result + value.hashCode();
            return result;
        }

        public boolean isSecure() {
            return secure;
        }
    }

    public static final String GO_ENVIRONMENT_NAME = "GO_ENVIRONMENT_NAME";

    private final List<EnvironmentVariable> properties = new ArrayList<>();

    public EnvironmentVariableContext() {
    }

    public EnvironmentVariableContext(String key, String value) {
        setProperty(key, value, false);
    }

    public void setProperty(String key, String value, boolean isSecure) {
        properties.add(new EnvironmentVariable(key, value, isSecure));
    }

    public void setPropertyWithEscape(String key, String value) {
        properties.add(new EnvironmentVariable(escapeEnvironmentVariable(key), value));
    }

    public String getProperty(String key) {
        EnvironmentVariable environmentVariable = getEnvironmentVariable(key);
        if (environmentVariable != null) {
            return environmentVariable.value();
        }
        return null;
    }

    public List<String> getPropertyKeys() {
        ArrayList<String> keys = new ArrayList<>(properties.size());
        for (EnvironmentVariable property : properties) {
            keys.add(property.name());
        }
        return keys;
    }

    public List<EnvironmentVariable> getSecureEnvironmentVariables() {
        List<EnvironmentVariable> environmentVariables = new ArrayList<>();
        for (EnvironmentVariable property : properties) {
            if(property.isSecure()) {
                environmentVariables.add(property);
            }
        }
        return environmentVariables;
    }

    public String getPropertyForDisplay(String key) {
        EnvironmentVariable environmentVariable = getEnvironmentVariable(key);
        if (environmentVariable != null) {
            return environmentVariable.valueForDisplay();
        }
        return null;
    }

    private EnvironmentVariable getEnvironmentVariable(String key) {
        for (int i = properties.size()-1; i >= 0 ; i--) {
            EnvironmentVariable property = properties.get(i);
            if (property.name().equals(key)) {
                return property;
            }
        }
        return null;
    }

    public Boolean isPropertySecure(String key) {
        EnvironmentVariable environmentVariable = getEnvironmentVariable(key);
        if (environmentVariable != null) {
            return environmentVariable.isSecure();
        }
        return false;
    }

    public boolean hasProperty(String name) {
        for (EnvironmentVariable property : properties) {
            if (property.name().equals(name)) {
                return true;
            }
        }
        return false;
    }

    public Map<String, String> getProperties() {
        HashMap<String, String> map = new HashMap<>();
        for (EnvironmentVariable property : properties) {
            map.put(property.name(), property.value());
        }
        return map;
    }

    public static String escapeEnvironmentVariable(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("[^A-Za-z0-9_]", "_").toUpperCase();
    }

    public void addAll(EnvironmentVariableContext another) {
        this.properties.addAll(another.properties);
    }

    @Override public String toString() {
        return "EnvironmentVariableContext{" +
                "properties=" + properties +
                '}';
    }

    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (that == null) {
            return false;
        }
        if (this.getClass() != that.getClass()) {
            return false;
        }

        return equals((EnvironmentVariableContext) that);
    }

    private boolean equals(EnvironmentVariableContext that) {
        if (!this.properties.equals(that.properties)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return properties != null ? properties.hashCode() : 0;
    }

    public List<String> report(Collection<String> predefinedEnvs) {
        ArrayList<String> lines = new ArrayList<>(properties.size());
        Set<String> existing = new HashSet<>(predefinedEnvs);
        for (EnvironmentVariable property : properties) {
            String name = property.name;
            String value = property.value;
            if (value != null) {
                if (existing.contains(name)) {
                    lines.add(format("[%s] overriding environment variable '%s' with value '%s'", GoConstants.PRODUCT_NAME, name, property.valueForDisplay()));
                } else {
                    lines.add(format("[%s] setting environment variable '%s' to value '%s'", GoConstants.PRODUCT_NAME, name, property.valueForDisplay()));
                }
                existing.add(name);
            }
        }
        return lines;
    }
}
