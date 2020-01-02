/*
 * Copyright 2020 ThoughtWorks, Inc.
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
package com.thoughtworks.go.server.scheduling;

import com.thoughtworks.go.config.EnvironmentVariableConfig;
import com.thoughtworks.go.security.CryptoException;
import com.thoughtworks.go.security.GoCipher;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class ScheduleOptionsTest {

    private GoCipher goCipher;

    @Before
    public void setUp() throws Exception {
        goCipher = new GoCipher();
    }

    @Test
    public void shouldReturnEnvironmentVariablesConfig() {
        HashMap<String, String> variables = new HashMap<>();
        variables.put("name", "value");
        variables.put("foo", "bar");
        ScheduleOptions scheduleOptions = new ScheduleOptions(new HashMap<>(), variables, new HashMap<>());
        assertThat(scheduleOptions.getVariables().size(), is(2));
        assertThat(scheduleOptions.getVariables(), hasItems(new EnvironmentVariableConfig("name","value"), new EnvironmentVariableConfig("foo","bar")));
    }

    @Test
    public void shouldReturnSecureEnvironmentVariablesConfig() throws CryptoException {
        String plainText = "secure_value";
        HashMap<String, String> secureVariables = new HashMap<>();
        secureVariables.put("secure_name", plainText);
        ScheduleOptions scheduleOptions = new ScheduleOptions(goCipher, new HashMap<>(), new HashMap<>(), secureVariables);
        assertThat(scheduleOptions.getVariables().size(), is(1));
        assertThat(scheduleOptions.getVariables(), hasItem(new EnvironmentVariableConfig(goCipher, "secure_name", plainText, true)));
    }
}
