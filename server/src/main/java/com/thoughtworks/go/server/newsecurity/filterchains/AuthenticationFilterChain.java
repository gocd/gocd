/*
 * Copyright 2020 ThoughtWorks, Inc.
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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.stereotype.Component;

import javax.servlet.Filter;

@Component("authenticationFilterChain")
public class AuthenticationFilterChain extends FilterChainProxy {

    @Autowired
    public AuthenticationFilterChain(
            @Qualifier("agentAuthenticationFilter") Filter x509AuthenticationFilter,
            @Qualifier("invalidateAuthenticationOnSecurityConfigChangeFilter") Filter invalidateAuthenticationOnSecurityConfigChangeFilter,
            @Qualifier("reAuthenticationWithRedirectToLoginFilter") Filter reAuthenticationWithRedirectToLoginPage,
            @Qualifier("reAuthenticationWithChallengeFilter") Filter reAuthenticationWithChallenge,
            @Qualifier("basicAuthenticationWithChallengeFilter") Filter basicAuthenticationWithChallengeFilter,
            @Qualifier("basicAuthenticationWithRedirectToLoginFilter") Filter basicAuthenticationWithRedirectToLoginFilter,
            @Qualifier("accessTokenAuthenticationFilter") Filter accessTokenAuthenticationFilter,
            @Qualifier("assumeAnonymousUserFilter") Filter assumeAnonymousUserFilter) {
        super(FilterChainBuilder.newInstance()
                // X509 for agent remoting
                .addFilterChain("/remoting/**", x509AuthenticationFilter)

                // For addons
                .addFilterChain("/add-on/**", assumeAnonymousUserFilter)

                // For API authentication
                .addFilterChain("/api/config-repository.git/**", invalidateAuthenticationOnSecurityConfigChangeFilter, assumeAnonymousUserFilter, reAuthenticationWithChallenge, basicAuthenticationWithChallengeFilter)
                .addFilterChain("/cctray.xml", invalidateAuthenticationOnSecurityConfigChangeFilter, reAuthenticationWithChallenge, assumeAnonymousUserFilter, basicAuthenticationWithChallengeFilter, accessTokenAuthenticationFilter)
                .addFilterChain("/api/webhooks/*/notify", assumeAnonymousUserFilter)
                .addFilterChain("/api/**", invalidateAuthenticationOnSecurityConfigChangeFilter, reAuthenticationWithChallenge, assumeAnonymousUserFilter, basicAuthenticationWithChallengeFilter, accessTokenAuthenticationFilter)
                .addFilterChain("/files/**", invalidateAuthenticationOnSecurityConfigChangeFilter, reAuthenticationWithChallenge, assumeAnonymousUserFilter, basicAuthenticationWithChallengeFilter, accessTokenAuthenticationFilter)
                .addFilterChain("/properties/**", invalidateAuthenticationOnSecurityConfigChangeFilter, reAuthenticationWithChallenge, assumeAnonymousUserFilter, basicAuthenticationWithChallengeFilter, accessTokenAuthenticationFilter)

                .addFilterChain("/api/version", invalidateAuthenticationOnSecurityConfigChangeFilter, reAuthenticationWithRedirectToLoginPage, assumeAnonymousUserFilter, basicAuthenticationWithRedirectToLoginFilter)

                .addFilterChain("/auth/*", assumeAnonymousUserFilter)
                .addFilterChain("/plugin/*/login", assumeAnonymousUserFilter)
                .addFilterChain("/plugin/*/authenticate", assumeAnonymousUserFilter)
                .addFilterChain("/**", invalidateAuthenticationOnSecurityConfigChangeFilter, reAuthenticationWithRedirectToLoginPage, assumeAnonymousUserFilter, basicAuthenticationWithRedirectToLoginFilter)
                .build()
        );
    }

}
