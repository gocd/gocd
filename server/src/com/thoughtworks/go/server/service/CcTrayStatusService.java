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

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.JobConfig;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.StageConfig;
import com.thoughtworks.go.domain.NullStage;
import com.thoughtworks.go.domain.PipelineGroupVisitor;
import com.thoughtworks.go.domain.PiplineConfigVisitor;
import com.thoughtworks.go.domain.Stage;
import com.thoughtworks.go.domain.activity.CcTrayStatus;
import com.thoughtworks.go.domain.activity.ProjectStatus;
import com.thoughtworks.go.server.domain.StageIdentity;
import com.thoughtworks.go.server.util.UserHelper;
import org.jdom.Document;
import org.jdom.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CcTrayStatusService {
    private GoConfigService goConfigService;
    private CcTrayStatus ccTrayStatus;
    private StageService stageService;
    private SecurityService securityService;
    private final ServerConfigService serverConfigService;
    private List<StageIdentity> stageIdentifiers;

    @Autowired
    public CcTrayStatusService(GoConfigService goConfigService, CcTrayStatus ccTrayStatus,
                               StageService stageService, SecurityService securityService, ServerConfigService serverConfigService) {
        this.goConfigService = goConfigService;
        this.ccTrayStatus = ccTrayStatus;
        this.stageService = stageService;
        this.securityService = securityService;
        this.serverConfigService = serverConfigService;
    }


    public Document createCctrayXmlDocument(String requestContextPath) throws URISyntaxException {
        Element root = new Element("Projects");
        initializeMissingStages();
        clearInactiveProjects();
        Collection<ProjectStatus> projects = cctrayModel();
        for (ProjectStatus project : projects) {
            root.addContent(project.ccTrayXmlElement(serverConfigService.siteUrlFor(requestContextPath, false)));
        }
        return new Document(root);
    }

    private void clearInactiveProjects() {
        Set<String> activeProjectNames = activeProjectNames();
        ccTrayStatus.removeProjectsNotIn(activeProjectNames);
    }

    private ArrayList<ProjectStatus> cctrayModel() {
        CcTrayFeedBuilder builder = new CcTrayFeedBuilder(ccTrayStatus);

        String userName = CaseInsensitiveString.str(UserHelper.getUserName().getUsername());
        PipelineGroupVisitor visitor = new SecurityFilter(builder, goConfigService, securityService, userName);

        CruiseConfig config = goConfigService.currentCruiseConfig();
        config.accept(visitor);
        return builder.getResult();
    }


    private Set<String> activeProjectNames() {
        final Set<String> activeProjectNames = new HashSet<String>();
        CruiseConfig cruiseConfig = goConfigService.getCurrentConfig();
        cruiseConfig.accept(new PiplineConfigVisitor() {
            public void visit(PipelineConfig pipelineConfig) {
                for (StageConfig stageConfig : pipelineConfig) {
                    String stageProjectName = projectName(pipelineConfig.name().toString(), stageConfig.name().toString());
                    activeProjectNames.add(stageProjectName);
                    for (JobConfig jobConfig : stageConfig.allBuildPlans()) {
                        String jobProjectName = String.format("%s :: %s :: %s", pipelineConfig.name(),
                                stageConfig.name(),
                                jobConfig.name());
                        activeProjectNames.add(jobProjectName);
                    }
                }
            }
        });
        return activeProjectNames;
    }

    private void initializeMissingStages() {
        // required to load latest counter for each stage only for first cc tray request since server startup
        if (stageIdentifiers == null) {
            stageIdentifiers = stageService.findLatestStageInstances();
        }
        CruiseConfig config = goConfigService.currentCruiseConfig();
        config.accept(new PiplineConfigVisitor() {
            public void visit(PipelineConfig pipelineConfig) {
                for (StageConfig stageConfig : pipelineConfig) {
                    String stageProjectName = projectName(pipelineConfig.name().toString(), stageConfig.name().toString());
                    if (!ccTrayStatus.containsProject(stageProjectName) && !ccTrayStatus.hasNoActivityFor(stageProjectName)) {
                        Long stageId = find(pipelineConfig, stageConfig);

                        Stage stage = stageId == null ? NullStage.createNullStage(stageConfig) : stageService.stageById(stageId);
                        ccTrayStatus.updateStatusFor(stageProjectName, stage);
                    }
                }
            }
        });
    }

    private Long find(PipelineConfig pipelineConfig, StageConfig stageConfig) {
        for (StageIdentity stageIdentity : stageIdentifiers) {
            if (stageIdentity.getPipelineName().equals(pipelineConfig.name().toString()) && stageIdentity.getStageName().equals(stageConfig.name().toString())) {
                return stageIdentity.getStageId();
            }
        }
        return null;
    }


    private String projectName(final String pipelineName, final String stageName) {
        return String.format("%s :: %s", pipelineName, stageName);
    }


}
