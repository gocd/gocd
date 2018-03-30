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

package com.thoughtworks.go.server.newsecurity.authentication.matchers;

import org.springframework.util.Assert;

import javax.servlet.http.HttpServletRequest;
import java.util.regex.Pattern;

public class RequestHeaderRequestMatcher {
    private final String expectedHeaderName;
    private final Pattern valueMatcherPattern;

    public RequestHeaderRequestMatcher(String expectedHeaderName) {
        this(expectedHeaderName, null);
    }

    public RequestHeaderRequestMatcher(String expectedHeaderName, Pattern valueMatcherPattern) {
        Assert.notNull(expectedHeaderName, "headerName cannot be null.");
        this.expectedHeaderName = expectedHeaderName;
        this.valueMatcherPattern = valueMatcherPattern;
    }

    public boolean matches(HttpServletRequest request) {
        String actualHeaderValue = request.getHeader(expectedHeaderName);

        if (valueMatcherPattern == null) {
            return actualHeaderValue != null;
        }

        return valueMatcherPattern.matcher(actualHeaderValue).matches();
    }

    @Override
    public String toString() {
        return "RequestHeaderRequestMatcher{" +
                "expectedHeaderName='" + expectedHeaderName + '\'' +
                ", valueMatcherPattern=" + valueMatcherPattern +
                '}';
    }
}
