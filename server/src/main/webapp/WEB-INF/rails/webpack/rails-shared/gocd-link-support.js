/*
 * Copyright 2023 Thoughtworks, Inc.
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
/* eslint-disable */
(function () {
  "use strict";

  var PIPELINE_NAME_KEY    = 'pipeline_name';
  var PIPELINE_COUNTER_KEY = 'pipeline_counter';
  var STAGE_NAME_KEY       = 'stage_name';
  var STAGE_COUNTER_KEY    = 'stage_counter';
  var JOB_NAME_KEY         = 'job_name';

//--- FetchFromParams
  var pipelineName    = function pipelineName(params) {
    return params[PIPELINE_NAME_KEY];
  };
  var pipelineCounter = function pipelineCounter(params) {
    return params[PIPELINE_COUNTER_KEY];
  };
  var stageName       = function stageName(params) {
    return params[STAGE_NAME_KEY];
  };
  var stageCounter    = function stageCounter(params) {
    return params[STAGE_COUNTER_KEY];
  };
  var jobName         = function jobName(params) {
    return params[JOB_NAME_KEY];
  };

  var openLinkInNewTab = function openLinkInNewTab(link) {
    window.open(link, '_blank');
  };

  var GoCDLinkSupport = {
    "job_details_page": function jobDetailsPage(params) {
      var jobDetailsPagePath = '/go/tab/build/detail/' + pipelineName(params) + '/' + pipelineCounter(params) + '/' + stageName(params) + '/' + stageCounter(params) + '/' + jobName(params);
      openLinkInNewTab(jobDetailsPagePath);
    },

    "pipeline_instance_page": function pipelineInstancePage(params) {
      var pipelineInstancePath = '/go/internal/pipelines/' + pipelineName(params) + '/' + pipelineCounter(params);
      openLinkInNewTab(pipelineInstancePath);
    },

    "vsm_page": function vsmPage(params) {
      var vsmPath = '/go/pipelines/value_stream_map/' + pipelineName(params) + '/' + pipelineCounter(params);
      openLinkInNewTab(vsmPath);
    },

    "stage_details_page": function stageDetailsPage(params) {
      var stageDetailsPagePath = '/go/pipelines/' + pipelineName(params) + '/' + pipelineCounter(params) + '/' + stageName(params) + '/' + stageCounter(params);
      openLinkInNewTab(stageDetailsPagePath);
    }
  };

  if ("undefined" !== typeof module) {
    module.exports = GoCDLinkSupport;
  } else {
    window.GoCDLinkSupport = GoCDLinkSupport;
  }

})();
