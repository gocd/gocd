/*
 * Copyright 2019 ThoughtWorks, Inc.
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
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.util.*;
import com.thoughtworks.go.util.command.CommandLine;
import com.thoughtworks.go.util.command.InMemoryStreamConsumer;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import static com.thoughtworks.go.helper.MaterialConfigsMother.git;
import static com.thoughtworks.go.util.command.ProcessOutputStreamConsumer.inMemoryConsumer;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

@Ignore
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:testPropertyConfigurer.xml",
        "classpath:WEB-INF/spring-all-servlet.xml",
})
public class PipelineConfigServicePerformanceTest {
    private static String consoleAppenderForPerformanceTest;

    private static String rollingFileAppenderForPerformanceTest;

    static {
        new SystemEnvironment().setProperty(GoConstants.USE_COMPRESSED_JAVASCRIPT, "false");
    }

    @Autowired
    private PipelineConfigService pipelineConfigService;
    @Autowired
    private GoConfigService goConfigService;
    @Autowired
    private GoConfigDao goConfigDao;
    @Autowired
    private DatabaseAccessHelper dbHelper;
    @Autowired
    private GoConfigMigration goConfigMigration;
    @Autowired
    private EntityHashingService entityHashingService;

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private GoConfigFileHelper configHelper;
    private final int numberOfRequests = 100;
    private HttpLocalizedOperationResult result;
    private Username user;
    private static Logger LOGGER = LoggerFactory.getLogger(PipelineConfigServicePerformanceTest.class.getName());

    @Before
    public void setup() throws Exception {
        configHelper = new GoConfigFileHelper();
        dbHelper.onSetUp();
        configHelper.usingCruiseConfigDao(goConfigDao).initializeConfigFile();
        configHelper.onSetUp();
        goConfigService.forceNotifyListeners();
        File dumpDir = tempFolder.newFolder("perf-pipelineapi-test");
        FileUtil.deleteDirectoryNoisily(dumpDir);
        dumpDir.mkdirs();
        result = new HttpLocalizedOperationResult();
        user = new Username(new CaseInsensitiveString("admin"));
        consoleAppenderForPerformanceTest = "ConsoleAppenderForPerformanceTest";
        rollingFileAppenderForPerformanceTest = "RollingFileAppenderForPerformanceTest";
    }

    @Test
    public void performanceTestForUpdatePipeline() throws Exception {
        setupPipelines(numberOfRequests);
        final ConcurrentHashMap<String, Boolean> results = new ConcurrentHashMap<>();
        run(new Runnable() {
            @Override
            public void run() {
                PipelineConfig pipelineConfig = goConfigService.getConfigForEditing().pipelineConfigByName(new CaseInsensitiveString(Thread.currentThread().getName()));
                pipelineConfig.add(new StageConfig(new CaseInsensitiveString("additional_stage"), new JobConfigs(new JobConfig(new CaseInsensitiveString("addtn_job")))));
                PerfTimer updateTimer = PerfTimer.start("Saving pipelineConfig : " + pipelineConfig.name());
                pipelineConfigService.updatePipelineConfig(user, pipelineConfig, entityHashingService.md5ForEntity(pipelineConfig), result);
                updateTimer.stop();
                results.put(Thread.currentThread().getName(), result.isSuccessful());
                if (!result.isSuccessful()) {
                    LOGGER.error(result.toString());
                    LOGGER.error("Errors on pipeline" + Thread.currentThread().getName() + " are : " + StringUtils.join(getAllErrors(pipelineConfig), ", "));
                }
            }
        }, numberOfRequests, results);
    }

    @Test
    public void performanceTestForDeletePipeline() throws Exception {
        setupPipelines(numberOfRequests);
        final ConcurrentHashMap<String, Boolean> results = new ConcurrentHashMap<>();
        run(new Runnable() {
            @Override
            public void run() {
                PipelineConfig pipelineConfig = goConfigService.getConfigForEditing().pipelineConfigByName(new CaseInsensitiveString(Thread.currentThread().getName()));
                pipelineConfig.add(new StageConfig(new CaseInsensitiveString("additional_stage"), new JobConfigs(new JobConfig(new CaseInsensitiveString("addtn_job")))));
                PerfTimer updateTimer = PerfTimer.start("Saving pipelineConfig : " + pipelineConfig.name());
                pipelineConfigService.deletePipelineConfig(user, pipelineConfig, result);
                updateTimer.stop();
                results.put(Thread.currentThread().getName(), result.isSuccessful());
                if (!result.isSuccessful()) {
                    LOGGER.error(result.toString());
                    LOGGER.error("Errors on pipeline" + Thread.currentThread().getName() + " are : " + StringUtils.join(getAllErrors(pipelineConfig), ", "));
                }
            }
        }, numberOfRequests, results);
    }

    @Test
    public void performanceTestForCreatePipeline() throws Exception {
        setupPipelines(0);
        final ConcurrentHashMap<String, Boolean> results = new ConcurrentHashMap<>();
        run(new Runnable() {
            @Override
            public void run() {
                JobConfig jobConfig = new JobConfig(new CaseInsensitiveString("job"));
                StageConfig stageConfig = new StageConfig(new CaseInsensitiveString("stage"), new JobConfigs(jobConfig));
                PipelineConfig pipelineConfig = new PipelineConfig(new CaseInsensitiveString(Thread.currentThread().getName()), new MaterialConfigs(git("FOO")), stageConfig);
                PerfTimer updateTimer = PerfTimer.start("Saving pipelineConfig : " + pipelineConfig.name());
                pipelineConfigService.createPipelineConfig(user, pipelineConfig, result, "jumbo");
                updateTimer.stop();
                results.put(Thread.currentThread().getName(), result.isSuccessful());
                if (!result.isSuccessful()) {
                    LOGGER.error(result.toString());
                    LOGGER.error("Errors on pipeline" + Thread.currentThread().getName() + " are : " + StringUtils.join(getAllErrors(pipelineConfig), ", "));
                }
            }
        }, numberOfRequests, results);
    }

    @After
    public void tearDown() throws Exception {
        configHelper.onTearDown();
        dbHelper.onTearDown();
    }

    private void run(Runnable runnable, int numberOfRequests, final ConcurrentHashMap<String, Boolean> results) throws InterruptedException {
        Boolean finalResult = true;
        LOGGER.info("Tests start now!");
        final ArrayList<Thread> threads = new ArrayList<>();
        for (int i = 0; i < numberOfRequests; i++) {
            Thread t = new Thread(runnable, "pipeline" + i);
            threads.add(t);
        }
        for (Thread t : threads) {
            Thread.sleep(1000 * (new Random().nextInt(3) + 1));
            t.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                public void uncaughtException(Thread t, Throwable e) {
                    LOGGER.error("Exception " + e + " from thread " + t);
                    results.put(t.getName(), false);
                }
            });
            t.start();
        }
        for (Thread t : threads) {
            int i = threads.indexOf(t);
            if (i == (numberOfRequests - 1)) {
//                takeHeapDump(dumpDir, i);
            }
            t.join();
        }
        for (String threadId : results.keySet()) {
            finalResult = results.get(threadId) && finalResult;
        }
        assertThat(finalResult, is(true));
    }

    private void takeHeapDump(File dumpDir, int i) {
        InMemoryStreamConsumer outputStreamConsumer = inMemoryConsumer();
        CommandLine commandLine = CommandLine.createCommandLine("jmap").withArgs("-J-d64", String.format("-dump:format=b,file=%s/%s.hprof", dumpDir.getAbsoluteFile(), i), ManagementFactory.getRuntimeMXBean().getName().split("@")[0]);
        LOGGER.info(commandLine.describe());
        int exitCode = commandLine.run(outputStreamConsumer, new NamedProcessTag("thread" + i));
        LOGGER.info(outputStreamConsumer.getAllOutput());
        assertThat(exitCode, is(0));
        LOGGER.info("Heap dump available at " + dumpDir.getAbsolutePath());
    }

    private static abstract class ErrorCollectingHandler implements GoConfigGraphWalker.Handler {
        private final List<ConfigErrors> allErrors;

        public ErrorCollectingHandler(List<ConfigErrors> allErrors) {
            this.allErrors = allErrors;
        }

        public void handle(Validatable validatable, ValidationContext context) {
            handleValidation(validatable, context);
            ConfigErrors configErrors = validatable.errors();

            if (!configErrors.isEmpty()) {
                allErrors.add(configErrors);
            }
        }

        public abstract void handleValidation(Validatable validatable, ValidationContext context);
    }


    private List<ConfigErrors> getAllErrors(Validatable v) {
        final List<ConfigErrors> allErrors = new ArrayList<>();
        new GoConfigGraphWalker(v).walk(new ErrorCollectingHandler(allErrors) {
            @Override
            public void handleValidation(Validatable validatable, ValidationContext context) {
                // do nothing here
            }
        });
        return allErrors;
    }

    private void setupPipelines(Integer numberOfPipelinesToBeCreated) throws Exception {
        String groupName = "jumbo";
        String configFile = "<FULL PATH TO YOUR CONFIG FILE>";
        String xml = FileUtils.readFileToString(new File(configFile), UTF_8);
        xml = goConfigMigration.upgradeIfNecessary(xml);
        goConfigService.fileSaver(false).saveConfig(xml, goConfigService.getConfigForEditing().getMd5());
        LOGGER.info("Total number of pipelines in this config: " + goConfigService.getConfigForEditing().allPipelines().size());
        if (goConfigService.getConfigForEditing().hasPipelineGroup(groupName)) {
            ((BasicPipelineConfigs) goConfigService.getConfigForEditing().findGroup(groupName)).clear();
        }
        final CruiseConfig configForEditing = goConfigService.getConfigForEditing();
        for (int i = 0; i < numberOfPipelinesToBeCreated; i++) {
            JobConfig jobConfig = new JobConfig(new CaseInsensitiveString("job"));
            StageConfig stageConfig = new StageConfig(new CaseInsensitiveString("stage"), new JobConfigs(jobConfig));
            PipelineConfig pipelineConfig = new PipelineConfig(new CaseInsensitiveString("pipeline" + i), new MaterialConfigs(git("FOO")), stageConfig);
            configForEditing.addPipeline(groupName, pipelineConfig);
        }

        goConfigService.updateConfig(new UpdateConfigCommand() {
            @Override
            public CruiseConfig update(CruiseConfig cruiseConfig) throws Exception {
                return configForEditing;
            }
        });
    }
}
