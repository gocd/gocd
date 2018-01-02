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

package com.thoughtworks.go.timer;

import com.thoughtworks.go.util.SystemEnvironment;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.context.ApplicationContext;

import java.util.TimerTask;

import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class ModeAwareMethodInvokingTimerTaskFactoryBeanTest {
    @Mock
    private SystemEnvironment systemEnvironment;
    @Mock
    private ApplicationContext applicationContext;
    @Mock
    private TimerTask targetTimeTask;

    private ModeAwareMethodInvokingTimerTaskFactoryBean bean;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        when(applicationContext.getBean(SystemEnvironment.class)).thenReturn(systemEnvironment);
        bean = spy(new ModeAwareMethodInvokingTimerTaskFactoryBean());
        bean.setApplicationContext(applicationContext);
    }

    @Test
    public void shouldRunTaskWhenServerIsActive() throws Exception {
        doReturn(targetTimeTask).when(bean).getTargetTimerTask();
        when(systemEnvironment.isServerActive()).thenReturn(true);

        TimerTask task = bean.getObject();
        task.run();

        verify(targetTimeTask).run();
    }

    @Test
    public void shouldNotRunTaskWhenServerIsNotActive() throws Exception {
        doReturn(targetTimeTask).when(bean).getTargetTimerTask();
        when(systemEnvironment.isServerActive()).thenReturn(false);

        TimerTask task = bean.getObject();
        task.run();

        verify(targetTimeTask, never()).run();
    }
}
