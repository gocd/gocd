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

package com.thoughtworks.go.apiv1.webhook.request.payload.pr;

import com.google.gson.annotations.SerializedName;
import com.thoughtworks.go.apiv1.webhook.request.json.HostedBitbucketRepository;

import static com.thoughtworks.go.apiv1.webhook.request.payload.pr.PrPayload.State.CLOSED;
import static com.thoughtworks.go.apiv1.webhook.request.payload.pr.PrPayload.State.OPEN;
import static java.lang.String.format;

@SuppressWarnings({"unused", "RedundantSuppression"})
public class HostedBitbucketPR implements PrPayload {
    @SerializedName("pullRequest")
    private PR pr;

    @Override
    public String identifier() {
        return format("#%s", pr.number);
    }

    @Override
    public State state() {
        return pr.open ? OPEN : CLOSED;
    }

    @Override
    public String hostname() {
        return pr.dest.repository.hostname();
    }

    @Override
    public String fullName() {
        return pr.dest.repository.fullName();
    }

    private static class PR {
        @SerializedName("toRef")
        private Ref dest;

        private boolean open;

        @SerializedName("id")
        private int number;
    }

    private static class Ref {
        private HostedBitbucketRepository repository;
    }
}
