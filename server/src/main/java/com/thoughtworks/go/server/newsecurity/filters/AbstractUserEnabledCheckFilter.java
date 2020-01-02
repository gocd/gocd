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
package com.thoughtworks.go.server.newsecurity.filters;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.domain.NullUser;
import com.thoughtworks.go.domain.User;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.newsecurity.utils.SessionUtils;
import com.thoughtworks.go.server.security.userdetail.GoUserPrinciple;
import com.thoughtworks.go.server.service.SecurityService;
import com.thoughtworks.go.server.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static com.thoughtworks.go.domain.PersistentObject.NOT_PERSISTED;

public abstract class AbstractUserEnabledCheckFilter extends OncePerRequestFilter {
    protected final Logger LOGGER = LoggerFactory.getLogger(getClass());

    private final UserService userService;
    private final SecurityService securityService;

    AbstractUserEnabledCheckFilter(UserService userService, SecurityService securityService) {
        this.userService = userService;
        this.securityService = securityService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!securityService.isSecurityEnabled()) {
            LOGGER.debug("Not checking if user is enabled since security is disabled.");
            filterChain.doFilter(request, response);
            return;
        }

        final User user = getUser(request);

        if (persisted(user) && notInSession(request)) {
            SessionUtils.setUserId(request, user.getId());
        }

        if (user.isEnabled()) {
            filterChain.doFilter(request, response);
        } else {
            SessionUtils.recreateSessionWithoutCopyingOverSessionState(request);
            handleFailure(request, response, "Your account has been disabled by the administrator");
        }
    }

    abstract void handleFailure(HttpServletRequest request, HttpServletResponse response, String errorMessage) throws IOException;

    private boolean notInSession(HttpServletRequest request) {
        return SessionUtils.getUserId(request) == null;
    }

    private boolean persisted(User user) {
        return user.getId() != NOT_PERSISTED;
    }

    private User getUser(HttpServletRequest request) {
        Long userId = SessionUtils.getUserId(request);
        if (userId == null) {
            final GoUserPrinciple currentUser = SessionUtils.getCurrentUser();

            Username userName = new Username(currentUser.getUsername());

            if (userName.isAnonymous() || userName.isGoAgentUser()) {
                return new NullUser();
            }

            return userService.findUserByName(CaseInsensitiveString.str(userName.getUsername()));
        } else {
            return userService.load(userId);
        }
    }
}
