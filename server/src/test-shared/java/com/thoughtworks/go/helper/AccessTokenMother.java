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
package com.thoughtworks.go.helper;

import com.thoughtworks.go.domain.AccessToken;
import com.thoughtworks.go.util.TestingClock;
import org.apache.commons.lang3.RandomStringUtils;

public class AccessTokenMother {
    public static AccessToken.AccessTokenWithDisplayValue randomAccessToken() {
        return randomAccessTokenForUser(RandomStringUtils.randomAlphabetic(32));
    }

    public static AccessToken.AccessTokenWithDisplayValue randomAccessTokenForUser(String username) {
        return AccessToken.create(RandomStringUtils.randomAlphabetic(32), username, RandomStringUtils.randomAlphabetic(32), new TestingClock());
    }
}
