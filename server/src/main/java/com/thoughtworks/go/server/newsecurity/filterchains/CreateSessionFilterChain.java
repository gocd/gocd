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

import com.thoughtworks.go.server.newsecurity.filters.AgentSessionReduceIdleTimeoutFilter;
import com.thoughtworks.go.server.newsecurity.filters.AlwaysCreateSessionFilter;
import com.thoughtworks.go.server.newsecurity.filters.ApiSessionReduceIdleTimeoutFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.stereotype.Component;

@Component
public class CreateSessionFilterChain extends FilterChainProxy {

    @Autowired
    public CreateSessionFilterChain(ApiSessionReduceIdleTimeoutFilter apiSessionReduceIdleTimeoutFilter,
                                    AgentSessionReduceIdleTimeoutFilter agentSessionReduceIdleTimeoutFilter,
                                    AlwaysCreateSessionFilter alwaysCreateSessionFilter) {
        super(FilterChainBuilder.newInstance()
                .addFilterChain("/admin/latest-agent.status", agentSessionReduceIdleTimeoutFilter, alwaysCreateSessionFilter)
                .addFilterChain("/admin/tfs-impl.jar", agentSessionReduceIdleTimeoutFilter, alwaysCreateSessionFilter)
                .addFilterChain("/admin/agent", agentSessionReduceIdleTimeoutFilter, alwaysCreateSessionFilter)
                .addFilterChain("/admin/agent/token", agentSessionReduceIdleTimeoutFilter, alwaysCreateSessionFilter)
                .addFilterChain("/admin/agent-plugins.zip", agentSessionReduceIdleTimeoutFilter, alwaysCreateSessionFilter)
                .addFilterChain("/cctray.xml", apiSessionReduceIdleTimeoutFilter, alwaysCreateSessionFilter)
                .addFilterChain("/add-on/*/api/**", apiSessionReduceIdleTimeoutFilter, alwaysCreateSessionFilter)
                .addFilterChain("/api/**", apiSessionReduceIdleTimeoutFilter, alwaysCreateSessionFilter)
                .addFilterChain("/**", alwaysCreateSessionFilter)
                .build()
        );
    }
}

