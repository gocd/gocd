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

package com.thoughtworks.go.server.newsecurity.filterchains;

import com.thoughtworks.go.server.newsecurity.filters.NoOpFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.stereotype.Component;

import javax.servlet.Filter;

@Component("authenticationFilterChain")
public class AuthenticationFilterChain extends FilterChainProxy {

    @Autowired
    public AuthenticationFilterChain(
            @Qualifier("x509AuthenticationFilter") Filter x509AuthenticationFilter,
            @Qualifier("invalidateAuthenticationOnSecurityConfigChangeFilter") Filter invalidateAuthenticationOnSecurityConfigChangeFilter,
            @Qualifier("reAuthenticationWithRedirectToLoginFilter") Filter reAuthenticationWithRedirectToLoginPage,
            @Qualifier("reAuthenticationWithChallengeFilter") Filter reAuthenticationWithChallenge,
            @Qualifier("basicAuthenticationWithChallengeFilter") Filter basicAuthenticationWithChallengeFilter,
            @Qualifier("basicAuthenticationWithRedirectToLoginFilter") Filter basicAuthenticationWithRedirectToLoginFilter,
            @Qualifier("oauthAuthenticationFilter") Filter oauthAuthenticationFilter) {
        super(FilterChainBuilder.newInstance()
                .addFilterChain("/remoting/**", x509AuthenticationFilter)
                .addFilterChain("/agent-websocket/**", x509AuthenticationFilter)

                .addFilterChain("/add-on/*/api/**", invalidateAuthenticationOnSecurityConfigChangeFilter, reAuthenticationWithRedirectToLoginPage, oauthAuthenticationFilter)

                .addFilterChain("/api/config-repository.git/**", invalidateAuthenticationOnSecurityConfigChangeFilter, reAuthenticationWithChallenge, basicAuthenticationWithChallengeFilter)
                .addFilterChain("/cctray.xml", invalidateAuthenticationOnSecurityConfigChangeFilter, reAuthenticationWithChallenge, basicAuthenticationWithChallengeFilter)
                .addFilterChain("/api/**", invalidateAuthenticationOnSecurityConfigChangeFilter, reAuthenticationWithChallenge, basicAuthenticationWithChallengeFilter)

                .addFilterChain("/api/version", invalidateAuthenticationOnSecurityConfigChangeFilter, reAuthenticationWithRedirectToLoginPage, basicAuthenticationWithRedirectToLoginFilter)
                .addFilterChain("/auth/*", new NoOpFilter())
                .addFilterChain("/plugin/*/login", new NoOpFilter())
                .addFilterChain("/plugin/*/authenticate", new NoOpFilter())
                .addFilterChain("/**", invalidateAuthenticationOnSecurityConfigChangeFilter, reAuthenticationWithRedirectToLoginPage, basicAuthenticationWithRedirectToLoginFilter)
                .build()
        );
    }

}
