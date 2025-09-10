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
package com.thoughtworks.go.server.security;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

import static com.thoughtworks.go.remote.StandardHeaders.REQUEST_CONFIRM_MODIFICATION;
import static com.thoughtworks.go.remote.StandardHeaders.REQUEST_CONFIRM_MODIFICATION_DEPRECATED;

public class ConfirmationConstraint {

    private static final List<String> HEADERS = List.of(REQUEST_CONFIRM_MODIFICATION_DEPRECATED, REQUEST_CONFIRM_MODIFICATION);

    public boolean isSatisfied(HttpServletRequest request) {
        return HEADERS.stream().anyMatch(header -> isValid(request, header));
    }

    private boolean isValid(HttpServletRequest request, String header) {
        String requestHeader = request.getHeader(header);
        return requestHeader != null && requestHeader.equalsIgnoreCase("true");
    }
}