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

package com.thoughtworks.go.helper;

import com.thoughtworks.go.config.EnvironmentVariableConfig;
import com.thoughtworks.go.config.EnvironmentVariablesConfig;
import com.thoughtworks.go.security.GoCipher;

public class EnvironmentVariablesConfigMother {
    public static EnvironmentVariablesConfig env(String name, String value) {
        EnvironmentVariablesConfig config = new EnvironmentVariablesConfig();
        config.add(name, value);
        return config;
    }

    public static EnvironmentVariablesConfig environmentVariables() {
        EnvironmentVariablesConfig variables = new EnvironmentVariablesConfig();
        GoCipher goCipher = new GoCipher();
        variables.add(new EnvironmentVariableConfig(goCipher, "MULTIPLE_LINES", "multiplelines", true));
        variables.add(new EnvironmentVariableConfig(goCipher, "COMPLEX", "This has very <complex> data", false));
        return variables;
    }

    public static EnvironmentVariablesConfig env(String[] names, String[] values) {
        EnvironmentVariablesConfig config = new EnvironmentVariablesConfig();
        for (int i = 0; i < names.length; i++) {
            config.add(names[i], values[i]);
        }
        return config;
    }
}
