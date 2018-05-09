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

import org.springframework.security.vote.AccessDecisionVoter;
import org.springframework.security.ConfigAttribute;
import org.springframework.security.Authentication;
import org.springframework.security.ConfigAttributeDefinition;
import org.springframework.security.SecurityConfig;
import static com.thoughtworks.go.server.security.GoAuthority.ROLE_OAUTH_USER;

/**
 * @understands if a principal has OAuth role.
 */
public class IsOAuthVoter implements AccessDecisionVoter {
    public boolean supports(ConfigAttribute attribute) {
        return attribute.getAttribute().equals(ROLE_OAUTH_USER.name());
    }

    public boolean supports(Class clazz) {
        return true;
    }

    public int vote(Authentication authentication, Object object, ConfigAttributeDefinition config) {
        if (authentication instanceof OauthAuthenticationToken) {
            if (!config.contains(new SecurityConfig(ROLE_OAUTH_USER.name()))) {
                return ACCESS_DENIED;
            } else {
                return ACCESS_GRANTED;
            }
        }
        return ACCESS_ABSTAIN;
    }
}
