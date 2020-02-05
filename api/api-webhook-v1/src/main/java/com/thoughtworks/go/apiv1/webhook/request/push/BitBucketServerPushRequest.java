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

package com.thoughtworks.go.apiv1.webhook.request.push;

import com.thoughtworks.go.apiv1.webhook.request.WebhookRequest;
import com.thoughtworks.go.apiv1.webhook.request.mixins.GuessUrlWebHook;
import com.thoughtworks.go.apiv1.webhook.request.mixins.bitbucketserver.BitBucketServerAuth;
import com.thoughtworks.go.apiv1.webhook.request.mixins.bitbucketserver.BitBucketServerEvents;
import com.thoughtworks.go.apiv1.webhook.request.payload.push.BitBucketServerPushPayload;
import spark.Request;

import java.util.List;

public class BitBucketServerPushRequest extends WebhookRequest<BitBucketServerPushPayload>
        implements BitBucketServerEvents, BitBucketServerAuth, GuessUrlWebHook {

    public BitBucketServerPushRequest(Request request) {
        super(request);
    }

    @Override
    public String[] allowedEvents() {
        return new String[]{"repo:refs_changed", "diagnostics:ping"};
    }

    @Override
    public boolean exemptFromAuth() {
        boolean skip = "diagnostics:ping".equals(event());
        if (skip) {
            LOGGER.debug("[WebHook] Skipping authentication for ping request!");
        }
        return skip;
    }

    @Override
    public String scmType() {
        return getPayload().getScmType();
    }

    @Override
    public List<String> webhookUrls() {
        return getPayload().getCloneUrls();
    }
}
