/*
 * Copyright 2016 ThoughtWorks, Inc.
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

package com.thoughtworks.go.server.security.providers;

import com.thoughtworks.go.server.service.GoConfigService;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.GrantedAuthority;
import org.springframework.security.userdetails.UserDetails;
import org.springframework.security.userdetails.ldap.LdapUserDetails;
import org.springframework.security.userdetails.ldap.LdapUserDetailsMapper;

import static com.thoughtworks.go.server.security.LdapAuthenticator.DISPLAY_NAME_KEY;

public class CustomLdapUserDetailsContextMapper extends LdapUserDetailsMapper {
    private GoConfigService goConfigService;

    public CustomLdapUserDetailsContextMapper(GoConfigService goConfigService) {
        this.goConfigService = goConfigService;
    }


    @Override
    public UserDetails mapUserFromContext(DirContextOperations ctx, String username, GrantedAuthority[] authority) {
        LdapUserDetails userDetails = (LdapUserDetails) super.mapUserFromContext(ctx, username, authority);
        userDetails.getAttributes().put(DISPLAY_NAME_KEY, ctx.getStringAttribute(DISPLAY_NAME_KEY));
        return userDetails;
    }
}
