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

import com.thoughtworks.go.domain.AccessToken;
import org.apache.commons.lang3.RandomStringUtils;

import java.util.Date;

public class AccessTokenMother {
    public static AccessToken accessTokenWithName(String tokenName) {
        String tokenValue = RandomStringUtils.randomAlphanumeric(32).toUpperCase();
        String tokenDescription = RandomStringUtils.randomAlphanumeric(512).toUpperCase();
        String saltId = RandomStringUtils.randomAlphanumeric(8).toUpperCase();
        String saltValue = RandomStringUtils.randomAlphanumeric(255).toUpperCase();
        String authConfigId = RandomStringUtils.randomAlphanumeric(255).toUpperCase();

        AccessToken accessToken = new AccessToken(tokenName, tokenValue, tokenDescription, false, new Date(), null);
        accessToken.setSaltId(saltId);
        accessToken.setSaltValue(saltValue);
        accessToken.setAuthConfigId(authConfigId);
        accessToken.setUsername("Bob");

        return accessToken;
    }

    public static AccessToken accessTokenWithNameForUser(String tokenName, String username) {
        AccessToken accessToken = accessTokenWithName(tokenName);
        accessToken.setUsername(username);

        return accessToken;
    }
}
