/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.plugin.api.material.packagerepository;

import com.thoughtworks.go.plugin.api.config.Property;

@Deprecated
//Will be moved to internal scope
public class PackageMaterialProperty extends Property implements Comparable {
    public PackageMaterialProperty(String key) {
        super(key);
        updateDefaults();
    }

    private void updateDefaults() {
        with(REQUIRED, true);
        with(PART_OF_IDENTITY, true);
        with(SECURE, false);
        with(DISPLAY_NAME, "");
        with(DISPLAY_ORDER, 0);
    }

    public PackageMaterialProperty(String key, String value) {
        super(key, value);
        updateDefaults();
    }

    @Override
    public int compareTo(Object o) {
        return this.getOption(DISPLAY_ORDER) - ((PackageMaterialProperty) o).getOption(DISPLAY_ORDER);
    }
}
