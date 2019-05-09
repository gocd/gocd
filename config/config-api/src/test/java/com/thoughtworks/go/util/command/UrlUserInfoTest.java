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

package com.thoughtworks.go.util.command;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class UrlUserInfoTest {

    static Stream<Arguments> userinfoString() {
        return Stream.of(
                Arguments.of(null, null, null),
                Arguments.of("", "", null),
                Arguments.of(":", "", ""),
                Arguments.of("username", "username", null),
                Arguments.of("username:", "username", ""),
                Arguments.of(":password", "", "password"),
                Arguments.of("username:password", "username", "password")
        );
    }

    static Stream<Arguments> userInfoWithMaskedPassword() {
        return Stream.of(
                Arguments.of(null, null),
                Arguments.of("", "******"),
                Arguments.of(":", ":******"),
                Arguments.of("username-or-token", "******"),
                Arguments.of("username:", "username:******"),
                Arguments.of(":password", ":******"),
                Arguments.of("username:password", "username:******")
        );
    }

    @ParameterizedTest
    @MethodSource("userinfoString")
    void shouldCreateUrlUserinfoFromString(String input, String expectedUsername, String expectedPassword) {
        final UrlUserInfo urlUserInfo = new UrlUserInfo(input);

        assertThat(urlUserInfo.getUsername()).isEqualTo(expectedUsername);
        assertThat(urlUserInfo.getPassword()).isEqualTo(expectedPassword);
    }

    @ParameterizedTest
    @MethodSource("userInfoWithMaskedPassword")
    void shouldMaskPasswordIfPresentInUserinfo(String input, String expectedMaskedUserinfo) {
        final UrlUserInfo urlUserInfo = new UrlUserInfo(input);

        assertThat(urlUserInfo.maskedUserInfo()).isEqualTo(expectedMaskedUserinfo);
    }
}