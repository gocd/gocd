/*************************GO-LICENSE-START*********************************
 * Copyright 2015 ThoughtWorks, Inc.
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

package com.thoughtworks.go.timer;

import org.apache.log4j.Logger;
import org.springframework.scheduling.timer.MethodInvokingTimerTaskFactoryBean;

import java.util.TimerTask;

public class ModeAwareMethodInvokingTimerTaskFactoryBean extends MethodInvokingTimerTaskFactoryBean {
    private static final Logger LOGGER = Logger.getLogger("GO_MODE_AWARE_TIMER");

    @Override
    public TimerTask getObject() {
        final TimerTask originalTimerTask = super.getObject();

        return new TimerTask() {
            @Override
            public void run() {
                LOGGER.info("Running: " + getTargetClass() + "#" + getTargetMethod());
                originalTimerTask.run();
            }
        };
    }

    @Override
    public Class<TimerTask> getObjectType() {
        return super.getObjectType();
    }

    @Override
    public boolean isSingleton() {
        return super.isSingleton();
    }
}
