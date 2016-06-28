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
 * Option could be used to specify metadata for a Property.
 * @param <T> type for the option value
 */
@Deprecated
//Will be moved to internal scope
public class Option<T> {
    private String name;
    private T value;

    public Option(String name, T value) {
        this.name = name;
        this.value = value;
    }

    /**
     * Sets the value for this option
     * @param value the value to be set for this option
     */
    public void setValue(T value) {
        this.value = value;
    }

    /**
     * Gets value for this option
     * @return value for this option
     */
    public T getValue() {
        return value;
    }

    /**
     * Creates copy of this option
     * @return copy of this option
     */
    public Option<T> copy() {
        return new Option<>(name, value);
    }

    /**
     * Checks if this option has same name as the provided option
     * @param <T> type for the option value
     * @param option the option for which name equality has to be checked
     * @return true if name matches
     */
    public <T> boolean hasSameNameAs(Option<T> option) {
        return name.equals(option.name);
    }
}
