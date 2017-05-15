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

package com.thoughtworks.go.server.security;

import com.thoughtworks.go.server.service.GoConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

public class AnonymousProcessingFilter extends AnonymousAuthenticationFilter {

    @Autowired
    public AnonymousProcessingFilter(GoConfigService configService) {
        super("anonymousKey", "anonymousUser", getGrantedAuthority(configService));
    }

    private static List<GrantedAuthority> getGrantedAuthority(GoConfigService configService) {
        List<GrantedAuthority> grantedAuthorities = new ArrayList<>();
        if (configService.isSecurityEnabled()) {
            grantedAuthorities.add(GoAuthority.ROLE_ANONYMOUS.asAuthority());
        } else {
            grantedAuthorities.add(GoAuthority.ROLE_SUPERVISOR.asAuthority());

        }
        return grantedAuthorities;
    }

    @Override
    public Authentication createAuthentication(HttpServletRequest request) {
        return super.createAuthentication(request);
    }
}
