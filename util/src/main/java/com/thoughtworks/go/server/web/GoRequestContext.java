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
package com.thoughtworks.go.server.web;

import javax.servlet.http.HttpServletRequest;

import static com.thoughtworks.go.util.GoConstants.BASE_URL_PATTERN;
import com.thoughtworks.go.util.StringUtil;
import org.springframework.web.servlet.support.RequestContext;

public class GoRequestContext extends RequestContext {
    private HttpServletRequest httpServletRequest;

    public GoRequestContext(HttpServletRequest httpServletRequest) {
        super(httpServletRequest);
        this.httpServletRequest = httpServletRequest;
    }

    public String getFullRequestPath() {
        String fullUrl = httpServletRequest.getRequestURL().toString();
        return StringUtil.matchPattern(BASE_URL_PATTERN, fullUrl) + httpServletRequest.getContextPath();
    }
}
