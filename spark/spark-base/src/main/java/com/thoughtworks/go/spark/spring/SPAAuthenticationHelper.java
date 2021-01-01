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
package com.thoughtworks.go.spark.spring;

import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.SecurityService;
import com.thoughtworks.go.spark.HtmlErrorPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import spark.HaltException;

import static spark.Spark.halt;

@Component
public class SPAAuthenticationHelper extends AbstractAuthenticationHelper {

    @Autowired
    public SPAAuthenticationHelper(SecurityService securityService, GoConfigService goConfigService) {
        super(securityService, goConfigService);
    }

    @Override
    protected HaltException renderForbiddenResponse() {
        return halt(403, HtmlErrorPage.errorPage(403, "Forbidden"));
    }

    @Override
    protected HaltException renderForbiddenResponse(String message) {
        return halt(403, HtmlErrorPage.errorPage(403, message));
    }

    public HaltException renderNotFoundResponse(String message) {
        return halt(404, HtmlErrorPage.errorPage(404, message));
    }
}
