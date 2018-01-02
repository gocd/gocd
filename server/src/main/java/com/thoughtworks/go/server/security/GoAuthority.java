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

import org.springframework.security.GrantedAuthority;
import org.springframework.security.GrantedAuthorityImpl;

public enum GoAuthority {
    ROLE_SUPERVISOR,
    ROLE_GROUP_SUPERVISOR,
    ROLE_ANONYMOUS,
    ROLE_OAUTH_USER,
    ROLE_USER,
    ROLE_TEMPLATE_SUPERVISOR,
    ROLE_TEMPLATE_VIEW_USER;

    public GrantedAuthority asAuthority() {
        return new GrantedAuthorityImpl(this.toString());
    }

}
