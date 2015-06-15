/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
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

package com.thoughtworks.go.server.service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.sql.DataSource;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.GoConfigDao;
import com.thoughtworks.go.config.GoMailSender;
import com.thoughtworks.go.config.ServerConfig;
import com.thoughtworks.go.config.materials.SubprocessExecutionContext;
import com.thoughtworks.go.config.materials.git.GitMaterial;
import com.thoughtworks.go.database.Database;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.materials.mercurial.StringRevision;
import com.thoughtworks.go.i18n.Localizable;
import com.thoughtworks.go.i18n.Localizer;
import com.thoughtworks.go.security.CipherProvider;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.messaging.SendEmailMessage;
import com.thoughtworks.go.server.persistence.ServerBackupRepository;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.server.util.ServerVersion;
import com.thoughtworks.go.service.ConfigRepository;
import com.thoughtworks.go.util.*;
import com.thoughtworks.go.util.GoConfigFileHelper;
import com.thoughtworks.go.util.command.InMemoryStreamConsumer;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.NameFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:WEB-INF/applicationContext-acegi-security.xml"
})
public class BackupServiceIntegrationTest {

    @Autowired BackupService backupService;
    @Autowired
    GoConfigService goConfigService;
    @Autowired DataSource dataSource;
    @Autowired ArtifactsDirHolder artifactsDirHolder;
    @Autowired DatabaseAccessHelper dbHelper;
    @Autowired
    GoConfigDao goConfigDao;
    @Autowired
    ServerBackupRepository backupInfoRepository;
    @Autowired TimeProvider timeProvider;
    @Autowired Localizer localizer;
    @Autowired SystemEnvironment systemEnvironment;
    @Autowired ServerVersion serverVersion;
    @Autowired ConfigRepository configRepository;
    @Autowired private SubprocessExecutionContext subprocessExecutionContext;
    @Autowired Database databaseStrategy;

    private GoConfigFileHelper configHelper = new GoConfigFileHelper();

    private File backupsDirectory;
    private TempFiles tempFiles;
    private byte[] originalCipher;
    private Username admin;

    @Before
    public void setUp() throws Exception {
        configHelper.onSetUp();
        dbHelper.onSetUp();
        admin = new Username(new CaseInsensitiveString("admin"));
        configHelper.addSecurityWithPasswordFile();
        configHelper.addAdmins(CaseInsensitiveString.str(admin.getUsername()));
        goConfigDao.forceReload();
        backupsDirectory = new File(artifactsDirHolder.getArtifactsDir(), ServerConfig.SERVER_BACKUPS);
        cleanupBackups();
        tempFiles = new TempFiles();
        originalCipher = new CipherProvider(systemEnvironment).getKey();

        FileUtil.writeContentToFile("invalid crapy config", new File(systemEnvironment.getConfigDir(), "cruise-config.xml"));
        FileUtil.writeContentToFile("invalid crapy cipher", new File(systemEnvironment.getConfigDir(), "cipher"));
    }

    @After
    public void tearDown() throws Exception {
        tempFiles.cleanUp();
        dbHelper.onTearDown();
        cleanupBackups();
        FileUtil.writeContentToFile(goConfigService.xml(), new File(systemEnvironment.getConfigDir(), "cruise-config.xml"));
        FileUtil.writeContentToFile(originalCipher, systemEnvironment.getCipherFile());
        configHelper.onTearDown();
    }

    @Test
    public void shouldFailIfUserIsNotAnAdmin() {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        backupService.startBackup(new Username(new CaseInsensitiveString("loser")), result);
        assertThat(result.isSuccessful(), is(false));
        assertThat(result.message(localizer), is("Unauthorized to initiate Go backup as you are not a Go administrator"));
    }

    @Test
    public void shouldPerformConfigBackupForAllConfigFiles() throws Exception {
        try {
            HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

            createConfigFile("foo", "foo_foo");
            createConfigFile("bar", "bar_bar");
            createConfigFile("baz", "hazar_bar");
            createConfigFile("hello/world/file", "hello world!");
            createConfigFile("some_dir/cruise-config.xml", "some-other-cruise-config");
            createConfigFile("some_dir/cipher", "some-cipher");

            backupService.startBackup(admin, result);
            assertThat(result.isSuccessful(), is(true));
            assertThat(result.message(localizer), is("Backup completed successfully."));

            File configZip = backedUpFile("config-dir.zip");
            assertThat(fileContents(configZip, "foo"), is("foo_foo"));
            assertThat(fileContents(configZip, "bar"), is("bar_bar"));
            assertThat(fileContents(configZip, "baz"), is("hazar_bar"));

            assertThat(fileContents(configZip, FilenameUtils.separatorsToSystem("hello/world/file")), is("hello world!"));
            assertThat(fileContents(configZip, FilenameUtils.separatorsToSystem("some_dir/cruise-config.xml")), is("some-other-cruise-config"));
            assertThat(fileContents(configZip, FilenameUtils.separatorsToSystem("some_dir/cipher")), is("some-cipher"));

            assertThat(fileContents(configZip, "cruise-config.xml"), is(goConfigService.xml()));
            byte[] realCipher = (byte[]) ReflectionUtil.invoke(new CipherProvider(systemEnvironment), "getKey");
            assertThat(fileContents(configZip, "cipher").getBytes(), is(realCipher));
        } finally {
            deleteConfigFileIfExists("foo", "bar", "baz", "hello", "some_dir");
        }
    }

    @Test
    public void shouldBackupConfigRepository() throws IOException {
        configHelper.addPipeline("too-unique-to-be-present", "stage-name");

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

        backupService.startBackup(admin, result);
        assertThat(result.isSuccessful(), is(true));
        assertThat(result.message(localizer), is("Backup completed successfully."));

        File repoZip = backedUpFile("config-repo.zip");
        File repoDir = tempFiles.createUniqueFolder("expanded-config-repo-backup");
        TestUtils.extractZipToDir(repoZip, repoDir);
        File cloneDir = tempFiles.createUniqueFolder("cloned-config-repo-backup");
        GitMaterial git = new GitMaterial(repoDir.getAbsolutePath());
        List<Modification> modifications = git.latestModification(cloneDir, subprocessExecutionContext);
        String latestChangeRev = modifications.get(0).getRevision();
        assertThat(FileUtil.readContentFromFile(new File(cloneDir, "cruise-config.xml")).indexOf("too-unique-to-be-present"), greaterThan(0));
        git.updateTo(new InMemoryStreamConsumer(), new StringRevision(latestChangeRev + "~1"), cloneDir, subprocessExecutionContext);
        assertThat(FileUtil.readContentFromFile(new File(cloneDir, "cruise-config.xml")).indexOf("too-unique-to-be-present"), is(-1));
    }

    @Test
    public void shouldCaptureVersionForEveryBackup() throws IOException {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        ServerVersion serverVersion = mock(ServerVersion.class);
        when(serverVersion.version()).thenReturn("some-test-version-007");
        BackupService backupService = new BackupService(dataSource, artifactsDirHolder, goConfigService, timeProvider, backupInfoRepository, systemEnvironment, serverVersion, configRepository, databaseStrategy);
        backupService.initialize();
        backupService.startBackup(admin, result);
        assertThat(result.isSuccessful(), is(true));
        assertThat(result.message(localizer), is("Backup completed successfully."));
        File version = backedUpFile("version.txt");
        assertThat(FileUtil.readContentFromFile(version), is("some-test-version-007"));
    }

    @Test
    public void shouldSendEmailToAdminAfterTakingBackup() throws InvalidCipherTextException {
        GoConfigService configService = mock(GoConfigService.class);
        GoMailSender goMailSender = mock(GoMailSender.class);
        when(configService.getMailSender()).thenReturn(goMailSender);
        when(configService.adminEmail()).thenReturn("mail@admin.com");
        when(configService.isUserAdmin(admin)).thenReturn(true);

        TimeProvider timeProvider = mock(TimeProvider.class);
        DateTime now = new DateTime();
        when(timeProvider.currentDateTime()).thenReturn(now);

        BackupService service = new BackupService(dataSource, artifactsDirHolder, configService, timeProvider, backupInfoRepository, systemEnvironment, serverVersion, configRepository,
                databaseStrategy);
        service.initialize();
        service.startBackup(admin, new HttpLocalizedOperationResult());

        String ipAddress = SystemUtil.getFirstLocalNonLoopbackIpAddress();
        String body = String.format("Backup of the Go server at '%s' was successfully completed. The backup is stored at location: %s. This backup was triggered by 'admin'.", ipAddress, backupDir(now).getAbsolutePath());

        verify(goMailSender).send(new SendEmailMessage("Server Backup Completed Successfully", body, "mail@admin.com"));
        verifyNoMoreInteractions(goMailSender);
    }

    @Test
    public void shouldSendEmailToAdminWhenTheBackupFails() throws Exception {
        GoConfigService configService = mock(GoConfigService.class);
        when(configService.adminEmail()).thenReturn("mail@admin.com");

        GoMailSender goMailSender = mock(GoMailSender.class);
        when(configService.getMailSender()).thenReturn(goMailSender);
        when(configService.isUserAdmin(admin)).thenReturn(true);

        DateTime now = new DateTime();
        TimeProvider timeProvider = mock(TimeProvider.class);
        when(timeProvider.currentDateTime()).thenReturn(now);

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

        Database databaseStrategyMock = mock(Database.class);
        doThrow(new RuntimeException("Oh no!")).when(databaseStrategyMock).backup(any(File.class));
        BackupService service = new BackupService(dataSource, artifactsDirHolder, configService, timeProvider, backupInfoRepository, systemEnvironment, serverVersion, configRepository,
                databaseStrategyMock);
        service.initialize();
        service.startBackup(admin, result);

        String ipAddress = SystemUtil.getFirstLocalNonLoopbackIpAddress();
        String body = String.format("Backup of the Go server at '%s' has failed. The reason is: %s", ipAddress, "Oh no!");

        assertThat(result.isSuccessful(), is(false));
        assertThat(result.message(localizer), is("Failed to perform backup. Reason: Oh no!"));
        verify(goMailSender).send(new SendEmailMessage("Server Backup Failed", body, "mail@admin.com"));
        verifyNoMoreInteractions(goMailSender);

        assertThat(FileUtils.listFiles(backupsDirectory, TrueFileFilter.TRUE, TrueFileFilter.TRUE).isEmpty(), is(true));
    }

    @Test
    public void shouldReturnBackupRunningSinceValue_inISO8601_format() throws InterruptedException {
        assertThat(backupService.backupRunningSinceISO8601(), is(nullValue()));

        final Semaphore waitForBackupToStart = new Semaphore(1);
        final Semaphore waitForAssertionToCompleteWhileBackupIsOn = new Semaphore(1);
        final HttpLocalizedOperationResult result = new HttpLocalizedOperationResult() {
            @Override public void setMessage(Localizable message) {
                waitForBackupToStart.release();
                super.setMessage(message);
                try {
                    waitForAssertionToCompleteWhileBackupIsOn.acquire();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

            }
        };
        waitForAssertionToCompleteWhileBackupIsOn.acquire();
        waitForBackupToStart.acquire();
        Thread backupThd = new Thread(new Runnable() {
            public void run() {
                backupService.startBackup(admin, result);
            }
        });

        backupThd.start();
        waitForBackupToStart.acquire();
        String backupStartedTimeString = backupService.backupRunningSinceISO8601();
        DateTimeFormatter dateTimeFormatter = ISODateTimeFormat.dateTime();
        DateTime dateTime = dateTimeFormatter.parseDateTime(backupStartedTimeString);
        assertThat(ReflectionUtil.getField(backupService, "backupRunningSince"), is((Object) dateTime));
        waitForAssertionToCompleteWhileBackupIsOn.release();
        backupThd.join();
    }

     @Test
    public void shouldReturnBackupStartedBy() throws InterruptedException {
        assertThat(backupService.backupStartedBy(), is(nullValue()));

        final Semaphore waitForBackupToStart = new Semaphore(1);
        final Semaphore waitForAssertionToCompleteWhileBackupIsOn = new Semaphore(1);
        final HttpLocalizedOperationResult result = new HttpLocalizedOperationResult() {
            @Override public void setMessage(Localizable message) {
                waitForBackupToStart.release();
                super.setMessage(message);
                try {
                    waitForAssertionToCompleteWhileBackupIsOn.acquire();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

            }
        };
        waitForAssertionToCompleteWhileBackupIsOn.acquire();
        waitForBackupToStart.acquire();
        Thread backupThd = new Thread(new Runnable() {
            public void run() {
                backupService.startBackup(admin, result);
            }
        });

        backupThd.start();
        waitForBackupToStart.acquire();
        String backupStartedBy = backupService.backupStartedBy();
        assertThat(ReflectionUtil.getField(backupService, "backupStartedBy"), is((Object) backupStartedBy));
        waitForAssertionToCompleteWhileBackupIsOn.release();
        backupThd.join();
    }

    private void deleteConfigFileIfExists(String ...fileNames) {
        for (String fileName : fileNames) {
            FileUtils.deleteQuietly(new File(configDir(), fileName));
        }
    }

    private String fileContents(File location, String filename) throws IOException {
        ZipInputStream zipIn = null;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            zipIn = new ZipInputStream(new FileInputStream(location));
            while (zipIn.available() > 0) {
                ZipEntry nextEntry = zipIn.getNextEntry();
                if (nextEntry.getName().equals(filename)) {
                    IOUtils.copy(zipIn, out);
                }
            }
        } finally {
            if (zipIn != null) {
                zipIn.close();
            }
        }
        return out.toString();
    }

    private void createConfigFile(String fileName, String content) throws IOException {
        FileOutputStream fos = null;
        try {
            File file = new File(configDir(), fileName);
            FileUtils.forceMkdir(file.getParentFile());
            fos = new FileOutputStream(file);
            fos.write(content.getBytes());
        } finally {
            if (fos != null) {
                fos.close();
            }
        }
    }

    @Test
    public void shouldReturnIfBackupIsInProgress() throws SQLException, InterruptedException {
        final Semaphore waitForBackupToBegin = new Semaphore(1);
        final Semaphore waitForAssertion_whichHasToHappen_whileBackupIsRunning = new Semaphore(1);

        Database databaseStrategyMock = mock(Database.class);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                waitForBackupToBegin.release();
                waitForAssertion_whichHasToHappen_whileBackupIsRunning.acquire();
                return null;
            }
        }).when(databaseStrategyMock).backup(any(File.class));


        final BackupService backupService = new BackupService(dataSource, artifactsDirHolder, goConfigService, new TimeProvider(), backupInfoRepository, systemEnvironment,
                serverVersion, configRepository, databaseStrategyMock);

        waitForBackupToBegin.acquire();
        Thread thd = new Thread(new Runnable() {
            public void run() {
                backupService.startBackup(admin, new HttpLocalizedOperationResult());
            }
        });
        thd.start();

        waitForAssertion_whichHasToHappen_whileBackupIsRunning.acquire();
        waitForBackupToBegin.acquire();
        assertThat(backupService.isBackingUp(), is(true));
        waitForAssertion_whichHasToHappen_whileBackupIsRunning.release();

        thd.join();
    }

    private File configDir() {
        return new File(new SystemEnvironment().getConfigDir());
    }

    private File backupDir(DateTime now) {
        return new File(backupsDirectory, BackupService.BACKUP + now.toString("YYYYMMdd-HHmmss"));
    }

    private File backedUpFile(final String filename) {
        return new ArrayList<File>(FileUtils.listFiles(backupsDirectory, new NameFileFilter(filename), TrueFileFilter.TRUE)).get(0);
    }

    private void cleanupBackups() throws IOException {
        FileUtils.deleteQuietly(artifactsDirHolder.getArtifactsDir());
    }
}
