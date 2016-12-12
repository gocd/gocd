/*
 * Copyright 2015 ThoughtWorks, Inc.
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

package com.thoughtworks.go.server.websocket;

import com.thoughtworks.go.websocket.Action;
import com.thoughtworks.go.websocket.Message;

import java.util.ArrayList;
import java.util.List;

public class AgentStub implements Agent {
    public List<Message> messages = new ArrayList<>();
    private boolean ignoreAcknowledgements = true;

    @Override
    public void send(Message msg) {
        // Ignore acknowledgements
        if(ignoreAcknowledgements && Action.acknowledge.equals(msg.getAction())) {
            return;
        }
        messages.add(msg);
    }

    public void setIgnoreAcknowledgements(boolean ignoreAcknowledgements) {
        this.ignoreAcknowledgements = ignoreAcknowledgements;
    }
}
