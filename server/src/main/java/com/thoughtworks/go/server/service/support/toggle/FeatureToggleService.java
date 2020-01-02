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
package com.thoughtworks.go.server.service.support.toggle;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.server.domain.support.toggle.FeatureToggle;
import com.thoughtworks.go.server.domain.support.toggle.FeatureToggles;
import javassist.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.MessageFormat;
import java.util.Collection;

@Service
public class FeatureToggleService {
    private static final String USER_TOGGLES_CACHE_KEY = "FeatureToggleService_USER_TOGGLES";
    private FeatureToggleRepository repository;
    private GoCache goCache;
    private final Multimap<String, FeatureToggleListener> listeners = HashMultimap.create();

    @Autowired
    public FeatureToggleService(FeatureToggleRepository repository, GoCache goCache) {
        this.repository = repository;
        this.goCache = goCache;
    }

    public boolean isToggleOn(String key) {
        FeatureToggle toggle = allToggles().find(key);
        return toggle != null && toggle.isOn();
    }

    public FeatureToggles allToggles() {
        FeatureToggles allToggles = (FeatureToggles) goCache.get(USER_TOGGLES_CACHE_KEY);
        if (allToggles != null) {
            return allToggles;
        }
        synchronized (USER_TOGGLES_CACHE_KEY) {
            allToggles = (FeatureToggles) goCache.get(USER_TOGGLES_CACHE_KEY);
            if (allToggles != null) {
                return allToggles;
            }

            FeatureToggles availableToggles = repository.availableToggles();
            FeatureToggles userToggles = repository.userToggles();
            allToggles = availableToggles.overrideWithTogglesIn(userToggles);
            goCache.put(USER_TOGGLES_CACHE_KEY, allToggles);

            return allToggles;
        }
    }

    public void changeValueOfToggle(String key, boolean newValue) {
        if (allToggles().find(key) == null) {
            throw new RecordNotFoundException(MessageFormat.format("Feature toggle: ''{0}'' is not valid.", key));
        }

        synchronized (USER_TOGGLES_CACHE_KEY) {
            repository.changeValueOfToggle(key, newValue);
            goCache.remove(USER_TOGGLES_CACHE_KEY);

            if (listeners.containsKey(key)) {
                Collection<FeatureToggleListener> featureToggleListeners = listeners.get(key);
                for (FeatureToggleListener listener : featureToggleListeners) {
                    listener.toggleChanged(newValue);
                }
            }
        }
    }

    public void registerListener(String key, FeatureToggleListener listener) {
        listeners.put(key, listener);
        listener.toggleChanged(isToggleOn(key));
    }
}
