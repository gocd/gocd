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
import java.util.List;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.exceptions.NoSuchEnvironmentException;
import com.thoughtworks.go.presentation.pipelinehistory.Environment;
import com.thoughtworks.go.presentation.pipelinehistory.PipelineModel;
import com.thoughtworks.go.server.domain.Username;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @understands instances of config objects wired under environments
 */
@Service
public class EnvironmentService {
    private final EnvironmentConfigService environmentConfigService;
    private final PipelineHistoryService pipelineHistoryService;

    @Autowired
    public EnvironmentService(EnvironmentConfigService environmentConfigService, PipelineHistoryService pipelineHistoryService) {
        this.environmentConfigService = environmentConfigService;
        this.pipelineHistoryService = pipelineHistoryService;
    }

    public List<Environment> getEnvironments(Username username) throws NoSuchEnvironmentException {
        List<CaseInsensitiveString> environmentNames = environmentConfigService.environmentNames();
        ArrayList<Environment> environments = new ArrayList<>();
        for (CaseInsensitiveString environmentName : environmentNames) {
            addEnvironmentFor(environmentName, username, environments);
        }
        return environments;
    }

    void addEnvironmentFor(CaseInsensitiveString environmentName, Username username, ArrayList<Environment> environments) throws NoSuchEnvironmentException {
        List<CaseInsensitiveString> pipelines = environmentConfigService.pipelinesFor(environmentName);
        if (pipelines.isEmpty()) {
            environments.add(new Environment(CaseInsensitiveString.str(environmentName), new ArrayList<PipelineModel>()));
            return;
        }
        List<PipelineModel> pipelineInstanceModels = getPipelinesInstanceForEnvironment(pipelines, username);
        if (!pipelineInstanceModels.isEmpty()) {
            environments.add(new Environment(CaseInsensitiveString.str(environmentName), pipelineInstanceModels));
        }
    }

    private List<PipelineModel> getPipelinesInstanceForEnvironment(List<CaseInsensitiveString> pipelines, Username username) throws NoSuchEnvironmentException {
        List<PipelineModel> pipelineList = new ArrayList<>();
        for (CaseInsensitiveString pipelineName : pipelines) {
            PipelineModel pipelineModel = pipelineHistoryService.latestPipelineModel(username, CaseInsensitiveString.str(pipelineName));
            if (pipelineModel != null) {
                pipelineList.add(pipelineModel);
            }
        }
        return pipelineList;
    }

}
