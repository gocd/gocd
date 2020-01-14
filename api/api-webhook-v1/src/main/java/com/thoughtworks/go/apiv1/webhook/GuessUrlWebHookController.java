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

import spark.Request;

import java.util.List;

import static java.lang.String.format;

public interface GuessUrlWebHookController {

    String repoHostName(Request request) throws Exception;

    String repoFullName(Request request);

    default List<String> possibleUrls(Request request) throws Exception {
        String repoHostName = repoHostName(request);
        String repoFullName = repoFullName(request);
        return List.of(
            format("https://%s/%s", repoHostName, repoFullName),
            format("https://%s/%s/", repoHostName, repoFullName),
            format("https://%s/%s.git", repoHostName, repoFullName),
            format("https://%s/%s.git/", repoHostName, repoFullName),
            format("http://%s/%s", repoHostName, repoFullName),
            format("http://%s/%s/", repoHostName, repoFullName),
            format("http://%s/%s.git", repoHostName, repoFullName),
            format("http://%s/%s.git/", repoHostName, repoFullName),
            format("git://%s/%s", repoHostName, repoFullName),
            format("git://%s/%s/", repoHostName, repoFullName),
            format("git://%s/%s.git", repoHostName, repoFullName),
            format("git://%s/%s.git/", repoHostName, repoFullName),
            format("git@%s:%s", repoHostName, repoFullName),
            format("git@%s:%s/", repoHostName, repoFullName),
            format("git@%s:%s.git", repoHostName, repoFullName),
            format("git@%s:%s.git/", repoHostName, repoFullName),
            format("ssh://git@%s/%s", repoHostName, repoFullName),
            format("ssh://git@%s/%s/", repoHostName, repoFullName),
            format("ssh://git@%s/%s.git", repoHostName, repoFullName),
            format("ssh://git@%s/%s.git/", repoHostName, repoFullName)
        );
    }
}
