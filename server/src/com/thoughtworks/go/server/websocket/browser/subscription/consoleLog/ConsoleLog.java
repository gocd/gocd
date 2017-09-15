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

package com.thoughtworks.go.server.websocket.browser.subscription.consoleLog;

import com.google.gson.annotations.Expose;
import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.server.websocket.browser.BrowserWebSocket;
import com.thoughtworks.go.server.websocket.browser.subscription.WebSocketSubscriptionManager;
import com.thoughtworks.go.server.websocket.browser.subscription.SubscriptionMessage;

public class ConsoleLog extends SubscriptionMessage {
    @Expose
    private final JobIdentifier jobIdentifier;

    @Expose
    private final long startLine;

    public ConsoleLog(JobIdentifier jobIdentifier, long startLine) {
        this.jobIdentifier = jobIdentifier;
        this.startLine = startLine;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ConsoleLog)) return false;

        ConsoleLog that = (ConsoleLog) o;

        if (startLine != that.startLine) return false;
        return jobIdentifier != null ? jobIdentifier.equals(that.jobIdentifier) : that.jobIdentifier == null;
    }

    @Override
    public int hashCode() {
        int result = jobIdentifier != null ? jobIdentifier.hashCode() : 0;
        result = 31 * result + (int) (startLine ^ (startLine >>> 32));
        return result;
    }


    @Override
    public String toString() {
        return "ConsoleLog{" +
                "jobIdentifier=" + jobIdentifier +
                ", startLine=" + startLine +
                '}';
    }

    @Override
    public void subscribe(WebSocketSubscriptionManager webSocketSubscriptionManager, BrowserWebSocket webSocket) throws Exception {
        webSocketSubscriptionManager.getSubscription().start(this, webSocket);
    }

    @Override
    public boolean isAuthorized(WebSocketSubscriptionManager webSocketSubscriptionManager, BrowserWebSocket webSocket) {
        return webSocketSubscriptionManager.getSubscription().isAuthorized(this, webSocket);
    }

    public JobIdentifier getJobIdentifier() {
        return jobIdentifier;
    }

    public long getStartLine() {
        return startLine;
    }
}
