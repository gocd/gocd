/*************************** GO-LICENSE-START*********************************
 * Copyright 2016 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ************************GO-LICENSE-END***********************************/
package com.thoughtworks.go.buildsession;

import com.jezhumble.javasysmon.JavaSysMon;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.util.Clock;
import com.thoughtworks.go.util.GoConstants;
import com.thoughtworks.go.util.HttpService;
import com.thoughtworks.go.util.command.ProcessOutputStreamConsumer;
import com.thoughtworks.go.util.command.SafeOutputStreamConsumer;
import com.thoughtworks.go.util.command.StreamConsumer;
import org.apache.commons.lang.text.StrLookup;
import org.apache.commons.lang.text.StrSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;
import static com.thoughtworks.go.util.ExceptionUtils.messageOf;
import static com.thoughtworks.go.util.FileUtil.applyBaseDirIfRelative;
import static java.lang.String.format;

public class BuildSession {
    private static final Logger LOG = LoggerFactory.getLogger(BuildSession.class);
    private final Map<String, String> envs;
    private final Map<String, String> secretSubstitutions;
    private final String buildId;
    private final BuildStateReporter buildStateReporter;
    private final StreamConsumer console;
    private final DownloadAction downloadAction;
    private final ExecutorService executorService;
    private File workingDir;
    private JobResult buildResult;
    private final StrLookup buildVariables;
    private ArtifactsRepository artifactsRepository;
    private HttpService httpService;
    private Clock clock;
    private final CountDownLatch doneLatch;
    private CountDownLatch cancelLatch;

    private static Map<String, BuildCommandExecutor> executors = new HashMap<>();

    static {
        executors.put("echo", new EchoCommandExecutor());
        executors.put("downloadDir", new DownloadDirCommandExecutor());
        executors.put("downloadFile", new DownloadFileCommandExecutor());
        executors.put("uploadArtifact", new UploadArtifactCommandExecutor());
        executors.put("secret", new SecretCommandExecutor());
        executors.put("export", new ExportCommandExecutor());
        executors.put("compose", new ComposeCommandExecutor());
        executors.put("fail", new FailCommandExecutor());
        executors.put("mkdirs", new MkdirsCommandExecutor());
        executors.put("cleandir", new CleandirCommandExecutor());
        executors.put("exec", new ExecCommandExecutor());
        executors.put("test", new TestCommandExecutor());
        executors.put("reportCurrentStatus", new ReportCurrentStatusCommandExecutor());
        executors.put("reportCompleting", new ReportCompletingCommandExecutor());
        executors.put("generateTestReport", new GenerateTestReportCommandExecutor());
        executors.put("generateProperty", new GeneratePropertyCommandExecutor());
        executors.put("error", new ErrorCommandExecutor());
    }

    public BuildSession(String buildId, BuildStateReporter buildStateReporter, StreamConsumer console, StrLookup buildVariables, ArtifactsRepository artifactsRepository, HttpService httpService, Clock clock, File workingDir) {
        this.buildId = buildId;
        this.buildStateReporter = buildStateReporter;
        this.console = console;
        this.buildVariables = buildVariables;
        this.artifactsRepository = artifactsRepository;
        this.httpService = httpService;
        this.clock = clock;
        this.workingDir = workingDir;
        this.envs = new HashMap<>();
        this.secretSubstitutions = new HashMap<>();
        this.buildResult = JobResult.Passed;
        this.doneLatch = new CountDownLatch(1);
        this.cancelLatch = new CountDownLatch(1);
        this.downloadAction = new DownloadAction(httpService, getPublisher(), clock);
        this.executorService = Executors.newCachedThreadPool();
    }

    public void setEnv(String name, String value) {
        if (value == null) {
            value = "";
        }
        envs.put(name, value);
    }

    /**
     * Cancel build and wait for build session done
     *
     * @return {@code true} if the build session is done and {@code false}
     * if time out happens
     */
    public boolean cancel(int timeout, TimeUnit timeoutUnit) throws InterruptedException {
        if (isCanceled()) {
            return true;
        }

        cancelLatch.countDown();

        try {
            return doneLatch.await(timeout, timeoutUnit);
        } finally {
            new JavaSysMon().infanticide();
        }
    }

    public JobResult build(BuildCommand command) {
        if (isDone()) {
            throw bomb("Shall not reuse a build session!");
        }

        try {
            processCommand(command);
            return buildResult;
        } finally {
            try {
                buildStateReporter.reportCompleted(buildId, buildResult);
            } catch (Exception e) {
                reportException(e);
            }

            try {
                executorService.shutdownNow();
            } catch (Exception e) {
                reportException(e);
            }

            doneLatch.countDown();
        }
    }

    boolean processCommand(BuildCommand command) {
        if (isCanceled()) {
            buildResult = JobResult.Cancelled;
            return false;
        }

        LOG.debug("Processing build command {}", command.getName());

        BuildCommandExecutor executor = executors.get(command.getName());
        if (executor == null) {
            LOG.error("Unknown command: " + command.getName());
            println("error: build command " + command.getName() + " is not supported. Please upgrade GoCD agent");
            buildResult = JobResult.Failed;
            return false;
        }

        boolean success = doProcess(command, executor);

        if (isCanceled()) {
            buildResult = JobResult.Cancelled;
            return false;
        }

        if(!success) {
            this.buildResult = JobResult.Failed;
        }

        return success;
    }

    private boolean doProcess(BuildCommand command, BuildCommandExecutor executor) {
        BuildCommand onCancelCommand = command.getOnCancel();

        try {
            if (("passed".equals(command.getRunIfConfig()) && buildResult.isFailed())
                    || ("failed".equals(command.getRunIfConfig()) && this.buildResult.isPassed())) {
                return true;
            }

            BuildCommand test = command.getTest();
            if (test != null) {
                if (newTestingSession(console).build(test) != JobResult.Passed) {
                    return true;
                }
            }

            if (isCanceled()) {
                return false;
            }

            return executor.execute(command, this);

        } catch (Exception e) {
            reportException(e);
            return false;
        } finally {
            if (isCanceled() && onCancelCommand != null) {
                newCancelSession().build(onCancelCommand);
            }
        }
    }


    File resolveRelativeDir(String... dirs) {
        if (dirs.length == 0) {
            return workingDir;
        }

        File result = new File(dirs[dirs.length - 1]);
        for (int i = dirs.length - 2; i >= 0; i--) {
            result = applyBaseDirIfRelative(new File(dirs[i]), result);
        }
        return applyBaseDirIfRelative(workingDir, result);
    }


    Future<?> submitRunnable(Runnable runnable) {
        return executorService.submit(runnable);
    }

    void addSecret(String secret, String substitution) {
        if (substitution == null) {
            substitution = "******";
        }
        this.secretSubstitutions.put(secret, substitution);
    }

    Map<String, String> getEnvs() {
        return Collections.unmodifiableMap(envs);
    }

    void printlnSafely(String line) {
        newSafeConsole().stdOutput(line);
    }

    void println(String line) {
        console.consumeLine(line);
    }

    public void printlnWithPrefix(String line) {
        this.println(String.format("[%s] %s", GoConstants.PRODUCT_NAME, line));
    }

    String buildVariableSubstitute(String str) {
        return new StrSubstitutor(buildVariables).replace(str);
    }

    void upload(File file, String dest) {
        getPublisher().upload(file, dest);
    }

    ProcessOutputStreamConsumer processOutputStreamConsumer() {
        return new ProcessOutputStreamConsumer<>(console, console);
    }

    Map<String, String> getSecretSubstitutions() {
        return Collections.unmodifiableMap(secretSubstitutions);
    }

    void waitUntilCanceled() throws InterruptedException {
        cancelLatch.await();
    }

    void reportBuildStatus(String status) {
        buildStateReporter.reportBuildStatus(buildId, JobState.valueOf(status));
    }

    void reportCompleting() {
        buildStateReporter.reportCompleting(buildId, buildResult);
    }

    BuildSession newTestingSession(StreamConsumer console) {
        BuildSession buildSession = new BuildSession(
                buildId, new UncaringBuildStateReport(), console, buildVariables, artifactsRepository, httpService, clock, workingDir);
        buildSession.cancelLatch = this.cancelLatch;
        return buildSession;
    }


    void download(FetchHandler handler, String url, ChecksumFileHandler checksumFileHandler, String checksumUrl) {
        try {
            if (checksumFileHandler != null) {
                downloadAction.perform(checksumUrl, checksumFileHandler);
                handler.useArtifactMd5Checksums(checksumFileHandler.getArtifactMd5Checksums());
            }

            downloadAction.perform(url, handler);
        } catch (InterruptedException e) {
            throw new RuntimeException("download interrupted");
        }
    }

    BuildSessionGoPublisher getPublisher() {
        return new BuildSessionGoPublisher(console, artifactsRepository, buildId);
    }


    private BuildSession newCancelSession() {
        return new BuildSession(buildId, new UncaringBuildStateReport(),
                console, buildVariables, artifactsRepository, httpService, clock, workingDir);
    }


    private boolean isDone() {
        return doneLatch.getCount() < 1;
    }

    private boolean isCanceled() {
        return cancelLatch.getCount() < 1;
    }

    private SafeOutputStreamConsumer newSafeConsole() {
        ProcessOutputStreamConsumer processConsumer = new ProcessOutputStreamConsumer<>(console, console);
        SafeOutputStreamConsumer streamConsumer = new SafeOutputStreamConsumer(processConsumer);
        for (String secret : secretSubstitutions.keySet()) {
            streamConsumer.addSecret(new SecretSubstitution(secret, secretSubstitutions.get(secret)));
        }
        return streamConsumer;
    }

    private void reportException(Exception e) {
        String msg = messageOf(e);
        try {
            LOG.error(msg, e);
            printlnSafely(msg);
        } catch (Exception reportException) {
            LOG.error(format("Unable to report error message - %s.", messageOf(e)), reportException);
        }
    }
}
