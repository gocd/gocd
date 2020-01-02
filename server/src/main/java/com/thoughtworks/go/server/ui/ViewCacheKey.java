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
package com.thoughtworks.go.server.ui;

import com.thoughtworks.go.config.TrackingTool;
import com.thoughtworks.go.domain.PipelineIdentifier;
import com.thoughtworks.go.domain.PipelinePauseInfo;
import com.thoughtworks.go.domain.Stage;
import com.thoughtworks.go.presentation.pipelinehistory.PipelineInstanceModel;
import com.thoughtworks.go.presentation.pipelinehistory.PipelineModel;
import com.thoughtworks.go.presentation.pipelinehistory.StageInstanceModel;
import com.thoughtworks.go.server.cache.CacheKeyGenerator;

public class ViewCacheKey {
    private static final String DELIMITER = "|";
    private final CacheKeyGenerator cacheKeyGenerator;

    public ViewCacheKey() {
        this.cacheKeyGenerator = new CacheKeyGenerator(getClass());
    }

    public String forFbhOfStagesUnderPipeline(PipelineIdentifier pipelineIdentifier) {
        return cacheKeyGenerator.generate("view", "fbhOfStagesUnderPipeline", pipelineIdentifier.pipelineLocator());
    }

    public String forFailedBuildHistoryStage(Stage stage, String format) {
        return cacheKeyGenerator.generate("view", "stageFailedBuildHistoryView", stage.getIdentifier().stageLocator(), stage.getState(), format);
    }

    public String forPipelineModelBox(PipelineModel model) {
        return keyForPipelineModelFragment(model, "dashboardPipelineFragment");
    }

    public String forEnvironmentPipelineBox(PipelineModel model) {
        return keyForPipelineModelFragment(model, "environmentPipelineFragment") + DELIMITER + model.hasNewRevisions();
    }

    private String keyForPipelineModelFragment(PipelineModel model, String name) {
        PipelinePauseInfo pauseInfo = model.getPausedInfo();
        return cacheKeyGenerator.generate("view", name, model.getName(),
                appendLockStatus(model),
                getPipelineInstanceModelAsKey(model),
                model.canOperate(),
                model.canForce(),
                pauseInfo.isPaused(),
                pauseInfo.getPauseCause().replaceAll("\\W", ""),
                pauseInfo.getPauseBy(),
                model.canAdminister()
        );
    }

    private String getPipelineInstanceModelAsKey(PipelineModel model) {
        final StringBuilder builder = new StringBuilder();
        for (PipelineInstanceModel pim : model.getActivePipelineInstances()) {
            builder.append("[");
            builder.append(pim.getId()).append(DELIMITER);
            for (StageInstanceModel stageInstanceModel : pim.getStageHistory()) {
                builder.append(stageInstanceModel.getName()).append(DELIMITER);
                builder.append(stageInstanceModel.getId()).append(DELIMITER);
                builder.append(stageInstanceModel.getState()).append(DELIMITER);
            }
            builder.append("]");
        }
        return builder.toString();
    }

    private String appendLockStatus(PipelineModel model) {
        StringBuilder builder = new StringBuilder();
        PipelineInstanceModel latestPIM = model.getLatestPipelineInstance();
        builder.append("{");
        builder.append(latestPIM.isLockable()).append(DELIMITER);
        builder.append(latestPIM.isCurrentlyLocked()).append(DELIMITER);
        builder.append(latestPIM.canUnlock());
        builder.append("}");
        return builder.toString();
    }

    public String forPipelineModelBuildCauses(PipelineModel model) {
        return cacheKeyGenerator.generate("view", "buildCausesForPipelineModel", model.getName(), appendActivePipelineInstanceModels(model, new StringBuilder()));
    }

    private String appendActivePipelineInstanceModels(PipelineModel model, StringBuilder s) {
        for (PipelineInstanceModel pim : model.getActivePipelineInstances()) {
            TrackingTool trackingTool = pim.getTrackingTool();
            int trackingToolHash = trackingTool == null ? -1 : trackingTool.hashCode();

            s.append("[").append(pim.getId()).append("|").append(trackingToolHash).append("]");
        }
        return s.toString();
    }
}
