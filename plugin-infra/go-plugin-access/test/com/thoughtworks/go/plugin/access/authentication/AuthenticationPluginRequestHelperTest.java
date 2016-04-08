package com.thoughtworks.go.plugin.access.authentication;

import com.google.gson.JsonObject;
import com.thoughtworks.go.plugin.api.request.GoPluginApiRequest;
import com.thoughtworks.go.plugin.api.response.DefaultGoPluginApiResponse;
import com.thoughtworks.go.plugin.infra.PluginManager;
import org.hamcrest.CustomMatcher;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AuthenticationPluginRequestHelperTest {

    private final PluginManager pluginManager = mock(PluginManager.class);
    private AuthenticationPluginRequestHelper authenticationPluginRequestHelper = new AuthenticationPluginRequestHelper(pluginManager);


    @Before
    public void setup() {
        when(pluginManager.isPluginOfType("authentication", "myEnabledPlugin")).thenReturn(true);
        when(pluginManager.isPluginOfType("authentication", "myDisabledPlugin")).thenReturn(true);
        when(pluginManager.isPluginOfType("authentication", "myLegacyPlugin")).thenReturn(true);

        when(pluginManager.resolveExtensionVersion("myEnabledPlugin", asList("1.0"))).thenReturn("1.0");
        when(pluginManager.resolveExtensionVersion("myDisabledPlugin", asList("1.0"))).thenReturn("1.0");
        when(pluginManager.resolveExtensionVersion("myLegacyPlugin", asList("1.0"))).thenReturn("1.0");

        when(pluginManager.submitTo(eq("myEnabledPlugin"), argThat(matchesGoApiRequest("authentication", "1.0", "go.authentication.auth_enabled"))))
                .thenReturn(enabledResponse());
        when(pluginManager.submitTo(eq("myDisabledPlugin"), argThat(matchesGoApiRequest("authentication", "1.0", "go.authentication.auth_enabled"))))
                .thenReturn(disabledResponse());

        when(pluginManager.submitTo(eq("myLegacyPlugin"), argThat(matchesGoApiRequest("authentication", "1.0", "go.authentication.auth_enabled"))))
                .thenReturn(new DefaultGoPluginApiResponse(404));
    }

    @Test
    public void helperShouldReturnTrueResponseForPluginRespondingEnabled() {
        assertThat(authenticationPluginRequestHelper.isEnabled("myEnabledPlugin"), is(true));
    }

    @Test
    public void helperShouldReturnFalseResponseForPluginRespondingDisabled() {
        assertThat(authenticationPluginRequestHelper.isEnabled("myDisabledPlugin"), is(false));
    }

    @Test
    public void helperShouldReturnFalseResponseForNonRespondingPlugin() {
        assertThat(authenticationPluginRequestHelper.isEnabled("myLegacyPlugin"), is(false));
    }

    private DefaultGoPluginApiResponse enabledResponse() {
        DefaultGoPluginApiResponse response = new DefaultGoPluginApiResponse(200);
        JsonObject responseBody = new JsonObject();
        responseBody.addProperty("enabled", true);
        response.setResponseBody(responseBody.toString());
        return response;
    }

    private DefaultGoPluginApiResponse disabledResponse() {
        DefaultGoPluginApiResponse response = new DefaultGoPluginApiResponse(200);
        JsonObject responseBody = new JsonObject();
        responseBody.addProperty("enabled", false);
        response.setResponseBody(responseBody.toString());
        return response;
    }

    private Matcher<? extends GoPluginApiRequest> matchesGoApiRequest(final String extension, final String version, final String requestName) {
        return new CustomMatcher<GoPluginApiRequest>(format("extension: %s, version: %s, message: %s", extension, version, requestName)) {
            @Override
            public boolean matches(final Object request) {
                if (request instanceof GoPluginApiRequest) {
                    GoPluginApiRequest actualRequest = (GoPluginApiRequest) request;
                    return actualRequest.extension().equals(extension) &&
                            actualRequest.extensionVersion().equals(version) &&
                            actualRequest.requestName().equals(requestName);

                }
                return false;
            }
        };
    }

}