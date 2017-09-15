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

package com.thoughtworks.go.server.websocket.browser.subscription;

import com.google.gson.annotations.Expose;
import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.server.websocket.browser.BrowserWebSocket;

public class JobStatusChange extends SubscriptionMessage {
    @Expose
    private final JobIdentifier jobIdentifier;

    public JobStatusChange(JobIdentifier jobIdentifier) {
        this.jobIdentifier = jobIdentifier;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof com.thoughtworks.go.server.websocket.browser.subscription.JobStatusChange)) return false;

        com.thoughtworks.go.server.websocket.browser.subscription.JobStatusChange that = (com.thoughtworks.go.server.websocket.browser.subscription.JobStatusChange) o;

        return jobIdentifier != null ? jobIdentifier.equals(that.jobIdentifier) : that.jobIdentifier == null;
    }

    @Override
    public int hashCode() {
        return jobIdentifier != null ? jobIdentifier.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "JobStatusChange{" +
                "jobIdentifier=" + jobIdentifier +
                '}';
    }

    @Override
    public void subscribe(WebSocketSubscriptionManager webSocketSubscriptionManager, BrowserWebSocket webSocket) {

    }

    @Override
    public boolean isAuthorized(WebSocketSubscriptionManager webSocketSubscriptionManager, BrowserWebSocket webSocket) {
        return false;
    }
}
