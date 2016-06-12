package com.thoughtworks.go.server.security;

import com.thoughtworks.go.plugin.access.authentication.AuthenticationPluginRegistry;
import com.thoughtworks.go.plugin.access.authentication.AuthenticationPluginRequestHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationService {

    private AuthenticationPluginRegistry authenticationPluginRegistry;
    private final AuthenticationPluginRequestHelper authenticationPluginRequestHelper;

    @Autowired
    public AuthenticationService(
            AuthenticationPluginRegistry authenticationPluginRegistry,
            AuthenticationPluginRequestHelper authenticationPluginRequestHelper
    ) {
        this.authenticationPluginRegistry = authenticationPluginRegistry;
        this.authenticationPluginRequestHelper = authenticationPluginRequestHelper;
    }

    public boolean isAuthEnabled() {
        for (final String authPlugin: authenticationPluginRegistry.getAuthenticationPlugins()) {
            if (authenticationPluginRequestHelper.isEnabled(authPlugin)) {
                return true;
            }
        }
        return false;
    }
}
