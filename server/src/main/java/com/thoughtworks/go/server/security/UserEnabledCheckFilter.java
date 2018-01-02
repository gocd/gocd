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

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.domain.NullUser;
import com.thoughtworks.go.domain.User;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.UserService;
import com.thoughtworks.go.server.util.UserHelper;
import org.springframework.security.DisabledException;
import org.springframework.security.context.SecurityContext;
import org.springframework.security.context.SecurityContextHolder;
import org.springframework.security.ui.FilterChainOrder;
import org.springframework.security.ui.SpringSecurityFilter;

import static com.thoughtworks.go.domain.PersistentObject.NOT_PERSISTED;
import static com.thoughtworks.go.server.security.SessionDenialAwareAuthenticationProcessingFilterEntryPoint.SESSION_DENIED;
import static org.springframework.security.ui.AbstractProcessingFilter.SPRING_SECURITY_LAST_EXCEPTION_KEY;

/**
 * @understands
 */
public class UserEnabledCheckFilter  extends SpringSecurityFilter {
    private final UserService userService;

    public UserEnabledCheckFilter(UserService userService) {
        this.userService = userService;
    }

    protected void doFilterHttp(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        User user = getUser(request);
        if(user.getId() != NOT_PERSISTED && UserHelper.getUserId(request) == null){
            UserHelper.setUserId(request, user.getId());
        }
        
        if (!user.isEnabled()) {
            SecurityContext context = SecurityContextHolder.getContext();
            request.getSession().setAttribute(SPRING_SECURITY_LAST_EXCEPTION_KEY, new DisabledException("Your account has been disabled by the administrator"));
            request.setAttribute(SESSION_DENIED, true);
            context.setAuthentication(null);
            UserHelper.setUserId(request, null);
        }
        chain.doFilter(request, response);
    }

    private User getUser(HttpServletRequest request) {
        Long userId = UserHelper.getUserId(request);
        if (userId == null) {
            Username userName = UserHelper.getUserName();
            return userName.isAnonymous() ? new NullUser() : userService.findUserByName(CaseInsensitiveString.str(userName.getUsername()));
        } else {
            return userService.load(userId);
        }
    }

    public int getOrder() {
        return FilterChainOrder.AUTHENTICATION_PROCESSING_FILTER + 1;
    }
}
