/*
 * Copyright Thoughtworks, Inc.
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import static com.thoughtworks.go.agent.bootstrapper.AgentBootstrapper.returnDesc;
import static com.thoughtworks.go.util.TestUtils.doInterruptiblyQuietlyRethrowInterrupt;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;

public class AgentBootstrapperTest {

    private URL serverUrl;

    @BeforeEach
    public void setUp() throws Exception {
        System.setProperty(AgentBootstrapper.WAIT_TIME_BEFORE_RELAUNCH_IN_MS, "0");
        serverUrl = URI.create("http://ghost-name:3518/go").toURL();
    }

    @AfterEach
    public void tearDown() throws IOException {
        System.clearProperty(AgentBootstrapper.WAIT_TIME_BEFORE_RELAUNCH_IN_MS);
        Files.deleteIfExists(new File(Downloader.AGENT_LAUNCHER).toPath());
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    public void shouldNotDieWhenCreationOfLauncherRaisesException() throws InterruptedException {
        final Semaphore waitForLauncherCreation = new Semaphore(1);
        waitForLauncherCreation.acquire();
        final boolean[] reLaunchWaitIsCalled = new boolean[1];
        final AgentBootstrapper bootstrapper = new AgentBootstrapper() {
            @Override
            void waitForRelaunchTime() {
                assertThat(waitTimeBeforeRelaunch).isEqualTo(0);
                reLaunchWaitIsCalled[0] = true;
                super.waitForRelaunchTime();
            }

            @Override
            AgentLauncherCreator getLauncherCreator() {
                return new AgentLauncherCreator() {
                    @Override
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
                    public void close() {
                    }
                };
            }
        };

        final AgentBootstrapper spyBootstrapper = stubJVMExit(bootstrapper);

        Thread stopLoopThd = new Thread(() -> {
            doInterruptiblyQuietlyRethrowInterrupt(waitForLauncherCreation::acquire);
            spyBootstrapper.stopLooping();
        });
        stopLoopThd.setDaemon(true);
        stopLoopThd.start();
        try {
            spyBootstrapper.go(new AgentBootstrapperArgs().setServerUrl(serverUrl).setRootCertFile(null).setSslVerificationMode(AgentBootstrapperArgs.SslMode.NONE));
            stopLoopThd.join();
        } catch (Exception e) {
            stopLoopThd.interrupt();
            fail("should not have propagated exception thrown while creating launcher", e);
        }
        assertThat(reLaunchWaitIsCalled[0]).isTrue();
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    public void shouldNotRelaunchAgentLauncherWhenItReturnsAnIrrecoverableCode() {
        final boolean[] destroyCalled = new boolean[1];
        final AgentBootstrapper bootstrapper = new AgentBootstrapper() {

            @Override
            AgentLauncherCreator getLauncherCreator() {
                return new AgentLauncherCreator() {
                    @Override
                    public AgentLauncher createLauncher() {
                        return descriptor -> AgentLauncher.IRRECOVERABLE_ERROR;
                    }

                    @Override
                    public void close() {
                        destroyCalled[0] = true;
                    }
                };
            }
        };

        final AgentBootstrapper spyBootstrapper = stubJVMExit(bootstrapper);

        try {
            spyBootstrapper.go(new AgentBootstrapperArgs().setServerUrl(serverUrl).setRootCertFile(null).setSslVerificationMode(AgentBootstrapperArgs.SslMode.NONE));
        } catch (Exception e) {
            fail("should not have propagated exception thrown while invoking the launcher", e);
        }
        assertThat(destroyCalled[0]).isTrue();
    }

    @Test
    public void shouldNotDieWhenInvocationOfLauncherRaisesException_butCreationOfLauncherWentThrough() throws InterruptedException {
        final Semaphore waitForLauncherInvocation = new Semaphore(1);
        waitForLauncherInvocation.acquire();
        final AgentBootstrapper bootstrapper = new AgentBootstrapper() {
            @Override
            AgentLauncherCreator getLauncherCreator() {
                return new AgentLauncherCreator() {
                    @Override
                    public AgentLauncher createLauncher() {
                        return descriptor -> {
                            try {
                                throw new RuntimeException("fail!!! i say.");
                            } finally {
                                if (waitForLauncherInvocation.availablePermits() == 0) {
                                    waitForLauncherInvocation.release();
                                }
                            }
                        };
                    }

                    @Override
                    public void close() {
                    }
                };
            }
        };

        final AgentBootstrapper spyBootstrapper = stubJVMExit(bootstrapper);

        Thread stopLoopThd = new Thread(() -> {
            doInterruptiblyQuietlyRethrowInterrupt(waitForLauncherInvocation::acquire);
            spyBootstrapper.stopLooping();
        });
        stopLoopThd.setDaemon(true);
        stopLoopThd.start();
        try {
            spyBootstrapper.go(new AgentBootstrapperArgs().setServerUrl(serverUrl).setRootCertFile(null).setSslVerificationMode(AgentBootstrapperArgs.SslMode.NONE));
            stopLoopThd.join();
        } catch (Exception e) {
            stopLoopThd.interrupt();
            fail("should not have propagated exception thrown while invoking the launcher", e);
        }
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    public void shouldRetainStateAcrossLauncherInvocations() throws Exception {

        final Map<String, String> expectedContext = new HashMap<>();
        AgentBootstrapper agentBootstrapper = new AgentBootstrapper() {
            @Override
            AgentLauncherCreator getLauncherCreator() {
                return new AgentLauncherCreator() {
                    @Override
                    public AgentLauncher createLauncher() {
                        return new AgentLauncher() {
                            public static final String COUNT = "count";

                            @Override
                            public int launch(AgentLaunchDescriptor descriptor) {

                                Map<String, String> descriptorContext = descriptor.context();
                                incrementCount(descriptorContext);
                                int expectedCount = incrementCount(expectedContext);
                                assertThat(descriptorContext.get(COUNT)).asInt().isEqualTo(expectedCount);
                                if (expectedCount > 3) {
                                    ((AgentBootstrapper) descriptor.getBootstrapper()).stopLooping();
                                }
                                return 0;
                            }

                            private int incrementCount(Map<String, String> map) {
                                return Integer.parseInt(map.compute(COUNT, (k, v) -> v == null ? "1" : Integer.toString(Integer.parseInt(v) + 1)));
                            }
                        };
                    }

                    @Override
                    public void close() {
                    }
                };
            }
        };
        AgentBootstrapper spy = stubJVMExit(agentBootstrapper);
        spy.go(new AgentBootstrapperArgs().setServerUrl(URI.create("http://localhost:80/go").toURL()).setRootCertFile(null).setSslVerificationMode(AgentBootstrapperArgs.SslMode.NONE));
    }

    private AgentBootstrapper stubJVMExit(AgentBootstrapper bootstrapper) {
        AgentBootstrapper spy = spy(bootstrapper);
        doNothing().when(spy).jvmExit(anyInt());
        return spy;
    }

    @Test
    void shouldFormatLauncherReturnDescriptions() {
        assertThat(returnDesc(0xBADBAD)).isEqualTo("IRRECOVERABLE_ERROR (12245933 / 0xbadbad)");
        assertThat(returnDesc(60)).isEqualTo("NOT_UP_TO_DATE (60 / 0x3c)");
        assertThat(returnDesc(0)).isEqualTo("DONE (0 / 0x0)");
        assertThat(returnDesc(-1)).isEqualTo("UNKNOWN (-1 / 0xffffffff)");
        assertThat(returnDesc(-373)).isEqualTo("AGENT_FATAL_EXCEPTION_OCCURRED (-373 / 0xfffffe8b)");
    }
}
