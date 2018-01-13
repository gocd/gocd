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

package com.thoughtworks.go.api.spring;

import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.SecurityService;
import com.thoughtworks.go.server.util.UserHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import spark.Request;
import spark.Response;

import static com.thoughtworks.go.api.util.HaltResponses.haltBecauseUnauthorized;

@Component
public class AuthenticationHelper {


    private final SecurityService securityService;
    private static final Logger LOG = LoggerFactory.getLogger(AuthenticationHelper.class);

    @Autowired
    public AuthenticationHelper(SecurityService securityService) {
        this.securityService = securityService;
    }

    public void checkAdminUserAnd401(Request req, Response res) {
        if (!securityService.isSecurityEnabled()) {
            return;
        }
        Username userName = UserHelper.getUserName();
        if (!securityService.isUserAdmin(userName)) {
            LOG.info("User {} attempted to perform an unauthorized action!", userName.getUsername());
            throw haltBecauseUnauthorized();
        }
    }

}
