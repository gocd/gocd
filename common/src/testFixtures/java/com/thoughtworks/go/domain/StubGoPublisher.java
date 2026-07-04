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
package com.thoughtworks.go.domain;

import com.thoughtworks.go.publishers.ArtifactManipulator;
import com.thoughtworks.go.server.service.AgentRuntimeInfo;
import com.thoughtworks.go.work.DefaultGoPublisher;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;

public class StubGoPublisher extends DefaultGoPublisher {
    private final Map<File, String> uploadedFiles = new HashMap<>();
    private String message = "";
    private boolean shouldFail;

    public StubGoPublisher() {
        this(false);
    }

    public StubGoPublisher(boolean shouldFail) {
        super(mock(ArtifactManipulator.class), null, null, AgentRuntimeInfo.initialState(NullAgent.createNullAgent()), StandardCharsets.UTF_8);
        this.shouldFail = shouldFail;
    }

    protected void init() {
    }

    @Override
    public void taggedConsumeLineWithPrefix(@NonNull String tag, String line) {
        this.message += line;
    }

    @Override
    public void taggedConsumeLine(@NotNull String tag, @NotNull String message) {
        this.message += message;
    }

    @Override
    public void consumeLineWithPrefix(@NonNull String line) {
        this.message += line;
    }

    @Override
    public void consumeLine(@NotNull String message) {
        this.message += message;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public void upload(File fileToUpload, String destPath) {
        if (shouldFail) {
            throw new RuntimeException("failed on purpose");
        }
        uploadedFiles.put(fileToUpload, destPath);
    }

    public Map<File, String> publishedFiles() {
        return uploadedFiles;
    }

    public void assertPublished(String endOfFileName, String destination) {

        for (File file : uploadedFiles.keySet()) {
            if (file.getAbsolutePath().endsWith(endOfFileName)) {
                assertThat(uploadedFiles.get(file)).isEqualTo(destination);
                return;
            }
        }
        fail(endOfFileName + " was not published to " + destination + ".\n" + uploadedFiles);
    }

    public void setShouldFail(boolean shouldFail) {
        this.shouldFail = shouldFail;
    }
}
