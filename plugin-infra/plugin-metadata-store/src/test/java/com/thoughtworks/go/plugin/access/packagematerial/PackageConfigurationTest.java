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
package com.thoughtworks.go.plugin.access.packagematerial;

import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class PackageConfigurationTest {
    @Test
    public void shouldGetOptionIfAvailable() {
        PackageConfiguration packageConfiguration = new PackageConfiguration("key");
        packageConfiguration.with(PackageConfiguration.REQUIRED, true);
        assertThat(packageConfiguration.hasOption(PackageConfiguration.REQUIRED), is(true));
        assertThat(packageConfiguration.hasOption(PackageConfiguration.SECURE), is(false));
    }

    @Test
    public void shouldGetOptionValue() {
        PackageConfiguration packageConfiguration = new PackageConfiguration("key");
        packageConfiguration.with(PackageConfiguration.DISPLAY_NAME, "some display name");
        packageConfiguration.with(PackageConfiguration.DISPLAY_ORDER, 3);
        assertThat(packageConfiguration.getOption(PackageConfiguration.DISPLAY_NAME), is("some display name"));
        assertThat(packageConfiguration.getOption(PackageConfiguration.DISPLAY_ORDER), is(3));
    }

    @Test
    public void shouldSortPackageConfigurationByDisplayOrder() throws Exception {
        PackageConfiguration p1 = new PackageConfiguration("k1").with(PackageConfiguration.DISPLAY_ORDER, 1);
        PackageConfiguration p2 = new PackageConfiguration("k2").with(PackageConfiguration.DISPLAY_ORDER, 3);
        assertThat(p2.compareTo(p1), is(2));

    }
}
