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

package com.thoughtworks.go.server.service.support.toggle;

import com.thoughtworks.go.server.domain.support.toggle.FeatureToggle;

import java.util.List;

public class FeatureToggleService {
    private FeatureToggleRepository repository;

    public FeatureToggleService(FeatureToggleRepository repository) {
        this.repository = repository;
    }

    public List<FeatureToggle> allToggles() {
        return repository.allToggles();
    }

    public boolean isToggleAvailable(String key) {
        return findToggle(repository.allToggles(), key) != null;
    }

    public boolean isToggleOn(String key) {
        FeatureToggle toggle = findToggle(repository.allToggles(), key);
        return toggle != null && toggle.isOn();
    }

    private FeatureToggle findToggle(List<FeatureToggle> availableToggles, String keyOfToggleToFind) {
        for (FeatureToggle toggle : availableToggles) {
            if (toggle.hasSameKeyAs(keyOfToggleToFind)) {
                return toggle;
            }
        }
        return null;
    }
}
