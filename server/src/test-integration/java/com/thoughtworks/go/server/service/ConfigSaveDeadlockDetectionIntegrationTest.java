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
package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.remote.ConfigRepoConfig;
import com.thoughtworks.go.config.remote.ConfigReposConfig;
import com.thoughtworks.go.config.remote.RepoConfigOrigin;
import com.thoughtworks.go.config.rules.Allow;
import com.thoughtworks.go.config.update.FullConfigUpdateCommand;
import com.thoughtworks.go.helper.ConfigFileFixture;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.helper.PartialConfigMother;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.server.service.support.ServerStatusService;
import com.thoughtworks.go.util.GoConfigFileHelper;
import com.thoughtworks.go.util.json.JsonHelper;
import org.apache.commons.lang3.SystemUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.thoughtworks.go.helper.MaterialConfigsMother.git;
import static com.thoughtworks.go.util.TestUtils.sleepQuietly;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {
        "classpath:/applicationContext-global.xml",
        "classpath:/applicationContext-dataLocalAccess.xml",
        "classpath:/testPropertyConfigurer.xml",
        "classpath:/spring-all-servlet.xml",
})
public class ConfigSaveDeadlockDetectionIntegrationTest {
    private static final Logger LOG = LoggerFactory.getLogger(ConfigSaveDeadlockDetectionIntegrationTest.class);
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
    private PartialConfigService partialConfigService;
    @Autowired
    private CachedGoPartials cachedGoPartials;
    private GoConfigFileHelper configHelper;

    @BeforeEach
    public void setup() throws Exception {
        configHelper = new GoConfigFileHelper(ConfigFileFixture.XML_WITH_SINGLE_ENVIRONMENT);
        configHelper.usingCruiseConfigDao(goConfigDao);
        configHelper.onSetUp();
        goConfigService.forceNotifyListeners();
    }

    @AfterEach
    public void tearDown() {
        configHelper.onTearDown();
    }

    @RegisterExtension
    TestExecutionExceptionHandler timeoutExceptionHandler = (context, throwable) -> {
        if (throwable instanceof TimeoutException && throwable.getSuppressed().length > 0
                && throwable.getSuppressed()[0] instanceof InterruptedException) {
            throw new RuntimeException(
                    "Test timed out, possible deadlock. Thread Dump: " +
                            JsonHelper.toJson(serverStatusService.asJsonCompatibleMap(Username.ANONYMOUS, new HttpLocalizedOperationResult())),
                    throwable);
        }
        throw throwable;
    };


    @Test
    @Timeout(value = 3, unit = TimeUnit.MINUTES)
    public void shouldNotDeadlockWhenAllPossibleWaysOfUpdatingTheConfigAreBeingUsedAtTheSameTime() throws InterruptedException {
        int EXISTING_ENV_COUNT = goConfigService.cruiseConfig().getEnvironments().size();
        final List<Thread> group1 = new ArrayList<>();
        final List<Thread> group2 = new ArrayList<>();
        final List<Thread> group3 = new ArrayList<>();
        final List<Thread> group4 = new ArrayList<>();
        final List<Thread> group5 = new ArrayList<>();
        int count = 100;

        for (int i = 0; i < count; i++) {
            Thread thread = configSaveThread(i);
            group1.add(thread);
        }

        for (int i = 0; i < count; i++) {
            Thread thread = pipelineSaveThread(i);
            group2.add(thread);
        }

        ConfigReposConfig configRepos = new ConfigReposConfig();
        for (int i = 0; i < count; i++) {
            ConfigRepoConfig configRepoConfig = ConfigRepoConfig.createConfigRepoConfig(git("url" + i), "plugin", "id-" + i);
            configRepoConfig.getRules().add(new Allow("refer", "*", "*"));
            configRepos.add(configRepoConfig);
            Thread thread = configRepoSaveThread(configRepoConfig, i);
            group3.add(thread);
        }

        for (int i = 0; i < count; i++) {
            ConfigRepoConfig configRepoConfig = ConfigRepoConfig.createConfigRepoConfig(git("to-be-deleted-url" + i), "plugin", "to-be-deleted-" + i);
            cachedGoPartials.cacheAsLastKnown(configRepoConfig.getRepo().getFingerprint(), PartialConfigMother.withPipeline("to-be-deleted" + i, new RepoConfigOrigin(configRepoConfig, "plugin")));
            configRepos.add(configRepoConfig);
            Thread thread = configRepoDeleteThread(configRepoConfig, i);
            group4.add(thread);
        }
        for (int i = 0; i < count; i++) {
            Thread thread = fullConfigSaveThread(i);
            group5.add(thread);
        }
        configHelper.setConfigRepos(configRepos);
        for (int i = 0; i < count; i++) {
            Thread timerThread;
            timerThread = createThread(() -> {
                try {
                    writeConfigToFile(new File(goConfigDao.fileLocation()));
                } catch (Exception e) {
                    fail("Failed with error: ", e);
                }
                cachedGoConfig.forceReload();
            }, "timer-thread");

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
        }

        assertThat(goConfigService.getAllPipelineConfigs().size()).isEqualTo(count + count + count);
        assertThat(goConfigService.getConfigForEditing().getAllPipelineConfigs().size()).isEqualTo(count + count);
        assertThat(goConfigService.getConfigForEditing().getEnvironments().size()).isEqualTo(count + EXISTING_ENV_COUNT);
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
                LOG.info("Retry attempt - {}. Error: {}", retries, e, e);
                sleepQuietly(10);
                retries = retries + 1;
            }
        }
        throw new RuntimeException(String.format("Could not write to config file after %s attempts", retries));
    }

    private void update(File configFile) throws IOException {
        String currentConfig = Files.readString(configFile.toPath(), UTF_8);
        String updatedConfig = currentConfig.replaceFirst("artifactsdir=\".*\"", "artifactsdir=\"" + UUID.randomUUID().toString() + "\"");
        Files.writeString(configFile.toPath(), updatedConfig, UTF_8);
    }

    private Thread configRepoSaveThread(final ConfigRepoConfig configRepoConfig, final int counter) {
        return createThread(() -> partialConfigService.onSuccessPartialConfig(configRepoConfig, PartialConfigMother.withPipeline("remote-pipeline" + counter, new RepoConfigOrigin(configRepoConfig, "1"))), "config-repo-save-thread" + counter);
    }

    private Thread fullConfigSaveThread(final int counter) {
        return createThread(() -> {
            CruiseConfig cruiseConfig = cachedGoConfig.loadForEditing();
            CruiseConfig cruiseConfig1 = configHelper.deepClone(cruiseConfig);
            cruiseConfig1.addEnvironment(UUID.randomUUID().toString());

            goConfigDao.updateFullConfig(new FullConfigUpdateCommand(cruiseConfig1, cruiseConfig.getMd5()));
        }, "full-config-save-thread" + counter);

    }

    private Thread configRepoDeleteThread(final ConfigRepoConfig configRepoToBeDeleted, final int counter) {
        return createThread(() -> goConfigService.updateConfig(cruiseConfig -> {
            ConfigRepoConfig repoConfig = cruiseConfig.getConfigRepos().stream().filter(item -> configRepoToBeDeleted.getRepo().equals(item.getRepo())).findFirst().orElse(null);
            cruiseConfig.getConfigRepos().remove(repoConfig);
            return cruiseConfig;
        }), "config-repo-delete-thread" + counter);
    }

    private Thread pipelineSaveThread(int counter) {
        return createThread(() -> {
            PipelineConfig pipelineConfig = GoConfigMother.createPipelineConfigWithMaterialConfig(UUID.randomUUID().toString(), git("FOO"));
            HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
            pipelineConfigService.createPipelineConfig(new Username(new CaseInsensitiveString("root")), pipelineConfig, result, "default");
            assertThat(result.isSuccessful()).describedAs(result.message()).isTrue();
        }, "pipeline-config-save-thread" + counter);
    }

    private Thread configSaveThread(final int counter) {
        return createThread(() -> goConfigService.updateConfig(cruiseConfig -> {
            PipelineConfig pipelineConfig = GoConfigMother.createPipelineConfigWithMaterialConfig(UUID.randomUUID().toString(), git("FOO"));
            cruiseConfig.addPipeline("default", pipelineConfig);
            return cruiseConfig;
        }), "config-save-thread" + counter);
    }

    private Thread createThread(Runnable runnable, String name) {
        Thread thread = new Thread(runnable, name);
        thread.setUncaughtExceptionHandler((t, e) -> {
            throw new RuntimeException(e.getMessage(), e);
        });
        return thread;
    }
}
