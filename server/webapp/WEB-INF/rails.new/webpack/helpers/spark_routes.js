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

const $ = require('jquery');

module.exports = {
  pipelinePausePath: (pipelineName) => {
    return `/go/api/pipelines/${pipelineName}/pause`;
  },

  pipelineUnpausePath: (pipelineName) => {
    return `/go/api/pipelines/${pipelineName}/unpause`;
  },

  pipelineUnlockPath: (pipelineName) => {
    return `/go/api/pipelines/${pipelineName}/unlock`;
  },

  pipelineTriggerWithOptionsViewPath: (pipelineName) => {
    return `/go/api/pipelines/${pipelineName}/trigger_options`;
  },

  pipelineMaterialSearchPath: (pipelineName, fingerprint, searchText) => {
    const queryString = $.param({
      fingerprint,
      pipeline_name: pipelineName, //eslint-disable-line camelcase
      search_text:   searchText //eslint-disable-line camelcase
    });
    return `/go/api/internal/material_search?${queryString}`;
  }
};
