/*
 * Copyright 2017 ThoughtWorks, Inc.
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

package com.thoughtworks.go.server.websocket;

import com.thoughtworks.go.domain.User;
import com.thoughtworks.go.server.security.GoAuthority;
import com.thoughtworks.go.server.service.RestfulService;
import com.thoughtworks.go.server.service.SecurityService;
import com.thoughtworks.go.server.service.UserService;
import com.thoughtworks.go.server.util.UserHelper;
import org.apache.commons.lang.reflect.FieldUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.eclipse.jetty.websocket.api.UpgradeResponse;
import org.springframework.security.Authentication;
import org.springframework.security.context.SecurityContext;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.http.HttpSession;
import javax.websocket.HandshakeResponse;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpointConfig;
import java.io.IOError;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import static org.springframework.security.context.HttpSessionContextIntegrationFilter.SPRING_SECURITY_CONTEXT_KEY;

public class ConsoleLogEndpointConfigurator extends ServerEndpointConfig.Configurator {

    @Override
    public void modifyHandshake(ServerEndpointConfig sec, HandshakeRequest request, HandshakeResponse response) {
        HttpSession session = (HttpSession) request.getHttpSession();
        WebApplicationContext wac = WebApplicationContextUtils.getWebApplicationContext(session.getServletContext());

        UserService userService = wac.getBean(UserService.class);
        SecurityContext sc = (SecurityContext) session.getAttribute(SPRING_SECURITY_CONTEXT_KEY);
        SecurityService securityService = wac.getBean(SecurityService.class);

        if (!isAuthenticated(sc) || !isAuthorizedForPipeline(pipelineName(request), sc.getAuthentication(), userService, securityService)) {
            sendForbidden(response);
            return;
        }

        Optional<NameValuePair> startLine = parseStartLine(request);

        sec.getUserProperties().put("startLine", startLine.isPresent() ? Long.valueOf(startLine.get().getValue()) : 0L);
        sec.getUserProperties().put("sender", wac.getBean(ConsoleLogSender.class));
        sec.getUserProperties().put("restfulService", wac.getBean(RestfulService.class));

        super.modifyHandshake(sec, request, response);
    }

    private Optional<NameValuePair> parseStartLine(HandshakeRequest request) {
        return URLEncodedUtils.parse(request.getRequestURI(), "UTF-8").stream().filter(new Predicate<NameValuePair>() {
            @Override
            public boolean test(NameValuePair pair) {
                return "startLine".equals(pair.getName());
            }
        }).findFirst();
    }

    boolean isAuthenticated(SecurityContext context) {
        return null != context && context.getAuthentication().isAuthenticated();
    }

    boolean isAuthorizedForPipeline(String pipelineName, Authentication authentication, UserService userService, SecurityService securityService) {
        if (null == pipelineName || pipelineName.isEmpty()) {
            return false;
        }

        boolean hasUserRole = Arrays.asList(authentication.getAuthorities()).contains(GoAuthority.ROLE_USER.asAuthority());
        if (!hasUserRole) {
            return false;
        }

        User user = getUser(userService, authentication);

        return user.isEnabled() && securityService.hasViewPermissionForPipeline(user.getUsername(), pipelineName);

    }

    private User getUser(UserService userService, Authentication authentication) {
        return userService.findUserByName(UserHelper.getUserName(authentication).getUsername().toString());
    }

    private String pipelineName(HandshakeRequest request) {
        try {
            List<String> segments = Arrays.asList(request.getRequestURI().getPath().split("/"));
            return segments.get(segments.indexOf("client-websocket") + 1);
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
    }

    private void sendForbidden(HandshakeResponse response) {
        UpgradeResponse upgradeResponse = upgradeResponse(response);

        try {
            upgradeResponse.sendForbidden("Not Authenticated");
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

    private UpgradeResponse upgradeResponse(HandshakeResponse response) {
        UpgradeResponse upgradeResponse;

        try {
            upgradeResponse = (UpgradeResponse) FieldUtils.readField(response, "response", true);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
        return upgradeResponse;
    }
}
