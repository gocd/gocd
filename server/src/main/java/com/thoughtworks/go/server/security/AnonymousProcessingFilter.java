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

package com.thoughtworks.go.server.security;

import com.thoughtworks.go.server.service.GoConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.memory.UserAttribute;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.UUID;

@Component
public class AnonymousProcessingFilter extends AnonymousAuthenticationFilter {
    private final GoConfigService configService;

    @Autowired
    public AnonymousProcessingFilter(GoConfigService configService) {
        super(UUID.randomUUID().toString(), "anonymousUser", Collections.singletonList(GoAuthority.ROLE_ANONYMOUS.asAuthority()));
        this.configService = configService;
    }

    private void setUserAttributeWithRole(final String role) {
        final UserAttribute initialAttribute = new UserAttribute();
        initialAttribute.setPassword("anonymousUser");
        initialAttribute.setAuthorities(Collections.singletonList(new SimpleGrantedAuthority(role)));
        setUserAttribute(initialAttribute);
    }

    public Authentication createAuthentication(HttpServletRequest request) {
        setUserAttributeWithRole(configService.isSecurityEnabled()
                ? GoAuthority.ROLE_ANONYMOUS.toString()
                : GoAuthority.ROLE_SUPERVISOR.toString());
        return super.createAuthentication(request);
    }
}
