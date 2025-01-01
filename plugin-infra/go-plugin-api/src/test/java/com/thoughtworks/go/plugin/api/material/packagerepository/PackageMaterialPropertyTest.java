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
package com.thoughtworks.go.plugin.api.material.packagerepository;

import com.thoughtworks.go.plugin.api.config.Property;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PackageMaterialPropertyTest {

    @Test
    public void validatePackagePropertyDefaults() {
        PackageMaterialProperty packageMaterialProperty = new PackageMaterialProperty("Test-Property");
        assertThat(packageMaterialProperty.getOptions().size()).isEqualTo(5);
        assertThat(packageMaterialProperty.getOption(Property.REQUIRED)).isTrue();
        assertThat(packageMaterialProperty.getOption(Property.PART_OF_IDENTITY)).isTrue();
        assertThat(packageMaterialProperty.getOption(Property.SECURE)).isFalse();
        assertThat(packageMaterialProperty.getOption(Property.DISPLAY_NAME)).isEqualTo("");
        assertThat(packageMaterialProperty.getOption(Property.DISPLAY_ORDER)).isEqualTo(0);

        packageMaterialProperty = new PackageMaterialProperty("Test-Property", "Dummy Value");
        assertThat(packageMaterialProperty.getOptions().size()).isEqualTo(5);
        assertThat(packageMaterialProperty.getOption(Property.REQUIRED)).isTrue();
        assertThat(packageMaterialProperty.getOption(Property.PART_OF_IDENTITY)).isTrue();
        assertThat(packageMaterialProperty.getOption(Property.SECURE)).isFalse();
        assertThat(packageMaterialProperty.getOption(Property.DISPLAY_NAME)).isEqualTo("");
        assertThat(packageMaterialProperty.getOption(Property.DISPLAY_ORDER)).isEqualTo(0);
    }
}
