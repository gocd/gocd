/*************************GO-LICENSE-START*********************************
 * Copyright 2016 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.domain.materials.svn;

import java.io.File;
import java.io.IOException;
import java.util.List;

import com.googlecode.junit.ext.JunitExtRunner;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.materials.ValidationBean;
import com.thoughtworks.go.helper.SvnRemoteRepository;
import com.thoughtworks.go.util.FileUtil;
import com.thoughtworks.go.util.TestFileUtil;
import com.thoughtworks.go.util.command.InMemoryStreamConsumer;
import com.thoughtworks.go.util.command.ProcessOutputStreamConsumer;
import org.apache.commons.io.FileUtils;
import org.hamcrest.core.Is;
import org.jdom.input.SAXBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.thoughtworks.go.util.command.ProcessOutputStreamConsumer.inMemoryConsumer;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

@RunWith(JunitExtRunner.class)
public class SvnCommandRemoteTest {
    public SvnRemoteRepository repository;
    private static final String HARRY = "harry";
    private static final String HARRYS_PASSWORD = "harryssecret";
    public SvnCommand command;
    public File workingDir;
    private InMemoryStreamConsumer outputStreamConsumer;

    @Before
    public void startRepo() throws Exception {
        repository = new SvnRemoteRepository();
        repository.addUser(HARRY, HARRYS_PASSWORD);
        repository.start();
        command = new SvnCommand(null, repository.getUrl(), HARRY, HARRYS_PASSWORD, true);
        workingDir = TestFileUtil.createTempFolder("workingDir" + System.currentTimeMillis());
        outputStreamConsumer = inMemoryConsumer();
    }

    @After
    public void stopRepo() throws Exception {
        if (repository!=null) repository.stop();
        FileUtil.deleteFolder(workingDir);
    }

    @Test public void shouldSupportSvnInfo() throws Exception {
        SvnCommand.SvnInfo info = command.remoteInfo(new SAXBuilder());
        assertThat(info.getUrl(), is(repository.getUrl()));
    }

    @Test public void shouldSupportSvnLog() throws Exception {
        List<Modification> info = command.latestModification();
        assertThat(info.get(0).getComment(), is("Added simple build shell to dump the environment to console."));
    }

    @Test public void shouldSupportModificationsSince() throws Exception {
        List<Modification> info = command.modificationsSince(new SubversionRevision(2));
        assertThat(info.size(), is(2));
        assertThat(info.get(0).getRevision(), is("4"));
        assertThat(info.get(1).getRevision(), is("3"));
    }

    @Test public void shouldSupportLocalSvnInfoWithoutPassword() throws Exception {
        command.checkoutTo(ProcessOutputStreamConsumer.inMemoryConsumer(), workingDir,
                new SubversionRevision(4));

        SvnCommand commandWithoutPassword = new SvnCommand(null, repository.getUrl(), null, null, true);
        SvnCommand.SvnInfo info = commandWithoutPassword.workingDirInfo(workingDir);
        assertThat(info.getUrl(), is(repository.getUrl()));
    }

    @Test
    public void shouldMaskPassword_CheckConnection() {
        ValidationBean goodResponse = command.checkConnection();
        assertThat(goodResponse.isValid(), Is.is(true));
        assertThat("Plain text password detected!", goodResponse.getError().contains(HARRYS_PASSWORD), Is.is(false));

        ValidationBean badResponse = badUserNameCommand().checkConnection();
        assertThat(badResponse.isValid(), Is.is(false));
        assertThat("Plain text password detected!", badResponse.getError().contains(HARRYS_PASSWORD), Is.is(false));

        badResponse = badPasswordCommand().checkConnection();
        assertThat(badResponse.isValid(), Is.is(false));
        assertThat("Plain text password detected!", badResponse.getError().contains("some_bad_password"), Is.is(false));

        badResponse = badUrlCommand().checkConnection();
        assertThat(badResponse.isValid(), Is.is(false));
        assertThat("Plain text password detected!", badResponse.getError().contains(HARRYS_PASSWORD), Is.is(false));
    }

    @Test
    public void shouldMaskPassword_UpdateTo() {
        command.checkoutTo(outputStreamConsumer, workingDir, new SubversionRevision(2));
        command.updateTo(outputStreamConsumer, workingDir, new SubversionRevision(2));
        assertThat("Plain text password detected!", outputStreamConsumer.getAllOutput().contains(HARRYS_PASSWORD), Is.is(false));

        try {
            badUserNameCommand().updateTo(outputStreamConsumer, workingDir, new SubversionRevision(2));
            fail("should have failed");
        } catch (Exception e) {
            assertThat("Plain text password detected!", e.getMessage().contains(HARRYS_PASSWORD), Is.is(false));
            assertThat("Plain text password detected!", outputStreamConsumer.getAllOutput().contains(HARRYS_PASSWORD), Is.is(false));
        }

        try {
            badPasswordCommand().updateTo(outputStreamConsumer, workingDir, new SubversionRevision(2));
            fail("should have failed");
        } catch (Exception e) {
            assertThat("Plain text password detected!", e.getMessage().contains("some_bad_password"), Is.is(false));
            assertThat("Plain text password detected!", outputStreamConsumer.getAllOutput().contains("some_bad_password"), Is.is(false));
        }

        try {
            badUrlCommand().updateTo(outputStreamConsumer, workingDir, new SubversionRevision(2));
            fail("should have failed");
        } catch (Exception e) {
            assertThat("Plain text password detected!", e.getMessage().contains(HARRYS_PASSWORD), Is.is(false));
            assertThat("Plain text password detected!", outputStreamConsumer.getAllOutput().contains(HARRYS_PASSWORD), Is.is(false));
        }
    }

    @Test
    public void shouldMaskPassword_CheckoutTo() {
        command.checkoutTo(outputStreamConsumer, workingDir, new SubversionRevision(2));
        assertThat("Plain text password detected!", outputStreamConsumer.getAllOutput().contains(HARRYS_PASSWORD), Is.is(false));

        try {
            FileUtil.deleteFolder(workingDir);
            badUserNameCommand().checkoutTo(outputStreamConsumer, workingDir, new SubversionRevision(2));
            fail("should have failed");
        } catch (Exception e) {
            assertThat("Plain text password detected!", e.getMessage().contains(HARRYS_PASSWORD), Is.is(false));
            assertThat("Plain text password detected!", outputStreamConsumer.getAllOutput().contains(HARRYS_PASSWORD), Is.is(false));
        }

        try {
            FileUtil.deleteFolder(workingDir);
            badPasswordCommand().checkoutTo(outputStreamConsumer, workingDir, new SubversionRevision(2));
            fail("should have failed");
        } catch (Exception e) {
            assertThat("Plain text password detected!", e.getMessage().contains("some_bad_password"), Is.is(false));
            assertThat("Plain text password detected!", outputStreamConsumer.getAllOutput().contains("some_bad_password"), Is.is(false));
        }

        try {
            FileUtil.deleteFolder(workingDir);
            badUrlCommand().checkoutTo(outputStreamConsumer, workingDir, new SubversionRevision(2));
            fail("should have failed");
        } catch (Exception e) {
            assertThat("Plain text password detected!", e.getMessage().contains(HARRYS_PASSWORD), Is.is(false));
            assertThat("Plain text password detected!", outputStreamConsumer.getAllOutput().contains(HARRYS_PASSWORD), Is.is(false));
        }
    }

    @Test
    public void shouldMaskPassword_getAllExternalURLs() {
        try {
            badUserNameCommand().getAllExternalURLs();
            fail("should have failed");
        } catch (Exception e) {
            assertThat("Plain text password detected!", e.getMessage().contains(HARRYS_PASSWORD), Is.is(false));
        }

        try {
            badPasswordCommand().getAllExternalURLs();
            fail("should have failed");
        } catch (Exception e) {
            assertThat("Plain text password detected!", e.getMessage().contains("some_bad_password"), Is.is(false));
        }

        try {
            badUrlCommand().getAllExternalURLs();
            fail("should have failed");
        } catch (Exception e) {
            assertThat("Plain text password detected!", e.getMessage().contains(HARRYS_PASSWORD), Is.is(false));
        }
    }

    @Test
    public void shouldMaskPassword_latestModification() {
        try {
            badUserNameCommand().latestModification();
            fail("should have failed");
        } catch (Exception e) {
            assertThat("Plain text password detected!", e.getMessage().contains(HARRYS_PASSWORD), Is.is(false));
        }

        try {
            badPasswordCommand().latestModification();
            fail("should have failed");
        } catch (Exception e) {
            assertThat("Plain text password detected!", e.getMessage().contains("some_bad_password"), Is.is(false));
        }

        try {
            badUrlCommand().latestModification();
            fail("should have failed");
        } catch (Exception e) {
            assertThat("Plain text password detected!", e.getMessage().contains(HARRYS_PASSWORD), Is.is(false));
        }
    }

    @Test
    public void shouldMaskPassword_modificationsSince() {
        try {
            badUserNameCommand().latestModification();
            fail("should have failed");
        } catch (Exception e) {
            assertThat("Plain text password detected!", e.getMessage().contains(HARRYS_PASSWORD), Is.is(false));
        }

        try {
            badPasswordCommand().latestModification();
            fail("should have failed");
        } catch (Exception e) {
            assertThat("Plain text password detected!", e.getMessage().contains("some_bad_password"), Is.is(false));
        }

        try {
            badUrlCommand().latestModification();
            fail("should have failed");
        } catch (Exception e) {
            assertThat("Plain text password detected!", e.getMessage().contains(HARRYS_PASSWORD), Is.is(false));
        }
    }

    @Test
    public void shouldMaskPassword_remoteInfo() {
        try {
            badUserNameCommand().remoteInfo(new SAXBuilder());
            fail("should have failed");
        } catch (Exception e) {
            assertThat("Plain text password detected!", e.getMessage().contains(HARRYS_PASSWORD), Is.is(false));
        }

        try {
            badPasswordCommand().remoteInfo(new SAXBuilder());
            fail("should have failed");
        } catch (Exception e) {
            assertThat("Plain text password detected!", e.getMessage().contains("some_bad_password"), Is.is(false));
        }

        try {
            badUrlCommand().remoteInfo(new SAXBuilder());
            fail("should have failed");
        } catch (Exception e) {
            assertThat("Plain text password detected!", e.getMessage().contains(HARRYS_PASSWORD), Is.is(false));
        }
    }

    @Test
    public void shouldMaskPassword_workingDirInfo() {
        try {
            badUserNameCommand().workingDirInfo(workingDir);
            fail("should have failed");
        } catch (Exception e) {
            assertThat("Plain text password detected!", e.getMessage().contains(HARRYS_PASSWORD), Is.is(false));
        }

        try {
            badPasswordCommand().workingDirInfo(workingDir);
            fail("should have failed");
        } catch (Exception e) {
            assertThat("Plain text password detected!", e.getMessage().contains("some_bad_password"), Is.is(false));
        }

        try {
            badUrlCommand().workingDirInfo(workingDir);
            fail("should have failed");
        } catch (Exception e) {
            assertThat("Plain text password detected!", e.getMessage().contains(HARRYS_PASSWORD), Is.is(false));
        }
    }

    @Test
    public void shouldMaskPassword_commit() throws IOException {
        command.checkoutTo(outputStreamConsumer, workingDir, new SubversionRevision(2));
        File newFile = new File(workingDir.getAbsolutePath() + "/foo");
        FileUtils.writeStringToFile(newFile, "content");
        command.add(outputStreamConsumer, newFile);

        try {
            badUserNameCommand().commit(outputStreamConsumer, workingDir, "message");
            fail("should have failed");
        } catch (Exception e) {
            assertThat("Plain text password detected!", outputStreamConsumer.getAllOutput().contains(HARRYS_PASSWORD), Is.is(false));
            assertThat("Plain text password detected!", e.getMessage().contains(HARRYS_PASSWORD), Is.is(false));
        }

        try {
            badPasswordCommand().commit(outputStreamConsumer, workingDir, "message");
            fail("should have failed");
        } catch (Exception e) {
            assertThat("Plain text password detected!", outputStreamConsumer.getAllOutput().contains("some_bad_password"), Is.is(false));
            assertThat("Plain text password detected!", e.getMessage().contains("some_bad_password"), Is.is(false));
        }

        try {
            badUrlCommand().commit(outputStreamConsumer, workingDir, "message");
            fail("should have failed");
        } catch (Exception e) {
            assertThat("Plain text password detected!", outputStreamConsumer.getAllOutput().contains(HARRYS_PASSWORD), Is.is(false));
            assertThat("Plain text password detected!", e.getMessage().contains(HARRYS_PASSWORD), Is.is(false));
        }
    }

    @Test
    public void shouldMaskPassword_propset() throws IOException {
        try {
            badUserNameCommand().propset(workingDir, "svn:ignore", "*.foo");
            fail("should have failed");
        } catch (Exception e) {
            assertThat("Plain text password detected!", e.getMessage().contains(HARRYS_PASSWORD), Is.is(false));
        }

        try {
            badPasswordCommand().propset(workingDir, "svn:ignore", "*.foo");
            fail("should have failed");
        } catch (Exception e) {
            assertThat("Plain text password detected!", e.getMessage().contains("some_bad_password"), Is.is(false));
        }

        try {
            badUrlCommand().propset(workingDir, "svn:ignore", "*.foo");
            fail("should have failed");
        } catch (Exception e) {
            assertThat("Plain text password detected!", e.getMessage().contains(HARRYS_PASSWORD), Is.is(false));
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
