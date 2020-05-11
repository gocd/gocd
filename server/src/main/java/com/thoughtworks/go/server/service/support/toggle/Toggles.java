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
package com.thoughtworks.go.server.service.support.toggle;

public class Toggles {
    public static String BROWSER_CONSOLE_LOG_WS = "browser_console_log_ws_key";
    public static String ALLOW_EMPTY_PIPELINE_GROUPS_DASHBOARD = "allow_empty_pipeline_groups_dashboard";
    public static String TEST_DRIVE = "test_drive";
    public static String NEW_PIPELINE_DROPDOWN = "new_pipeline_dropdown";
    public static String FAST_PIPELINE_SAVE = "fast_pipeline_save";
    public static String SHOW_OLD_PKG_REPOS_SPA = "show_old_pkg_repos_spa";
    public static String SHOW_OLD_COMPARISON_SPA = "show_old_comparison_spa";
    public static String NEW_PIPELINE_CONFIG_SPA = "new_pipeline_config_spa";
    public static String USE_RAILS_TEMPLATE_AUTHORIZATION_API = "use_rails_template_authorization_api";
    public static String USE_RAILS_TEMPLATE_AUTHORIZATION_PAGE = "use_rails_template_authorization_page";

    private static FeatureToggleService service;

    public static void initializeWith(FeatureToggleService featureToggleService) {
        service = featureToggleService;
    }

    /**
     * Used by tests for teardown
     */
    public static void deinitialize() {
        service = null;
    }

    public static boolean isToggleOn(String key) {
        if (service == null) {
            throw new RuntimeException("Toggles not initialized with feature toggle service");
        }
        return service.isToggleOn(key);
    }

    public static boolean isToggleOff(String key) {
        return !isToggleOn(key);
    }
}
