package com.thoughtworks.go.server.service.support.toggle;

public class Toggles {
    public static String API_REQUESTS_SHORT_SESSION_FEATURE_TOGGLE_KEY = "api_requests_short_session_feature_toggle_key";
    public static String PIPELINE_COMMENT_FEATURE_TOGGLE_KEY = "pipeline_comment_feature_toggle_key";
    public static String PLUGIN_UPLOAD_FEATURE_TOGGLE_KEY = "plugin_upload_feature_toggle_key";
    public static String COLOR_LOGS_FEATURE_TOGGLE_KEY = "color_logs_feature_toggle_key";
    public static String NEW_CCTRAY_FEATURE_TOGGLE_KEY = "new_cctray_feature_toggle_key";

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