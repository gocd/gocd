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

package com.thoughtworks.go.agent.statusapi;

import com.thoughtworks.go.util.Clock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AgentHealthHolder {
    private final Clock clock;
    private final long pingInterval;
    private long lastPingTime;

    @Autowired
    public AgentHealthHolder(Clock clock, @Value("${agent.ping.interval}") long pingInterval) {
        this.clock = clock;
        this.pingInterval = pingInterval;
    }

    public void pingSuccess() {
        this.lastPingTime = clock.currentTimeMillis();
    }

    // assume disconnected if unable to ping for 2 intervals
    public boolean hasLostContact() {
        return (clock.currentTimeMillis() - lastPingTime) >= (pingInterval * 2);
    }

}

