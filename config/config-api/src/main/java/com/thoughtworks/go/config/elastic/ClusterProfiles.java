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
package com.thoughtworks.go.config.elastic;

import com.thoughtworks.go.config.ConfigCollection;
import com.thoughtworks.go.config.ConfigTag;
import com.thoughtworks.go.config.PluginProfiles;
import com.thoughtworks.go.config.Validatable;

import java.util.Arrays;

@ConfigTag("clusterProfiles")
@ConfigCollection(ClusterProfile.class)
public class ClusterProfiles extends PluginProfiles<ClusterProfile> implements Validatable {

    public ClusterProfiles() {
    }

    public ClusterProfiles(ClusterProfile... clusters) {
        super(Arrays.asList(clusters));
    }

}
