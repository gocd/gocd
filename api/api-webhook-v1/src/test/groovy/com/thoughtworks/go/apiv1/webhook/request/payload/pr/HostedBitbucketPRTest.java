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

class HostedBitbucketPRTest {
    @ParameterizedTest
    @FileSource(files = "/hosted-bitbucket-pr.json")
    void deserializes(String json) {
        final HostedBitbucketPR payload = GsonTransformer.getInstance().fromJson(json, HostedBitbucketPR.class);

        assertEquals("#9", payload.identifier());
        assertEquals(OPEN, payload.state());
        assertEquals("gocd/spaceship", payload.fullName());
        assertEquals("bitbucket-server", payload.hostname());
        assertEquals("git", payload.scmType());
    }
}