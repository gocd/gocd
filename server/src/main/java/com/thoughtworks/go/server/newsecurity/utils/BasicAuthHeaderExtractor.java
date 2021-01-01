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
package com.thoughtworks.go.server.newsecurity.utils;

import com.thoughtworks.go.server.newsecurity.models.UsernamePassword;
import org.springframework.security.authentication.BadCredentialsException;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.commons.lang3.StringUtils.isBlank;

public class BasicAuthHeaderExtractor {
    private static final Pattern BASIC_AUTH_EXTRACTOR_PATTERN = Pattern.compile("basic (.*)", Pattern.CASE_INSENSITIVE);

    public static UsernamePassword extractBasicAuthenticationCredentials(String authorizationHeader) {
        if (isBlank(authorizationHeader)) {
            return null;
        }

        final Matcher matcher = BASIC_AUTH_EXTRACTOR_PATTERN.matcher(authorizationHeader);
        if (matcher.matches()) {
            final String encodedCredentials = matcher.group(1);
            final byte[] decode = Base64.getDecoder().decode(encodedCredentials);
            String decodedCredentials = new String(decode, StandardCharsets.UTF_8);

            final int indexOfSeparator = decodedCredentials.indexOf(':');
            if (indexOfSeparator == -1) {
                throw new BadCredentialsException("Invalid basic authentication credentials specified in request.");
            }

            final String username = decodedCredentials.substring(0, indexOfSeparator);
            final String password = decodedCredentials.substring(indexOfSeparator + 1);

            return new UsernamePassword(username, password);
        }

        return null;
    }
}
