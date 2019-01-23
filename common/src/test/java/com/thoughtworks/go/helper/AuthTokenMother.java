/*
 * Copyright 2019 ThoughtWorks, Inc.
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

package com.thoughtworks.go.helper;

import com.thoughtworks.go.domain.AuthToken;
import org.apache.commons.lang3.RandomStringUtils;

import java.util.Date;

public class AuthTokenMother {
    public static AuthToken authTokenWithName(String tokenName) {
        String tokenValue = RandomStringUtils.randomAlphanumeric(32).toUpperCase();
        String tokenDescription = RandomStringUtils.randomAlphanumeric(512).toUpperCase();
        AuthToken authToken = new AuthToken(tokenName, tokenValue, tokenDescription, false, new Date(), null);
        authToken.setUsername("Bob");

        return authToken;
    }

    public static AuthToken authTokenWithNameForUser(String tokenName, String username) {
        AuthToken authToken = authTokenWithName(tokenName);
        authToken.setUsername(username);

        return authToken;
    }
}
