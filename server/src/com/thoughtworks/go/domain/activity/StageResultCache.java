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

package com.thoughtworks.go.domain.activity;

import java.util.HashMap;
import java.util.Map;

import com.thoughtworks.go.domain.Stage;
import com.thoughtworks.go.domain.StageConfigIdentifier;
import com.thoughtworks.go.domain.StageEvent;
import com.thoughtworks.go.domain.StageResult;
import com.thoughtworks.go.server.dao.StageDao;
import com.thoughtworks.go.server.messaging.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class StageResultCache implements GoMessageListener<StageStatusMessage> {
    private Map<StageConfigIdentifier, StageResult> currentResults = new HashMap<>();
    private Map<StageConfigIdentifier, StageResult> previousResults = new HashMap<>();

    private final StageDao stageDao;
    private final StageResultTopic stageResultTopic;

    @Autowired
    public StageResultCache(StageDao stageDao, StageResultTopic stageResultTopic, StageStatusTopic stageStatusTopic) {
        this.stageDao = stageDao;
        this.stageResultTopic = stageResultTopic;
        stageStatusTopic.addListener(this);
    }

    public void onMessage(StageStatusMessage message) {
        StageConfigIdentifier identifier = message.getStageIdentifier().stageConfigIdentifier();
        updateCache(identifier, message.getStageResult());

        StageEvent event = message.getStageResult().describeChangeEvent(previousResult(identifier));
        stageResultTopic.post(new StageResultMessage(message.getStageIdentifier(), event, message.username()));
    }

    void updateCache(StageConfigIdentifier identifier, StageResult stageResult) {
        previousResults.put(identifier, currentResult(identifier));
        currentResults.put(identifier, stageResult);
    }

    StageResult previousResult(StageConfigIdentifier identifier) {
        StageResult stageResult = previousResults.get(identifier);
        return stageResult == null ? StageResult.Unknown : stageResult;
    }

    private StageResult currentResult(StageConfigIdentifier identifier) {
        StageResult stageResult = currentResults.get(identifier);
        if (stageResult == null) {
            Stage stage = stageDao.mostRecentCompleted(identifier);
            if (stage != null) {
                stageResult = stage.getResult();
                currentResults.put(identifier, stageResult);
            }
        }
        return stageResult == null ? StageResult.Unknown : stageResult;
    }

}
