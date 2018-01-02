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

import com.thoughtworks.go.server.service.GoConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.Authentication;
import org.springframework.security.ConfigAttribute;
import org.springframework.security.ConfigAttributeDefinition;
import org.springframework.security.vote.AccessDecisionVoter;

public class IsSecurityEnabledVoter implements AccessDecisionVoter {
    private GoConfigService goConfigService;

    @Autowired
    public IsSecurityEnabledVoter(GoConfigService goConfigService) {
        this.goConfigService = goConfigService;
    }

    public boolean supports(ConfigAttribute configAttribute) {
        return true;
    }

    public boolean supports(Class aClass) {
        return true;
    }

    public int vote(Authentication authentication, Object o, ConfigAttributeDefinition configAttributeDefinition) {
        return goConfigService.isSecurityEnabled() ? ACCESS_ABSTAIN : ACCESS_GRANTED;
    }
}
