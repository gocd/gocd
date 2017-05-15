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

package com.thoughtworks.go.timer;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.scheduling.support.MethodInvokingRunnable;

import java.util.TimerTask;

public class MethodInvokingTimerTaskFactoryBean extends MethodInvokingRunnable implements FactoryBean<TimerTask> {

    private TimerTask timerTask;

    public MethodInvokingTimerTaskFactoryBean() {
    }

    public void afterPropertiesSet() throws ClassNotFoundException, NoSuchMethodException {
        super.afterPropertiesSet();
        this.timerTask = new DelegatingTimerTask(this);
    }

    public TimerTask getObject() {
        return this.timerTask;
    }

    public Class<TimerTask> getObjectType() {
        return TimerTask.class;
    }

    public boolean isSingleton() {
        return true;
    }
}
