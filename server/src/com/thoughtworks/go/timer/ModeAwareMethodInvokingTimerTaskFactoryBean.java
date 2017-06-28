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

import com.thoughtworks.go.util.SystemEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.scheduling.timer.MethodInvokingTimerTaskFactoryBean;

import java.util.TimerTask;

public class ModeAwareMethodInvokingTimerTaskFactoryBean extends MethodInvokingTimerTaskFactoryBean implements ApplicationContextAware {
    private static final Logger LOGGER = LoggerFactory.getLogger("GO_MODE_AWARE_TIMER");
    private ApplicationContext applicationContext;

    @Override
    public TimerTask getObject() {
        final TimerTask originalTimerTask = getTargetTimerTask();
        final SystemEnvironment systemEnvironment = applicationContext.getBean(SystemEnvironment.class);
        return new TimerTask() {
            @Override
            public void run() {
                if (systemEnvironment.isServerActive()) {
                    LOGGER.debug("Server is in active state, Running: {}#{}", getTargetClass(), getTargetMethod());
                    originalTimerTask.run();
                } else {
                    LOGGER.debug("Server is not in active state, Skipping: {}#{}", getTargetClass(), getTargetMethod());
                }
            }
        };
    }

    @Deprecated
        // TODO: KETAN Inline this?
    TimerTask getTargetTimerTask() {
        return super.getObject();
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
