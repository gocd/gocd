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

import org.apache.commons.lang.text.StrSubstitutor;

import static com.google.common.collect.ImmutableMap.of;

public class Routes {

    public static class Backups {
        public static final String BASE = "/api/backups";
        public static final String DOC = "https://api.gocd.org/#backups";
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
    }

    public static class Roles {
        public static final String BASE = "/api/admin/security/roles";
        public static final String DOC = "https://api.gocd.org/#roles";
        public static final String NAME_PATH = "/:role_name";

        public static String find() {
            return BASE + NAME_PATH;
        }

        public static String name(String name) {
            return BASE + NAME_PATH.replaceAll(":role_name", name);
        }
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

        public static String settings(String pipelineName) {
            return "/admin/pipelines/:pipeline_name/general"
                    .replaceAll(":pipeline_name", pipelineName);
        }

    }

    public static class PipelineInstance {
        public static String compare(String pipelineName, int fromCounter, int toCounter) {
            return StrSubstitutor.replace("/compare/${pipeline_name}/${from_counter}/with/${to_counter}",
                    of("pipeline_name", pipelineName, "from_counter", fromCounter, "to_counter", toCounter));
        }

        public static String vsm(String pipelineName, int pipelineCounter) {
            return StrSubstitutor.replace("/pipelines/value_stream_map/${pipeline_name}/${pipeline_counter}",
                    of("pipeline_name", pipelineName, "pipeline_counter", pipelineCounter));
        }
    }

    public static class Stage {
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
        public static final String DOC = "https://api.gocd.org/#users";
        public static final String CURRENT_USER = "/api/current_user";
        public static final String BASE = "/api/users/";

        public static String self(String loginName) {
            return StrSubstitutor.replace(BASE + "${loginName}", of("loginName", loginName));
        }

        public static String find() {
            return BASE + ":login_name";
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
    }
}
