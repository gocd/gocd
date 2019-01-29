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

package com.thoughtworks.go.spark;

import com.google.common.net.UrlEscapers;
import org.apache.commons.lang3.text.StrSubstitutor;

import static com.google.common.collect.ImmutableMap.of;
import static com.thoughtworks.go.CurrentGoCDVersion.apiDocsUrl;

public class Routes {

    public static class Backups {
        public static final String BASE = "/api/backups";
        public static final String DOC = apiDocsUrl("#backups");
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

        public static final String STATUS_PATH = "/:id/status";
        public static final String TRIGGER_UPDATE_PATH = "/:id/trigger_update";

        public static final String BASE = "/api/admin/config_repos";
        public static final String DOC = apiDocsUrl("#config-repos");

        public static final String INDEX_PATH = "";
        public static final String REPO_PATH = "/:id";
        public static final String CREATE_PATH = INDEX_PATH;
        public static final String UPDATE_PATH = REPO_PATH;
        public static final String DELETE_PATH = REPO_PATH;

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
        public static final String DOC = apiDocsUrl("#system_admins");

    }

    public static class Dashboard {
        public static final String SELF = "/api/dashboard";
        public static final String DOC = "https://api.go.cd/current/#dashboard";
    }

    public static class Materials {
        public static String vsm(String materialFingerprint, String revision) {
            return StrSubstitutor.replace("/materials/value_stream_map/${material_fingerprint}/${revision}", of(
                    "material_fingerprint", materialFingerprint,
                    "revision", revision));
        }
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

        public static final String SCHEDULE_PATH = "/:pipeline_name/schedule";
        public static final String HISTORY_PATH = "/:pipeline_name/history";
        public static final String INSTANCE_PATH = "/:pipeline_name/instance/:pipeline_counter";

        public static String history(String pipelineName) {
            return BASE + HISTORY_PATH.replaceAll(":pipeline_name", pipelineName);
        }

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

        public static String instance(String pipelineName, int pipelineCounter) {
            return BASE + INSTANCE_PATH
                    .replaceAll(":pipeline_name", pipelineName)
                    .replaceAll(":pipeline_counter", String.valueOf(pipelineCounter));
        }
    }

    public static class PipelineInstance {
        public static String vsm(String pipelineName, int pipelineCounter) {
            return StrSubstitutor.replace("/pipelines/value_stream_map/${pipeline_name}/${pipeline_counter}",
                    of("pipeline_name", pipelineName, "pipeline_counter", pipelineCounter));
        }
    }

    public static class Stage {
        public static final String BASE = "/api/stages";
        public static final String TRIGGER_STAGE_PATH = "/:pipeline_name/:pipeline_counter/:stage_name/run";
        public static final String TRIGGER_FAILED_JOBS_PATH = "/:pipeline_name/:pipeline_counter/:stage_name/:stage_counter/run-failed-jobs";
        public static final String TRIGGER_SELECTED_JOBS_PATH = "/:pipeline_name/:pipeline_counter/:stage_name/:stage_counter/run-selected-jobs";
        public static final String INSTANCE_BY_COUNTER = "/:pipeline_name/:stage_name/instance/:pipeline_counter/:stage_counter";

        public static String self(String pipelineName, String pipelineCounter, String stageName, String stageCounter) {
            return StrSubstitutor.replace("/api/stages/${pipeline_name}/${pipeline_counter}/${stage_name}/${stage_counter}", of(
                    "pipeline_name", pipelineName,
                    "pipeline_counter", pipelineCounter,
                    "stage_name", stageName,
                    "stage_counter", stageCounter));
        }

        public static String stageDetailTab(String pipelineName, int pipelineCounter, String stageName, int stageCounter) {
            return StrSubstitutor.replace("/pipelines/${pipeline_name}/${pipeline_counter}/${stage_name}/${stage_counter}", of(
                    "pipeline_name", pipelineName,
                    "pipeline_counter", pipelineCounter,
                    "stage_name", stageName,
                    "stage_counter", stageCounter));
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
        public static final String DOC = apiDocsUrl("#artifact_stores");

        public static String find() {
            return BASE + ID;
        }

        public static String id(String id) {
            return find().replaceAll(":id", id);
        }
    }

    public static class PipelineConfig {
        public static final String BASE = "/api/admin/pipelines";
        public static final String NAME = "/:pipeline_name";
        public static final String DOC = apiDocsUrl("#pipeline-config");

        public static String find() {
            return BASE + NAME;
        }

        public static String name(String name) {
            return find().replaceAll(":pipeline_name", name);
        }
    }

    public static class PipelineTemplateConfig {
        public static final String BASE = "/api/admin/templates";
        public static final String NAME = "/:template_name";
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

    public class ServerHealthMessages {
        public static final String BASE = "/api/server_health_messages";
    }

    public class MaterialSearch {
        public static final String BASE = "/api/internal/material_search";
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
    }

    public class AnalyticsSPA {
        public static final String BASE = "/analytics";
        public static final String SHOW_PATH = ":plugin_id/:type/:id";
    }

    public class ElasticProfilesSPA {
        public static final String BASE = "/admin/elastic_profiles";
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

    public class ServerHealth {
        public static final String BASE = "/api/v1/health";
    }

    public class KitchenSink {
        public static final String SPA_BASE = "/kitchen-sink";
    }

    public static class Version {
        public static final String BASE = "/api/version";
        public static final String DOC = apiDocsUrl("#version");
        public static final String COMMIT_URL = "https://github.com/gocd/gocd/commit/";
    }

    public class AuthConfigs {
        public static final String SPA_BASE = "/admin/security/auth_configs";
    }

    public static class Users {
        public static final String BASE = "/api/users";
        public static final String USER_NAME = "/:login_name";
        public static final String SPA_BASE = "/admin/users";
        public static final String DOC = apiDocsUrl("#users");
        public static final String USER_STATE = "/operations/state";
    }

    public class ArtifactStores {
        public static final String SPA_BASE = "/admin/artifact_stores";
    }
}
