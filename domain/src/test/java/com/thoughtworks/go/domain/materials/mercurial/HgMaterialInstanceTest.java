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
package com.thoughtworks.go.domain.materials.mercurial;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.materials.mercurial.HgMaterial;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HgMaterialInstanceTest {
    @Test
    void shouldCreateMaterialFromMaterialInstance() {
        final HgMaterialInstance materialInstance = new HgMaterialInstance("https://example.com", "bob",
                "feature", "some-flyweight");
        materialInstance.setId(100L);

        final HgMaterial material = (HgMaterial) materialInstance.toOldMaterial("example", "destination", "pass");

        assertThat(material.getName()).isEqualTo(new CaseInsensitiveString("example"));
        assertThat(material.getUrl()).isEqualTo("https://example.com");
        assertThat(material.getUserName()).isEqualTo("bob");
        assertThat(material.getPassword()).isEqualTo("pass");
        assertThat(material.getBranch()).isEqualTo("feature");
        assertThat(material.getFolder()).isEqualTo("destination");
        assertThat(material.getId()).isEqualTo(materialInstance.getId());
    }
}