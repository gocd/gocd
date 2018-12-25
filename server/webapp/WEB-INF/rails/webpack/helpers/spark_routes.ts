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

import * as m from "mithril";

export default class {
  static adminPipelineConfigPath(pipelineName: string): string {
    return `/go/api/admin/pipelines/${pipelineName}`;
  }

  static pipelinePausePath(pipelineName: string): string {
    return `/go/api/pipelines/${pipelineName}/pause`;
  }

  static pipelineUnpausePath(pipelineName: string): string {
    return `/go/api/pipelines/${pipelineName}/unpause`;
  }

  static pipelineUnlockPath(pipelineName: string): string {
    return `/go/api/pipelines/${pipelineName}/unlock`;
  }

  static pipelineTriggerPath(pipelineName: string): string {
    return `/go/api/pipelines/${pipelineName}/schedule`;
  }

  static pipelineTriggerWithOptionsViewPath(pipelineName: string): string {
    return `/go/api/pipelines/${pipelineName}/trigger_options`;
  }

  static pipelineMaterialSearchPath(pipelineName: string, fingerprint: string, searchText: string): string {
    const queryString = m.buildQueryString({
                                             fingerprint,
                                             pipeline_name: pipelineName,
                                             search_text: searchText
                                           });
    return `/go/api/internal/material_search?${queryString}`;
  }

  static pipelineSelectionPath(): string {
    return "/go/api/internal/pipeline_selection";
  }

  static pipelineSelectionPipelinesDataPath(): string {
    return "/go/api/internal/pipeline_selection/pipelines_data";
  }

  static buildCausePath(pipelineName: string, pipelineCounter: string): string {
    return `/go/api/internal/build_cause/${pipelineName}/${pipelineCounter}`;
  }

  static artifactStoresPath(id?: string): string {
    if (id) {
      return `/go/api/admin/artifact_stores/${id}`;
    } else {
      return "/go/api/admin/artifact_stores";
    }
  }

  static DataSharingSettingsPath(): string {
    return "/go/api/data_sharing/settings";
  }

  static DataSharingUsageDataPath(): string {
    return "/go/api/internal/data_sharing/usagedata";
  }

  static DataSharingUsageDataEncryptedPath(): string {
    return "/go/api/internal/data_sharing/usagedata/encrypted";
  }

  static DataReportingInfoPath(): string {
    return "/go/api/internal/data_sharing/reporting/info";
  }

  static DataReportingStartReportingPath(): string {
    return "/go/api/internal/data_sharing/reporting/start";
  }

  static DataReportingCompleteReportingPath(): string {
    return "/go/api/internal/data_sharing/reporting/complete";
  }

  static ApiConfigReposListPath(): string {
    return `/go/api/admin/config_repos`;
  }

  static ApiConfigRepoPath(id: string): string {
    return `/go/api/admin/config_repos/${id}`;
  }

  static apiConfigReposInternalPath(): string {
    return `/go/api/internal/config_repos`;
  }

  static configRepoTriggerUpdatePath(id: string): string {
    return `/go/api/internal/config_repos/${id}/trigger_update`;
  }

  static configRepoRevisionStatusPath(id: string): string {
    return `/go/api/internal/config_repos/${id}/status`;
  }

  static elasticProfilePath(profileId: string): string {
    return `/go/api/elastic/profiles/${profileId}`;
  }

  static elasticProfileListPath(): string {
    return "/go/api/elastic/profiles";
  }

  static elasticProfileUsagePath(profileId: string): string {
    return `/go/api/internal/elastic/profiles/${profileId}/usages`;
  }

  static agentsPath(uuid?: string): string {
    if (uuid) {
      return `/go/api/agents/${uuid}`;
    } else {
      return `/go/api/agents`;
    }
  }

  static enableDrainModePath(): string {
    return `/go/api/admin/drain_mode/enable`;
  }

  static disableDrainModePath(): string {
    return `/go/api/admin/drain_mode/disable`;
  }

  static drainModeInfoPath(): string {
    return `/go/api/admin/drain_mode/info`;
  }

  static cancelStage(pipelineName: string, stageName: string): string {
    return `/go/api/stages/${pipelineName}/${stageName}/cancel`;
  }

  static authConfigPath(authConfigId?: string): string {
    if (authConfigId) {
      return `/go/api/admin/security/auth_configs/${authConfigId}`;
    } else {
      return "/go/api/admin/security/auth_configs";
    }
  }

  static adminInternalVerifyConnectionPath(): string {
    return `/go/api/admin/internal/security/auth_configs/verify_connection`;
  }

  static apiUsersListPath() {
    return "/go/api/users";
  }
}
