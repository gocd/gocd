/*
 * Copyright 2017 ThoughtWorks, Inc.
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
package com.thoughtworks.go.server.security;

import com.thoughtworks.go.util.SystemEnvironment;

import javax.servlet.http.HttpServletRequest;

public class HeaderConstraint {
    private SystemEnvironment systemEnvironment;

    public HeaderConstraint(SystemEnvironment systemEnvironment) {

        this.systemEnvironment = systemEnvironment;
    }

    public boolean isSatisfied(HttpServletRequest request) {
        if(!systemEnvironment.isApiSafeModeEnabled()) {
            return true;
        }

        String requestHeader = request.getHeader("Confirm");
        if(requestHeader == null || !requestHeader.equalsIgnoreCase("true")) {
            return false;
        }

        return true;
    }
}