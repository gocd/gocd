/*
 * Copyright 2016 ThoughtWorks, Inc. *
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
 */

package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.materials.git.GitMaterialConfig;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.i18n.Localizer;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.server.service.support.ServerStatusService;
import com.thoughtworks.go.util.FileUtil;
import com.thoughtworks.go.util.GoConfigFileHelper;
import com.thoughtworks.go.util.SystemUtil;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.rules.Timeout;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:WEB-INF/applicationContext-acegi-security.xml"
})
public class ConfigSaveDeadlockDetectionIntegrationTest {
    @Autowired
    private GoConfigDao goConfigDao;
    @Autowired
    private GoConfigService goConfigService;
    @Autowired
    private MergedGoConfig mergedGoConfig;
    @Autowired
    private PipelineConfigService pipelineConfigService;
    @Autowired
    private ServerStatusService serverStatusService;
    @Autowired
    private Localizer localizer;
    private GoConfigFileHelper configHelper;
    private final int TWO_MINUTES = 2 * 60 * 1000;

    @Before
    public void setup() throws Exception {
        configHelper = new GoConfigFileHelper();
        configHelper.usingCruiseConfigDao(goConfigDao).initializeConfigFile();
        configHelper.onSetUp();
        goConfigService.forceNotifyListeners();
    }

    @After
    public void tearDown() throws Exception {
        configHelper.onTearDown();
    }

    @Rule
    public final TestRule timeout = RuleChain
            .outerRule(new TestWatcher() {
                @Override
                protected void failed(Throwable e, Description description) {
                    if (e.getMessage().contains("test timed out") || e instanceof TimeoutException) {
                        try {
                            fail("Test timed out, possible deadlock. Thread Dump:" + serverStatusService.captureServerInfo(Username.ANONYMOUS, new HttpLocalizedOperationResult()));
                        } catch (IOException e1) {
                            throw new RuntimeException(e1);
                        }
                    }

                }
            })
            .around(new Timeout(TWO_MINUTES));

    @Test
    public void shouldNotDeadlockWhenAllPossibleWaysOfUpdatingTheConfigAreBeingUsedAtTheSameTime() throws Exception {
        final ArrayList<Thread> configSaveThreads = new ArrayList<>();
        final int pipelineCreatedThroughApiCount = 100;
        final int pipelineCreatedThroughUICount = 100;

        for (int i = 0; i < pipelineCreatedThroughUICount; i++) {
            Thread thread = configSaveThread(i);
            configSaveThreads.add(thread);
        }

        for (int i = 0; i < pipelineCreatedThroughApiCount; i++) {
            Thread thread = pipelineSaveThread(i);
            configSaveThreads.add(thread);
        }

        for (Thread configSaveThread : configSaveThreads) {
            Thread timerThread = null;
            try {
                timerThread = createThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            writeConfigToFile(new File(goConfigDao.fileLocation()));
                        } catch (Exception e) {
                            e.printStackTrace();
                            fail("Failed with error: " + e.getMessage());
                        }
                        mergedGoConfig.forceReload();
                    }
                }, "timer-thread");
            } catch (InterruptedException e) {
                fail(e.getMessage());
            }

            try {
                configSaveThread.start();
                timerThread.start();
                configSaveThread.join();
                timerThread.join();
            } catch (InterruptedException e) {
                fail(e.getMessage());
            }
        }
        assertThat(goConfigService.getAllPipelineConfigs().size(), is(pipelineCreatedThroughApiCount + pipelineCreatedThroughUICount));
    }

    private void writeConfigToFile(File configFile) throws IOException {
        if (!SystemUtil.isWindows()) {
            update(configFile);
            return;
        }
        int retries = 1;
        while (retries <= 5) {
            try {
                update(configFile);
                return;
            } catch (IOException e) {
                try {
                    System.out.println(String.format("Retry attempt - %s. Error: %s", retries, e.getMessage()));
                    e.printStackTrace();
                    Thread.sleep(10);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
                retries = retries + 1;
            }
        }
        throw new RuntimeException(String.format("Could not write to config file after %s attempts", retries));
    }

    private void update(File configFile) throws IOException {
        String currentConfig = FileUtil.readContentFromFile(configFile);
        String updatedConfig = currentConfig.replaceFirst("artifactsdir=\".*\"", "artifactsdir=\"" + UUID.randomUUID().toString() + "\"");
        FileUtil.writeContentToFile(updatedConfig, configFile);
    }

    private Thread pipelineSaveThread(int counter) throws InterruptedException {
        return createThread(new Runnable() {
            @Override
            public void run() {
                PipelineConfig pipelineConfig = GoConfigMother.createPipelineConfigWithMaterialConfig(UUID.randomUUID().toString(), new GitMaterialConfig("FOO"));
                HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
                pipelineConfigService.createPipelineConfig(new Username(new CaseInsensitiveString("root")), pipelineConfig, result, "default");
                assertThat(result.message(localizer), result.isSuccessful(), is(true));
            }
        }, "pipeline-config-save-thread" + counter);
    }

    private Thread configSaveThread(final int counter) throws InvalidCipherTextException, InterruptedException {
        return createThread(new Runnable() {
            @Override
            public void run() {
                goConfigService.updateConfig(new UpdateConfigCommand() {
                    @Override
                    public CruiseConfig update(CruiseConfig cruiseConfig) throws Exception {
                        PipelineConfig pipelineConfig = GoConfigMother.createPipelineConfigWithMaterialConfig(UUID.randomUUID().toString(), new GitMaterialConfig("FOO"));
                        cruiseConfig.addPipeline("default", pipelineConfig);
                        return cruiseConfig;
                    }
                });

            }
        }, "config-save-thread" + counter);
    }

    private Thread createThread(Runnable runnable, String name) throws InterruptedException {
        Thread thread = new Thread(runnable, name);
        thread.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            public void uncaughtException(Thread t, Throwable e) {
                e.printStackTrace();
                throw new RuntimeException(e.getMessage(), e);
            }
        });
        return thread;
    }
}
