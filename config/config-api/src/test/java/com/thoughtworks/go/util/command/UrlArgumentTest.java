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

import org.junit.jupiter.api.BeforeEach;
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

class UrlArgumentTest {
    private static final String URL_WITH_PASSWORD = "http://username:password@somehere";

    private CommandArgument argument;

    @BeforeEach
    void setup() {
        argument = new UrlArgument(URL_WITH_PASSWORD);
    }

    @Test
    void shouldBeTypeOfCommandArgument() {
        assertThat(new UrlArgument("foo")).isInstanceOf(CommandArgument.class);
    }

    @Test
    void shouldErrorOutIfGivenUrlIsNull() {
        assertThatCode(() -> new UrlArgument(null))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Url cannot be null.");
    }

    @Nested
    class originalArgument {
        @Test
        void shouldReturnGivenUrlAsItIs() {
            assertThat(argument.originalArgument()).isEqualTo(URL_WITH_PASSWORD);
        }
    }

    @Nested
    class forCommandLine {
        @Test
        void shouldReturnGivenUrlAsItIs() {
            final UrlArgument url = new UrlArgument("https://username:password@something/foo");

            assertThat(url.forCommandLine()).isEqualTo("https://username:password@something/foo");
        }
    }

    @Nested
    @TestInstance(PER_CLASS)
    class forDisplay {
        Stream<Arguments> urls() {
            return Stream.of(
                    Arguments.of("", ""),
                    Arguments.of("http://username:password@somehere", "http://username:******@somehere"),
                    Arguments.of("http://username@somehere", "http://******@somehere"),
                    Arguments.of("http://:@somehere", "http://:******@somehere"),
                    Arguments.of("http://:password@somehere", "http://:******@somehere"),
                    Arguments.of("http://username:@somehere", "http://username:******@somehere"),
                    Arguments.of("http://something/somewhere", "http://something/somewhere")
            );
        }

        @ParameterizedTest
        @MethodSource("urls")
        void shouldMaskPasswordInGivenUrl(String input, String expectedMaskedUrl) {
            final UrlArgument simpleUrlArgument = new UrlArgument(input);

            assertThat(simpleUrlArgument.forDisplay()).isEqualTo(expectedMaskedUrl);
        }

        @Test
        void shouldNotMaskWithJustUserForSvnSshProtocol() {
            String normal = "svn+ssh://user@10.18.7.51:8153";
            UrlArgument url = new UrlArgument(normal);
            assertThat(url.forDisplay()).isEqualTo("svn+ssh://user@10.18.7.51:8153");
        }

        @Test
        void shouldNotMaskWithJustUserForSshProtocol() {
            String normal = "ssh://user@10.18.7.51:8153";
            UrlArgument url = new UrlArgument(normal);
            assertThat(url.forDisplay()).isEqualTo("ssh://user@10.18.7.51:8153");
        }

        @Test
        void shouldMaskWithUsernameAndPasswordForSshProtocol() {
            String normal = "ssh://user:password@10.18.7.51:8153";
            UrlArgument url = new UrlArgument(normal);
            assertThat(url.forDisplay()).isEqualTo("ssh://user:******@10.18.7.51:8153");
        }

        //BUG #5471
        @Test
        void shouldMaskAuthTokenInUrl() {
            UrlArgument url = new UrlArgument("https://9bf58jhrb32f29ad0c3983a65g594f1464jgf9a3@somewhere");
            assertThat(url.forDisplay()).isEqualTo("https://******@somewhere");
        }
    }

    @Nested
    class toString {
        @Test
        void shouldReturnValueForToString() {
            assertThat(argument.toString()).isEqualTo("http://username:******@somehere");
        }

        @Test
        void shouldNotChangeNormalURL() {
            String normal = "http://normal/foo/bar/baz?a=b&c=d#fragment";
            UrlArgument url = new UrlArgument(normal);
            assertThat(url.toString()).isEqualTo(normal);
        }

        @Test
        void shouldWorkWithSvnSshUrl() {
            String normal = "svn+ssh://user:password@10.18.7.51:8153";
            UrlArgument url = new UrlArgument(normal);
            assertThat(url.toString()).isEqualTo("svn+ssh://user:******@10.18.7.51:8153");
        }

        @Test
        void shouldIgnoreArgumentsThatAreNotRecognisedUrls() {
            String notAUrl = "C:\\foo\\bar\\baz";
            UrlArgument url = new UrlArgument(notAUrl);
            assertThat(url.toString()).isEqualTo(notAUrl);
        }
    }

    @Nested
    class Equals {
        @Test
        void shouldBeEqualBasedOnRawUrl() {
            UrlArgument url1 = new UrlArgument("svn+ssh://user:password@10.18.7.51:8153");
            UrlArgument url3 = new UrlArgument("svn+ssh://user:password@10.18.7.51:8153");
            assertThat(url1).isEqualTo(url3);
        }

        @Test
        void shouldBeEqualBasedOnRawUrl1() {
            UrlArgument url1 = new UrlArgument("svn+ssh://user:password@10.18.7.51:8153");
            UrlArgument url3 = new UrlArgument("svn+ssh://user:password@10.18.7.51:8153");
            assertThat(url1).isEqualTo(url3);
        }

        @Test
        void shouldBeEqualBasedOnRawUrlForHttpUrls() {
            UrlArgument url1 = new UrlArgument("http://user:password@10.18.7.51:8153");
            UrlArgument url2 = new UrlArgument("http://user:other@10.18.7.51:8153");
            UrlArgument url3 = new UrlArgument("http://user:password@10.18.7.51:8153");
            assertThat(url1).isEqualTo(url3);
            assertThat(url1).isNotEqualTo(url2);
        }

        @Test
        void shouldIgnoreTrailingSlashesOnURIs() {
            UrlArgument url1 = new UrlArgument("file:///not-exist/svn/trunk/");
            UrlArgument url2 = new UrlArgument("file:///not-exist/svn/trunk");
            assertThat(url1).isEqualTo(url2);
        }
    }

    @Nested
    class withoutCredentials {
        @Test
        void shouldRemoveCredentials() {
            final UrlArgument url = new UrlArgument("https://username:password@something/foo");

            assertThat(url.withoutCredentials()).isEqualTo("https://something/foo");
        }
    }

    //BUG #2973
    @Nested
    @TestInstance(PER_CLASS)
    class replaceSecretInfo {
        Stream<Arguments> urls() {
            return Stream.of(
                    Arguments.of("http://username:password@somewhere?name=bob", "http://username:******@somewhere?name=bob"),
                    Arguments.of("http://username:@somewhere/gocd/gocd.git", "http://username:******@somewhere/gocd/gocd.git"),
                    Arguments.of("http://somewhere:1234/gocd/gocd.git", "http://somewhere:1234/gocd/gocd.git")
            );
        }

        @Test
        void shouldReturnLineAsItIsIfLineIsBlank() {
            final UrlArgument urlArgument = new UrlArgument("http://username:password@somewhere?name=bob");
            assertThat(urlArgument.replaceSecretInfo("")).isEqualTo("");
        }

        @Test
        void shouldReturnLineAsItIsIfLineIsNull() {
            final UrlArgument urlArgument = new UrlArgument("http://username:password@somewhere?name=bob");
            assertThat(urlArgument.replaceSecretInfo(null)).isEqualTo(null);
        }

        @Test
        void shouldReturnLineAsItIsIfUrlIsBlank() {
            final UrlArgument urlArgument = new UrlArgument("");
            assertThat(urlArgument.replaceSecretInfo("some-content")).isEqualTo("some-content");
        }

        @ParameterizedTest
        @MethodSource("urls")
        void shouldMaskPasswordInGivenConsoleOutput(String input, String maskedUrl) {
            final UrlArgument urlArgument = new UrlArgument(input);
            final String originalLine = format("[go] Start updating repo at revision 08e7cc03 from %s", input);

            final String expectedLine = format("[go] Start updating repo at revision 08e7cc03 from %s", maskedUrl);
            assertThat(urlArgument.replaceSecretInfo(originalLine)).isEqualTo(expectedLine);
        }

        @Test
        void shouldMaskMultipleOccurrencesOfUserInfo() {
            final String url = "http://username:password@somewhere?name=bob";
            final String originalLine = format("[go] echoing same url twice: %s and %s", url, url);
            final UrlArgument urlArgument = new UrlArgument(url);

            final String actual = urlArgument.replaceSecretInfo(originalLine);

            final String maskedUrl = "http://username:******@somewhere?name=bob";
            final String expectedLine = format("[go] echoing same url twice: %s and %s", maskedUrl, maskedUrl);
            assertThat(actual).isEqualTo(expectedLine);
        }

        @Test
        void shouldReplaceAllThePasswordsInSvnInfo() {
            String output = "<?xml version=\"1.0\"?>\n"
                    + "<info>\n"
                    + "<entry\n"
                    + "   kind=\"dir\"\n"
                    + "   path=\".\"\n"
                    + "   revision=\"294\">\n"
                    + "<url>http://cce:password@10.18.3.171:8080/svn/connect4/trunk</url>\n"
                    + "<repository>\n"
                    + "<root>http://cce:password@10.18.3.171:8080/svn/connect4</root>\n"
                    + "<uuid>b7cc39fa-2f96-0d44-9079-2001927d4b22</uuid>\n"
                    + "</repository>\n"
                    + "<wc-info>\n"
                    + "<schedule>normal</schedule>\n"
                    + "<depth>infinity</depth>\n"
                    + "</wc-info>\n"
                    + "<commit\n"
                    + "   revision=\"294\">\n"
                    + "<author>cce</author>\n"
                    + "<date>2009-06-09T06:13:05.109375Z</date>\n"
                    + "</commit>\n"
                    + "</entry>\n"
                    + "</info>";

            UrlArgument url = new UrlArgument("http://cce:password@10.18.3.171:8080/svn/connect4/trunk");
            String result = url.replaceSecretInfo(output);
            assertThat(result).contains("<url>http://cce:******@10.18.3.171:8080/svn/connect4/trunk</url>");
            assertThat(result).contains("<root>http://cce:******@10.18.3.171:8080/svn/connect4</root>");
            assertThat(result).doesNotContain("cce:password");
        }
    }
}