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

import static org.assertj.core.api.Assertions.assertThat;

class GitHubPushPayloadTest {
    @ParameterizedTest
    @FileSource(files = "/github-payload.json")
    void shouldParseRequestBodyOfContentTypeOfApplicationJson(String inputJSON) {
        GitHubPushPayload gitHubPushPayload = GsonTransformer.getInstance().fromJson(inputJSON, GitHubPushPayload.class);

        assertThat(gitHubPushPayload.getBranch()).isEqualTo("release");
        assertThat(gitHubPushPayload.getFullName()).isEqualTo("gocd/spaceship");
        assertThat(gitHubPushPayload.getHostname()).isEqualTo("github.com");
    }
}
