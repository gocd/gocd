/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.plugin.infra.plugininfo;

import java.util.ArrayList;
import java.util.List;

public class PluginStatus {
    private State state;
    private List<String> messages = new ArrayList<>();
    private Exception rootCauseIfInvalid;

    public enum State {
        ACTIVE,
        DISABLED,
        INVALID;
    }

    protected PluginStatus(State state) {
        this.state = state;
    }

    public boolean isInvalid() {
        return state == State.INVALID;
    }

    public List<String> getMessages() {
        return messages;
    }

    protected PluginStatus setMessages(List<String> messages, Exception rootCause) {
        this.messages = messages;
        this.rootCauseIfInvalid = rootCause;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        PluginStatus that = (PluginStatus) o;

        if (state != that.state) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return state != null ? state.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "PluginStatus{" +
                "state=" + state +
                ", messages=" + messages +
                ", rootCauseIfInvalid=" + rootCauseIfInvalid +
                '}';
    }
}
