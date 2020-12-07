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

package com.thoughtworks.go.apiv1.webhook.request.payload.push;

import com.thoughtworks.go.api.util.GsonTransformer;
import com.thoughtworks.go.junit5.FileSource;
import org.junit.jupiter.params.ParameterizedTest;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BitbucketPushTest {
    @ParameterizedTest
    @FileSource(files = "/bitbucket-push.json")
    void deserializes(String json) {
        final BitbucketPush payload = GsonTransformer.getInstance().fromJson(json, BitbucketPush.class);

        assertEquals("release", payload.branch());
        assertEquals("gocd/spaceship", payload.fullName());
        assertEquals("bitbucket.org", payload.hostname());
        assertEquals("git", payload.scmType());
    }

    @ParameterizedTest
    @FileSource(files = "/bitbucket-push.json")
    void guessesRepositoryUrls(String json) {
        final BitbucketPush payload = GsonTransformer.getInstance().fromJson(json, BitbucketPush.class);

        assertEquals(Set.of("https://bitbucket.org/gocd/spaceship",
                "https://bitbucket.org/gocd/spaceship/",
                "https://bitbucket.org/gocd/spaceship.git",
                "https://bitbucket.org/gocd/spaceship.git/",
                "http://bitbucket.org/gocd/spaceship",
                "http://bitbucket.org/gocd/spaceship/",
                "http://bitbucket.org/gocd/spaceship.git",
                "http://bitbucket.org/gocd/spaceship.git/",
                "git://bitbucket.org/gocd/spaceship",
                "git://bitbucket.org/gocd/spaceship/",
                "git://bitbucket.org/gocd/spaceship.git",
                "git://bitbucket.org/gocd/spaceship.git/",
                "git@bitbucket.org:gocd/spaceship",
                "git@bitbucket.org:gocd/spaceship/",
                "git@bitbucket.org:gocd/spaceship.git",
                "git@bitbucket.org:gocd/spaceship.git/",
                "ssh://git@bitbucket.org/gocd/spaceship",
                "ssh://git@bitbucket.org/gocd/spaceship/",
                "ssh://git@bitbucket.org/gocd/spaceship.git",
                "ssh://git@bitbucket.org/gocd/spaceship.git/",
                "ssh://bitbucket.org/gocd/spaceship",
                "ssh://bitbucket.org/gocd/spaceship/",
                "ssh://bitbucket.org/gocd/spaceship.git",
                "ssh://bitbucket.org/gocd/spaceship.git/"), payload.repoUrls());
    }

}
