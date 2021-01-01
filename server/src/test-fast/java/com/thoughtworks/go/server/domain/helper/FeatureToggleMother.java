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
package com.thoughtworks.go.server.domain.helper;

import com.thoughtworks.go.server.domain.support.toggle.FeatureToggle;
import com.thoughtworks.go.server.domain.support.toggle.FeatureToggles;

public class FeatureToggleMother {
    public static FeatureToggles noToggles() {
        return new FeatureToggles();
    }

    public static FeatureToggles someToggles() {
        return new FeatureToggles(
                new FeatureToggle("key1", "desc1", true).withValueHasBeenChangedFlag(false),
                new FeatureToggle("key2", "desc2", false).withValueHasBeenChangedFlag(true));
    }
}
