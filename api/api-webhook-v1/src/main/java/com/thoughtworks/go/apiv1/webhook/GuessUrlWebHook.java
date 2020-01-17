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

package com.thoughtworks.go.apiv1.webhook;

import java.util.List;

import static java.lang.String.format;

public interface GuessUrlWebHook {

    default List<String> possibleUrls(String hostname, String repoFullName) {
        return List.of(
            format("https://%s/%s", hostname, repoFullName),
            format("https://%s/%s/", hostname, repoFullName),
            format("https://%s/%s.git", hostname, repoFullName),
            format("https://%s/%s.git/", hostname, repoFullName),
            format("http://%s/%s", hostname, repoFullName),
            format("http://%s/%s/", hostname, repoFullName),
            format("http://%s/%s.git", hostname, repoFullName),
            format("http://%s/%s.git/", hostname, repoFullName),
            format("git://%s/%s", hostname, repoFullName),
            format("git://%s/%s/", hostname, repoFullName),
            format("git://%s/%s.git", hostname, repoFullName),
            format("git://%s/%s.git/", hostname, repoFullName),
            format("git@%s:%s", hostname, repoFullName),
            format("git@%s:%s/", hostname, repoFullName),
            format("git@%s:%s.git", hostname, repoFullName),
            format("git@%s:%s.git/", hostname, repoFullName),
            format("ssh://git@%s/%s", hostname, repoFullName),
            format("ssh://git@%s/%s/", hostname, repoFullName),
            format("ssh://git@%s/%s.git", hostname, repoFullName),
            format("ssh://git@%s/%s.git/", hostname, repoFullName),
            format("ssh://%s/%s", hostname, repoFullName),
            format("ssh://%s/%s/", hostname, repoFullName),
            format("ssh://%s/%s.git", hostname, repoFullName),
            format("ssh://%s/%s.git/", hostname, repoFullName)
        );
    }
}
