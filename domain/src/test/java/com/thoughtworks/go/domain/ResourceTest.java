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
package com.thoughtworks.go.domain;

import com.thoughtworks.go.util.ClonerFactory;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ResourceTest {
    @Test
    void shouldBeAbleToCreateACopyOfItself() {
        Resource existingResource = new Resource("some-name");
        existingResource.setId(2);
        existingResource.setBuildId(10);

        assertThat(existingResource).isEqualTo(new Resource(existingResource));
        assertThat(existingResource).isEqualTo(ClonerFactory.instance().deepClone(existingResource));
    }
}
