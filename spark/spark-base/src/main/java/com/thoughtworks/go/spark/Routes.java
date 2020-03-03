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
package com.thoughtworks.go.spark;

import com.google.common.net.UrlEscapers;
import org.apache.commons.lang3.text.StrSubstitutor;

import static com.google.common.collect.ImmutableMap.of;
import static com.thoughtworks.go.CurrentGoCDVersion.apiDocsUrl;

public class Routes {

    public interface FindUrlBuilder<Identifier> {
        String find();

        String find(Identifier id);

        String doc();

        String base();
    }

    public static class InternalResources {
        public static final String BASE = "/api/admin/internal/resources";
    }

    public static class InternalEnvironments {
        public static final String BASE = "/api/admin/internal/environments";
        public static final String ENV_NAME = "/:env_name";
    }

    public static class InternalCommandSnippets {
        public static final String BASE = "/api/admin/internal/command_snippets";

        public static String self(String searchTerm) {
            return String.format("%s?prefix=%s", BASE, searchTerm);
        }
    }

    public static class Backups {
        public static final String BASE = "/api/backups";
        public static final String DOC = apiDocsUrl("#backups");
        public static final String ID_PATH = "/:id";

        public static String serverBackup(String id) {
            return BASE + ID_PATH.replaceAll(":id", id);
        }
    }

    public static class MaintenanceMode {
        public static final String BASE = "/api/admin/maintenance_mode";
        public static final String SPA_BASE = "/admin/maintenance_mode";
        public static final String ENABLE = "/enable";
        public static final String DISABLE = "/disable";
        public static final String INFO = "/info";
        public static final String INFO_DOC = apiDocsUrl("#maintenance-mode-info");
    }

    public static class CurrentUser {
        public static final String BASE = "/api/current_user";
    }

    public static class Encrypt {
        public static final String BASE = "/api/admin/encrypt";
    }

    public static class PluginImages {
        public static final String BASE = "/api/plugin_images";
        public static final String PLUGIN_ID_HASH_PATH = "/:plugin_id/:hash";

        public static String pluginImage(String pluginId, String hash) {
            return BASE + PLUGIN_ID_HASH_PATH.replaceAll(":plugin_id", pluginId).replaceAll(":hash", hash);
        }
    }

    public static class ConfigView {
        public static final String SELF = "/admin/config_xml";
    }

    public static class ConfigRepos {
        public static final String SPA_BASE = "/admin/config_repos";
        public static final String INTERNAL_BASE = "/api/internal/config_repos";
        public static final String OPERATIONS_BASE = "/api/admin/config_repo_ops";

        public static final String PREFLIGHT_PATH = "/preflight";

        public static final String BASE = "/api/admin/config_repos";
        public static final String DOC = apiDocsUrl("#config-repos");

        public static final String INDEX_PATH = "";
        public static final String REPO_PATH = "/:id";
        public static final String CREATE_PATH = INDEX_PATH;
        public static final String UPDATE_PATH = REPO_PATH;
        public static final String DELETE_PATH = REPO_PATH;
        public static final String DEFINITIONS_PATH = REPO_PATH + "/definitions";
        public static final String STATUS_PATH = REPO_PATH + "/status";
        public static final String TRIGGER_UPDATE_PATH = REPO_PATH + "/trigger_update";

        // For building _links entry in API response
        public static String find() {
            return BASE + REPO_PATH;
        }

        public static String id(String id) {
            return find().replaceAll(":id", id);
        }
    }

    public static class Export {
        public static final String BASE = "/api/admin/export";
        public static final String PIPELINES_PATH = "/pipelines/:pipeline_name";

        public static String pipeline(String name) {
            return (BASE + PIPELINES_PATH).replaceAll(":pipeline_name", name);
        }
    }

    public static class Roles {
        public static final String BASE = "/api/admin/security/roles";
        public static final String INTERNAL_BASE = "/api/admin/internal/roles";
        public static final String SPA_BASE = "/admin/security/roles";
        public static final String DOC = apiDocsUrl("#roles");
        public static final String NAME_PATH = "/:role_name";

        public static String find() {
            return BASE + NAME_PATH;
        }

        public static String name(String name) {
            return BASE + NAME_PATH.replaceAll(":role_name", name);
        }
    }

    public static class SystemAdmins {
        public static final String BASE = "/api/admin/security/system_admins";
        public static final String DOC = apiDocsUrl("#system-admins");

    }

    public static class Dashboard {
        public static final String SELF = "/api/dashboard";
        public static final String DOC = "https://api.go.cd/current/#dashboard";
    }

    public static class MaterialConfig {
        public static final String BASE = "/api/config/materials";
        public static final String DOC = apiDocsUrl("#materials");

        public static String vsm(String materialFingerprint, String revision) {
            return StrSubstitutor.replace("/materials/value_stream_map/${material_fingerprint}/${revision}", of(
                    "material_fingerprint", materialFingerprint,
                    "revision", revision));
        }
    }

    public static class MailServer {
        public static final String BASE = "/api/config/mailserver";
        public static final String TEST_EMAIL = "/test";
        public static final String DOC = apiDocsUrl("#mailserver-config");
    }

    public static class MaterialModifications {
        public static final String BASE = "/api/materials/:fingerprint/modifications";
        public static final String OFFSET = "/:offset";
        public static final String FIND = BASE + "/{offset}";

        public static String modification(String fingerprint) {
            return BASE.replaceAll(":fingerprint", fingerprint);
        }
    }

    public static class MaterialNotify {
        public static final String BASE = "/api/admin/materials";
        public static final String SVN = "/svn/notify";
        public static final String GIT = "/git/notify";
        public static final String HG = "/hg/notify";
        public static final String SCM = "/scm/notify";
    }

    public static class PipelineGroup {
        public static final String DOC = "https://api.go.cd/current/#pipeline-groups";
        public static final String SELF = "/api/config/pipeline_groups";
    }

    public static class PipelineGroupsAdmin {
        public static final String DOC = "https://api.go.cd/current/#pipeline-group-config";
        public static final String BASE = "/api/admin/pipeline_groups";

        public static final String NAME_PATH = "/:group_name";

        public static String find() {
            return BASE + NAME_PATH;
        }

        public static String name(String name) {
            return BASE + NAME_PATH.replaceAll(":group_name", name);
        }
    }

    public static class SCM {
        public static final String DOC = apiDocsUrl("#scms");
        public static final String BASE = "/api/admin/scms";
        public static final String ID = "/:material_name";

        public static String find() {
            return BASE + ID;
        }

        public static String name(String name) {
            return find().replaceAll(":material_name", name);
        }
    }

    public static class EnvironmentConfig {
        public static final String DOC = apiDocsUrl("#environment-config");
        static final String NAME = "/api/admin/environments/:name";

        public static String name(String name) {
            return NAME.replaceAll(":name", name);
        }
    }

    public static class Environments {
        public static final String DOC = "https://api.go.cd/current/#environment-config";
        public static final String BASE = "/api/admin/environments";
        public static final String NAME = "/:name";

        public static String find() {
            return BASE + NAME;
        }

        public static String name(String name) {
            return BASE + NAME.replaceAll(":name", name);
        }
    }

    public static class DataSharing {
        public static final String USAGE_DATA_PATH = "/api/internal/data_sharing/usagedata";
        public static final String SETTINGS_PATH = "/api/data_sharing/settings";
        public static final String REPORTING_PATH = "/api/internal/data_sharing/reporting";

        public static final String SETTINGS_DOC = "https://api.go.cd/current/#data_sharing_settings";
    }

    public static class Pipeline {
        public static final String BASE = "/api/pipelines";
        public static final String DOC = "https://api.go.cd/current/#pipelines";
        public static final String DOC_TRIGGER_OPTIONS = "https://api.go.cd/current/#pipeline-trigger-options";

        public static final String PAUSE_PATH = "/:pipeline_name/pause";
        public static final String UNPAUSE_PATH = "/:pipeline_name/unpause";
        public static final String UNLOCK_PATH = "/:pipeline_name/unlock";
        public static final String TRIGGER_OPTIONS_PATH = "/:pipeline_name/trigger_options";

        public static final String STATUS_PATH = "/:pipeline_name/status";
        public static final String SCHEDULE_PATH = "/:pipeline_name/schedule";

        public static String triggerOptions(String pipelineName) {
            return BASE + TRIGGER_OPTIONS_PATH.replaceAll(":pipeline_name", pipelineName);
        }

        public static String schedule(String pipelineName) {
            return BASE + SCHEDULE_PATH.replaceAll(":pipeline_name", pipelineName);
        }

        public static String pause(String pipelineName) {
            return BASE + PAUSE_PATH.replaceAll(":pipeline_name", pipelineName);
        }

        public static String unpause(String pipelineName) {
            return BASE + UNPAUSE_PATH.replaceAll(":pipeline_name", pipelineName);
        }

        public static String unlock(String pipelineName) {
            return BASE + UNLOCK_PATH.replaceAll(":pipeline_name", pipelineName);
        }
    }

    public static class PipelineInstance {
        public static String vsm(String pipelineName, int pipelineCounter) {
            return StrSubstitutor.replace("/pipelines/value_stream_map/${pipeline_name}/${pipeline_counter}",
                    of("pipeline_name", pipelineName, "pipeline_counter", pipelineCounter));
        }

        public static final String BASE = "/api/pipelines";
        public static final String INSTANCE_PATH = "/:pipeline_name/:pipeline_counter/instance";
        public static final String HISTORY_PATH = "/:pipeline_name/history";
        public static final String COMMENT_PATH = "/:pipeline_name/:pipeline_counter/comment";


        public static String instance(String pipelineName, int pipelineCounter) {
            return BASE + INSTANCE_PATH
                    .replaceAll(":pipeline_name", pipelineName)
                    .replaceAll(":pipeline_counter", String.valueOf(pipelineCounter));
        }

        public static String history(String pipelineName) {
            return BASE + HISTORY_PATH.replaceAll(":pipeline_name", pipelineName);
        }

        public static String previous(String pipelineName, long before) {
            return BASE + HISTORY_PATH.replaceAll(":pipeline_name", pipelineName) + "?before=" + before;
        }

        public static String next(String pipelineName, long after) {
            return BASE + HISTORY_PATH.replaceAll(":pipeline_name", pipelineName) + "?after=" + after;
        }
    }

    public static class Stage {
        public static final String BASE = "/api/stages";
        public static final String TRIGGER_STAGE_PATH = "/:pipeline_name/:pipeline_counter/:stage_name/run";
        public static final String TRIGGER_FAILED_JOBS_PATH = "/:pipeline_name/:pipeline_counter/:stage_name/:stage_counter/run-failed-jobs";
        public static final String TRIGGER_SELECTED_JOBS_PATH = "/:pipeline_name/:pipeline_counter/:stage_name/:stage_counter/run-selected-jobs";
        public static final String CANCEL_STAGE_PATH = "/:pipeline_name/:pipeline_counter/:stage_name/:stage_counter/cancel";
        public static final String INSTANCE_BY_COUNTER = "/:pipeline_name/:stage_name/instance/:pipeline_counter/:stage_counter";
        public static final String INSTANCE_V2 = "/:pipeline_name/:pipeline_counter/:stage_name/:stage_counter";
        public static final String STAGE_HISTORY = "/:pipeline_name/:stage_name/history";
        public static final String STAGE_HISTORY_OFFSET = "/:pipeline_name/:stage_name/history/:offset";

        public static String self(String pipelineName, String pipelineCounter, String stageName, String stageCounter) {
            return StrSubstitutor.replace("/api/stages/${pipeline_name}/${pipeline_counter}/${stage_name}/${stage_counter}", of(
                    "pipeline_name", pipelineName,
                    "pipeline_counter", pipelineCounter,
                    "stage_name", stageName,
                    "stage_counter", stageCounter));
        }

        public static String stageDetailTab(String pipelineName,
                                            int pipelineCounter,
                                            String stageName,
                                            int stageCounter) {
            return StrSubstitutor.replace("/pipelines/${pipeline_name}/${pipeline_counter}/${stage_name}/${stage_counter}", of(
                    "pipeline_name", pipelineName,
                    "pipeline_counter", pipelineCounter,
                    "stage_name", stageName,
                    "stage_counter", stageCounter));
        }


        public static String previous(String pipelineName, String stageName, long before) {
            return BASE + STAGE_HISTORY.replaceAll(":pipeline_name", pipelineName).replaceAll(":stage_name", stageName) + "?before=" + before;
        }

        public static String next(String pipelineName, String stageName, long after) {
            return BASE + STAGE_HISTORY.replaceAll(":pipeline_name", pipelineName).replaceAll(":stage_name", stageName) + "?after=" + after;
        }
    }

    public static class Job {
        public static final String BASE = "/api/jobs";
        public static final String JOB_HISTORY = "/:pipeline_name/:stage_name/:job_name/history";
        public static final String JOB_INSTANCE = "/:pipeline_name/:pipeline_counter/:stage_name/:stage_counter/:job_name";

        public static String previous(String pipelineName, String stageName, String jobConfigName, long before) {
            return BASE
                    + JOB_HISTORY.replaceAll(":pipeline_name", pipelineName)
                    .replaceAll(":stage_name", stageName)
                    .replaceAll(":job_name", jobConfigName)
                    + "?before=" + before;
        }

        public static String next(String pipelineName, String stageName, String jobConfigName, long after) {
            return BASE
                    + JOB_HISTORY.replaceAll(":pipeline_name", pipelineName)
                    .replaceAll(":stage_name", stageName)
                    .replaceAll(":job_name", jobConfigName)
                    + "?after=" + after;
        }
    }

    public static class UserSummary {
        public static final String DOC = apiDocsUrl("#users");
        public static final String CURRENT_USER = "/api/current_user";
        public static final String BASE = "/api/users/";

        public static String self(String loginName) {
            return StrSubstitutor.replace(BASE + "${loginName}", of("loginName", loginName));
        }

        public static String find() {
            return BASE + ":login_name";
        }
    }

    public static class UserSearch {
        public static final String BASE = "/api/user_search";
        public static final String DOC = apiDocsUrl("#user-search");

        public static String self(String searchTerm) {
            return StrSubstitutor.replace(BASE + "?q=${searchTerm}", of("searchTerm", UrlEscapers.urlFormParameterEscaper().escape(searchTerm)));
        }

        public static String find() {
            return BASE + "?q=:search_term";
        }
    }

    public static class ArtifactStoreConfig {
        public static final String BASE = "/api/admin/artifact_stores";
        public static final String ID = "/:id";
        public static final String DOC = apiDocsUrl("#artifact-store");

        public static String find() {
            return BASE + ID;
        }

        public static String id(String id) {
            return find().replaceAll(":id", id);
        }
    }

    public static class PipelineConfig {
        public static final String SPA_CREATE = "/create";
        public static final String SPA_AS_CODE = "/as-code";
        public static final String SPA_BASE = "/admin/pipelines";
        public static final String BASE = "/api/admin/pipelines";
        public static final String NAME = "/:pipeline_name";
        public static final String EXTRACT_TO_TEMPLATE = "/:pipeline_name/extract_to_template";
        public static final String DOC = apiDocsUrl("#pipeline-config");

        public static String find() {
            return BASE + NAME;
        }

        public static String name(String name) {
            return find().replaceAll(":pipeline_name", name);
        }
    }

    public static class PaC {
        public static final String BASE_INTERNAL_API = "/api/admin/internal/pac";
        public static final String PREVIEW = "/preview/:plugin_id";
        public static final String CONFIG_FILES = "/config_files/:plugin_id";
    }

    public static class PipelineTemplateConfig {
        public static final String BASE = "/api/admin/templates";
        public static final String NAME = "/:template_name";
        public static final String PARAMETERS = "/:template_name/parameters";
        public static final String DOC = apiDocsUrl("#template-config");

        public static String find() {
            return BASE + NAME;
        }

        public static String name(String name) {
            return find().replaceAll(":template_name", name);
        }
    }

    public static class ElasticProfileAPI {
        public static final String BASE = "/api/elastic/profiles";
        public static final String INTERNAL_BASE = "/api/internal/elastic/profiles";
        public static final String ID = "/:profile_id";
        public static final String DOC = apiDocsUrl("#elastic-agent-profiles");
        public static final String USAGES = "/usages";

        public static String find() {
            return BASE + ID;
        }

        public static String id(String id) {
            return find().replaceAll(":profile_id", id);
        }
    }

    public static class SecretConfigsAPI {
        public static final String BASE = "/api/admin/secret_configs";
        public static final String ID = "/:config_id";
        public static final String DOC = apiDocsUrl("#secret-configs");

        public static String find() {
            return BASE + ID;
        }

        public static String id(String id) {
            return find().replaceAll(":config_id", id);
        }
    }

    public static class SecretConfigs {
        public static final String SPA_BASE = "/admin/secret_configs";
    }

    public static class ClusterProfilesAPI {
        public static final String BASE = "/api/admin/elastic/cluster_profiles";
        public static final String ID = "/:cluster_id";
        public static final String DOC = apiDocsUrl("#cluster-profiles");

        public static String find() {
            return BASE + ID;
        }

        public static String id(String id) {
            return find().replaceAll(":cluster_id", id);
        }
    }

    public static class PluginSettingsAPI {
        public static final String BASE = "/api/admin/plugin_settings";
        public static final String ID = "/:plugin_id";
        public static final String DOC = apiDocsUrl("#plugin-settings");

        public static String find() {
            return BASE + ID;
        }

        public static String id(String id) {
            return find().replaceAll(":plugin_id", id);
        }
    }

    public static class SecurityAuthConfigAPI {
        public static final String BASE = "/api/admin/security/auth_configs";
        public static final String ID = "/:id";
        public static final String DOC = apiDocsUrl("#auth_configs");
        public static final String VERIFY_CONNECTION = "/verify_connection";
        public static final String INTERNAL_BASE = "/api/admin/internal/security/auth_configs";

        public static String find() {
            return BASE + ID;
        }

        public static String id(String id) {
            return find().replaceAll(":id", id);
        }
    }

    public static class PluginInfoAPI {
        public static final String BASE = "/api/admin/plugin_info";
        public static final String ID = "/:id";
        public static final String DOC = apiDocsUrl("#plugin-info");

        public static String find() {
            return BASE + ID;
        }

        public static String id(String id) {
            return find().replaceAll(":id", id);
        }
    }

    public static class AgentsAPI {
        public static final String BASE = "/api/agents";
        public static final String UUID = "/:uuid";
        public static final String DOC = apiDocsUrl("#agents");

        public static String find() {
            return BASE + UUID;
        }

        public static String uuid(String uuid) {
            return find().replaceAll(":uuid", uuid);
        }
    }

    public static class Version {
        public static final String BASE = "/api/version";
        public static final String DOC = apiDocsUrl("#version");
        public static final String COMMIT_URL = "https://github.com/gocd/gocd/commit/";
    }

    public static class Users {
        public static final String BASE = "/api/users";
        public static final String USER_NAME = "/:login_name";
        public static final String SPA_BASE = "/admin/users";
        public static final String DOC = apiDocsUrl("#users");
        public static final String USER_STATE = "/operations/state";
    }

    public static class CurrentUserAccessToken implements FindUrlBuilder<Long> {
        public static final String BASE = "/api/current_user/access_tokens";
        public static final String ID = "/:id";
        public static final String REVOKE = ID + "/revoke";
        private static final String DOC = apiDocsUrl("#access-tokens");

        @Override
        public String find() {
            return BASE + ID;
        }

        @Override
        public String find(Long id) {
            return find().replaceAll(":id", String.valueOf(id));
        }

        @Override
        public String doc() {
            return DOC;
        }

        @Override
        public String base() {
            return BASE;
        }
    }

    public static class AdminUserAccessToken implements FindUrlBuilder<Long> {
        public static final String BASE = "/api/admin/access_tokens";
        public static final String ID = "/:id";
        public static final String REVOKE = ID + "/revoke";
        private static final String DOC = apiDocsUrl("#access-tokens");

        @Override
        public String find() {
            return BASE + ID;
        }

        @Override
        public String find(Long id) {
            return find().replaceAll(":id", String.valueOf(id));
        }

        @Override
        public String doc() {
            return DOC;
        }

        @Override
        public String base() {
            return BASE;
        }
    }

    public class ServerHealthMessages {
        public static final String BASE = "/api/server_health_messages";
    }

    public class MaterialSearch {
        public static final String BASE = "/api/internal/material_search";
    }

    public class DependencyMaterialAutocomplete {
        public static final String BASE = "/api/internal/dependency_material/autocomplete_suggestions";
    }

    public class RolesSPA {
        public static final String BASE = "/admin/security/roles";
    }

    public class PipelineSelection {
        public static final String BASE = "/api/internal/pipeline_selection";
        public static final String PIPELINES_DATA = "/pipelines_data";
    }

    public class BuildCause {
        public static final String BASE = "/api/internal/build_cause";
        public static final String PATH = "/:pipeline_name/:pipeline_counter";
    }

    public class AgentsSPA {
        public static final String BASE = "/agents";
        public static final String AGENT_UUID = "/:uuid";
    }

    public class AnalyticsSPA {
        public static final String BASE = "/analytics";
        public static final String SHOW_PATH = ":plugin_id/:type/:id";
    }

    public class ElasticAgentConfigSPA {
        public static final String BASE = "/admin/elastic_agent_configurations";
    }

    public class NewDashboardSPA {
        public static final String BASE = "/dashboard";
    }

    public class PluginsSPA {
        public static final String BASE = "/admin/plugins";
    }

    public class DataSharingSettingsSPA {
        public static final String BASE = "/admin/data_sharing/settings";
    }

    public class BackupsSPA {
        public static final String BASE = "/admin/backup";
    }

    public class ServerHealth {
        public static final String BASE = "/api/v1/health";
    }

    public class KitchenSink {
        public static final String SPA_BASE = "/kitchen-sink";
    }

    public class AuthConfigs {
        public static final String SPA_BASE = "/admin/security/auth_configs";
    }

    public class ArtifactStores {
        public static final String SPA_BASE = "/admin/artifact_stores";
    }

    public class AccessTokens {
        public static final String SPA_BASE = "/access_tokens";
    }

    public class AdminAccessTokens {
        public static final String SPA_BASE = "/admin/admin_access_tokens";
    }

    public class CCTray {
        public static final String BASE = "/cctray.xml";
    }

    public class LoginPage {
        public static final String SPA_BASE = "/auth/login";
    }

    public class LogoutPage {
        public static final String SPA_BASE = "/auth/logout";
    }

    public class Support {
        public static final String BASE = "/api/support";
        public static final String PROCESS_LIST = "/process_list";
    }

    public class ClusterProfiles {
        public static final String SPA_BASE = "/admin/cluster_profiles";
    }

    public class ServerInfo {
        public static final String SPA_BASE = "/about";
    }

    public static class BackupConfig {
        public static final String BASE = "/api/config/backup";
        public static final String DOC = apiDocsUrl("#backup-config");
    }

    public class InternalPipelineStructure {
        public static final String BASE = "/api/internal/pipeline_structure";
    }

    public class NewEnvironments {
        public static final String SPA_BASE = "/admin/new-environments";
    }

    public class FeatureToggle {
        public static final String FEATURE_TOGGLE_KEY = "/:toggle_key";
        public static final String BASE = "/api/admin/feature_toggles";
    }

    public class ServerSiteUrlsConfig {
        public static final String BASE = "/api/admin/config/server/site_urls";
    }

    public class DefaultJobTimeout {
        public static final String BASE = "/api/admin/config/server/default_job_timeout";
    }

    public class ArtifactConfig {
        public static final String BASE = "/api/admin/config/server/artifact_config";
    }

    public static class CompareAPI {
        public static final String BASE = "/api/pipelines/:pipeline_name/compare/:from_counter/:to_counter";
        public static final String DOC = apiDocsUrl("#compare");

        public static final String INTERNAL_BASE = "/api/internal/compare";
        public static final String INTERNAL_LIST = "/:pipeline_name/list";
    }

    public static class Packages {
        public static final String BASE = "/api/admin/packages";
        public static final String DOC = apiDocsUrl("packages");
        public static final String PACKAGE_ID = "/:package_id";
        public static final String FIND = BASE + PACKAGE_ID;

        public static String self(String id) {
            return BASE + "/" + id;
        }
    }

    public static class PackageRepository {
        public static final String BASE = "/api/admin/repositories";
        public static final String DOC = apiDocsUrl("package-repositories");
        public static final String REPO_ID = "/:repo_id";
        public static final String FIND = BASE + REPO_ID;

        public static String self(String id) {
            return BASE + "/" + id;
        }
    }

    public static class AdminPipelines {
        public static final String SPA_BASE = "/admin/pipelines";
    }

    public class ServerConfiguration {
        public static final String SPA_BASE = "/admin/server_configuration";
    }

    public class AdminTemplates {
        public static final String SPA_BASE = "/admin/templates";
    }

    public static class AgentJobHistory {
        public static final String DOC = apiDocsUrl("#agent-job-run-history");
        public static final String JOB_RUN_HISTORY = "/job_run_history";
        public static final String BASE = AgentsAPI.BASE + AgentsAPI.UUID + JOB_RUN_HISTORY;

        public static final String forAgent(String uuid) {
            return BASE.replace(":uuid", uuid);
        }
    }

    public static class PipelineActivity {
        public static final String SPA_BASE = "/pipeline/activity/:pipeline_name";
    }

    public static class NotificationFilterAPI {
        public static final String API_BASE = "/api/notification_filters";
        public static final String ID = "/:id";
        public static final String DOC = apiDocsUrl("notification-filters");
        public static final String FIND = API_BASE + ID;

        public static String self(long id) {
            return API_BASE + "/" + id;
        }
    }

    public class InternalMaterialTest {
        public static final String BASE = "/api/admin/internal/material_test";
    }

    public class StatusReports {
        public static final String SPA_BASE = "/admin/status_reports";
    }

    public static class ApiInfo {
        public static final String BASE = "/api/apis";
    }

    public static class FeedsAPI {
        public static final String BASE = "/api/feed";
        public static final String PIPELINES_XML = "/pipelines.xml";
        public static final String PIPELINE_XML = "/pipelines/:pipeline_name/:pipeline_counter";
        public static final String STAGE_XML = "/pipelines/:pipeline_name/:pipeline_counter/:stage_name/:stage_counter";
        public static final String JOB_XML = "/pipelines/:pipeline_name/:pipeline_counter/:stage_name/:stage_counter/:job_name";
        public static final String STAGES_XML = "/pipelines/:pipeline_name/stages.xml";
        public static final String SCHEDULED_JOB_XML = "/jobs/scheduled.xml";
        public static final String MATERIAL_URL = "/materials/:pipeline_name/:pipeline_counter/:fingerprint";
    }

    public static class Webhook {
        public static String BASE = "/api/webhooks";

        public static class Push {
            public static final String GITHUB = "/github/notify";
            public static final String GITLAB = "/gitlab/notify";
            public static final String BIT_BUCKET_CLOUD = "/bitbucket/notify";
            public static final String BIT_BUCKET_SERVER = "/hosted_bitbucket/notify";
        }
    }

    public static class Compare {
        public static final String SPA_BASE = "/compare";
        public static final String COMPARE = "/:pipeline_name/:from_counter/with/:to_counter";

        public static String compare(String pipelineName, String fromCounter, String toCounter) {
            return StrSubstitutor.replace("/go/compare/${pipeline_name}/${from_counter}/with/${to_counter}", of(
                    "pipeline_name", pipelineName,
                    "from_counter", fromCounter,
                    "to_counter", toCounter
            ));
        }
    }

    public static class Permissions {
        public static final String BASE = "/api/auth/permissions";
        public static final String DOC = apiDocsUrl("permissions");
    }

    public class InternalDependencyPipelines {
        public static final String BASE = "/api/internal/pipelines/:pipeline_name/:stage_name/upstream";
    }
}
