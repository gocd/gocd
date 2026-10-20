/*
 * Copyright Thoughtworks, Inc.
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
package com.thoughtworks.go.config;

import com.thoughtworks.go.config.remote.PartialConfig;
import com.thoughtworks.go.config.remote.RepoConfigOrigin;
import com.thoughtworks.go.serverhealth.HealthStateScope;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class CachedGoPartials implements PartialConfigResolver {
    private final ServerHealthService serverHealthService;
    private final PartialConfigHelper partialConfigHelper;
    private final Map<String, PartialConfig> fingerprintToLatestValidConfigMap = new ConcurrentHashMap<>();
    private final Map<String, PartialConfig> fingerprintToLatestKnownConfigMap = new ConcurrentHashMap<>();

    @Autowired
    public CachedGoPartials(ServerHealthService serverHealthService, PartialConfigHelper partialConfigHelper) {
        this.serverHealthService = serverHealthService;
        this.partialConfigHelper = partialConfigHelper;
    }

    public List<PartialConfig> lastValidPartials() {
        return new ArrayList<>(fingerprintToLatestValidConfigMap.values());
    }

    public List<PartialConfig> lastKnownPartials() {
        return new ArrayList<>(fingerprintToLatestKnownConfigMap.values());
    }

    boolean hasValidPartials() {
        return !fingerprintToLatestValidConfigMap.isEmpty();
    }

    boolean hasEquivalentPartials() {
        return fingerprintToLatestKnownConfigMap.isEmpty() || partialConfigHelper.isEquivalent(lastKnownPartials(), lastValidPartials());
    }

    public PartialConfig getValid(String fingerprint) {
        return fingerprintToLatestValidConfigMap.get(fingerprint);
    }

    public PartialConfig getKnown(String fingerprint) {
        return fingerprintToLatestKnownConfigMap.get(fingerprint);
    }

    public void removeValid(String fingerprint) {
        PartialConfig removed = fingerprintToLatestValidConfigMap.remove(fingerprint);
        if (removed != null) {
            serverHealthService.removeByScope(HealthStateScope.forPartialConfigRepo(fingerprint));
        }
    }

    public void removeKnown(String fingerprint) {
        PartialConfig removed = fingerprintToLatestKnownConfigMap.remove(fingerprint);
        if (removed != null) {
            serverHealthService.removeByScope(HealthStateScope.forPartialConfigRepo(fingerprint));
        }
    }

    public void removeValidWithoutClearingServerHealthMessage(String fingerprint) {
        fingerprintToLatestValidConfigMap.remove(fingerprint);
    }

    public void cacheAsLastKnown(String fingerprint, PartialConfig incoming) {
        fingerprintToLatestKnownConfigMap.put(fingerprint, incoming);
    }

    /**
     * Records the last partial for {@code fingerprint} known to have survived a full config merge
     * and validation, clearing any server health messages for that config repo.
     * <p>
     * The replacement is skipped when the new partial is equivalent (same origin AND same structure) to the one already
     * cached as valid. This guard matters on the fallback path: when a save fails on the last known partials and is
     * retried with the last valid ones, re-marking those as valid must not clear health messages describing why the
     * newer (still broken) partials were rejected.
     * <p>
     * Structure is compared as well as origin because a config repo plugin may legitimately produce different output
     * for the same material revision (e.g. branch/PR scanning), and such a partial must still supersede the stale one
     * once it has been successfully merged.
     */
    public void markAsValid(String fingerprint, PartialConfig incoming) {
        PartialConfig previousValid = fingerprintToLatestValidConfigMap.get(fingerprint);
        if (previousValid == null || !partialConfigHelper.isEquivalent(previousValid, incoming)) {
            markAsValidAndUpdateServerHealthMessage(fingerprint, incoming);
        }
    }

    public void markAsValid(List<PartialConfig> partials) {
        for (PartialConfig partial : partials) {
            if (partial.getOrigin() instanceof RepoConfigOrigin) {
                String fingerprint = ((RepoConfigOrigin) partial.getOrigin()).getMaterial().getFingerprint();
                markAsValid(fingerprint, partial);
            }
        }
    }

    /**
     * Tests whether a given {@link PartialConfig} is different from the last known cached attempt.
     *
     * @param fingerprint the config repo material fingerprint ({@link String})
     * @param incoming    a {@link PartialConfig}
     * @return whether the incoming partial is different from the last cached partial
     */
    boolean isPartialDifferentFromLastKnown(String fingerprint, PartialConfig incoming) {
        return !partialConfigHelper.isEquivalent(getKnown(fingerprint), incoming);
    }

    public Map<String, PartialConfig> getFingerprintToLatestKnownConfigMap() {
        return fingerprintToLatestKnownConfigMap;
    }

    public Map<String, PartialConfig> getFingerprintToLatestValidConfigMap() {
        return fingerprintToLatestValidConfigMap;
    }

    public void clear() {
        fingerprintToLatestKnownConfigMap.clear();
        fingerprintToLatestValidConfigMap.clear();
    }

    public void markAllKnownAsValid() {
        markAsValid(lastKnownPartials());
    }

    @Override
    public @Nullable PartialConfig findPartialByFingerprint(CruiseConfig cruiseConfig, String fingerprint) {
        return Optional.ofNullable(getValid(fingerprint))
            .or(() -> Optional.ofNullable(getKnown(fingerprint)))
            .flatMap(partial -> cruiseConfig.getPartials().stream()
                .filter(partialConfig -> partialConfig.getOrigin() instanceof RepoConfigOrigin origin && origin.getMaterial().getFingerprint().equals(fingerprint))
                .findFirst()
            )
            .orElse(null);
    }

    private void markAsValidAndUpdateServerHealthMessage(String fingerprint, PartialConfig newPart) {
        fingerprintToLatestValidConfigMap.put(fingerprint, newPart);
        serverHealthService.removeByScope(HealthStateScope.forPartialConfigRepo(fingerprint));
    }
}
