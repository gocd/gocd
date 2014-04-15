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

package com.thoughtworks.go.config;

import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class PasswordFileConfigTest {
    @Test
    public void shouldConvertNullAttributesToEmptyStringUponConstruct() {
        assertThat(new PasswordFileConfig(null).path(), is(""));
    }

    @Test
    public void shouldReturnFalseIfPasswordFileIsNotConfigured() {
        PasswordFileConfig config = new PasswordFileConfig(null);
        assertThat(config.isEnabled(), is(false));
        config = new PasswordFileConfig("");
        assertThat(config.isEnabled(), is(false));
    }

    @Test
    public void shouldReturnTrueIfPasswordFileIsConfigured() {
        PasswordFileConfig config = new PasswordFileConfig("boo");
        assertThat(config.isEnabled(), is(true));
    }
}
