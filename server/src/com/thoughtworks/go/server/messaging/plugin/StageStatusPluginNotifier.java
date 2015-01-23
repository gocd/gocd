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

package com.thoughtworks.go.server.messaging.plugin;

import com.thoughtworks.go.domain.Stage;
import com.thoughtworks.go.server.domain.StageStatusListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class StageStatusPluginNotifier implements StageStatusListener {
    public static final String DATE_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    private PluginNotificationQueue pluginNotificationQueue;

    @Autowired
    public StageStatusPluginNotifier(PluginNotificationQueue pluginNotificationQueue) {
        this.pluginNotificationQueue = pluginNotificationQueue;
    }

    @Override
    public void stageStatusChanged(final Stage stage) {
        Map data = createRequestDataMap(stage);

        pluginNotificationQueue.post(new PluginNotificationMessage("stage-status", data));
    }

    Map createRequestDataMap(Stage stage) {
        Map data = new LinkedHashMap();
        data.put("pipeline-name", stage.getIdentifier().getPipelineName());
        data.put("pipeline-counter", stage.getIdentifier().getPipelineCounter());
        data.put("stage-name", stage.getIdentifier().getStageName());
        data.put("stage-counter", stage.getIdentifier().getStageCounter());
        data.put("stage-state", stage.getState());
        data.put("stage-result", stage.getResult());
        data.put("create-time", timestampToString(stage.getCreatedTime()));
        data.put("last-transition-time", timestampToString(stage.getLastTransitionedTime()));
        return data;
    }

    private String timestampToString(Timestamp timestamp) {
        return timestamp == null ? "" : new SimpleDateFormat(DATE_PATTERN).format(timestamp);
    }
}
