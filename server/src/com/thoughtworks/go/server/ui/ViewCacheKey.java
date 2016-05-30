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

package com.thoughtworks.go.server.ui;

import com.thoughtworks.go.config.MingleConfig;
import com.thoughtworks.go.config.TrackingTool;
import com.thoughtworks.go.domain.Stage;
import com.thoughtworks.go.domain.PipelineIdentifier;
import com.thoughtworks.go.domain.PipelinePauseInfo;
import com.thoughtworks.go.presentation.pipelinehistory.PipelineModel;
import com.thoughtworks.go.presentation.pipelinehistory.PipelineInstanceModel;
import com.thoughtworks.go.presentation.pipelinehistory.StageInstanceModel;

public class ViewCacheKey {
    private static final String DELIMITER = "|";

    public String forFbhOfStagesUnderPipeline(PipelineIdentifier pipelineIdentifier) {
        return key("fbhOfStagesUnderPipeline_%s", pipelineIdentifier.pipelineLocator());
    }

    public String forFailedBuildHistoryStage(Stage stage, String format) {
        return key("stageFailedBuildHistoryView_%s_%s_%s", stage.getIdentifier().stageLocator(), stage.getState(),format);
    }

    private String key(String format, Object... args) {
        return String.format("view_" + format, args).intern();
    }

    public String forPipelineModelBox(PipelineModel model) {
        return keyForPipelineModelFragment(model, "dashboardPipelineFragment_");
    }

    public String forEnvironmentPipelineBox(PipelineModel model) {
        return keyForPipelineModelFragment(model, "environmentPipelineFragment_") + DELIMITER + model.hasNewRevisions();
    }

    private String keyForPipelineModelFragment(PipelineModel model, String name) {
        StringBuilder s = new StringBuilder();
        s.append(name);
        s.append(model.getName());//FIXME: use the delimiter, the two values appended at this point can combine to get something completely different (ALWAYS USE DELIMITER.)!!! - Sara & JJ
        appendLockStatus(model, s);
        for (PipelineInstanceModel pim : model.getActivePipelineInstances()) {
            s.append("[");
            s.append(pim.getId()).append(DELIMITER);
            for (StageInstanceModel stageInstanceModel : pim.getStageHistory()) {
                s.append(stageInstanceModel.getName()).append(DELIMITER);
                s.append(stageInstanceModel.getId()).append(DELIMITER);
                s.append(stageInstanceModel.getState()).append(DELIMITER);
            }
            s.append("]");
        }
        s.append(model.canOperate()).append(DELIMITER);
        s.append(model.canForce()).append(DELIMITER);
        PipelinePauseInfo pauseInfo = model.getPausedInfo();
        s.append(pauseInfo.isPaused()).append(DELIMITER).append(pauseInfo.getPauseCause().replaceAll("\\W", "")).append(DELIMITER).append(pauseInfo.getPauseBy());
        s.append(DELIMITER).append(model.canAdminister());
        return key(s.toString());
    }

    private void appendLockStatus(PipelineModel model, StringBuilder s) {
        PipelineInstanceModel latestPIM = model.getLatestPipelineInstance();
        s.append("{");
        s.append(latestPIM.isLockable()).append(DELIMITER);
        s.append(latestPIM.isCurrentlyLocked()).append(DELIMITER);
        s.append(latestPIM.canUnlock());
        s.append("}");
    }

    public String forPipelineModelBuildCauses(PipelineModel model) {
        StringBuilder s = new StringBuilder();
        s.append("view_buildCausesForPipelineModel_");
        s.append(model.getName());

        for (PipelineInstanceModel pim : model.getActivePipelineInstances()) {
            TrackingTool trackingTool = pim.getTrackingTool();
            MingleConfig mingleConfig = pim.getMingleConfig();
            int trackingToolHash = trackingTool == null ? -1 : trackingTool.hashCode();
            int mingleToolHash = mingleConfig == null ? -1 : mingleConfig.hashCode();
            s.append("[").append(pim.getId()).append("|").append(trackingToolHash).append("|").append(mingleToolHash).append("]");
        }
        return s.toString();
    }
}
