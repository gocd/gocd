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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

class SimpleUrlArgumentTest {
    @Test
    void shouldBeTypeOfCommandArgument() {
        assertThat(new SimpleUrlArgument("foo")).isInstanceOf(CommandArgument.class);
    }

    @Test
    void shouldErrorOutIfGivenUrlIsNull() {
        assertThatCode(() -> new SimpleUrlArgument(null))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Url cannot be null.");
    }

    @Nested
    class originalArgument {
        @Test
        void shouldReturnGivenUrlAsItIs() {
            final String originalUrl = "http://username:password@somehere";
            final SimpleUrlArgument simpleUrlArgument = new SimpleUrlArgument(originalUrl);

            assertThat(simpleUrlArgument.originalArgument()).isEqualTo(originalUrl);
        }
    }

    @Nested
    class forCommandLine {
        @Test
        void shouldReturnGivenUrlAsItIs() {
            final String originalUrl = "http://username:password@somehere";
            final SimpleUrlArgument simpleUrlArgument = new SimpleUrlArgument(originalUrl);

            assertThat(simpleUrlArgument.forCommandLine()).isEqualTo(originalUrl);
        }
    }

    @Nested
    @TestInstance(PER_CLASS)
    class forDisplay {

        @ParameterizedTest
        @MethodSource("urls")
        void shouldMaskPasswordInGivenUrl(String input, String expectedMaskedUrl) {
            final SimpleUrlArgument simpleUrlArgument = new SimpleUrlArgument(input);

            assertThat(simpleUrlArgument.forDisplay()).isEqualTo(expectedMaskedUrl);
        }

        Stream<Arguments> urls() {
            return Stream.of(
                    Arguments.of("", ""),
                    Arguments.of("http://username:password@somehere", "http://username:******@somehere"),
                    Arguments.of("http://username@somehere", "http://username@somehere"),
                    Arguments.of("http://:@somehere", "http://:******@somehere"),
                    Arguments.of("http://:password@somehere", "http://:******@somehere"),
                    Arguments.of("http://username:@somehere", "http://username:******@somehere"),
                    Arguments.of("http://something/somewhere", "http://something/somewhere")
            );
        }
    }

    @Nested
    class Equals {
        @Test
        void shouldBeEqualBasedOnRawUrl() {
            SimpleUrlArgument url1 = new SimpleUrlArgument("svn+ssh://user:password@10.18.7.51:8153");
            SimpleUrlArgument url3 = new SimpleUrlArgument("svn+ssh://user:password@10.18.7.51:8153");
            assertThat(url1).isEqualTo(url3);
        }

        @Test
        void shouldBeEqualBasedOnRawUrl1() {
            SimpleUrlArgument url1 = new SimpleUrlArgument("svn+ssh://user:password@10.18.7.51:8153");
            SimpleUrlArgument url3 = new SimpleUrlArgument("svn+ssh://user:password@10.18.7.51:8153");
            assertThat(url1).isEqualTo(url3);
        }

        @Test
        void shouldBeEqualBasedOnRawUrlForHttpUrls() {
            SimpleUrlArgument url1 = new SimpleUrlArgument("http://user:password@10.18.7.51:8153");
            SimpleUrlArgument url2 = new SimpleUrlArgument("http://user:other@10.18.7.51:8153");
            SimpleUrlArgument url3 = new SimpleUrlArgument("http://user:password@10.18.7.51:8153");
            assertThat(url1).isEqualTo(url3);
            assertThat(url1).isNotEqualTo(url2);
        }

        @Test
        void shouldIgnoreTrailingSlashesOnURIs() {
            SimpleUrlArgument url1 = new SimpleUrlArgument("file:///not-exist/svn/trunk/");
            SimpleUrlArgument url2 = new SimpleUrlArgument("file:///not-exist/svn/trunk");
            assertThat(url1).isEqualTo(url2);
        }
    }

    @Nested
    @TestInstance(PER_CLASS)
    class hostInfoForCommandline {
        Stream<Arguments> urls() {
            return Stream.of(
                    Arguments.of("http://username:password@somewhere?name=bob", "http://username:******@somewhere?name=bob"),
                    Arguments.of("http://username:@somewhere/gocd/gocd.git", "http://username:******@somewhere/gocd/gocd.git"),
                    Arguments.of("http://somewhere:1234/gocd/gocd.git", "http://somewhere:1234/gocd/gocd.git")
            );
        }

        @Test
        void shouldReturnLineAsItIsIfLineIsBlank() {
            final SimpleUrlArgument simpleUrlArgument = new SimpleUrlArgument("http://username:password@somewhere?name=bob");
            assertThat(simpleUrlArgument.replaceSecretInfo("")).isEqualTo("");
        }

        @Test
        void shouldReturnLineAsItIsIfLineIsNull() {
            final SimpleUrlArgument simpleUrlArgument = new SimpleUrlArgument("http://username:password@somewhere?name=bob");
            assertThat(simpleUrlArgument.replaceSecretInfo(null)).isEqualTo(null);
        }

        @Test
        void shouldReturnLineAsItIsIfUrlIsBlank() {
            final SimpleUrlArgument simpleUrlArgument = new SimpleUrlArgument("");
            assertThat(simpleUrlArgument.replaceSecretInfo("some-content")).isEqualTo("some-content");
        }

        @ParameterizedTest
        @MethodSource("urls")
        void shouldMaskPasswordInGivenConsoleOutput(String input, String maskedUrl) {
            final SimpleUrlArgument simpleUrlArgument = new SimpleUrlArgument(input);
            final String originalLine = format("[go] Start updating repo at revision 08e7cc03 from %s", input);

            final String expectedLine = format("[go] Start updating repo at revision 08e7cc03 from %s", maskedUrl);
            assertThat(simpleUrlArgument.replaceSecretInfo(originalLine)).isEqualTo(expectedLine);
        }

        @Test
        void shouldMaskMultipleOccurrencesOfUserInfo() {
            final String url = "http://username:password@somewhere?name=bob";
            final String originalLine = format("[go] echoing same url twice: %s and %s", url, url);
            final SimpleUrlArgument simpleUrlArgument = new SimpleUrlArgument(url);

            final String actual = simpleUrlArgument.replaceSecretInfo(originalLine);

            final String maskedUrl = "http://username:******@somewhere?name=bob";
            final String expectedLine = format("[go] echoing same url twice: %s and %s", maskedUrl, maskedUrl);
            assertThat(actual).isEqualTo(expectedLine);
        }
    }
}