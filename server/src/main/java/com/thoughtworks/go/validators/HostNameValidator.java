/*
 * Copyright 2020 ThoughtWorks, Inc.
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
package com.thoughtworks.go.validators;

import com.thoughtworks.go.server.service.result.LocalizedOperationResult;

public class HostNameValidator implements Validator<String> {

    private static final String IPV6_ADDRESS_PATTERN = "([a-fA-F0-9]+\\:)*[a-fA-F0-9]+";
    static final String HOSTNAME_PATTERN = "([-_0-9\\w]*\\.)*[-_0-9\\w]+";
    static final String INVALID_HOSTNAME_KEY = "INVALID_HOSTNAME";

    @Override
    public void validate(String hostname, LocalizedOperationResult result) {
        if (hostname == null || (!hostname.matches(HOSTNAME_PATTERN) && !hostname.matches(IPV6_ADDRESS_PATTERN))) {
            result.notAcceptable("Invalid hostname. A valid hostname can only contain letters (A-z) digits (0-9) hyphens (-) dots (.) and underscores (_).");
        }
    }
}
