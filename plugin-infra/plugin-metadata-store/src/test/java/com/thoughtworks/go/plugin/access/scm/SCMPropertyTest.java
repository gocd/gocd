/*
 * Copyright Thoughtworks, Inc.
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
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SCMPropertyTest {
    @Test
    public void validateSCMPropertyDefaults() {
        SCMProperty scmProperty = new SCMProperty("Test-Property");

        assertThat(scmProperty.getOptions().size()).isEqualTo(5);
        assertThat(scmProperty.getOption(Property.REQUIRED)).isEqualTo(true);
        assertThat(scmProperty.getOption(Property.PART_OF_IDENTITY)).isEqualTo(true);
        assertThat(scmProperty.getOption(Property.SECURE)).isEqualTo(false);
        assertThat(scmProperty.getOption(Property.DISPLAY_NAME)).isEqualTo("");
        assertThat(scmProperty.getOption(Property.DISPLAY_ORDER)).isEqualTo(0);

        scmProperty = new SCMProperty("Test-Property", "Dummy Value");

        assertThat(scmProperty.getOptions().size()).isEqualTo(5);
        assertThat(scmProperty.getOption(Property.REQUIRED)).isEqualTo(true);
        assertThat(scmProperty.getOption(Property.PART_OF_IDENTITY)).isEqualTo(true);
        assertThat(scmProperty.getOption(Property.SECURE)).isEqualTo(false);
        assertThat(scmProperty.getOption(Property.DISPLAY_NAME)).isEqualTo("");
        assertThat(scmProperty.getOption(Property.DISPLAY_ORDER)).isEqualTo(0);
    }
}
