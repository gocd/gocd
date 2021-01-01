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
package com.thoughtworks.go.server.service.materials;

import java.util.List;

import com.thoughtworks.go.domain.config.Configuration;
import com.thoughtworks.go.plugin.access.packagematerial.PackageConfiguration;
import com.thoughtworks.go.plugin.api.config.Property;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class PackageMaterialTestHelper {

    public static void assertPackageConfigurations(List<PackageConfiguration> actual, Configuration expected) {
        assertThat(actual, hasSize(expected.size()));
        for (int i = 0; i < actual.size(); i++) {
             assertThat(actual.get(i).getKey(), is(expected.get(i).getConfigurationKey().getName()));
             assertThat(actual.get(i).getValue(), is(expected.get(i).getValue()));
        }
    }
    public static void assertPackageConfiguration(List<? extends Property> actual, Configuration expected) {
        assertThat(actual, hasSize(expected.size()));
        for (int i = 0; i < actual.size(); i++) {
             assertThat(actual.get(i).getKey(), is(expected.get(i).getConfigurationKey().getName()));
             assertThat(actual.get(i).getValue(), is(expected.get(i).getValue()));
        }
    }
}
