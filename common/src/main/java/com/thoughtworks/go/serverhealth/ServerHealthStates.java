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
package com.thoughtworks.go.serverhealth;

import java.util.List;

import com.thoughtworks.go.domain.BaseCollection;

/**
 * @understands a collection of server health states
 */
public class ServerHealthStates extends BaseCollection<ServerHealthState> {
    public ServerHealthStates(List<ServerHealthState> serverHealthStates) {
        super(serverHealthStates);
    }

    public ServerHealthStates() {
    }

    public ServerHealthStates(ServerHealthState... states) {
        super(states);
    }

    public int errorCount() {
        int errors = 0;
        for (ServerHealthState serverHealthState : this) {
            if (!serverHealthState.isSuccess()) {
                errors++;
            }
        }
        return errors;
    }

    public int warningCount() {
        int warnings = 0;
        for (ServerHealthState serverHealthState : this) {
            if (serverHealthState.isWarning()) {
                warnings++;
            }
        }
        return warnings;
    }

    public boolean isRealSuccess() {
        return errorCount() == 0 && warningCount() == 0;
    }
}
