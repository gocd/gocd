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
package com.thoughtworks.go.server.service.support;

import com.thoughtworks.go.server.service.support.toggle.FeatureToggleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class FeatureToggleInfoProvider implements ServerInfoProvider {
    private FeatureToggleService featureToggleService;

    @Autowired
    public FeatureToggleInfoProvider(FeatureToggleService featureToggleService){
        this.featureToggleService = featureToggleService;
    }

    @Override
    public double priority() {
        return 12.5;
    }

    @Override
    public Map<String, Object> asJson() throws Exception {
        LinkedHashMap<String, Object> json = new LinkedHashMap<>();
        json.put("Available Toggles", featureToggleService.allToggles().all());
        return json;
    }

    @Override
    public String name() {
        return "Feature Toggles";
    }
}
