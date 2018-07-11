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

const m = require('mithril');

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

  pipelineTriggerPath: (pipelineName) => {
    return `/go/api/pipelines/${pipelineName}/schedule`;
  },

  pipelineTriggerWithOptionsViewPath: (pipelineName) => {
    return `/go/api/pipelines/${pipelineName}/trigger_options`;
  },

  pipelineMaterialSearchPath: (pipelineName, fingerprint, searchText) => {
    const queryString = m.buildQueryString({
      fingerprint,
      pipeline_name: pipelineName, //eslint-disable-line camelcase
      search_text:   searchText //eslint-disable-line camelcase
    });
    return `/go/api/internal/material_search?${queryString}`;
  },

  pipelineSelectionPath: () => {
    return '/go/api/internal/pipeline_selection';
  },

  pipelineSelectionPipelinesDataPath: () => {
    return '/go/api/internal/pipeline_selection/pipelines_data';
  },

  buildCausePath: (pipelineName, pipelineCounter) => {
    return `/go/api/internal/build_cause/${pipelineName}/${pipelineCounter}`;
  },

  artifactStoresPath: (id) => {
    if (id) {
      return `/go/api/admin/artifact_stores/${id}`;
    } else {
      return "/go/api/admin/artifact_stores";
    }
  },

  DataSharingSettingsPath: () => {
    return '/go/api/data_sharing/settings';
  },

  DataSharingUsageDataPath: () => {
    return '/go/api/internal/data_sharing/usagedata';
  },

	DataSharingUsageDataEncryptedPath: () => {
    return '/go/api/internal/data_sharing/usagedata/encrypted';
  },

	DataReportingInfoPath: () => {
		return '/go/api/internal/data_sharing/reporting/info';
	},

  DataReportingStartReportingPath: () => {
		return '/go/api/internal/data_sharing/reporting/start';
	},

  DataReportingCompleteReportingPath: () => {
    return '/go/api/internal/data_sharing/reporting/complete';
  }
};
