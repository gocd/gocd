package com.thoughtworks.go.plugin.access.authentication;

import com.google.gson.Gson;
import com.thoughtworks.go.plugin.api.request.DefaultGoPluginApiRequest;
import com.thoughtworks.go.plugin.api.response.DefaultGoApiResponse;
import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse;
import com.thoughtworks.go.plugin.infra.PluginManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

import static java.lang.String.format;
import static java.util.Arrays.asList;

@Component
public class AuthenticationPluginRequestHelper {

    private static final Logger LOG = Logger.getLogger(AuthenticationPluginRequestHelper.class);
    private PluginManager pluginManager;

    private final static String REQUEST_NAME = "go.authentication.auth_enabled";
    private final static String EXTENSION_NAME = "authentication";
    private static final List<String> GO_SUPPORTED_VERSIONS = asList("1.0");

    @Autowired
    public AuthenticationPluginRequestHelper(PluginManager pluginManager) {
        this.pluginManager = pluginManager;
    }

    public boolean isEnabled(String pluginId) {
        if (!pluginManager.isPluginOfType(EXTENSION_NAME, pluginId)) {
            throw new RuntimeException(format("Did not find '%s' plugin with id '%s'. Looks like plugin is missing", EXTENSION_NAME, pluginId));
        }
        try {
            final String resolvedExtensionVersion = pluginManager.resolveExtensionVersion(pluginId, GO_SUPPORTED_VERSIONS);
            final DefaultGoPluginApiRequest apiRequest = new DefaultGoPluginApiRequest("authentication", resolvedExtensionVersion, REQUEST_NAME);
            apiRequest.setRequestBody("{}");
            final GoPluginApiResponse response = pluginManager.submitTo(pluginId, apiRequest);
            if (DefaultGoApiResponse.SUCCESS_RESPONSE_CODE == response.responseCode()) {
                final String responseBody = response.responseBody();
                final AuthEnabledResponse authEnabledResponse = new Gson().fromJson(responseBody, AuthEnabledResponse.class);
                return authEnabledResponse.enabled;
            } else if (404 == response.responseCode()) {
                LOG.warn(format("The authentication plugin %s responded with a 404. It does not support authentication enablement.", pluginId));
                return false;
            }
            throw new RuntimeException(format("The plugin sent a response that could not be understood by Go. Plugin returned with code '%s' and the following response: '%s'", response.responseCode(), response.responseBody()));
        } catch (Exception e) {
            throw new RuntimeException(format("Interaction with plugin with id '%s' implementing '%s' extension failed while requesting for '%s'. Reason: [%s]", pluginId, EXTENSION_NAME, REQUEST_NAME, e.getMessage()), e);
        }
    }


    public static class AuthEnabledResponse {
        public boolean enabled;
    }
}
