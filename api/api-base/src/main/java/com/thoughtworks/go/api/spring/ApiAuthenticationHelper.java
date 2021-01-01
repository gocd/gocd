/*
 * Copyright 2021 ThoughtWorks, Inc.
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

import com.thoughtworks.go.api.util.HaltApiResponses;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.SecurityService;
import com.thoughtworks.go.spark.spring.AbstractAuthenticationHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import spark.HaltException;
import spark.Request;
import spark.Response;

@Component
public class ApiAuthenticationHelper extends AbstractAuthenticationHelper {

    @Autowired
    public ApiAuthenticationHelper(SecurityService securityService, GoConfigService goConfigService) {
        super(securityService, goConfigService);
    }

    @Override
    public HaltException renderForbiddenResponse() throws HaltException {
        LOG.info("User {} attempted to perform an unauthorized action!", currentUserLoginName());
        return HaltApiResponses.haltBecauseForbidden();
    }

    @Override
    protected HaltException renderForbiddenResponse(String message) {
        LOG.info("{}", message);
        return HaltApiResponses.haltBecauseForbidden(message);
    }

    public void ensureSecurityEnabled(Request request, Response response) {
        if (!securityService.isSecurityEnabled()) {
            throw HaltApiResponses.haltBecauseSecurityIsNotEnabled();
        }
    }
}
