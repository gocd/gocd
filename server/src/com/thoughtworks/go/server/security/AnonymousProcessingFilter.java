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

package com.thoughtworks.go.server.security;

import java.util.ArrayList;
import javax.servlet.http.HttpServletRequest;

import com.thoughtworks.go.server.service.GoConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.Authentication;
import org.springframework.security.GrantedAuthorityImpl;
import org.springframework.security.userdetails.memory.UserAttribute;

public class AnonymousProcessingFilter
        extends org.springframework.security.providers.anonymous.AnonymousProcessingFilter {
    private final GoConfigService configService;

    @Autowired
    public AnonymousProcessingFilter(GoConfigService configService) {
        this.configService = configService;
        setKey("anonymousKey");
        setUserAttributeWithRole(GoAuthority.ROLE_ANONYMOUS.toString());
    }

    private void setUserAttributeWithRole(final String role) {
        final UserAttribute initialAttribute = new UserAttribute();
        initialAttribute.setPassword("anonymousUser");
        initialAttribute.setAuthorities(new ArrayList() {
            {
                add(new GrantedAuthorityImpl(role));
            }
        });
        setUserAttribute(initialAttribute);
    }

    public Authentication createAuthentication(HttpServletRequest request) {
        setUserAttributeWithRole(configService.isSecurityEnabled()
                ? GoAuthority.ROLE_ANONYMOUS.toString()
                : GoAuthority.ROLE_SUPERVISOR.toString());
        return super.createAuthentication(request);
    }
}
