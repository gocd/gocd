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

package com.thoughtworks.go.server.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.PipelineNotFoundException;
import com.thoughtworks.go.config.PipelineConfigs;
import com.thoughtworks.go.domain.PipelineIdentifier;
import com.thoughtworks.go.util.json.Json;
import com.thoughtworks.go.util.json.JsonList;
import com.thoughtworks.go.server.presentation.models.PipelineJsonPresentationModel;
import com.thoughtworks.go.server.presentation.models.StageJsonPresentationModel;
import com.thoughtworks.go.server.service.result.ServerHealthStateOperationResult;
import com.thoughtworks.go.util.Filter;
import static com.thoughtworks.go.util.ListUtil.filterInto;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @understands How to construct json for current activity
 */
@Service
public class JsonCurrentActivityService {

    private GoConfigService goConfigService;
    private CurrentActivityService currentActivity;
    private SecurityService securityService;
    private SchedulingCheckerService checkerService;

    @Autowired
    public JsonCurrentActivityService(GoConfigService goConfigService,
                                      CachedCurrentActivityService currentActivity,
                                      SecurityService securityService,
                                      SchedulingCheckerService checkerService) {
        this.goConfigService = goConfigService;
        this.currentActivity = currentActivity;
        this.securityService = securityService;
        this.checkerService = checkerService;
    }

    public Json pipelinesActivityAsJson(String username, String pipelineName) {
        Collection<PipelineJsonPresentationModel> pipelines = pipelinesActivity(username, pipelineName);
        return toJsonList(pipelines);
    }

    Collection<PipelineJsonPresentationModel> pipelinesActivity(String username, String pipelineName) {
        Collection<PipelineJsonPresentationModel> pipelines;
        if (StringUtils.isBlank(pipelineName)) {
            pipelines = allPipelines(username);
        } else {
            pipelines = onePipeline(username, pipelineName);
            if (pipelines.isEmpty()) {
                throw new PipelineNotFoundException("Failed to find the pipeline " + pipelineName);
            }
        }
        addDynamicPipelineStatusTo(pipelines, username);
        return pipelines;
    }

    private void addDynamicPipelineStatusTo(Collection<PipelineJsonPresentationModel> pipelines, String username) {
        for (PipelineJsonPresentationModel pipeline : pipelines) {
            PipelineConfig pipelineConfig = goConfigService.pipelineConfigNamed(new CaseInsensitiveString(pipeline.getName()));

            boolean canForce = checkerService.canManuallyTrigger(pipelineConfig, username,
                    new ServerHealthStateOperationResult());
            pipeline.setCanForce(canForce);
            pipeline.setCanPause(securityService.hasOperatePermissionForPipeline(new CaseInsensitiveString(username), pipeline.getName()));

            for (StageJsonPresentationModel stageModel : pipeline.getStages()) {
                boolean canCancel = securityService.hasOperatePermissionForStage(pipeline.getName(),
                        stageModel.getName(), username);
                stageModel.setCanCancel(canCancel);

                if (stageModel.stageHasHistory()) {
                    String stageName = stageModel.getName();

                    PipelineIdentifier pipelineIdentifier = stageModel.getPipeline().getIdentifier();
                    boolean canRun = checkerService.canScheduleStage(pipelineIdentifier, stageName, username, new ServerHealthStateOperationResult());

                    stageModel.setCanRun(canRun);
                } else {
                    stageModel.setCanRun(false);
                }
            }

        }
    }


    private Collection<PipelineJsonPresentationModel> onePipeline(String username, final String pipelineName) {
        List<PipelineJsonPresentationModel> models = new ArrayList<PipelineJsonPresentationModel>();
        PipelineJsonPresentationModel model = currentActivity.getPipelineStatus(pipelineName);
        if (model != null) {
            models.add(model);
        }
        return filter(username, models);
    }

    private Collection<PipelineJsonPresentationModel> allPipelines(String username) {
        List<PipelineJsonPresentationModel> pipelines = new ArrayList<PipelineJsonPresentationModel>();
        for (PipelineConfigs group : goConfigService.getCurrentConfig().getGroups()) {
            if (securityService.hasViewPermissionForGroup(username, group.getGroup())) {
                for (PipelineConfig config : group) {

                    PipelineJsonPresentationModel jsonPresentationModel = currentActivity.getPipelineStatus(CaseInsensitiveString.str(config.name()));
                    if (jsonPresentationModel != null) {
                        pipelines.add(jsonPresentationModel);
                    }
                }
            }
        }
        return filter(username, pipelines);
    }

    private Collection<PipelineJsonPresentationModel> filter(final String username,
                                                             Collection<PipelineJsonPresentationModel> source) {
        Filter<PipelineJsonPresentationModel> securityFilter = new Filter<PipelineJsonPresentationModel>() {

            public boolean matches(PipelineJsonPresentationModel model) {
                return securityService.hasViewPermissionForGroup(username, model.getGroupName());
            }
        };
        return filterInto(new ArrayList<PipelineJsonPresentationModel>(), source, securityFilter);
    }



    private JsonList toJsonList(Collection<PipelineJsonPresentationModel> pipelines) {
        JsonList list = new JsonList();
        for (PipelineJsonPresentationModel pipelineModel : pipelines) {
            list.add(pipelineModel.toJson());
        }
        return list;
    }


}
