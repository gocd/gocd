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

package com.thoughtworks.go.server.dao;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.presentation.pipelinehistory.PipelineInstanceModel;
import com.thoughtworks.go.presentation.pipelinehistory.PipelineInstanceModels;

import java.util.List;

public interface PipelineInstanceModelDao {
    PipelineInstanceModel loadHistory(long id);

    PipelineInstanceModels loadHistory(String pipelineName, int resultsPerPage, int start);

    PipelineInstanceModels loadHistory(String pipelineName);

    PipelineInstanceModel findPipelineHistoryByNameAndCounter(String pipelineName, int pipelineCounter);

    PipelineInstanceModels loadActivePipelines();

    PipelineInstanceModels loadActivePipelineInstancesFor(CaseInsensitiveString pipelineName);

    PipelineInstanceModel loadHistoryByIdWithBuildCause(Long id);

    PipelineInstanceModels findMatchingPipelineInstances(String pipelineName, String pattern, int limit);

    PipelineInstanceModels loadHistoryForDashboard(List<String> pipelineNames);

    PipelineInstanceModels loadHistory(String pipelineName, FeedModifier modifier, long cursor, Integer pageSize);
}
