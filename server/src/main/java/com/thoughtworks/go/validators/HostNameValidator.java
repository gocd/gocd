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
package com.thoughtworks.go.validators;

import com.thoughtworks.go.server.service.result.LocalizedOperationResult;

import java.util.regex.Pattern;

public class HostNameValidator implements Validator<String>, com.thoughtworks.go.validation.HostNameValidator {

    private static final Pattern IPV6_ADDRESS = Pattern.compile("([a-fA-F0-9]+\\:)*[a-fA-F0-9]+");
    private static final Pattern HOSTNAME = Pattern.compile(HOSTNAME_PATTERN);

    @Override
    public void validate(String hostname, LocalizedOperationResult result) {
        if (hostname == null || (!HOSTNAME.matcher(hostname).matches() && !IPV6_ADDRESS.matcher(hostname).matches())) {
            result.notAcceptable("Invalid hostname. A valid hostname can only contain letters (A-z) digits (0-9) hyphens (-) dots (.) and underscores (_).");
        }
    }
}
