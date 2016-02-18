/*
 * Copyright 2016 ThoughtWorks, Inc.
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

package com.thoughtworks.go.agent.bootstrapper;

import com.thoughtworks.cruise.agent.common.launcher.AgentLaunchDescriptor;
import com.thoughtworks.cruise.agent.common.launcher.AgentLauncher;
import com.thoughtworks.go.agent.common.AgentBootstrapperArgs;
import com.thoughtworks.go.agent.common.util.Downloader;
import com.thoughtworks.go.util.ReflectionUtil;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;

public class AgentBootstrapperTest {

    @Before
    public void setUp() throws Exception {
        System.setProperty(AgentBootstrapper.WAIT_TIME_BEFORE_RELAUNCH_IN_MS, "0");
    }

    @After
    public void tearDown() throws Exception {
        System.clearProperty(AgentBootstrapper.WAIT_TIME_BEFORE_RELAUNCH_IN_MS);
        FileUtils.deleteQuietly(new File(Downloader.AGENT_LAUNCHER));
    }

    @Test
    public void shouldNotDieWhenCreationOfLauncherRaisesException() throws InterruptedException {
        final Semaphore waitForLauncherCreation = new Semaphore(1);
        waitForLauncherCreation.acquire();
        final boolean[] reLaunchWaitIsCalled = new boolean[1];
        final AgentBootstrapper bootstrapper = new AgentBootstrapper() {
            @Override
            void waitForRelaunchTime() {
                assertThat(waitTimeBeforeRelaunch, is(0));
                reLaunchWaitIsCalled[0] = true;
                super.waitForRelaunchTime();
            }

            @Override
            AgentLauncherCreator getLauncherCreator() {

                return new AgentLauncherCreator() {
                    public AgentLauncher createLauncher() {
                        try {
                            throw new RuntimeException("i bombed");
                        } finally {
                            if (waitForLauncherCreation.availablePermits() == 0) {
                                waitForLauncherCreation.release();
                            }
                        }
                    }

                    @Override
                    public void destroy() {
                    }
                };
            }
        };

        final AgentBootstrapper spyBootstrapper = stubJVMExit(bootstrapper);

        Thread stopLoopThd = new Thread(new Runnable() {
            public void run() {
                try {
                    waitForLauncherCreation.acquire();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                ReflectionUtil.setField(spyBootstrapper, "loop", false);
            }
        });
        stopLoopThd.start();
        try {
            spyBootstrapper.go(true, new AgentBootstrapperArgs(new URL("http://" + "ghost-name" + ":" + 3518 + "/go"), null, AgentBootstrapperArgs.SslMode.NONE));
            stopLoopThd.join();
        } catch (Exception e) {
            fail("should not have propagated exception thrown while creating launcher");
        }
        assertThat(reLaunchWaitIsCalled[0], is(true));
    }


    @Test(timeout = 10 * 1000)
    public void shouldNotRelaunchAgentLauncherWhenItReturnsAnIrrecoverableCode() throws InterruptedException {
        final boolean[] destroyCalled = new boolean[1];
        final AgentBootstrapper bootstrapper = new AgentBootstrapper(new AgentLauncherCreator() {
            public AgentLauncher createLauncher() {
                return new AgentLauncher() {
                    public int launch(AgentLaunchDescriptor descriptor) {
                        return AgentLauncher.IRRECOVERABLE_ERROR;
                    }

                };
            }

            @Override
            public void destroy() {
                destroyCalled[0] = true;
            }
        });

        final AgentBootstrapper spyBootstrapper = stubJVMExit(bootstrapper);

        try {
            spyBootstrapper.go(true, new AgentBootstrapperArgs(new URL("http://" + "ghost-name" + ":" + 3518 + "/go"), null, AgentBootstrapperArgs.SslMode.NONE));
        } catch (Exception e) {
            fail("should not have propagated exception thrown while invoking the launcher");
        }
        assertThat(destroyCalled[0], is(true));
    }

    @Test
    public void shouldNotDieWhenInvocationOfLauncherRaisesException_butCreationOfLauncherWentThrough() throws InterruptedException {
        final Semaphore waitForLauncherInvocation = new Semaphore(1);
        waitForLauncherInvocation.acquire();
        final AgentBootstrapper bootstrapper = new AgentBootstrapper() {
            @Override
            AgentLauncherCreator getLauncherCreator() {
                return new AgentLauncherCreator() {
                    public AgentLauncher createLauncher() {
                        return new AgentLauncher() {
                            public int launch(AgentLaunchDescriptor descriptor) {
                                try {
                                    throw new RuntimeException("fail!!! i say.");
                                } finally {
                                    if (waitForLauncherInvocation.availablePermits() == 0) {
                                        waitForLauncherInvocation.release();
                                    }
                                }
                            }

                        };
                    }

                    @Override
                    public void destroy() {
                    }
                };
            }
        };

        final AgentBootstrapper spyBootstrapper = stubJVMExit(bootstrapper);

        Thread stopLoopThd = new Thread(new Runnable() {
            public void run() {
                try {
                    waitForLauncherInvocation.acquire();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                ReflectionUtil.setField(spyBootstrapper, "loop", false);
            }
        });
        stopLoopThd.start();
        try {
            spyBootstrapper.go(true, new AgentBootstrapperArgs(new URL("http://" + "ghost-name" + ":" + 3518 + "/go"), null, AgentBootstrapperArgs.SslMode.NONE));
            stopLoopThd.join();
        } catch (Exception e) {
            fail("should not have propagated exception thrown while invoking the launcher");
        }
    }

    @Test
    public void shouldRetainStateAcrossLauncherInvocations() throws Exception {

        final Map expectedContext = new HashMap();
        AgentBootstrapper agentBootstrapper = new AgentBootstrapper() {
            @Override
            AgentLauncherCreator getLauncherCreator() {
                return new AgentLauncherCreator() {
                    public AgentLauncher createLauncher() {
                        return new AgentLauncher() {
                            public static final String COUNT = "count";

                            public int launch(AgentLaunchDescriptor descriptor) {

                                Map descriptorContext = descriptor.context();
                                incrementCount(descriptorContext);
                                incrementCount(expectedContext);
                                Integer expectedCount = (Integer) expectedContext.get(COUNT);
                                assertThat((Integer) descriptorContext.get(COUNT), is(expectedCount));
                                if (expectedCount > 3) {
                                    ((AgentBootstrapper) descriptor.getBootstrapper()).stopLooping();
                                }
                                return 0;
                            }

                            private void incrementCount(Map map) {
                                Integer currentInvocationCount = map.containsKey(COUNT) ? (Integer) map.get(COUNT) : 0;
                                map.put(COUNT, currentInvocationCount + 1);
                            }

                        };
                    }

                    @Override
                    public void destroy() {
                    }
                };
            }
        };
        AgentBootstrapper spy = stubJVMExit(agentBootstrapper);
        spy.go(true, new AgentBootstrapperArgs(new URL("http://" + "localhost" + ":" + 80 + "/go"), null, AgentBootstrapperArgs.SslMode.NONE));
    }

    private AgentBootstrapper stubJVMExit(AgentBootstrapper bootstrapper) {
        AgentBootstrapper spy = spy(bootstrapper);
        doNothing().when(spy).jvmExit(anyInt());
        return spy;
    }

}
