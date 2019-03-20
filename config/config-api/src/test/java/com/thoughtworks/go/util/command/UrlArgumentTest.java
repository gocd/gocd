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

import com.thoughtworks.go.config.SecretParam;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UrlArgumentTest {
    private static final String URL_WITH_PASSWORD = "http://username:password@somehere";
    private static final String URL_WITH_SECRET_PARAMS = "https://${SECRET[secret-config-id][username]}:${SECRET[secret-config-id][password]}@gocd.org/foo/bar.git";

    private CommandArgument argument;

    @BeforeEach
    void setup() {
        argument = new UrlArgument(URL_WITH_PASSWORD);
    }

    @Nested
    class rawUrl {
        @Test
        void shouldReturnStringValue() {
            assertThat(argument.originalArgument()).isEqualTo(URL_WITH_PASSWORD);
        }
    }

    @Nested
    class forDisplay {
        @Test
        void shouldReturnStringValueForReporting() {
            assertThat(argument.forDisplay()).isEqualTo("http://username:******@somehere");
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

        @Test
        void shouldMaskSecretParamsInUrl() {
            final UrlArgument argument = new UrlArgument(URL_WITH_SECRET_PARAMS);

            assertThat(argument.forDisplay()).isEqualTo("https://******:******@gocd.org/foo/bar.git");
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

        @Test
        void shouldMaskSecretParamsInUrl() {
            final UrlArgument argument = new UrlArgument(URL_WITH_SECRET_PARAMS);

            assertThat(argument.toString()).isEqualTo("https://******:******@gocd.org/foo/bar.git");
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
        void shouldBeEqualBasedOnRawUrlWhenHasSecretParams() {
            UrlArgument url1 = new UrlArgument(URL_WITH_SECRET_PARAMS);
            UrlArgument url2 = new UrlArgument(URL_WITH_SECRET_PARAMS);
            assertThat(url1).isEqualTo(url2);
        }

        @Test
        void shouldIgnoreTrailingSlashesOnURIs() {
            UrlArgument url1 = new UrlArgument("file:///not-exist/svn/trunk/");
            UrlArgument url2 = new UrlArgument("file:///not-exist/svn/trunk");
            assertThat(url1).isEqualTo(url2);
        }
    }

    @Nested
    class hostInfoForDisplay {
        @Test
        void shouldMaskPasswordInHgUrlWithBranch() {
            UrlArgument url = new UrlArgument("http://cce:password@10.18.3.171:8080/hg/connect4/trunk#foo");
            assertThat(url.hostInfoForDisplay()).isEqualTo("http://cce:******@10.18.3.171:8080");
        }

        @Test
        void shouldMaskSecretParamInHostInfo() {
            UrlArgument url = new UrlArgument("https://${SECRET[secret-config-id][username]}:${SECRET[secret-config-id][password]}@10.18.3.171:8080/foo/bar.git");
            assertThat(url.hostInfoForDisplay()).isEqualTo("https://******:******@10.18.3.171:8080");
        }
    }

    @Nested
    class hostInfoForCommandline {
        @Test
        void shouldReturnPasswordInUrl() {
            UrlArgument url = new UrlArgument("http://cce:password@10.18.3.171:8080/hg/connect4/trunk#foo");
            assertThat(url.hostInfoForCommandline()).isEqualTo("http://cce:password@10.18.3.171:8080");
        }

        @Test
        void shouldSubstituteSecretParams() {
            UrlArgument url = new UrlArgument("https://${SECRET[secret-config-id][username]}:${SECRET[secret-config-id][password]}@10.18.3.171:8080/foo/bar.git");

            url.getSecretParams().findFirst("username").ifPresent(secretParam -> secretParam.setValue("bob"));
            url.getSecretParams().findFirst("password").ifPresent(secretParam -> secretParam.setValue("badger"));

            final String actual = url.hostInfoForCommandline();

            assertThat(actual).isEqualTo("https://bob:badger@10.18.3.171:8080");
        }
    }

    @Nested
    class hasSecretParams {
        @Test
        void shouldReturnTrueIfHasSecretParams() {
            final UrlArgument url = new UrlArgument(URL_WITH_SECRET_PARAMS);

            assertThat(url.hasSecretParams()).isTrue();
        }

        @Test
        void shouldReturnFalseIfHasNoSecretParams() {
            final UrlArgument url = new UrlArgument("https://username:password@gocd.org/foo");

            assertThat(url.hasSecretParams()).isFalse();
        }
    }

    @Nested
    class getSecretParams {
        @Test
        void shouldReturnSecretParamsIfHasSecretParams() {
            final UrlArgument url = new UrlArgument(URL_WITH_SECRET_PARAMS);

            assertThat(url.getSecretParams()).hasSize(2)
                    .contains(
                            new SecretParam("secret-config-id", "username"),
                            new SecretParam("secret-config-id", "password")
                    );
        }

        @Test
        void shouldReturnEmptyIfHasNoSecretParams() {
            final UrlArgument url = new UrlArgument("https://username:password@gocd.org/foo");

            assertThat(url.getSecretParams()).hasSize(0);
        }
    }

    @Nested
    class forCommandLine {
        @Test
        void shouldReturnAppropriateUrl() {
            final UrlArgument url = new UrlArgument("https://username:password@something/foo");

            assertThat(url.forCommandLine()).isEqualTo("https://username:password@something/foo");
        }

        @Test
        void shouldSubstituteSecretParamValue() {
            final UrlArgument url = new UrlArgument(URL_WITH_SECRET_PARAMS);

            url.getSecretParams().findFirst("username").ifPresent(secretParam -> secretParam.setValue("bob"));
            url.getSecretParams().findFirst("password").ifPresent(secretParam -> secretParam.setValue("some-password"));

            assertThat(url.forCommandLine()).isEqualTo("https://bob:some-password@gocd.org/foo/bar.git");
        }
    }

    @Nested
    class withoutCredentials {
        @Test
        void shouldRemoveCredentials() {
            final UrlArgument url = new UrlArgument("https://username:password@something/foo");

            assertThat(url.withoutCredentials()).isEqualTo("https://something/foo");
        }

        @Test
        void shouldRemoveSecretParams() {
            final UrlArgument url = new UrlArgument(URL_WITH_SECRET_PARAMS);

            assertThat(url.withoutCredentials()).isEqualTo("https://gocd.org/foo/bar.git");
        }
    }

    //BUG #2973
    @Nested
    class replaceSecretInfo {
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
