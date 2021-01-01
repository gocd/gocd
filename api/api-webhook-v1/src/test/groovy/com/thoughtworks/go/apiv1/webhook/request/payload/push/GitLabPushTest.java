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

package com.thoughtworks.go.apiv1.webhook.request.payload.push;

import com.thoughtworks.go.api.util.GsonTransformer;
import com.thoughtworks.go.junit5.FileSource;
import org.junit.jupiter.params.ParameterizedTest;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GitLabPushTest {
    @ParameterizedTest
    @FileSource(files = "/gitlab-push.json")
    void deserializes(String json) {
        final GitLabPush payload = GsonTransformer.getInstance().fromJson(json, GitLabPush.class);

        assertEquals("release", payload.branch());
        assertEquals("gocd/spaceship", payload.fullName());
        assertEquals("gitlab.example.com", payload.hostname());
    }

    @ParameterizedTest
    @FileSource(files = "/gitlab-push.json")
    void guessesRepositoryUrls(String json) {
        final GitLabPush payload = GsonTransformer.getInstance().fromJson(json, GitLabPush.class);

        assertEquals(Set.of("https://gitlab.example.com/gocd/spaceship",
                "https://gitlab.example.com/gocd/spaceship/",
                "https://gitlab.example.com/gocd/spaceship.git",
                "https://gitlab.example.com/gocd/spaceship.git/",
                "http://gitlab.example.com/gocd/spaceship",
                "http://gitlab.example.com/gocd/spaceship/",
                "http://gitlab.example.com/gocd/spaceship.git",
                "http://gitlab.example.com/gocd/spaceship.git/",
                "git://gitlab.example.com/gocd/spaceship",
                "git://gitlab.example.com/gocd/spaceship/",
                "git://gitlab.example.com/gocd/spaceship.git",
                "git://gitlab.example.com/gocd/spaceship.git/",
                "git@gitlab.example.com:gocd/spaceship",
                "git@gitlab.example.com:gocd/spaceship/",
                "git@gitlab.example.com:gocd/spaceship.git",
                "git@gitlab.example.com:gocd/spaceship.git/",
                "ssh://git@gitlab.example.com/gocd/spaceship",
                "ssh://git@gitlab.example.com/gocd/spaceship/",
                "ssh://git@gitlab.example.com/gocd/spaceship.git",
                "ssh://git@gitlab.example.com/gocd/spaceship.git/",
                "ssh://gitlab.example.com/gocd/spaceship",
                "ssh://gitlab.example.com/gocd/spaceship/",
                "ssh://gitlab.example.com/gocd/spaceship.git",
                "ssh://gitlab.example.com/gocd/spaceship.git/",
                "gitlab@gitlab.example.com:gocd/spaceship",
                "gitlab@gitlab.example.com:gocd/spaceship/",
                "gitlab@gitlab.example.com:gocd/spaceship.git",
                "gitlab@gitlab.example.com:gocd/spaceship.git/"), payload.repoUrls());
    }

}
