/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.server.service.plugins.processor;

import com.google.gson.Gson;
import com.thoughtworks.go.plugin.access.authentication.model.User;
import com.thoughtworks.go.plugin.api.request.GoApiRequest;
import com.thoughtworks.go.plugin.api.response.DefaultGoApiResponse;
import com.thoughtworks.go.plugin.api.response.GoApiResponse;
import com.thoughtworks.go.plugin.infra.DefaultGoApplicationAccessor;
import com.thoughtworks.go.plugin.infra.GoPluginApiRequestProcessor;
import com.thoughtworks.go.server.security.AuthorityGranter;
import com.thoughtworks.go.server.security.userdetail.GoUserPrinciple;
import com.thoughtworks.go.server.service.UserService;
import com.thoughtworks.go.server.util.UserHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.Authentication;
import org.springframework.security.context.SecurityContextHolder;
import org.springframework.security.providers.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class AuthenticateUserRequestProcessor implements GoPluginApiRequestProcessor {
    private static final String AUTHENTICATE_USER_REQUEST = "authenticate-user";
    private AuthorityGranter authorityGranter;
    private UserService userService;

    @Autowired
    public AuthenticateUserRequestProcessor(DefaultGoApplicationAccessor goApplicationAccessor, AuthorityGranter authorityGranter, UserService userService) {
        this.authorityGranter = authorityGranter;
        this.userService = userService;
        goApplicationAccessor.registerProcessorFor(AUTHENTICATE_USER_REQUEST, this);
    }

    @Override
    public GoApiResponse process(GoApiRequest goPluginApiRequest) {
        Map<String, String> userMap = new Gson().fromJson(goPluginApiRequest.requestBody(), Map.class);
        User user = new User(userMap.get("id"), userMap.get("username"), userMap.get("first-name"), userMap.get("last-name"), userMap.get("email-id"));
        GoUserPrinciple goUserPrinciple = new GoUserPrinciple(user.getUsername(), String.format("%s %s", user.getFirstName(), user.getLastName()), "", true, true, true, true, authorityGranter.authorities(user.getUsername()));
        Authentication authentication = new PreAuthenticatedAuthenticationToken(goUserPrinciple, null, goUserPrinciple.getAuthorities());
        userService.addUserIfDoesNotExist(UserHelper.getUserName(authentication));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        return new DefaultGoApiResponse(200);
    }
}
