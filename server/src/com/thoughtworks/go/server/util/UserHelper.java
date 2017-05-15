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

package com.thoughtworks.go.server.util;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.domain.User;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.security.X509AuthoritiesPopulator;
import com.thoughtworks.go.server.security.userdetail.GoUserPrinciple;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.ldap.userdetails.LdapUserDetails;

import javax.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.thoughtworks.go.server.domain.Username.ANONYMOUS;

public class UserHelper {

    public static final String USERID = "USERID";

    public static Username getUserName() {
        SecurityContext securityContext = SecurityContextHolder.getContext();
        if (securityContext != null && securityContext.getAuthentication() != null) {
            return getUserName(securityContext.getAuthentication());
        }
        return ANONYMOUS;
    }

    public static Username getUserName(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof LdapUserDetails) {
            LdapUserDetails userDetails = (LdapUserDetails) principal;
            return new Username(new CaseInsensitiveString(userDetails.getUsername()), resolveDisplayName(userDetails.getUsername(), userDetails.getDn()));
        }
        if (principal instanceof GoUserPrinciple) {
            GoUserPrinciple userPrincipleDetails = (GoUserPrinciple) principal;
            return new Username(new CaseInsensitiveString(userPrincipleDetails.getUsername()), userPrincipleDetails.getDisplayName());
        }
        if (principal instanceof UserDetails) {
            UserDetails userDetails = (UserDetails) principal;
            return new Username(new CaseInsensitiveString(userDetails.getUsername()));
        }
        return ANONYMOUS;
    }

    public static User getUser(Authentication authentication) {
        return new User(CaseInsensitiveString.str(getUserName(authentication).getUsername()));
    }


    public static boolean isAgent() {
        return matchesRole(X509AuthoritiesPopulator.ROLE_AGENT);
    }

    private static boolean matchesRole(String role) {
        SecurityContext securityContext = SecurityContextHolder.getContext();
        if (securityContext != null && securityContext.getAuthentication() != null) {
            return matchesRole(securityContext.getAuthentication(), role);
        }
        return false;
    }

    static boolean matchesRole(Authentication authentication, String roleAgent) {
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        if (authorities == null) {
            return false;
        }
        for (GrantedAuthority authority : authorities) {
            if (authority.getAuthority().equals(roleAgent)) {
                return true;
            }
        }
        return false;
    }

    private static String resolveDisplayName(String username, String dn) {
        Pattern pattern = Pattern.compile("cn=(.*?),\\s");
        Matcher matcher = pattern.matcher(dn);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return username;
    }

    public static String getSessionKeyForUserId() {
        return USERID;
    }

    public static Long getUserId(HttpServletRequest request) {
        return (Long) request.getSession().getAttribute(USERID);
    }

    public static void setUserId(HttpServletRequest request, Long id) {
        request.getSession().setAttribute(USERID, id);
    }
}
