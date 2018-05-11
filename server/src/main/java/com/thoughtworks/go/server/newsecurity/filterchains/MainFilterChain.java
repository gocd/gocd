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

import com.thoughtworks.go.server.newsecurity.filters.AssumeAnonymousUserFilter;
import com.thoughtworks.go.server.newsecurity.filters.ModeAwareFilter;
import com.thoughtworks.go.server.newsecurity.filters.ThreadLocalUserFilter;
import com.thoughtworks.go.server.web.FlashLoadingFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.stereotype.Component;

@Component("mainFilterChain")
public class MainFilterChain extends FilterChainProxy {

    @Autowired
    public MainFilterChain(ModeAwareFilter modeAwareFilter,
                           CreateSessionFilterChain createSessionFilterChain,
                           RememberLastRequestUrlFilterChain rememberLastRequestUrlFilterChain,
                           AuthenticationFilterChain authenticationFilterChain,
                           AssumeAnonymousUserFilter assumeAnonymousUserFilter,
                           ThreadLocalUserFilter threadLocalUserFilter,
                           AuthorizeFilterChain authorizeFilterChain,
                           DenyGoCDAccessForArtifactsFilterChain denyGoCDAccessForArtifactsFilterChain,
                           ArtifactSizeEnforcementFilterChain artifactSizeEnforcementFilterChain,
                           FlashLoadingFilter flashLoadingFilter) {

        super(FilterChainBuilder.newInstance()
                .addFilterChain("/**",
                        modeAwareFilter,
                        createSessionFilterChain,
                        rememberLastRequestUrlFilterChain,
                        authenticationFilterChain,
                        assumeAnonymousUserFilter,
                        threadLocalUserFilter,
                        authorizeFilterChain,
                        denyGoCDAccessForArtifactsFilterChain,
                        artifactSizeEnforcementFilterChain,
                        flashLoadingFilter)
                .build());
    }


}
