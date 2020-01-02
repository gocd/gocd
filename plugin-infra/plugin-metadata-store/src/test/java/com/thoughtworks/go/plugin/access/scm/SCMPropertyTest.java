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
package com.thoughtworks.go.plugin.access.scm;

import com.thoughtworks.go.plugin.api.config.Property;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

public class SCMPropertyTest {
    @Test
    public void validateSCMPropertyDefaults() throws Exception {
        SCMProperty scmProperty = new SCMProperty("Test-Property");

        assertThat(scmProperty.getOptions().size(), is(5));
        assertThat(scmProperty.getOption(Property.REQUIRED), is(true));
        assertThat(scmProperty.getOption(Property.PART_OF_IDENTITY), is(true));
        assertThat(scmProperty.getOption(Property.SECURE), is(false));
        assertThat(scmProperty.getOption(Property.DISPLAY_NAME), is(""));
        assertThat(scmProperty.getOption(Property.DISPLAY_ORDER), is(0));

        scmProperty = new SCMProperty("Test-Property", "Dummy Value");

        assertThat(scmProperty.getOptions().size(), is(5));
        assertThat(scmProperty.getOption(Property.REQUIRED), is(true));
        assertThat(scmProperty.getOption(Property.PART_OF_IDENTITY), is(true));
        assertThat(scmProperty.getOption(Property.SECURE), is(false));
        assertThat(scmProperty.getOption(Property.DISPLAY_NAME), is(""));
        assertThat(scmProperty.getOption(Property.DISPLAY_ORDER), is(0));
    }
}
