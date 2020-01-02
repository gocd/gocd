/*
 * Copyright 2020 ThoughtWorks, Inc.
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
package com.thoughtworks.go.server.service;

import com.google.gson.Gson;
import com.rits.cloning.Cloner;
import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.remote.ConfigRepoConfig;
import com.thoughtworks.go.config.remote.ConfigReposConfig;
import com.thoughtworks.go.config.remote.RepoConfigOrigin;
import com.thoughtworks.go.config.update.FullConfigUpdateCommand;
import com.thoughtworks.go.helper.ConfigFileFixture;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.helper.PartialConfigMother;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.server.service.support.ServerStatusService;
import com.thoughtworks.go.util.GoConfigFileHelper;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;
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
import java.util.function.Predicate;

import static com.thoughtworks.go.helper.MaterialConfigsMother.git;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:/applicationContext-global.xml",
        "classpath:/applicationContext-dataLocalAccess.xml",
        "classpath:/testPropertyConfigurer.xml",
        "classpath:/spring-all-servlet.xml",
})
public class ConfigSaveDeadlockDetectionIntegrationTest {
    @Autowired
    private GoConfigDao goConfigDao;
    @Autowired
    private GoConfigService goConfigService;
    @Autowired
    private CachedGoConfig cachedGoConfig;
    @Autowired
    private PipelineConfigService pipelineConfigService;
    @Autowired
    private ServerStatusService serverStatusService;
    @Autowired
    private GoPartialConfig goPartialConfig;
    @Autowired
    private CachedGoPartials cachedGoPartials;
    private GoConfigFileHelper configHelper;
    private final int THREE_MINUTES = 3 * 60 * 1000;

    @Before
    public void setup() throws Exception {
        configHelper = new GoConfigFileHelper(ConfigFileFixture.XML_WITH_SINGLE_ENVIRONMENT);
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
                            fail("Test timed out, possible deadlock. Thread Dump:" + new Gson().toJson(serverStatusService.asJson(Username.ANONYMOUS, new HttpLocalizedOperationResult())));
                        } catch (Exception e1) {
                            throw new RuntimeException(e1);
                        }
                    }

                }
            })
            .around(new Timeout(THREE_MINUTES));

    @Test
    public void shouldNotDeadlockWhenAllPossibleWaysOfUpdatingTheConfigAreBeingUsedAtTheSameTime() throws Exception {
        int EXISTING_ENV_COUNT = goConfigService.cruiseConfig().getEnvironments().size();
        final ArrayList<Thread> group1 = new ArrayList<>();
        final ArrayList<Thread> group2 = new ArrayList<>();
        final ArrayList<Thread> group3 = new ArrayList<>();
        final ArrayList<Thread> group4 = new ArrayList<>();
        final ArrayList<Thread> group5 = new ArrayList<>();
        int count = 100;
        final int pipelineCreatedThroughApiCount = count;
        final int pipelineCreatedThroughUICount = count;
        final int configRepoAdditionThreadCount = count;
        final int configRepoDeletionThreadCount = count;
        final int fullConfigSaveThreadCount = count;

        for (int i = 0; i < pipelineCreatedThroughUICount; i++) {
            Thread thread = configSaveThread(i);
            group1.add(thread);
        }

        for (int i = 0; i < pipelineCreatedThroughApiCount; i++) {
            Thread thread = pipelineSaveThread(i);
            group2.add(thread);
        }

        ConfigReposConfig configRepos = new ConfigReposConfig();
        for (int i = 0; i < configRepoAdditionThreadCount; i++) {
            ConfigRepoConfig configRepoConfig = new ConfigRepoConfig(git("url" + i), "plugin");
            configRepos.add(configRepoConfig);
            Thread thread = configRepoSaveThread(configRepoConfig, i);
            group3.add(thread);
        }

        for (int i = 0; i < configRepoDeletionThreadCount; i++) {
            ConfigRepoConfig configRepoConfig = new ConfigRepoConfig(git("to-be-deleted-url" + i), "plugin");
            cachedGoPartials.addOrUpdate(configRepoConfig.getMaterialConfig().getFingerprint(), PartialConfigMother.withPipeline("to-be-deleted" + i, new RepoConfigOrigin(configRepoConfig, "plugin")));
            configRepos.add(configRepoConfig);
            Thread thread = configRepoDeleteThread(configRepoConfig, i);
            group4.add(thread);
        }
        for (int i = 0; i < fullConfigSaveThreadCount; i++) {
            Thread thread = fullConfigSaveThread(i);
            group5.add(thread);
        }
        configHelper.setConfigRepos(configRepos);
        for (int i = 0; i < count; i++) {
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
                        cachedGoConfig.forceReload();
                    }
                }, "timer-thread");
            } catch (InterruptedException e) {
                fail(e.getMessage());
            }

            try {
                group1.get(i).start();
                group2.get(i).start();
                group3.get(i).start();
                group4.get(i).start();
                group5.get(i).start();
                timerThread.start();
                group1.get(i).join();
                group2.get(i).join();
                group3.get(i).join();
                group4.get(i).join();
                group5.get(i).join();
                timerThread.join();
            } catch (InterruptedException e) {
                fail(e.getMessage());
            }

        }

        assertThat(goConfigService.getAllPipelineConfigs().size(), is(pipelineCreatedThroughApiCount + pipelineCreatedThroughUICount + configRepoAdditionThreadCount));
        assertThat(goConfigService.getConfigForEditing().getAllPipelineConfigs().size(), is(pipelineCreatedThroughApiCount + pipelineCreatedThroughUICount));
        assertThat(goConfigService.getConfigForEditing().getEnvironments().size(), is(fullConfigSaveThreadCount + EXISTING_ENV_COUNT));
    }

    private void writeConfigToFile(File configFile) throws IOException {
        if (!SystemUtils.IS_OS_WINDOWS) {
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
        String currentConfig = FileUtils.readFileToString(configFile, UTF_8);
        String updatedConfig = currentConfig.replaceFirst("artifactsdir=\".*\"", "artifactsdir=\"" + UUID.randomUUID().toString() + "\"");
        FileUtils.writeStringToFile(configFile, updatedConfig, UTF_8);
    }

    private Thread configRepoSaveThread(final ConfigRepoConfig configRepoConfig, final int counter) throws InterruptedException {
        return createThread(new Runnable() {
            @Override
            public void run() {
                goPartialConfig.onSuccessPartialConfig(configRepoConfig, PartialConfigMother.withPipeline("remote-pipeline" + counter, new RepoConfigOrigin(configRepoConfig, "1")));
            }
        }, "config-repo-save-thread" + counter);
    }

    private Thread fullConfigSaveThread(final int counter) throws InterruptedException {
        return createThread(new Runnable() {
            @Override
            public void run() {
                try {
                    CruiseConfig cruiseConfig = cachedGoConfig.loadForEditing();
                    CruiseConfig cruiseConfig1 = new Cloner().deepClone(cruiseConfig);
                    cruiseConfig1.addEnvironment(UUID.randomUUID().toString());

                    goConfigDao.updateFullConfig(new FullConfigUpdateCommand(cruiseConfig1, cruiseConfig.getMd5()));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, "full-config-save-thread" + counter);

    }

    private Thread configRepoDeleteThread(final ConfigRepoConfig configRepoToBeDeleted, final int counter) throws InterruptedException {
        return createThread(new Runnable() {
            @Override
            public void run() {
                goConfigService.updateConfig(new UpdateConfigCommand() {
                    @Override
                    public CruiseConfig update(CruiseConfig cruiseConfig) throws Exception {
                        ConfigRepoConfig repoConfig = cruiseConfig.getConfigRepos().stream().filter(new Predicate<ConfigRepoConfig>() {
                            @Override
                            public boolean test(ConfigRepoConfig item) {
                                return configRepoToBeDeleted.getMaterialConfig().equals(item.getMaterialConfig());
                            }
                        }).findFirst().orElse(null);
                        cruiseConfig.getConfigRepos().remove(repoConfig);
                        return cruiseConfig;
                    }
                });
            }
        }, "config-repo-delete-thread" + counter);
    }

    private Thread pipelineSaveThread(int counter) throws InterruptedException {
        return createThread(new Runnable() {
            @Override
            public void run() {
                PipelineConfig pipelineConfig = GoConfigMother.createPipelineConfigWithMaterialConfig(UUID.randomUUID().toString(), git("FOO"));
                HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
                pipelineConfigService.createPipelineConfig(new Username(new CaseInsensitiveString("root")), pipelineConfig, result, "default");
                assertThat(result.message(), result.isSuccessful(), is(true));
            }
        }, "pipeline-config-save-thread" + counter);
    }

    private Thread configSaveThread(final int counter) throws InterruptedException {
        return createThread(new Runnable() {
            @Override
            public void run() {
                goConfigService.updateConfig(new UpdateConfigCommand() {
                    @Override
                    public CruiseConfig update(CruiseConfig cruiseConfig) throws Exception {
                        PipelineConfig pipelineConfig = GoConfigMother.createPipelineConfigWithMaterialConfig(UUID.randomUUID().toString(), git("FOO"));
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
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                e.printStackTrace();
                throw new RuntimeException(e.getMessage(), e);
            }
        });
        return thread;
    }
}
