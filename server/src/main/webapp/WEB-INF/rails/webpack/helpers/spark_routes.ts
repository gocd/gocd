/*
 * Copyright 2019 ThoughtWorks, Inc.
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
import m, {Params} from "mithril";
import {SupportedEntity} from "models/shared/permissions";
import {AnalyticsCapability} from "models/shared/plugin_infos_new/analytics_plugin_capabilities";
import {PluginInfoQuery} from "models/shared/plugin_infos_new/plugin_info_crud";

export class SparkRoutes {

  static serverHealthMessagesPath() {
    return `/go/api/server_health_messages`;
  }

  static pipelineGroupsPath(groupName?: string) {
    if (groupName) {
      return `/go/api/admin/pipeline_groups/${groupName}`;
    } else {
      return `/go/api/admin/pipeline_groups`;
    }
  }

  static pipelineGroupsSPAPath(groupName?: string) {
    if (groupName) {
      return `/go/admin/pipelines/#!${groupName}`;
    } else {
      return `/go/admin/pipelines`;
    }
  }

  static adminPipelineConfigPath(pipelineName?: string): string {
    if (pipelineName) {
      return `/go/api/admin/pipelines/${pipelineName}`;
    }
    return `/go/api/admin/pipelines`;
  }

  static adminExtractTemplateFromPipelineConfigPath(pipelineName: string): string {
    return `/go/api/admin/pipelines/${pipelineName}/extract_to_template`;
  }

  static newCreatePipelinePath(group?: string): string {
    const basePath = `/go/admin/pipelines/create`;
    if (!group) {
      return basePath;
    } else {
      return `${basePath}?${m.buildQueryString({group})}`;
    }
  }

  static createPipelineAsCodePath(): string {
    return `/go/admin/pipelines/as-code`;
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
                                             search_text:   searchText
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

  static showDashboardPath(viewName?: string, allowEmpty?: boolean): string {
    const params: any = {};

    if (viewName) {
      Object.assign(params, {viewName});
    }

    if ("boolean" === typeof allowEmpty) {
      Object.assign(params, {allowEmpty});
    }

    return Object.keys(params).length ?
      `/go/api/dashboard?${m.buildQueryString(params)}` :
      "/go/api/dashboard";
  }

  static templatesPath(id?: string): string {
    if (id) {
      return `/go/api/admin/templates/${id}`;
    } else {
      return "/go/api/admin/templates";
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

  static ConfigRepoViewPath(id?: string): string {
    if (id) {
      return `/go/admin/config_repos#!/${id}`;
    } else {
      return `/go/admin/config_repos`;
    }
  }

  static apiConfigReposInternalPath(): string {
    return `/go/api/internal/config_repos`;
  }

  static configRepoTriggerUpdatePath(id: string): string {
    return `/go/api/admin/config_repos/${id}/trigger_update`;
  }

  static pacListConfigFiles(pluginId: string) {
    return `/go/api/admin/internal/pac/config_files/${encodeURIComponent(pluginId)}`;
  }

  static pacPreview(pluginId: string, group: string, validate?: boolean): string {
    const url = `/go/api/admin/internal/pac/preview/${pluginId}`;

    const q = new URLSearchParams();
    q.append("group", group);

    if (void 0 !== validate) {
      q.append("validate", "" + validate);
    }

    return `${url}?${q}`;
  }

  static materialConnectionCheck(): string {
    return `/go/api/admin/internal/material_test`;
  }

  static configRepoRevisionStatusPath(id: string): string {
    return `/go/api/admin/config_repos/${id}/status`;
  }

  static configRepoDefinedConfigsPath(id: string): string {
    return `/go/api/admin/config_repos/${id}/definitions`;
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

  static agentJobRunHistoryAPIPath(uuid: string, offset: number): string {
    return `/go/api/agents/${uuid}/job_run_history?${m.buildQueryString({offset})}`;
  }

  static maintenanceModeInfoPath(): string {
    return `/go/api/admin/maintenance_mode/info`;
  }

  static cancelStage(pipelineName: string,
                     pipelineCounter: number | string,
                     stageName: string,
                     stageCounter: number | string): string {
    return `/go/api/stages/${pipelineName}/${pipelineCounter}/${stageName}/${stageCounter}/cancel`;
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

  static internalRolesPath(type?: "gocd" | "plugin") {
    if (type) {
      return `/go/api/admin/internal/roles?type=${type}`;
    } else {
      return "/go/api/admin/internal/roles";
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

  static apiUserPath(username: string) {
    return `/go/api/users/${username}`;
  }

  static apiBulkUserStateUpdatePath() {
    return "/go/api/users/operations/state";
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

  static apiSecretConfigsPath(id?: string) {
    if (id) {
      return `/go/api/admin/secret_configs/${id}`;
    }
    return `/go/api/admin/secret_configs`;
  }

  static apiEnvironmentPath(name?: string) {
    if (name) {
      return `/go/api/admin/environments/${name}`;
    }
    return "/go/api/admin/environments";
  }

  static apiPluginInfoPath(query: PluginInfoQuery) {
    const queryString = m.buildQueryString(query as m.Params);
    return `/go/api/admin/plugin_info?${queryString}`;
  }

  static backupConfigPath() {
    return "/go/api/config/backup";
  }

  static apiAdminPluginSettingPath(id?: string) {
    if (id) {
      return `/go/api/admin/plugin_settings/${id}`;
    }
    return `/go/api/admin/plugin_settings`;
  }

  static mailServerConfigPath() {
    return "/go/api/config/mailserver";
  }

  static testMailForMailServerConfigPath() {
    return "/go/api/config/mailserver/test";
  }

  static siteUrlsPath() {
    return "/go/api/admin/config/server/site_urls";
  }

  static artifactConfigPath() {
    return "/go/api/admin/config/server/artifact_config";
  }

  static apiAdminInternalResourcesPath() {
    return "/go/api/admin/internal/resources";
  }

  static apiAdminInternalEnvironmentsPath(name?: string) {
    if (name) {
      return `/go/api/admin/internal/environments/${name}`;
    }
    return "/go/api/admin/internal/environments";
  }

  static apiAdminEnvironmentsPath(name?: string) {
    if (name) {
      return `/go/api/admin/environments/${name}`;
    }

    return "/go/api/admin/environments";
  }

  static apiAdminInternalMergedEnvironmentsPath() {
    return "/go/api/admin/internal/environments/merged";
  }

  static apiAdminInternalPipelinesListPath(groupAuthorization: "view" | "operate" | "administer",
                                           templateAuthorization: "view" | "administer",
                                           withAdditionalInfo: boolean = false) {
    const values: Params = {
      pipeline_group_authorization: groupAuthorization,
      template_authorization:       templateAuthorization,
    };
    if (withAdditionalInfo) {
      values.with_additional_info = true;
    }
    const queryString = m.buildQueryString(values);

    return `/go/api/internal/pipeline_structure?${queryString}`;
  }

  static showAnalyticsPath(pluginId: string, metric: AnalyticsCapability, params: { [key: string]: string | number }) {
    return `/go/analytics/${pluginId}/${metric.type}/${metric.id}?${m.buildQueryString(params)}`;
  }

  static exportPipelinePath(pluginId: string, pipeline: string) {
    return `/go/api/admin/export/pipelines/${pipeline}?${m.buildQueryString({plugin_id: pluginId})}`;
  }

  static jobTimeoutPath() {
    return "/go/api/admin/config/server/default_job_timeout";
  }

  static editTemplatePermissions(templateName: string) {
    return `/go/admin/templates/${templateName}/permissions`;
  }

  static getEnvironmentPathOnSPA(environmentName: string) {
    return `/go/admin/environments/#!${environmentName}`;
  }

  static apiPipelineActivity(params: { [key: string]: string | number }) {
    return `/go/pipelineHistory.json?${m.buildQueryString(params)}`;
  }

  static pipelineVsmLink(pipelineName: string, counter: string) {
    return `/go/pipelines/value_stream_map/${pipelineName}/${counter}`;
  }

  static runStage(pipelineName: string, counter: string, stageName: string) {
    return `/go/api/stages/${pipelineName}/${counter}/${stageName}/run`;
  }

  static cancelStageInstance(pipelineName: string, pipelineCounter: string, stageName: string, stageCounter: number) {
    return `/go/api/stages/${pipelineName}/${pipelineCounter}/${stageName}/${stageCounter}/cancel`;
  }

  static commentOnPipelineInstance(pipelineName: string, pipelineCounter: string | number) {
    return `/go/api/pipelines/${pipelineName}/${pipelineCounter}/comment`;
  }

  static pluginStatusReportPath(plugin_id: string): string {
    return `/go/admin/status_reports/${plugin_id}`;
  }

  static clusterStatusReportPath(plugin_id: string, cluster_profile_id: string): string {
    return `/go/admin/status_reports/${plugin_id}/cluster/${cluster_profile_id}`;
  }

  static pipelineHistoryPath(pipelineName: string) {
    return `/go/pipeline/activity/${pipelineName}`;
  }

  static comparePipelines(pipelineName: string, fromCounter: number, toCounter: number) {
    return `/go/api/pipelines/${pipelineName}/compare/${fromCounter}/${toCounter}`;
  }

  static getPipelineInstance(pipelineName: string, pipelineCounter: number) {
    return `/go/api/pipelines/${pipelineName}/${pipelineCounter}`;
  }

  static getPipelineHistory(pipelineName: string) {
    return `/go/api/pipelines/${pipelineName}/history`;
  }

  static getMatchingPipelineInstances(pipelineName: string, pattern: string) {
    return `/go/api/internal/compare/${pipelineName}/list?pattern=${pattern}`;
  }

  static getStageDetailsPageUrl(pipelineName: string,
                                pipelineCounter: number,
                                stageName: string,
                                stageCounter: string) {
    return `/go/pipelines/${pipelineName}/${pipelineCounter}/${stageName}/${stageCounter}`;
  }

  static apiPermissionsPath(type?: SupportedEntity[]) {
    const basePath = `/go/api/auth/permissions`;
    if (!type || type.length === 0) {
      return basePath;
    } else {
      return `${basePath}?${m.buildQueryString({type: type.join(",")})}`;
    }
  }

  static getUpstreamPipelines(pipelineName: string, stageName: string, isTemplate: boolean) {
    let params: string = "";
    if (isTemplate) {
      params = `?${m.buildQueryString({template: true})}`;
    }
    return `/go/api/internal/pipelines/${pipelineName}/${stageName}/upstream${params}`;
  }

  static packageRepositoryPath(id?: string) {
    if (id) {
      return `/go/api/admin/repositories/${id}`;
    }
    return '/go/api/admin/repositories';
  }

  static packagePath(id?: string) {
    if (id) {
      return `/go/api/admin/packages/${id}`;
    }
    return '/go/api/admin/packages';
  }

  static packagesUsagePath(packageId: string): string {
    return `/go/api/admin/packages/${packageId}/usages`;
  }

  static pluggableScmPath(id?: string) {
    if (id) {
      return `/go/api/admin/scms/${id}`;
    }
    return '/go/api/admin/scms';
  }

  static pluggableScmCheckConnectionPath() {
    return '/go/api/admin/internal/scms/verify_connection';
  }

  static packageRepositoriesSPA() {
    return '/go/admin/package_repositories';
  }

  static adminInternalPackageRepositoriesVerifyConnectionPath(): string {
    return `/go/api/admin/internal/repositories/verify_connection`;
  }

  static adminInternalPackagesVerifyConnectionPath(): string {
    return `/go/api/admin/internal/packages/verify_connection`;
  }

  static pipelineStatusApiPath(pipelineName: string) {
    return `/go/api/pipelines/${pipelineName}/status`;
  }

  static scmUsagePath(scm_name: string): string {
    return `/go/api/admin/scms/${scm_name}/usages`;
  }

  static pluggableScmSPA() {
    return '/go/admin/scms';
  }

  static templateAuthorizationPath(id: string): string {
    return `/go/api/admin/templates/${id}/authorization`;

  }
}
