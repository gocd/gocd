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

const PIPELINE_NAME_KEY    = 'pipeline_name';
const PIPELINE_COUNTER_KEY = 'pipeline_counter';
const STAGE_NAME_KEY       = 'stage_name';
const STAGE_COUNTER_KEY    = 'stage_counter';
const JOB_NAME_KEY         = 'job_name';

//--- FetchFromParams
const pipelineName    = (params) => params[PIPELINE_NAME_KEY];
const pipelineCounter = (params) => params[PIPELINE_COUNTER_KEY];
const stageName       = (params) => params[STAGE_NAME_KEY];
const stageCounter    = (params) => params[STAGE_COUNTER_KEY];
const jobName         = (params) => params[JOB_NAME_KEY];

const openLinkInNewTab = (link) => {
  window.open(link, '_blank');
};

module.exports = {
  "job_details_page": (params) => {
    const jobDetailsPagePath = `/go/tab/build/detail/${pipelineName(params)}/${pipelineCounter(params)}/${stageName(params)}/${stageCounter(params)}/${jobName(params)}`;
    openLinkInNewTab(jobDetailsPagePath);
  },

  "pipeline_instance_page": (params) => {
    const pipelineInstancePath = `/go/internal/pipelines/${pipelineName(params)}/${pipelineCounter(params)}`;
    openLinkInNewTab(pipelineInstancePath);
  }
};
