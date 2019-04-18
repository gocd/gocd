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
package com.thoughtworks.go.server.service.support.toggle;

public class Toggles {
    public static String PIPELINE_COMMENT_FEATURE_TOGGLE_KEY = "pipeline_comment_feature_toggle_key";
    public static String BROWSER_CONSOLE_LOG_WS = "browser_console_log_ws_key";
    public static String USERS_PAGE_USING_RAILS = "users_page_using_rails";
    public static String BACKUP_PAGE_USING_RAILS = "backup_page_using_rails";
    public static String USE_OLD_ENVIRONMENTS_API = "use_old_environments_api";
    public static String ENABLE_ADMIN_ACCESS_TOKENS_SPA = "enable_admin_access_tokens_spa";
    public static String NEW_ADD_PIPELINE_FLOW = "new_add_pipeline_flow";

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
