/*
 * Copyright 2016 ThoughtWorks, Inc.
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

package com.thoughtworks.go.server.service.plugins.processor.authentication;

import com.thoughtworks.go.plugin.access.authentication.JsonMessageHandler;
import com.thoughtworks.go.plugin.access.authentication.JsonMessageHandler1_0;
import com.thoughtworks.go.plugin.access.authentication.model.User;
import com.thoughtworks.go.plugin.api.request.GoApiRequest;
import com.thoughtworks.go.plugin.api.response.DefaultGoApiResponse;
import com.thoughtworks.go.plugin.api.response.GoApiResponse;
import com.thoughtworks.go.plugin.infra.PluginRequestProcessorRegistry;
import com.thoughtworks.go.plugin.infra.GoPluginApiRequestProcessor;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.server.security.AuthorityGranter;
import com.thoughtworks.go.server.security.userdetail.GoUserPrinciple;
import com.thoughtworks.go.server.service.UserService;
import com.thoughtworks.go.server.util.UserHelper;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.Authentication;
import org.springframework.security.context.SecurityContext;
import org.springframework.security.context.SecurityContextHolder;
import org.springframework.security.providers.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;

@Component
public class AuthenticationRequestProcessor implements GoPluginApiRequestProcessor {
    private static final Logger LOGGER = Logger.getLogger(AuthenticationRequestProcessor.class);

    public static final String AUTHENTICATE_USER_REQUEST = "go.processor.authentication.authenticate-user";
    private static final List<String> goSupportedVersions = asList("1.0");

    private AuthorityGranter authorityGranter;
    private UserService userService;
    private Map<String, JsonMessageHandler> messageHandlerMap = new HashMap<>();

    @Autowired
    public AuthenticationRequestProcessor(PluginRequestProcessorRegistry registry, AuthorityGranter authorityGranter, UserService userService) {
        this.authorityGranter = authorityGranter;
        this.userService = userService;
        this.messageHandlerMap.put("1.0", new JsonMessageHandler1_0());
        registry.registerProcessorFor(AUTHENTICATE_USER_REQUEST, this);
    }

    @Override
    public GoApiResponse process(GoPluginDescriptor pluginDescriptor, GoApiRequest goPluginApiRequest) {
        try {
            String version = goPluginApiRequest.apiVersion();
            if (!goSupportedVersions.contains(version)) {
                throw new RuntimeException(String.format("Unsupported '%s' API version: %s. Supported versions: %s", AUTHENTICATE_USER_REQUEST, version, goSupportedVersions));
            }

            User user = messageHandlerMap.get(version).responseMessageForAuthenticateUser(goPluginApiRequest.requestBody());
            if (user == null) {
                throw new RuntimeException(String.format("Could not parse User details. Request Body: %s", goPluginApiRequest.requestBody()));
            }

            GoUserPrinciple goUserPrincipal = getGoUserPrincipal(user);
            Authentication authentication = getAuthenticationToken(goUserPrincipal);

            userService.addUserIfDoesNotExist(UserHelper.getUserName(authentication));
            getSecurityContext().setAuthentication(authentication);
            return new DefaultGoApiResponse(200);
        } catch (Exception e) {
            LOGGER.error("Error occurred while authenticating user", e);
        }
        return new DefaultGoApiResponse(500);
    }

    GoUserPrinciple getGoUserPrincipal(User user) {
        return new GoUserPrinciple(user.getUsername(), user.getDisplayName(), "", true, true, true, true, authorityGranter.authorities(user.getUsername()));
    }

    PreAuthenticatedAuthenticationToken getAuthenticationToken(GoUserPrinciple goUserPrincipal) {
        return new PreAuthenticatedAuthenticationToken(goUserPrincipal, null, goUserPrincipal.getAuthorities());
    }

    SecurityContext getSecurityContext() {
        return SecurityContextHolder.getContext();
    }

    Map<String, JsonMessageHandler> getMessageHandlerMap() {
        return messageHandlerMap;
    }
}
