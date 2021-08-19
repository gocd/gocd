/*
 * Copyright 2021 ThoughtWorks, Inc.
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
package com.thoughtworks.go.domain.materials.perforce;

import com.thoughtworks.go.helper.P4TestRepo;
import com.thoughtworks.go.util.TempDirUtils;
import com.thoughtworks.go.util.command.InMemoryStreamConsumer;
import com.thoughtworks.go.util.command.ProcessOutputStreamConsumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;

public abstract class PerforceFixture {
    protected P4Client p4;
    protected File clientFolder;
    public static final String DEFAULT_CLIENT_NAME = "p4test_1";
    protected P4Fixture p4Fixture;
    protected InMemoryStreamConsumer outputconsumer;

    @TempDir
    protected Path tempDir;

    protected File workingDir;

    @BeforeEach
    public void setUp() throws Exception {
        p4Fixture = new P4Fixture();
        clientFolder = TempDirUtils.createTempDirectoryIn(tempDir, "p4Client").toFile();
        p4Fixture.setRepo(createTestRepo());
        outputconsumer = ProcessOutputStreamConsumer.inMemoryConsumer();
        p4 = p4Fixture.createClient();
        workingDir = TempDirUtils.createRandomDirectoryIn(tempDir).toFile();
    }

    protected abstract P4TestRepo createTestRepo() throws Exception;

    @AfterEach
    public void stopP4Server() {
        p4Fixture.stop(p4);
    }

    protected static String clientConfig(String clientName, File clientFolder) {
        return "Client: " + clientName + "\n\n"
                + "Owner: cruise\n\n"
                + "Root: " + clientFolder.getAbsolutePath() + "\n\n"
                + "Options: rmdir\n\n"
                + "LineEnd: local\n\n"
                + "View:\n"
                + "\t//depot/... //" + clientName + "/...\n";

    }
}
