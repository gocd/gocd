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
package com.thoughtworks.go.plugin.api.material.packagerepository;

import com.thoughtworks.go.plugin.api.config.Property;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class PackageConfigurationTest {
    @Test
    public void shouldGetAllPackageConfigurationsSortedByDisplayOrder() throws Exception {
        Property c1 = new PackageMaterialProperty("k1").with(Property.DISPLAY_ORDER, 2);
        Property c2 = new PackageMaterialProperty("k2").with(Property.DISPLAY_ORDER, 0);
        Property c3 = new PackageMaterialProperty("k3").with(Property.DISPLAY_ORDER, 1);
        PackageConfiguration packageConfiguration = new PackageConfiguration();
        packageConfiguration.add(c1);
        packageConfiguration.add(c2);
        packageConfiguration.add(c3);
        assertThat(packageConfiguration.list().get(0), is(c2));
        assertThat(packageConfiguration.list().get(1), is(c3));
        assertThat(packageConfiguration.list().get(2), is(c1));
    }
}
