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
import com.thoughtworks.go.util.command.InMemoryStreamConsumer;
import com.thoughtworks.go.util.command.ProcessOutputStreamConsumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.rules.TemporaryFolder;

import java.io.File;

public abstract class PerforceFixture {
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    protected P4Client p4;
    protected File clientFolder;
    public static final String DEFAULT_CLIENT_NAME = "p4test_1";
    protected P4Fixture p4Fixture;
    protected InMemoryStreamConsumer outputconsumer;
    protected File tempDir;

    @BeforeEach
    public void setUp() throws Exception {
        p4Fixture = new P4Fixture();
        temporaryFolder.create();
        clientFolder = temporaryFolder.newFolder("p4Client");
        p4Fixture.setRepo(createTestRepo());
        outputconsumer = ProcessOutputStreamConsumer.inMemoryConsumer();
        p4 = p4Fixture.createClient();
        tempDir = temporaryFolder.newFolder();
    }

    protected abstract P4TestRepo createTestRepo() throws Exception;

    @AfterEach
    public void stopP4Server() {
        p4Fixture.stop(p4);
        temporaryFolder.delete();
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
