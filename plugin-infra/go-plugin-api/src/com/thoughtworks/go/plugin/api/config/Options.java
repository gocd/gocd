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

import java.util.ArrayList;
import java.util.List;

import com.thoughtworks.go.plugin.api.config.Option;

/**
 * Container for {@link com.thoughtworks.go.plugin.api.config.Option}
 */
@Deprecated
//Will be moved to internal scope
public class Options {
    private List<Option> options = new ArrayList<>();

    /**
     * Adds given option to container
     * @param option the instance of option to be added to container
     * @param <T> option type
     */
    public <T> void add(Option<T> option) {
        options.add(option.copy());
    }

    /**
     * Finds matching option by option name and sets specified value
     * @param option the option used for matching
     * @param value the value to be set to matched option
     * @param <T> the type of the option
     */
    public <T> void set(Option<T> option, T value) {
        findOption(option).setValue(value);
    }

    /**
     * Finds matching option by option name
     * @param option the option used for matching
     * @param <T> the type of the option
     * @return matched option
     * @throws RuntimeException when matching option not found
     */
    public <T> Option<T> findOption(Option<T> option) {
        for (Option candidateOption : options) {
            if (candidateOption.hasSameNameAs(option)) {
                return candidateOption;
            }
        }
        throw new RuntimeException("You tried to set an unexpected option");
    }

    public <T> boolean hasOption(Option<T> option) {
        for (Option candidateOption : options) {
            if (candidateOption.hasSameNameAs(option)) {
                return true;
            }
        }
        return false;
    }

    public <T> void addOrSet(Option<T> option, T value) {
        if (!hasOption(option)) {
            add(option);
        }
        set(option, value);
    }

    public int size() {
        return options.size();
    }
}
