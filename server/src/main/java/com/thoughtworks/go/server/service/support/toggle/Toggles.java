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
package com.thoughtworks.go.server.service.support.toggle;

public class Toggles {
    public static String PIPELINE_COMMENT_FEATURE_TOGGLE_KEY = "pipeline_comment_feature_toggle_key";
    public static String PIPELINE_CONFIG_SINGLE_PAGE_APP = "pipeline_config_single_page_app_key";
    public static String QUICK_EDIT_PAGE_DEFAULT = "quick_edit_page_toggle_key";
    public static String BROWSER_CONSOLE_LOG_WS = "browser_console_log_ws_key";
    public static String AGENT_APIS_OVER_RAILS = "agent_apis_over_rails";
    public static String SERVER_DRAIN_MODE_API_TOGGLE_KEY = "server_drain_mode_api_toggle_key";
    public static String USE_OLD_ELASTIC_PROFILE_SPA = "use_old_elastic_profile_spa";
    public static String USE_NEW_AUTH_CONFIG_SPA = "use_new_auth_config_spa";

    private static FeatureToggleService service;

    public static void initializeWith(FeatureToggleService featureToggleService) {
        service = featureToggleService;
    }

    public static boolean isToggleOn(String key) {
        if (service == null) {
            throw new RuntimeException("Toggles not initialized with feature toggle service");
        }
        return service.isToggleOn(key);
    }
}
