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

package com.thoughtworks.go.apiv1.webhook.request.payload.pr;

import com.thoughtworks.go.api.util.GsonTransformer;
import com.thoughtworks.go.junit5.FileSource;
import org.junit.jupiter.params.ParameterizedTest;

import static com.thoughtworks.go.apiv1.webhook.request.payload.pr.PrPayload.State.OPEN;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class GitLabPRTest {
    @ParameterizedTest
    @FileSource(files = "/gitlab-pr.json")
    void deserializes(String json) {
        final GitLabPR payload = GsonTransformer.getInstance().fromJson(json, GitLabPR.class);

        assertEquals("gocd/spaceship", payload.fullName());
        assertEquals("gitlab.example.com", payload.hostname());
        assertEquals(OPEN, payload.state());
        assertEquals("#9", payload.identifier());
    }
}
