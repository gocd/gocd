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
import com.thoughtworks.go.metrics.domain.context.GoTimerContext;
import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Timer;

public class TimerProbe implements Probe {
    private Timer timer;

    public TimerProbe(ProbeType type, String scope) {
        timer = Metrics.newTimer(this.getClass(), type.getName(), scope);
    }

    public Timer getTimer() {
        return timer;
    }

    @Override
    public Context begin() {
        return new GoTimerContext(timer.time());
    }

    @Override
    public void end(Context context) {
        if (context == null || !(context instanceof GoTimerContext)) {
            return;
        }
        ((GoTimerContext) context).stop();
    }
}

