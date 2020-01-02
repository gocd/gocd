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

import com.thoughtworks.go.server.newsecurity.filters.VerifyAuthorityFilter;
import com.thoughtworks.go.server.newsecurity.handlers.ResponseHandler;
import com.thoughtworks.go.server.security.GoAuthority;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import javax.servlet.Filter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

class FilterChainBuilder {
    final List<SecurityFilterChain> filterChain = new ArrayList<>();

    public static FilterChainBuilder newInstance() {
        return new FilterChainBuilder();
    }

    FilterChainBuilder addFilterChain(String antPattern, Filter... filters) {
        filterChain.add(new DefaultSecurityFilterChain(new AntPathRequestMatcher(antPattern), filters));
        return this;
    }

    FilterChainBuilder addAuthorityFilterChain(String antPattern, ResponseHandler responseHandler,
                                               GoAuthority... grantedAuthorities) {
        final Set<GrantedAuthority> collect = new HashSet<>();
        for (GoAuthority grantedAuthority : grantedAuthorities) {
            GrantedAuthority asAuthority = grantedAuthority.asAuthority();
            collect.add(asAuthority);
        }

        return addFilterChain(antPattern, new VerifyAuthorityFilter(collect, responseHandler));
    }


    public List<SecurityFilterChain> build() {
        return filterChain;
    }
}
