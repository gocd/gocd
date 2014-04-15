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

package com.thoughtworks.go.metrics.domain.probes;

import com.thoughtworks.go.metrics.domain.context.Context;
import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Counter;

public class MessageQueueCounterProbe implements Probe {
    private Counter pendingItems;

    public MessageQueueCounterProbe(ProbeType type, String scope) {
        pendingItems = Metrics.newCounter(MessageQueueCounterProbe.class, type.getName(), scope);
    }

    public Context begin() {
        pendingItems.inc();
        return null;
    }

    public void end(Context context) {
        pendingItems.dec();
    }

    Counter getPendingItems() {
        return pendingItems;
    }
}

