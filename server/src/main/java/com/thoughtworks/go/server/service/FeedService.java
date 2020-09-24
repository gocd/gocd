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
package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.exceptions.EntityType;
import com.thoughtworks.go.config.exceptions.NotAuthorizedException;
import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.domain.feed.FeedEntries;
import com.thoughtworks.go.presentation.pipelinehistory.PipelineInstanceModel;
import com.thoughtworks.go.presentation.pipelinehistory.PipelineInstanceModels;
import com.thoughtworks.go.server.dao.FeedModifier;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.domain.xml.*;
import com.thoughtworks.go.server.domain.xml.materials.MaterialXmlRepresenter;
import org.dom4j.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FeedService {
    private final PipelineHistoryService pipelineHistoryService;
    private final GoConfigService goConfigService;
    private final StageService stageService;
    private final JobInstanceService jobInstanceService;
    private final SecurityService securityService;
    private final XmlApiService xmlApiService;
    private static final String NOT_AUTHORIZED_TO_VIEW_PIPELINE = "Not authorized to view pipeline";

    @Autowired
    public FeedService(PipelineHistoryService pipelineHistoryService,
                       GoConfigService goConfigService,
                       StageService stageService,
                       JobInstanceService jobInstanceService,
                       SecurityService securityService,
                       XmlApiService xmlApiService) {
        this.pipelineHistoryService = pipelineHistoryService;
        this.goConfigService = goConfigService;
        this.stageService = stageService;
        this.jobInstanceService = jobInstanceService;
        this.securityService = securityService;
        this.xmlApiService = xmlApiService;
    }

    public Document pipelinesXml(Username username, String baseUrl) {
        PipelineInstanceModels pipelineInstanceModels = pipelineHistoryService.latestInstancesForConfiguredPipelines(username);
        XmlRepresentable representable = new PipelinesXmlRepresenter(pipelineInstanceModels);

        return xmlApiService.write(representable, baseUrl);
    }

    public Document stagesXml(Username username, String pipelineName, Integer pipelineCounter, String baseUrl) {
        if (!goConfigService.hasPipelineNamed(new CaseInsensitiveString(pipelineName))) {
            throw new RecordNotFoundException(EntityType.Pipeline, pipelineName);
        }

        if (!securityService.hasViewPermissionForPipeline(username, pipelineName)) {
            throw new NotAuthorizedException(NOT_AUTHORIZED_TO_VIEW_PIPELINE);
        }

        FeedEntries feedEntries = stageService.findStageFeedBy(pipelineName, pipelineCounter, FeedModifier.Before, username);
        FeedEntriesRepresenter representable = new FeedEntriesRepresenter(pipelineName, feedEntries);

        return xmlApiService.write(representable, baseUrl);
    }

    public Document pipelineXml(Username username, String pipelineName, Integer pipelineCounter, String baseUrl) {
        PipelineInstanceModel model = pipelineHistoryService.load(pipelineName, pipelineCounter, username);
        return xmlApiService.write(new PipelineXmlRepresenter(model), baseUrl);
    }

    public Document stageXml(Username username, String pipelineName, Integer pipelineCounter, String stageName, Integer stageCounter, String baseUrl) {
        Stage stage = stageService.findStageWithIdentifier(pipelineName, pipelineCounter, stageName, String.valueOf(stageCounter), username);
        return xmlApiService.write(new StageXmlRepresenter(stage), baseUrl);
    }

    public Document jobXml(Username username, String pipelineName, Integer pipelineCounter, String stageName,
                           Integer stageCounter, String jobName, String baseUrl) {
        JobInstance jobInstance = jobInstanceService.findJobInstance(pipelineName, stageName, jobName, pipelineCounter, stageCounter, username);
        return xmlApiService.write(new JobXmlRepresenter(jobInstance), baseUrl);
    }

    public Document waitingJobPlansXml(String baseUrl, Username username) {
        List<WaitingJobPlan> waitingJobPlans = jobInstanceService.waitingJobPlans(username);
        return xmlApiService.write(new JobPlanXmlRepresenter(waitingJobPlans), baseUrl);
    }

    public Document materialXml(Username username, String pipelineName, Integer pipelineCounter, String fingerprint, String baseUrl) {
        PipelineInstanceModel model = pipelineHistoryService.load(pipelineName, pipelineCounter, username);
        MaterialRevision revision = model.getLatestRevisions().findRevisionForPipelineUniqueFingerprint(fingerprint);
        if (revision == null) {
            throw new RecordNotFoundException(String.format("Material with pipeline unique fingerprint '%s' was not found for pipeline run(%s/%s)!", fingerprint, pipelineName, pipelineCounter));
        }
        MaterialXmlRepresenter materialXmlRepresenter = MaterialXmlRepresenter.representerFor(pipelineName, pipelineCounter, revision);
        return xmlApiService.write(materialXmlRepresenter, baseUrl);
    }
}
