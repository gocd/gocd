/*
 * Copyright 2024 Thoughtworks, Inc.
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
    public static final String DEFAULT_CLIENT_NAME = "p4test_1";

    protected P4Client p4;
    protected File clientFolder;
    protected P4Fixture p4Fixture;
    protected InMemoryStreamConsumer outputconsumer;

    @TempDir
    protected Path tempDir;

    @BeforeEach
    public void setUp() throws Exception {
        p4Fixture = new P4Fixture();
        clientFolder = TempDirUtils.createTempDirectoryIn(tempDir, "p4Client").toFile();
        p4Fixture.setRepo(createTestRepo().onSetup());
        outputconsumer = ProcessOutputStreamConsumer.inMemoryConsumer();
        p4 = p4Fixture.createClient();
    }

    protected P4TestRepo createTestRepo() throws Exception {
        return P4TestRepo.createP4TestRepo(tempDir, clientFolder);
    }

    @AfterEach
    public void stopP4Server() {
        p4Fixture.stop(p4);
    }

    protected static String clientConfig(String clientName, File clientFolder) {
        return ("""
                Client: %s

                Owner: cruise

                Root: %s

                Options: rmdir

                LineEnd: local

                View:
                \t//depot/... //%s/...
                """).formatted(clientName, clientFolder.getAbsolutePath(), clientName);

    }
}
