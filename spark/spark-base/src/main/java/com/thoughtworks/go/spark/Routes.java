/*
 * Copyright Thoughtworks, Inc.
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

import org.apache.commons.text.StringSubstitutor;
import org.springframework.web.util.UriUtils;

import java.io.UnsupportedEncodingException;
import java.util.Map;

import static com.thoughtworks.go.CurrentGoCDVersion.apiDocsUrl;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

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
        public static final String CHECK_CONNECTION_PATH = REPO_PATH + "/material_test";

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
        public static final String DOC = "https://api.gocd.org/current/#dashboard";
    }

    public static class MaterialConfig {
        public static final String BASE = "/api/config/materials";
        public static final String DOC = apiDocsUrl("#materials");

        public static String vsm(String materialFingerprint, String revision) {
            return StringSubstitutor.replace("/materials/value_stream_map/${material_fingerprint}/${revision}", Map.of(
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
        public static final String DOC = "https://api.gocd.org/current/#pipeline-groups";
        public static final String SELF = "/api/config/pipeline_groups";
    }

    public static class PipelineGroupsAdmin {
        public static final String DOC = "https://api.gocd.org/current/#pipeline-group-config";
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
        public static final String USAGES = ID + "/usages";

        public static final String SPA_BASE = "/admin/scms";

        public static final String INTERNAL_BASE = "/api/admin/internal/scms";
        public static final String VERIFY_CONNECTION = "/verify_connection";

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
        public static final String DOC = "https://api.gocd.org/current/#environment-config";
        public static final String BASE = "/api/admin/environments";
        public static final String NAME = "/:name";

        public static String find() {
            return BASE + NAME;
        }

        public static String name(String name) {
            return BASE + NAME.replaceAll(":name", name);
        }
    }

    public static class Pipeline {
        public static final String BASE = "/api/pipelines";
        public static final String DOC = "https://api.gocd.org/current/#pipelines";
        public static final String DOC_TRIGGER_OPTIONS = "https://api.gocd.org/current/#pipeline-trigger-options";

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
    }

    public static class PipelineInstance {
        public static String vsm(String pipelineName, int pipelineCounter) {
            return StringSubstitutor.replace("/pipelines/value_stream_map/${pipeline_name}/${pipeline_counter}",
                    Map.of("pipeline_name", pipelineName, "pipeline_counter", pipelineCounter));
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
        public static final String INSTANCE_V2 = "/:pipeline_name/:pipeline_counter/:stage_name/:stage_counter";
        public static final String STAGE_HISTORY = "/:pipeline_name/:stage_name/history";

        public static String self(String pipelineName, String pipelineCounter, String stageName, String stageCounter) {
            return StringSubstitutor.replace("/api/stages/${pipeline_name}/${pipeline_counter}/${stage_name}/${stage_counter}", Map.of(
                    "pipeline_name", pipelineName,
                    "pipeline_counter", pipelineCounter,
                    "stage_name", stageName,
                    "stage_counter", stageCounter));
        }

        public static String stageDetailTab(String pipelineName,
                                            int pipelineCounter,
                                            String stageName,
                                            int stageCounter) {
            return StringSubstitutor.replace("/pipelines/${pipeline_name}/${pipeline_counter}/${stage_name}/${stage_counter}", Map.of(
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
            return StringSubstitutor.replace(BASE + "${loginName}", Map.of("loginName", loginName));
        }

        public static String find() {
            return BASE + ":login_name";
        }
    }

    public static class UserSearch {
        public static final String BASE = "/api/user_search";
        public static final String DOC = apiDocsUrl("#user-search");

        public static String self(String searchTerm) {
            return StringSubstitutor.replace(BASE + "?q=${searchTerm}", Map.of("searchTerm", encodeQueryParam(searchTerm)));
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
        public static final String AUTHORIZATION = "/:template_name/authorization";
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
        public static final String INTERNAL_BASE = "/api/admin/internal/secret_configs";
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
        public static final String KILL_RUNNING_TASKS = "/:uuid/kill_running_tasks";

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

    public static class ServerHealthMessages {
        public static final String BASE = "/api/server_health_messages";
    }

    public static class MaterialSearch {
        public static final String BASE = "/api/internal/material_search";
    }

    public static class DependencyMaterialAutocomplete {
        public static final String BASE = "/api/internal/dependency_material/autocomplete_suggestions";
    }

    public static class PipelineSelection {
        public static final String BASE = "/api/internal/pipeline_selection";
        public static final String PIPELINES_DATA = "/pipelines_data";
    }

    public static class BuildCause {
        public static final String BASE = "/api/internal/build_cause";
        public static final String PATH = "/:pipeline_name/:pipeline_counter";
    }

    public static class AgentsSPA {
        public static final String BASE = "/agents";
        public static final String AGENT_UUID = "/:uuid";
    }

    public static class AnalyticsSPA {
        public static final String BASE = "/analytics";
        public static final String SHOW_PATH = ":plugin_id/:type/:id";
    }

    public static class ElasticAgentConfigSPA {
        public static final String BASE = "/admin/elastic_agent_configurations";
    }

    public static class NewDashboardSPA {
        public static final String BASE = "/dashboard";
    }

    public static class PluginsSPA {
        public static final String BASE = "/admin/plugins";
    }

    public static class BackupsSPA {
        public static final String BASE = "/admin/backup";
    }

    public static class ServerHealth {
        public static final String BASE = "/api/v1/health";
    }

    public static class KitchenSink {
        public static final String SPA_BASE = "/kitchen-sink";
    }

    public static class AuthConfigs {
        public static final String SPA_BASE = "/admin/security/auth_configs";
    }

    public static class ArtifactStores {
        public static final String SPA_BASE = "/admin/artifact_stores";
    }

    public static class AccessTokens {
        public static final String SPA_BASE = "/access_tokens";
    }

    public static class AdminAccessTokens {
        public static final String SPA_BASE = "/admin/admin_access_tokens";
    }

    public static class CCTray {
        public static final String BASE = "/cctray.xml";
    }

    public static class LoginPage {
        public static final String SPA_BASE = "/auth/login";
    }

    public static class LogoutPage {
        public static final String SPA_BASE = "/auth/logout";
    }

    public static class Support {
        public static final String BASE = "/api/support";
        public static final String PROCESS_LIST = "/process_list";
    }

    public static class ServerInfo {
        public static final String SPA_BASE = "/about";
    }

    public static class BackupConfig {
        public static final String BASE = "/api/config/backup";
        public static final String DOC = apiDocsUrl("#backup-config");
    }

    public static class InternalPipelineStructure {
        public static final String BASE = "/api/internal/pipeline_structure";
    }

    public static class InternalPipelineGroups {
        public static final String BASE = "/api/internal/pipeline_groups";
    }

    public static class NewEnvironments {
        public static final String SPA_BASE = "/admin/new-environments";
    }

    public static class FeatureToggle {
        public static final String FEATURE_TOGGLE_KEY = "/:toggle_key";
        public static final String BASE = "/api/admin/feature_toggles";
    }

    public static class ServerSiteUrlsConfig {
        public static final String BASE = "/api/admin/config/server/site_urls";
    }

    public static class DefaultJobTimeout {
        public static final String BASE = "/api/admin/config/server/default_job_timeout";
    }

    public static class ArtifactConfig {
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
        public static final String INTERNAL_BASE = "/api/admin/internal/packages";
        public static final String DOC = apiDocsUrl("packages");
        public static final String PACKAGE_ID = "/:package_id";
        public static final String FIND = BASE + PACKAGE_ID;
        public static final String USAGES = PACKAGE_ID + "/usages";
        public static final String VERIFY_CONNECTION = "/verify_connection";

        public static String self(String id) {
            return BASE + "/" + id;
        }
    }

    public static class PackageRepository {
        public static final String SPA_BASE = "/admin/package_repositories";
        public static final String BASE = "/api/admin/repositories";
        public static final String INTERNAL_BASE = "/api/admin/internal/repositories";
        public static final String VERIFY_CONNECTION = "/verify_connection";
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

    public static class ServerConfiguration {
        public static final String SPA_BASE = "/admin/server_configuration";
    }

    public static class AdminTemplates {
        public static final String SPA_BASE = "/admin/templates";
        public static final String NAME = "/:template_name";
    }

    public static class AgentJobHistory {
        public static final String DOC = apiDocsUrl("#agent-job-run-history");
        public static final String JOB_RUN_HISTORY = "/job_run_history";
        public static final String BASE = AgentsAPI.BASE + AgentsAPI.UUID + JOB_RUN_HISTORY;

        public static String forAgent(String uuid) {
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

    public static class InternalMaterialTest {
        public static final String BASE = "/api/admin/internal/material_test";
    }

    public static class StatusReports {
        public static final String SPA_BASE = "/admin/status_reports";
    }

    public static class ApiInfo {
        public static final String BASE = "/api/internal/apis";
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
        public static final  String BASE = "/api/webhooks";

        public static class Notify {
            public static final String GITHUB = "/github/notify";
            public static final String GITLAB = "/gitlab/notify";
            public static final String BITBUCKET = "/bitbucket/notify";
            public static final String HOSTED_BITBUCKET = "/hosted_bitbucket/notify";
        }

        public static class ConfigRepo {
            public static final String GITHUB = "/github/config_repos/:id";
            public static final String GITLAB = "/gitlab/config_repos/:id";
            public static final String BITBUCKET = "/bitbucket/config_repos/:id";
            public static final String HOSTED_BITBUCKET = "/hosted_bitbucket/config_repos/:id";
        }
    }

    public static class Compare {
        public static final String SPA_BASE = "/compare";
        public static final String COMPARE = "/:pipeline_name/:from_counter/with/:to_counter";

        public static String compare(String pipelineName, String fromCounter, String toCounter) {
            return StringSubstitutor.replace("/go/compare/${pipeline_name}/${from_counter}/with/${to_counter}", Map.of(
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

    public static class InternalDependencyPipelines {
        public static final String BASE = "/api/internal/pipelines/:pipeline_name/:stage_name/upstream";
    }

    public static class VersionInfos {
        public static final String BASE = "/api/version_infos";
        public static final String STALE = "/stale";
        public static final String LATEST_VERSION = "/latest_version";
        public static final String GO_SERVER = "/go_server";
    }

    public static class MaterialsSPA {
        public static final String BASE = "/materials";
    }

    public static class InternalMaterialConfig {
        public static final String INTERNAL_BASE = "/api/internal/materials";
        public static final String USAGES = "/:fingerprint/usages";
        public static final String TRIGGER_UPDATE = "/:fingerprint/trigger_update";

        public static final String INTERNAL_BASE_MODIFICATIONS = "/api/internal/materials/:fingerprint/modifications";

        public static String usages(String fingerprint) {
            return (MaterialConfig.BASE + USAGES).replaceAll(":fingerprint", fingerprint);
        }

        public static String previous(String pipelineName, long before, String pattern, Integer pageSize) {
            String link = INTERNAL_BASE_MODIFICATIONS.replaceAll(":fingerprint", pipelineName) + "?before=" + before;
            link = appendQueryString(link, pattern, pageSize);
            return link;
        }

        public static String next(String pipelineName, long after, String pattern, Integer pageSize) {
            String link = INTERNAL_BASE_MODIFICATIONS.replaceAll(":fingerprint", pipelineName) + "?after=" + after;
            link = appendQueryString(link, pattern, pageSize);
            return link;
        }

        private static String appendQueryString(String link, String pattern, Integer pageSize) {
            if (pageSize > 10) {
                link += "&page_size=" + pageSize;
            }
            if (isNotBlank(pattern)) {
                link += "&pattern=" + encodeQueryParam(pattern);
            }
            return link;
        }
    }

    public static class Preferences {
        public static final String SPA_BASE = "/preferences/notifications";
    }

    public static class InternalAgent {
        public static final String BASE = "/remoting/api/agent";
        public static final String PING = "/ping";
        public static final String REPORT_CURRENT_STATUS = "/report_current_status";
        public static final String REPORT_COMPLETING = "/report_completing";
        public static final String REPORT_COMPLETED = "/report_completed";
        public static final String IS_IGNORED = "/is_ignored";
        public static final String GET_COOKIE = "/get_cookie";
        public static final String GET_WORK = "/get_work";
    }

    private static String encodeQueryParam(String value) {
        try {
            return UriUtils.encodeQueryParam(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
}
