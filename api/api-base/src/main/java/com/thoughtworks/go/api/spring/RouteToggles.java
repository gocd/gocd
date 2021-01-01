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

package com.thoughtworks.go.api.spring;

import com.thoughtworks.go.server.service.support.toggle.FeatureToggleService;
import com.thoughtworks.go.spark.spring.RouteEntry;

import java.util.List;

class RouteToggles {
    private final List<RouteToggle> matchers;
    private final FeatureToggleService features;

    RouteToggles(List<RouteToggle> matchers, FeatureToggleService features) {
        this.matchers = matchers;
        this.features = features;
    }

    boolean matches(RouteEntry entry) {
        return matchers.stream().anyMatch((routeToggle -> routeToggle.matches(entry)));
    }

    boolean isToggledOn(RouteEntry entry) {
        return matchers.stream().filter(r -> r.matches(entry)).allMatch(routeToggle -> routeToggle.isToggleOn(features));
    }
}
