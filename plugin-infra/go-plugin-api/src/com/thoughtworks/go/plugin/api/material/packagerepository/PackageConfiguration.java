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

import java.util.Collections;
import java.util.List;

import com.thoughtworks.go.plugin.api.config.Configuration;
import com.thoughtworks.go.plugin.api.config.Property;

/**
 * Represents {@link com.thoughtworks.go.plugin.api.config.Configuration} specific to package
 */
@Deprecated
//Will be moved to internal scope
public class PackageConfiguration extends Configuration {

    @Override
    public List<? extends Property> list() {
        List<PackageMaterialProperty> list = (List<PackageMaterialProperty>) super.list();
        Collections.sort(list);
        return list;
    }
}
