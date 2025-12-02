/*
 * Copyright Thoughtworks, Inc.
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
package com.thoughtworks.go.agent.statusapi;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.HttpURLConnection;

@Component
public class IsConnectedToServerV1 implements HttpHandler {

    private final AgentHealthHolder agentHealthHolder;

    @Autowired
    public IsConnectedToServerV1(AgentHealthHolder agentHealthHolder) {
        this.agentHealthHolder = agentHealthHolder;
    }

    @Override
    public Response response() {
        return isPassed() ? new Response(HttpURLConnection.HTTP_OK, "OK!") : new Response(HttpURLConnection.HTTP_UNAVAILABLE, "Bad!");
    }

    protected boolean isPassed() {
        return !agentHealthHolder.hasLostContact();
    }
}
