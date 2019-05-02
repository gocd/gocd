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

  static serverHealthMessagesPath() {
    return `/go/api/server_health_messages`;
  }

  static pipelineGroupsListPath() {
    return `/go/api/admin/pipeline_groups`;
  }

  static adminPipelineConfigPath(pipelineName: string): string {
    return `/go/api/admin/pipelines/${pipelineName}`;
  }

  static newCreatePipelinePath(): string {
    return `/go/admin/pipelines/create`;
  }

  static pipelineConfigCreatePath(): string {
    return `/go/api/admin/pipelines`;
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

  static internalDependencyMaterialSuggestionsPath(): string {
    return `/go/api/internal/dependency_material/autocomplete_suggestions`;
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

  static showDashboardPath(viewName?: string): string {
    if (viewName) {
      return `/go/api/dashboard?${m.buildQueryString({viewName})}`;
    }
    return "/go/api/dashboard";
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

  static materialConnectionCheck(): string {
    return `/go/api/admin/internal/material_test`;
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

  static clusterProfilesListPath(): string {
    return "/go/api/admin/elastic/cluster_profiles";
  }

  static agentsPath(uuid?: string): string {
    if (uuid) {
      return `/go/api/agents/${uuid}`;
    } else {
      return `/go/api/agents`;
    }
  }

  static enableMaintenanceModePath(): string {
    return `/go/api/admin/maintenance_mode/enable`;
  }

  static disableMaintenanceModePath(): string {
    return `/go/api/admin/maintenance_mode/disable`;
  }

  static maintenanceModeInfoPath(): string {
    return `/go/api/admin/maintenance_mode/info`;
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

  static rolesPath(type?: "gocd" | "plugin") {
    if (type) {
      return `/go/api/admin/security/roles?type=${type}`;
    } else {
      return "/go/api/admin/security/roles";
    }
  }

  static rolePath(roleName: string): string {
    return `/go/api/admin/security/roles/${roleName}`;
  }

  static adminInternalVerifyConnectionPath(): string {
    return `/go/api/admin/internal/security/auth_configs/verify_connection`;
  }

  static apiUsersPath() {
    return "/go/api/users";
  }

  static apiBulkUserStateUpdatePath() {
    return "/go/api/users/operations/state";
  }

  static apiUserPath(username: string) {
    return `/go/api/users/${username}`;
  }

  static apiUsersSearchPath(searchText: string) {
    return `/go/api/user_search?${m.buildQueryString({q: searchText})}`;
  }

  static apisystemAdminsPath() {
    return "/go/api/admin/security/system_admins";
  }

  static apiCreateServerBackupPath() {
    return `/go/api/backups`;
  }

  static apiRunningServerBackupsPath() {
    return `/go/api/backups/running`;
  }

  static apiCurrentAccessTokenPath(id: number) {
    return `/go/api/current_user/access_tokens/${id}`;
  }

  static apiCurrentAccessTokenRevokePath(id: number) {
    return `${this.apiCurrentAccessTokenPath(id)}/revoke`;
  }

  static apiCurrentAccessTokensPath(filter?: "all" | "revoked" | "active") {
    if (filter) {
      return `/go/api/current_user/access_tokens?${m.buildQueryString({filter})}`;
    } else {
      return `/go/api/current_user/access_tokens`;
    }
  }

  static apiAdminAccessTokensBasePath() {
    return `/go/api/admin/access_tokens`;
  }

  static apiAdminAccessTokensPath(filter: string) {
    return `${this.apiAdminAccessTokensBasePath()}?${filter}`;
  }

  static apiAdminAccessTokenRevokePath(id: number) {
    return `${this.apiAdminAccessTokensBasePath()}/${id}/revoke`;
  }

  static apiAdminAccessClusterProfilesPath(id?: string) {
    if (id) {
      return `/go/api/admin/elastic/cluster_profiles/${id}`;
    }
    return `/go/api/admin/elastic/cluster_profiles`;
  }
}
