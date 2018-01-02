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

package com.thoughtworks.go.plugin.api.config;


/**
 * Represents single configuration property.
 * A given property can have set of metadata which can be represented by options.
 * The valid options are Property.REQUIRED, Property.PART_OF_IDENTITY, Property.SECURE, Property.DISPLAY_NAME and Property.DISPLAY_ORDER
 */
@Deprecated
//Will be moved to internal scope
public class Property {

    /**
     * Option to specify if a property is a mandatory when configuration is captured
     */
    public static final Option<Boolean> REQUIRED = new Option<>("REQUIRED", Boolean.TRUE);

    /**
     * Option to specify if a property is part of group of properties used to uniquely identify material
     */
    public static final Option<Boolean> PART_OF_IDENTITY = new Option<>("PART_OF_IDENTITY", Boolean.TRUE);

    /**
     * Option to specify if a property is a secure property.
     * If the property is secure, property value will be always stored (in configuration) and displayed in encrypted text
     */
    public static final Option<Boolean> SECURE = new Option<>("SECURE", Boolean.FALSE);

    /**
     * Option to specify the display name for the property
     */
    public static final Option<String> DISPLAY_NAME = new Option<>("DISPLAY_NAME", "");

    /**
     * Option to specify order of display of property on screen
     */
    public static final Option<Integer> DISPLAY_ORDER = new Option<>("DISPLAY_ORDER", 0);

    private final Options options;

    private String key;
    private String value;
    private String defaultValue;

    public Property(String key) {
        this.key = key;
        this.options = new Options();
    }

    protected Property(String key, String value) {
        this(key);
        this.value = value;
    }

    public Property(String key, String value, String defaultValue) {
        this(key, value);
        this.defaultValue = defaultValue;
    }

    /**
     * Gets property key
     * @return property key
     */
    public String getKey() {
        return key;
    }

    /**
     * Adds an option
     * @param option Option type to be added
     * @param value Option value
     * @param <T> Type of option value
     * @return current property instance (this)
     */
    final public <T> Property with(Option<T> option, T value) {
        if(value != null){
            options.addOrSet(option, value);
        }
        return this;
    }

    /**
     * Gets property value.
     *
     * @return property value
     */
    public String getValue() {
        return value == null ? defaultValue : value;
    }

      /**
     * Gets value for given option
     * @param option for which value needs to fetched
     * @param <T> type of option
     * @return option value
     */
    public  <T> T getOption(Option<T> option) {
        return options.findOption(option).getValue();
    }

    /**
     * Gets all options of property
     * @return all options of property
     */
    public Options getOptions() {
        return options;
    }

    public Property withDefault(String defaultValue) {
        this.defaultValue = defaultValue;
        return this;
    }
}


