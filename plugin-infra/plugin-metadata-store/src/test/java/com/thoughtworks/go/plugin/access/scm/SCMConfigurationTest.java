/*
 * Copyright 2024 Thoughtworks, Inc.
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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SCMConfigurationTest {
    @Test
    public void shouldGetOptionIfAvailable() {
        SCMConfiguration scmConfiguration = new SCMConfiguration("key");
        scmConfiguration.with(SCMConfiguration.REQUIRED, true);

        assertThat(scmConfiguration.hasOption(SCMConfiguration.REQUIRED)).isTrue();
        assertThat(scmConfiguration.hasOption(SCMConfiguration.SECURE)).isFalse();
    }

    @Test
    public void shouldGetOptionValue() {
        SCMConfiguration scmConfiguration = new SCMConfiguration("key");
        scmConfiguration.with(SCMConfiguration.DISPLAY_NAME, "some display name");
        scmConfiguration.with(SCMConfiguration.DISPLAY_ORDER, 3);

        assertThat(scmConfiguration.getOption(SCMConfiguration.DISPLAY_NAME)).isEqualTo("some display name");
        assertThat(scmConfiguration.getOption(SCMConfiguration.DISPLAY_ORDER)).isEqualTo(3);
    }

    @Test
    public void shouldSortByDisplayOrder() {
        SCMConfiguration p1 = new SCMConfiguration("k1").with(SCMConfiguration.DISPLAY_ORDER, 1);
        SCMConfiguration p2 = new SCMConfiguration("k2").with(SCMConfiguration.DISPLAY_ORDER, 3);

        assertThat(p2.compareTo(p1)).isEqualTo(2);
    }
}
