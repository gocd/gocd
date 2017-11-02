/*
 * Copyright 2017 ThoughtWorks, Inc.
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

package com.thoughtworks.go.server.websocket.browser.subscription.jobstatuschange;

import com.thoughtworks.go.server.service.RestfulService;
import com.thoughtworks.go.server.service.SecurityService;
import com.thoughtworks.go.server.websocket.browser.BrowserWebSocket;
import com.thoughtworks.go.server.websocket.browser.subscription.SubscriptionMessage;
import com.thoughtworks.go.server.websocket.browser.subscription.WebSocketSubscriptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Component
public class JobStatusChangeSubscription implements WebSocketSubscriptionHandler<JobStatusChange> {
    private final SecurityService securityService;
    private RestfulService restfulService;

    @Autowired
    public JobStatusChangeSubscription(SecurityService securityService, RestfulService restfulService) {
        this.securityService = securityService;
        this.restfulService = restfulService;
    }

    @Override
    public void start(JobStatusChange message, BrowserWebSocket socket) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isAuthorized(SubscriptionMessage message, BrowserWebSocket securityService) {
        return true;
    }

    @Override
    public Class<JobStatusChange> getType() {
        return JobStatusChange.class;
    }
}
