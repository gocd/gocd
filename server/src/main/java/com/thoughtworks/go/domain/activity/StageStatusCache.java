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
package com.thoughtworks.go.domain.activity;

import com.thoughtworks.go.domain.NullStage;
import com.thoughtworks.go.domain.Stage;
import com.thoughtworks.go.domain.StageConfigIdentifier;
import com.thoughtworks.go.server.dao.StageDao;
import com.thoughtworks.go.server.domain.StageStatusListener;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class StageStatusCache implements StageStatusListener {
    private final StageDao stageDao;
    private final Map<StageConfigIdentifier, Stage> stages = new ConcurrentHashMap<>();

    private static final Stage NEVER_BUILT = new NullStage("NEVER_BUILT");

    @Autowired
    public StageStatusCache(StageDao stageDao) {
        this.stageDao = stageDao;
    }

    @Override
    public void stageStatusChanged(Stage stage) {
        StageConfigIdentifier configIdentifier = stage.getIdentifier().stageConfigIdentifier();
        stages.put(configIdentifier, stage);
    }

    public @Nullable Stage currentStage(StageConfigIdentifier identifier) {
        Stage instance = stages.computeIfAbsent(identifier, k -> Objects.requireNonNullElse(stageDao.mostRecentStage(k), NEVER_BUILT));
        return instance == NEVER_BUILT ? null : instance;
    }
}
