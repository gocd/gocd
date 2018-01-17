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

package com.thoughtworks.go.spark.spring;

import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.SecurityService;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import spark.HaltException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static java.lang.String.valueOf;
import static spark.Spark.halt;

@Component
public class SPAAuthenticationHelper extends AbstractAuthenticationHelper {

    private final String fileContents;

    @Autowired
    public SPAAuthenticationHelper(SecurityService securityService, GoConfigService goConfigService) throws IOException {
        super(securityService, goConfigService);
        try (InputStream in = getClass().getResourceAsStream("/error.html")) {
            fileContents = IOUtils.toString(in, StandardCharsets.UTF_8);
        }
    }

    @Override
    protected HaltException renderUnauthorizedResponse() {
        return halt(401, replaceHtml(401, "Unauthorized"));
    }

    public String replaceHtml(int code, String message) {
        return fileContents.replaceAll(buildRegex("status_code"), valueOf(code))
                .replaceAll(buildRegex("error_message"), message);
    }

    private String buildRegex(final String value) {
        return "\\{\\{" + value + "\\}\\}";
    }
}
