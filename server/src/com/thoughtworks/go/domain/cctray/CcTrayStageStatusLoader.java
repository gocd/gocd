/*************************GO-LICENSE-START*********************************
 * Copyright 2015 ThoughtWorks, Inc.
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

package com.thoughtworks.go.domain.cctray;

import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.StageConfig;
import com.thoughtworks.go.domain.activity.ProjectStatus;
import com.thoughtworks.go.server.dao.StageDao;
import com.thoughtworks.go.server.domain.StageIdentity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/* Understands how to load a stage from DB and convert it and its jobs to CcTray statuses. */
@Component
public class CcTrayStageStatusLoader {
    private List<StageIdentity> stageIdentifiers;
    private StageDao stageDao;
    private CcTrayStageStatusChangeHandler stageChangeHandler;

    @Autowired
    public CcTrayStageStatusLoader(StageDao stageDao, CcTrayStageStatusChangeHandler stageChangeHandler) {
        this.stageDao = stageDao;
        this.stageChangeHandler = stageChangeHandler;
    }

    public List<ProjectStatus> getStatusesForStageAndJobsOf(PipelineConfig pipelineConfig, StageConfig stageConfig) {
        // Old CCTray code says: required to load latest counter for each stage only for first cc tray request since server startup
        if (stageIdentifiers == null) {
            stageIdentifiers = stageDao.findLatestStageInstances();
        }

        Long stageId = findStageIdOf(pipelineConfig, stageConfig);
        if (stageId == null) {
            return new ArrayList<>();
        }

        return stageChangeHandler.statusesOfStageAndItsJobsFor(stageDao.stageById(stageId));
    }

    private Long findStageIdOf(PipelineConfig pipelineConfig, StageConfig stageConfig) {
        for (StageIdentity stageIdentity : stageIdentifiers) {
            if (stageIdentity.getPipelineName().equals(pipelineConfig.name().toString()) && stageIdentity.getStageName().equals(stageConfig.name().toString())) {
                return stageIdentity.getStageId();
            }
        }
        return null;
    }
}
