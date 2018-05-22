/*
 * Copyright 2018 ThoughtWorks, Inc.
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
import com.thoughtworks.go.config.exceptions.NoSuchEnvironmentException;
import com.thoughtworks.go.presentation.pipelinehistory.Environment;
import com.thoughtworks.go.presentation.pipelinehistory.PipelineModel;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.util.SystemEnvironment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * @understands instances of config objects wired under environments
 */
@Service
public class EnvironmentService {
    private final EnvironmentConfigService environmentConfigService;
    private final PipelineHistoryService pipelineHistoryService;
    private SystemEnvironment systemEnvironment;

    @Autowired
    public EnvironmentService(EnvironmentConfigService environmentConfigService, PipelineHistoryService pipelineHistoryService, SystemEnvironment systemEnvironment) {
        this.environmentConfigService = environmentConfigService;
        this.pipelineHistoryService = pipelineHistoryService;
        this.systemEnvironment = systemEnvironment;
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
        if (pipelines.isEmpty() || !systemEnvironment.displayPipelineInstancesOnEnvironmentsPage()) {
            environments.add(new Environment(CaseInsensitiveString.str(environmentName), new ArrayList<>()));
            return;
        }
        List<PipelineModel> pipelineInstanceModels = getPipelinesInstanceForEnvironment(pipelines, username);
        if (!pipelineInstanceModels.isEmpty()) {
            environments.add(new Environment(CaseInsensitiveString.str(environmentName), pipelineInstanceModels));
        }
    }

    private List<PipelineModel> getPipelinesInstanceForEnvironment(List<CaseInsensitiveString> pipelines, Username username) {
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
