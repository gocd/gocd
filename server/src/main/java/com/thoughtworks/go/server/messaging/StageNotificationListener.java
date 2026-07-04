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
package com.thoughtworks.go.server.messaging;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.thoughtworks.go.domain.Stage;
import com.thoughtworks.go.domain.StageConfigIdentifier;
import com.thoughtworks.go.domain.StageEvent;
import com.thoughtworks.go.domain.StageResult;
import com.thoughtworks.go.server.dao.StageDao;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.StageNotificationService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ConcurrentMap;

@Component
public class StageNotificationListener implements GoMessageListener<StageStatusMessage> {
    private static final Duration CACHE_DURATION = Duration.ofHours(12);
    private final GoConfigService goConfigService;
    private final StageNotificationService stageNotificationService;
    private final StageDao stageDao;

    private final ConcurrentMap<StageConfigIdentifier, CacheEntry> resultCache = Caffeine
        .newBuilder()
        .expireAfterWrite(CACHE_DURATION)
        .<StageConfigIdentifier, CacheEntry>build()
        .asMap();

    private record CacheEntry(StageResult previous, StageResult current) {
        StageEvent toEvent() {
            return current.toEventFromPrevious(previous);
        }
    }

    @Autowired
    public StageNotificationListener(StageNotificationService stageNotificationService, GoConfigService goConfigService,
                                     StageDao stageDao, StageStatusTopic stageStatusTopic) {
        this.stageNotificationService = stageNotificationService;
        this.goConfigService = goConfigService;
        this.stageDao = stageDao;
        stageStatusTopic.addListener(this);
    }

    @Override
    public void onMessage(StageStatusMessage message) {
        if (goConfigService.isSecurityEnabled()) {
            StageEvent event = computeEventFrom(message.getStageIdentifier().stageConfigIdentifier(), message.getStageResult());
            stageNotificationService.sendNotifications(message.getStageIdentifier(), event, message.username());
        }
    }

    @VisibleForTesting
    @NotNull StageEvent computeEventFrom(StageConfigIdentifier identifier, StageResult next) {
        return resultCache.compute(identifier, (key, state) ->
                new CacheEntry(state != null ? state.current : loadFromDatabase(identifier), next))
            .toEvent();
    }

    private @NotNull StageResult loadFromDatabase(StageConfigIdentifier identifier) {
        return Optional.ofNullable(stageDao.mostRecentCompleted(identifier))
            .map(Stage::getResult)
            .orElse(StageResult.Unknown);
    }
}
