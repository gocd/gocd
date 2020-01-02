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

import com.thoughtworks.go.server.newsecurity.filters.RememberLastRequestUrlFilter;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.stereotype.Component;

@Component
public class RememberLastRequestUrlFilterChain extends FilterChainProxy {

    public RememberLastRequestUrlFilterChain() {
        super(FilterChainBuilder.newInstance()
                .addFilterChain("/cctray.xml")
                .addFilterChain("/api/**")
                .addFilterChain("/remoting/**")
                .addFilterChain("/auth/**")
                .addFilterChain("/plugin/*/authenticate")
                .addFilterChain("/plugin/*/login")
                .addFilterChain("/assets/**")
                .addFilterChain("/**/*.json")
                .addFilterChain("/**", new RememberLastRequestUrlFilter())
                .build()
        );
    }

}
