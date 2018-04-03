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

package com.thoughtworks.go.server.newsecurity.authentication.filters;

import com.thoughtworks.go.server.newsecurity.authentication.handlers.GoAccessDeniedHandler;
import com.thoughtworks.go.server.newsecurity.authentication.providers.PasswordBasedPluginAuthenticationProvider;
import com.thoughtworks.go.server.service.SecurityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class BasicAuthenticationWithChallengeFilter extends AbstractBasicAuthenticationFilter {

    @Autowired
    public BasicAuthenticationWithChallengeFilter(SecurityService securityService,
                                                  PasswordBasedPluginAuthenticationProvider authenticationProvider) {
        super(securityService, authenticationProvider, new GoAccessDeniedHandler());
    }
}
