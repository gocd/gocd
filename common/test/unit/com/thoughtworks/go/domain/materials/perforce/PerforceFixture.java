/*
 * Copyright 2017 ThoughtWorks, Inc.
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

import java.io.File;

import com.thoughtworks.go.helper.P4TestRepo;
import com.thoughtworks.go.util.TempFiles;
import com.thoughtworks.go.util.TestFileUtil;
import com.thoughtworks.go.util.command.InMemoryStreamConsumer;
import com.thoughtworks.go.util.command.ProcessOutputStreamConsumer;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;

public abstract class PerforceFixture {
    protected P4Client p4;
    protected static File clientFolder;
    public static final String DEFAULT_CLIENT_NAME = "p4test_1";
    protected P4Fixture p4Fixture;
    protected InMemoryStreamConsumer outputconsumer;
    protected File tempDir;
    private TempFiles tmpFiles;

    @Before
    public void setUp() throws Exception {
        p4Fixture = new P4Fixture();
        p4Fixture.setRepo(createTestRepo());
        clientFolder = TestFileUtil.createTempFolder("p4Client");
        if (clientFolder == null) {
            throw new RuntimeException();
        }
        outputconsumer = ProcessOutputStreamConsumer.inMemoryConsumer();
        p4 = p4Fixture.createClient();
        tmpFiles = new TempFiles();
        tempDir = tmpFiles.mkdir("tempDir");
    }

    protected abstract P4TestRepo createTestRepo() throws Exception;

    @After
    public void stopP4Server() {
        p4Fixture.stop(p4);
        FileUtils.deleteQuietly(clientFolder);
        tmpFiles.cleanUp();
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
