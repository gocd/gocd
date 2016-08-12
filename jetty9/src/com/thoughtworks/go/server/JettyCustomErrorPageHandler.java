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

package com.thoughtworks.go.server;

import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.servlet.ErrorPageErrorHandler;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

import static java.lang.String.valueOf;

public class JettyCustomErrorPageHandler extends ErrorPageErrorHandler {

    private final String fileContents;

    public JettyCustomErrorPageHandler() throws IOException {
        try (InputStream in = getClass().getResourceAsStream("/error.html")) {
            fileContents = IOUtils.toString(in, StandardCharsets.UTF_8);
        }
    }

    @Override
    protected void writeErrorPage(HttpServletRequest request, Writer writer, int code, String message, boolean showStacks) throws IOException {

        String defaultErrorMessage = HttpStatus.getMessage(code);
        String errorPage = replaceHtml(code, defaultErrorMessage);
        writer.write(errorPage);
    }

    private String replaceHtml(int code, String message) {
        return fileContents.replaceAll(buildRegex("status_code"), valueOf(code))
                .replaceAll(buildRegex("error_message"), message);
    }

    private String buildRegex(final String value) {
        return "\\{\\{" + value + "\\}\\}";
    }
}
