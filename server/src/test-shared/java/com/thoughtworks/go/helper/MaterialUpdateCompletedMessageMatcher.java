/*
 * Copyright 2018 ThoughtWorks, Inc.
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

package com.thoughtworks.go.helper;

import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.server.materials.MaterialUpdateCompletedMessage;
import org.mockito.ArgumentMatcher;

import static org.mockito.ArgumentMatchers.argThat;

public class MaterialUpdateCompletedMessageMatcher {
    public static MaterialUpdateCompletedMessage matchMaterialUpdateCompletedMessage(final Material expectedMaterial) {
        return argThat(new ArgumentMatcher<MaterialUpdateCompletedMessage>() {
            @Override
            public boolean matches(MaterialUpdateCompletedMessage o) {
                return expectedMaterial.equals(o.getMaterial());
            }

            @Override
            public String toString() {
                return "Expected material to be: " + expectedMaterial;
            }
        });
    }
}
