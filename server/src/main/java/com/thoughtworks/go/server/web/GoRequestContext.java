/*
 * Copyright Thoughtworks, Inc.
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
package com.thoughtworks.go.server.web;

import org.springframework.web.servlet.support.RequestContext;

import javax.servlet.http.HttpServletRequest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GoRequestContext extends RequestContext {
    private static final Pattern BASE_URL_PATTERN = Pattern.compile("^(.+://.+?)/");
    private final HttpServletRequest httpServletRequest;

    public GoRequestContext(HttpServletRequest httpServletRequest) {
        super(httpServletRequest);
        this.httpServletRequest = httpServletRequest;
    }

    public String getFullRequestPath() {
        return baseUrl(httpServletRequest.getRequestURL().toString()) + httpServletRequest.getContextPath();
    }

    private static String baseUrl(String s) {
        Matcher matcher = BASE_URL_PATTERN.matcher(s);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
}
