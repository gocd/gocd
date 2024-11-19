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
package com.thoughtworks.go.domain.materials.svn;

import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.materials.ValidationBean;
import com.thoughtworks.go.helper.SvnRemoteRepository;
import com.thoughtworks.go.util.TempDirUtils;
import com.thoughtworks.go.util.command.InMemoryStreamConsumer;
import com.thoughtworks.go.util.command.ProcessOutputStreamConsumer;
import org.apache.commons.io.FileUtils;
import org.jdom2.input.SAXBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static com.thoughtworks.go.util.command.ProcessOutputStreamConsumer.inMemoryConsumer;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

public class SvnCommandRemoteTest {
    public SvnRemoteRepository repository;
    private static final String HARRY = "harry";
    private static final String HARRYS_PASSWORD = "harryssecret";
    public SvnCommand command;
    public File workingDir;
    private InMemoryStreamConsumer outputStreamConsumer;

    @BeforeEach
    public void startRepo(@TempDir Path tempDir) throws Exception {
        repository = new SvnRemoteRepository(tempDir);
        repository.addUser(HARRY, HARRYS_PASSWORD);
        repository.start();
        command = new SvnCommand(null, repository.getUrl(), HARRY, HARRYS_PASSWORD, true);
        workingDir = TempDirUtils.createTempDirectoryIn(tempDir, "working-dir").toFile();
        outputStreamConsumer = inMemoryConsumer();
    }

    @AfterEach
    public void stopRepo() throws Exception {
        if (repository != null) repository.stop();
    }

    @Test
    public void shouldSupportSvnInfo() {
        SvnCommand.SvnInfo info = command.remoteInfo(new SAXBuilder());
        assertThat(info.getUrl()).isEqualTo(repository.getUrl());
    }

    @Test
    public void shouldSupportSvnLog() {
        List<Modification> info = command.latestModification();
        assertThat(info.get(0).getComment()).isEqualTo("Added simple build shell to dump the environment to console.");
    }

    @Test
    public void shouldSupportModificationsSince() {
        List<Modification> info = command.modificationsSince(new SubversionRevision(2));
        assertThat(info.size()).isEqualTo(2);
        assertThat(info.get(0).getRevision()).isEqualTo("4");
        assertThat(info.get(1).getRevision()).isEqualTo("3");
    }

    @Test
    public void shouldSupportLocalSvnInfoWithoutPassword() throws Exception {
        command.checkoutTo(ProcessOutputStreamConsumer.inMemoryConsumer(), workingDir,
                new SubversionRevision(4));

        SvnCommand commandWithoutPassword = new SvnCommand(null, repository.getUrl(), null, null, true);
        SvnCommand.SvnInfo info = commandWithoutPassword.workingDirInfo(workingDir);
        assertThat(info.getUrl()).isEqualTo(repository.getUrl());
    }

    @Test
    public void shouldMaskPassword_CheckConnection() {
        ValidationBean goodResponse = command.checkConnection();
        assertThat(goodResponse.isValid()).isTrue();
        assertThat(goodResponse.getError().contains(HARRYS_PASSWORD)).isFalse();

        ValidationBean badResponse = badUserNameCommand().checkConnection();
        assertThat(badResponse.isValid()).isFalse();
        assertThat(badResponse.getError().contains(HARRYS_PASSWORD)).isFalse();

        badResponse = badPasswordCommand().checkConnection();
        assertThat(badResponse.isValid()).isFalse();
        assertThat(badResponse.getError().contains("some_bad_password")).isFalse();

        badResponse = badUrlCommand().checkConnection();
        assertThat(badResponse.isValid()).isFalse();
        assertThat(badResponse.getError().contains(HARRYS_PASSWORD)).isFalse();
    }

    @Test
    public void shouldMaskPassword_UpdateTo() {
        command.checkoutTo(outputStreamConsumer, workingDir, new SubversionRevision(2));
        command.updateTo(outputStreamConsumer, workingDir, new SubversionRevision(2));
        assertThat(outputStreamConsumer.getAllOutput().contains(HARRYS_PASSWORD)).isFalse();

        try {
            badUserNameCommand().updateTo(outputStreamConsumer, workingDir, new SubversionRevision(2));
            fail("should have failed");
        } catch (Exception e) {
            assertThat(e.getMessage().contains(HARRYS_PASSWORD)).isFalse();
            assertThat(outputStreamConsumer.getAllOutput().contains(HARRYS_PASSWORD)).isFalse();
        }

        try {
            badPasswordCommand().updateTo(outputStreamConsumer, workingDir, new SubversionRevision(2));
            fail("should have failed");
        } catch (Exception e) {
            assertThat(e.getMessage().contains("some_bad_password")).isFalse();
            assertThat(outputStreamConsumer.getAllOutput().contains("some_bad_password")).isFalse();
        }

        try {
            badUrlCommand().updateTo(outputStreamConsumer, workingDir, new SubversionRevision(2));
            fail("should have failed");
        } catch (Exception e) {
            assertThat(e.getMessage().contains(HARRYS_PASSWORD)).isFalse();
            assertThat(outputStreamConsumer.getAllOutput().contains(HARRYS_PASSWORD)).isFalse();
        }
    }

    @Test
    public void shouldMaskPassword_CheckoutTo() {
        command.checkoutTo(outputStreamConsumer, workingDir, new SubversionRevision(2));
        assertThat(outputStreamConsumer.getAllOutput().contains(HARRYS_PASSWORD)).isFalse();

        try {
            FileUtils.deleteQuietly(workingDir);
            badUserNameCommand().checkoutTo(outputStreamConsumer, workingDir, new SubversionRevision(2));
            fail("should have failed");
        } catch (Exception e) {
            assertThat(e.getMessage().contains(HARRYS_PASSWORD)).isFalse();
            assertThat(outputStreamConsumer.getAllOutput().contains(HARRYS_PASSWORD)).isFalse();
        }

        try {
            FileUtils.deleteQuietly(workingDir);
            badPasswordCommand().checkoutTo(outputStreamConsumer, workingDir, new SubversionRevision(2));
            fail("should have failed");
        } catch (Exception e) {
            assertThat(e.getMessage().contains("some_bad_password")).isFalse();
            assertThat(outputStreamConsumer.getAllOutput().contains("some_bad_password")).isFalse();
        }

        try {
            FileUtils.deleteQuietly(workingDir);
            badUrlCommand().checkoutTo(outputStreamConsumer, workingDir, new SubversionRevision(2));
            fail("should have failed");
        } catch (Exception e) {
            assertThat(e.getMessage().contains(HARRYS_PASSWORD)).isFalse();
            assertThat(outputStreamConsumer.getAllOutput().contains(HARRYS_PASSWORD)).isFalse();
        }
    }

    @Test
    public void shouldMaskPassword_getAllExternalURLs() {
        try {
            badUserNameCommand().getAllExternalURLs();
            fail("should have failed");
        } catch (Exception e) {
            assertThat(e.getMessage().contains(HARRYS_PASSWORD)).isFalse();
        }

        try {
            badPasswordCommand().getAllExternalURLs();
            fail("should have failed");
        } catch (Exception e) {
            assertThat(e.getMessage().contains("some_bad_password")).isFalse();
        }

        try {
            badUrlCommand().getAllExternalURLs();
            fail("should have failed");
        } catch (Exception e) {
            assertThat(e.getMessage().contains(HARRYS_PASSWORD)).isFalse();
        }
    }

    @Test
    public void shouldMaskPassword_latestModification() {
        try {
            badUserNameCommand().latestModification();
            fail("should have failed");
        } catch (Exception e) {
            assertThat(e.getMessage().contains(HARRYS_PASSWORD)).isFalse();
        }

        try {
            badPasswordCommand().latestModification();
            fail("should have failed");
        } catch (Exception e) {
            assertThat(e.getMessage().contains("some_bad_password")).isFalse();
        }

        try {
            badUrlCommand().latestModification();
            fail("should have failed");
        } catch (Exception e) {
            assertThat(e.getMessage().contains(HARRYS_PASSWORD)).isFalse();
        }
    }

    @Test
    public void shouldMaskPassword_modificationsSince() {
        try {
            badUserNameCommand().latestModification();
            fail("should have failed");
        } catch (Exception e) {
            assertThat(e.getMessage().contains(HARRYS_PASSWORD)).isFalse();
        }

        try {
            badPasswordCommand().latestModification();
            fail("should have failed");
        } catch (Exception e) {
            assertThat(e.getMessage().contains("some_bad_password")).isFalse();
        }

        try {
            badUrlCommand().latestModification();
            fail("should have failed");
        } catch (Exception e) {
            assertThat(e.getMessage().contains(HARRYS_PASSWORD)).isFalse();
        }
    }

    @Test
    public void shouldMaskPassword_remoteInfo() {
        try {
            badUserNameCommand().remoteInfo(new SAXBuilder());
            fail("should have failed");
        } catch (Exception e) {
            assertThat(e.getMessage().contains(HARRYS_PASSWORD)).isFalse();
        }

        try {
            badPasswordCommand().remoteInfo(new SAXBuilder());
            fail("should have failed");
        } catch (Exception e) {
            assertThat(e.getMessage().contains("some_bad_password")).isFalse();
        }

        try {
            badUrlCommand().remoteInfo(new SAXBuilder());
            fail("should have failed");
        } catch (Exception e) {
            assertThat(e.getMessage().contains(HARRYS_PASSWORD)).isFalse();
        }
    }

    @Test
    public void shouldMaskPassword_workingDirInfo() {
        try {
            badUserNameCommand().workingDirInfo(workingDir);
            fail("should have failed");
        } catch (Exception e) {
            assertThat(e.getMessage().contains(HARRYS_PASSWORD)).isFalse();
        }

        try {
            badPasswordCommand().workingDirInfo(workingDir);
            fail("should have failed");
        } catch (Exception e) {
            assertThat(e.getMessage().contains("some_bad_password")).isFalse();
        }

        try {
            badUrlCommand().workingDirInfo(workingDir);
            fail("should have failed");
        } catch (Exception e) {
            assertThat(e.getMessage().contains(HARRYS_PASSWORD)).isFalse();
        }
    }

    @Test
    public void shouldMaskPassword_commit() throws IOException {
        command.checkoutTo(outputStreamConsumer, workingDir, new SubversionRevision(2));
        File newFile = new File(workingDir.getAbsolutePath() + "/foo");
        FileUtils.writeStringToFile(newFile, "content", UTF_8);
        command.add(outputStreamConsumer, newFile);

        try {
            badUserNameCommand().commit(outputStreamConsumer, workingDir, "message");
            fail("should have failed");
        } catch (Exception e) {
            assertThat(outputStreamConsumer.getAllOutput().contains(HARRYS_PASSWORD)).isFalse();
            assertThat(e.getMessage().contains(HARRYS_PASSWORD)).isFalse();
        }

        try {
            badPasswordCommand().commit(outputStreamConsumer, workingDir, "message");
            fail("should have failed");
        } catch (Exception e) {
            assertThat(outputStreamConsumer.getAllOutput().contains("some_bad_password")).isFalse();
            assertThat(e.getMessage().contains("some_bad_password")).isFalse();
        }

        try {
            badUrlCommand().commit(outputStreamConsumer, workingDir, "message");
            fail("should have failed");
        } catch (Exception e) {
            assertThat(outputStreamConsumer.getAllOutput().contains(HARRYS_PASSWORD)).isFalse();
            assertThat(e.getMessage().contains(HARRYS_PASSWORD)).isFalse();
        }
    }

    @Test
    public void shouldMaskPassword_propset() {
        try {
            badUserNameCommand().propset(workingDir, "svn:ignore", "*.foo");
            fail("should have failed");
        } catch (Exception e) {
            assertThat(e.getMessage().contains(HARRYS_PASSWORD)).isFalse();
        }

        try {
            badPasswordCommand().propset(workingDir, "svn:ignore", "*.foo");
            fail("should have failed");
        } catch (Exception e) {
            assertThat(e.getMessage().contains("some_bad_password")).isFalse();
        }

        try {
            badUrlCommand().propset(workingDir, "svn:ignore", "*.foo");
            fail("should have failed");
        } catch (Exception e) {
            assertThat(e.getMessage().contains(HARRYS_PASSWORD)).isFalse();
        }
    }

    private SvnCommand badUrlCommand() {
        return new SvnCommand(null, "https://invalid", "blrstdcrspair", HARRYS_PASSWORD, false);
    }

    private SvnCommand badUserNameCommand() {
        return new SvnCommand(null, repository.getUrl(), "some_bad_user", HARRYS_PASSWORD, false);
    }

    private SvnCommand badPasswordCommand() {
        return new SvnCommand(null, repository.getUrl(), HARRY, "some_bad_password", false);
    }

}
