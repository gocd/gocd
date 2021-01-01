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
package com.thoughtworks.go.plugin.access.scm;


import com.thoughtworks.go.plugin.api.config.Option;
import com.thoughtworks.go.plugin.api.config.Options;
import com.thoughtworks.go.plugin.api.config.Property;

public class SCMConfiguration implements Comparable {

    public static final Option<Boolean> REQUIRED = Property.REQUIRED;
    public static final Option<Boolean> PART_OF_IDENTITY = Property.PART_OF_IDENTITY;
    public static final Option<Boolean> SECURE = Property.SECURE;
    public static final Option<String> DISPLAY_NAME = Property.DISPLAY_NAME;
    public static final Option<Integer> DISPLAY_ORDER = Property.DISPLAY_ORDER;

    private final Options options;
    private String key;
    private String value;

    public SCMConfiguration(String key) {
        this.key = key;
        this.options = new Options();

        this.options.add(REQUIRED);
        this.options.add(PART_OF_IDENTITY);
        this.options.add(SECURE);
        this.options.add(DISPLAY_NAME);
        this.options.add(DISPLAY_ORDER);
    }

    public SCMConfiguration(String key, String value) {
        this(key);
        this.value = value;
    }

    public SCMConfiguration(Property property) {
        this.key = property.getKey();
        this.value = property.getValue();
        this.options = property.getOptions();
    }

    public String getKey() {
        return key;
    }

    public <T> SCMConfiguration with(Option<T> option, T value) {
        options.set(option, value);
        return this;
    }

    public String getValue() {
        return value;
    }

    public boolean hasOption(Option<Boolean> option) {
        return getOption(option) == true;
    }

    public <T> T getOption(Option<T> option) {
        return options.findOption(option).getValue();
    }

    @Override
    public int compareTo(Object o) {
        return this.getOption(DISPLAY_ORDER) - ((SCMConfiguration) o).getOption(DISPLAY_ORDER);
    }
}


