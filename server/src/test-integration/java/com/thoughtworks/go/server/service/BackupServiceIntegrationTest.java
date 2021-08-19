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
package com.thoughtworks.go.server.service;

import com.thoughtworks.go.CurrentGoCDVersion;
import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.materials.SubprocessExecutionContext;
import com.thoughtworks.go.config.materials.git.GitMaterial;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.materials.RevisionContext;
import com.thoughtworks.go.domain.materials.mercurial.StringRevision;
import com.thoughtworks.go.security.AESCipherProvider;
import com.thoughtworks.go.security.DESCipherProvider;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.database.Database;
import com.thoughtworks.go.server.domain.BackupProgressStatus;
import com.thoughtworks.go.server.domain.ServerBackup;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.messaging.SendEmailMessage;
import com.thoughtworks.go.server.messaging.ServerBackupQueue;
import com.thoughtworks.go.server.persistence.ServerBackupRepository;
import com.thoughtworks.go.server.service.backup.BackupUpdateListener;
import com.thoughtworks.go.service.ConfigRepository;
import com.thoughtworks.go.util.*;
import com.thoughtworks.go.util.command.InMemoryStreamConsumer;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.NameFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.sql.DataSource;
import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Semaphore;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.codec.binary.Hex.encodeHexString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {
        "classpath:/applicationContext-global.xml",
        "classpath:/applicationContext-dataLocalAccess.xml",
        "classpath:/testPropertyConfigurer.xml",
        "classpath:/spring-all-servlet.xml",
})
public class BackupServiceIntegrationTest {

    BackupService backupService;
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
    @Autowired SystemEnvironment systemEnvironment;
    @Autowired ConfigRepository configRepository;
    @Autowired private SubprocessExecutionContext subprocessExecutionContext;
    @Autowired Database databaseStrategy;
    @Autowired ServerBackupQueue backupQueue;

    private GoConfigFileHelper configHelper = new GoConfigFileHelper();

    private File backupsDirectory;
    private byte[] originalCipher;
    private Username admin;
    private final String WRAPPER_CONFIG_DIR  = "wrapper-config";
    private SystemEnvironment systemEnvSpy;


    @BeforeEach
    public void setUp() throws Exception {
        configHelper.onSetUp();
        dbHelper.onSetUp();
        admin = new Username(new CaseInsensitiveString("admin"));
        configHelper.enableSecurity();
        configHelper.addAdmins(CaseInsensitiveString.str(admin.getUsername()));
        goConfigDao.forceReload();
        backupsDirectory = new File(artifactsDirHolder.getArtifactsDir(), ServerConfig.SERVER_BACKUPS);
        cleanupBackups();
        originalCipher = new DESCipherProvider(systemEnvironment).getKey();

        FileUtils.writeStringToFile(new File(systemEnvironment.getConfigDir(), "cruise-config.xml"), "invalid crapy config", UTF_8);
        FileUtils.writeStringToFile(new File(systemEnvironment.getConfigDir(), "cipher"), "invalid crapy cipher", UTF_8);
        FileUtils.writeStringToFile(new File(systemEnvironment.getConfigDir(), "cipher.aes"), "invalid crapy cipher", UTF_8);

        systemEnvSpy = spy(systemEnvironment);
        when(systemEnvSpy.wrapperConfigDirPath()).thenReturn(Optional.of(WRAPPER_CONFIG_DIR));
        backupService = new BackupService(artifactsDirHolder, goConfigService, timeProvider, backupInfoRepository,
                systemEnvSpy, configRepository, databaseStrategy, null);
    }

    @AfterEach
    public void tearDown() throws Exception {
        dbHelper.onTearDown();
        cleanupBackups();
        FileUtils.writeStringToFile(new File(systemEnvironment.getConfigDir(), "cruise-config.xml"), goConfigService.xml(), UTF_8);
        FileUtils.writeByteArrayToFile(systemEnvironment.getDESCipherFile(), originalCipher);
        configHelper.onTearDown();
    }

    @Test
    public void shouldPerformConfigBackupForAllConfigFiles() throws Exception {
        try {
            createConfigFile("foo", "foo_foo");
            createConfigFile("bar", "bar_bar");
            createConfigFile("baz", "hazar_bar");
            createConfigFile("hello/world/file", "hello world!");
            createConfigFile("some_dir/cruise-config.xml", "some-other-cruise-config");
            createConfigFile("some_dir/cipher", "some-cipher");

            ServerBackup backup = backupService.startBackup(admin);
            assertThat(backup.isSuccessful(), is(true));
            assertThat(backup.getMessage(), is("Backup was generated successfully."));

            File configZip = backedUpFile("config-dir.zip");
            assertThat(fileContents(configZip, "foo"), is("foo_foo"));
            assertThat(fileContents(configZip, "bar"), is("bar_bar"));
            assertThat(fileContents(configZip, "baz"), is("hazar_bar"));

            assertThat(fileContents(configZip, FilenameUtils.separatorsToSystem("hello/world/file")), is("hello world!"));
            assertThat(fileContents(configZip, FilenameUtils.separatorsToSystem("some_dir/cruise-config.xml")), is("some-other-cruise-config"));
            assertThat(fileContents(configZip, FilenameUtils.separatorsToSystem("some_dir/cipher")), is("some-cipher"));

            assertThat(fileContents(configZip, "cruise-config.xml"), is(goConfigService.xml()));

            byte[] realDesCipher = new DESCipherProvider(systemEnvironment).getKey();
            byte[] realAESCipher = new AESCipherProvider(systemEnvironment).getKey();
            assertThat(fileContents(configZip, "cipher"), is(encodeHexString(realDesCipher)));
            assertThat(fileContents(configZip, "cipher.aes"), is(encodeHexString(realAESCipher)));
        } finally {
            deleteConfigFileIfExists("foo", "bar", "baz", "hello", "some_dir");
        }
    }

    @Test
    public void shouldPerformWrapperConfigBackupForAllTanukiConfigFiles() throws Exception {
        try {
            createWrapperConfigFile("foo", "foo_foo");
            createWrapperConfigFile("bar", "bar_bar");
            createWrapperConfigFile("baz", "hazar_bar");
            createWrapperConfigFile("hello/world/file", "hello world!");

            ServerBackup backup = backupService.startBackup(admin);
            assertThat(backup.isSuccessful(), is(true));
            assertThat(backup.getMessage(), is("Backup was generated successfully."));

            File wrapperConfigZip = backedUpFile("wrapper-config-dir.zip");
            assertThat(fileContents(wrapperConfigZip, "foo"), is("foo_foo"));
            assertThat(fileContents(wrapperConfigZip, "bar"), is("bar_bar"));
            assertThat(fileContents(wrapperConfigZip, "baz"), is("hazar_bar"));

            assertThat(fileContents(wrapperConfigZip, FilenameUtils.separatorsToSystem("hello/world/file")), is("hello world!"));
        } finally {
            deleteWrapperConfigFileIfExists("foo", "bar", "baz", "hello", "some_dir");
        }
    }

    @Test
    public void shouldNotBackupWrapperConfigsIfWrapperConfigDirEnvVariableNotSet() throws Exception {
        try {
            createWrapperConfigFile("foo", "foo_foo");
            createWrapperConfigFile("bar", "bar_bar");

            when(systemEnvSpy.wrapperConfigDirPath()).thenReturn(Optional.ofNullable(null));

            ServerBackup backup = backupService.startBackup(admin);

            assertThat(backup.isSuccessful(), is(true));
            assertThat(backup.getMessage(), is("Backup was generated successfully. Backup of wrapper configuration was skipped as the wrapper configuration directory path is unknown."));

            assertFalse(fileExists("wrapper-config-dir.zip"));
        } finally {
            deleteWrapperConfigFileIfExists("foo", "bar");
        }
    }

    @Test
    public void shouldBackupConfigRepository(@TempDir Path temporaryFolder) throws IOException {
        configHelper.addPipeline("too-unique-to-be-present", "stage-name");

        ServerBackup backup = backupService.startBackup(admin);
        assertThat(backup.isSuccessful(), is(true));
        assertThat(backup.getMessage(), is("Backup was generated successfully."));

        File repoZip = backedUpFile("config-repo.zip");
        File repoDir = TempDirUtils.createTempDirectoryIn(temporaryFolder, "expanded-config-repo-backup").toFile();
        new ZipUtil().unzip(repoZip, repoDir);
        File cloneDir = TempDirUtils.createTempDirectoryIn(temporaryFolder, "cloned-config-repo-backup").toFile();
        GitMaterial git = new GitMaterial(repoDir.getAbsolutePath());

        List<Modification> modifications = git.latestModification(cloneDir, subprocessExecutionContext);
        String latestChangeRev = modifications.get(0).getRevision();
        git.checkout(cloneDir, new StringRevision(latestChangeRev), subprocessExecutionContext);
        assertThat(FileUtils.readFileToString(new File(cloneDir, "cruise-config.xml"), UTF_8).indexOf("too-unique-to-be-present"), greaterThan(0));
        StringRevision revision = new StringRevision(latestChangeRev + "~1");
        git.updateTo(new InMemoryStreamConsumer(), cloneDir, new RevisionContext(revision), subprocessExecutionContext);
        assertThat(FileUtils.readFileToString(new File(cloneDir, "cruise-config.xml"), UTF_8).indexOf("too-unique-to-be-present"), is(-1));

        // Workaround issue with deletion of symlinks via JUnit TempDir by pre-deleting
        FileUtils.deleteQuietly(cloneDir);
    }

    @Test
    public void shouldCaptureVersionForEveryBackup() throws IOException {
        BackupService backupService = new BackupService(artifactsDirHolder, goConfigService, timeProvider, backupInfoRepository, systemEnvSpy, configRepository, databaseStrategy, null);
        ServerBackup backup = backupService.startBackup(admin);
        assertThat(backup.isSuccessful(), is(true));
        assertThat(backup.getMessage(), is("Backup was generated successfully."));

        File version = backedUpFile("version.txt");
        assertThat(FileUtils.readFileToString(version, UTF_8), is(CurrentGoCDVersion.getInstance().formatted()));
    }

    @Test
    public void shouldSendEmailToAdminAfterTakingBackup() {
        GoConfigService configService = mock(GoConfigService.class);
        ServerConfig serverConfig = new ServerConfig();
        serverConfig.setBackupConfig(new BackupConfig().setEmailOnSuccess(true).setEmailOnFailure(true));
        when(configService.serverConfig()).thenReturn(serverConfig);
        GoMailSender goMailSender = mock(GoMailSender.class);
        when(configService.getMailSender()).thenReturn(goMailSender);
        when(configService.adminEmail()).thenReturn("mail@admin.com");
        when(configService.isUserAdmin(admin)).thenReturn(true);

        TimeProvider timeProvider = mock(TimeProvider.class);
        DateTime now = new DateTime();
        when(timeProvider.currentDateTime()).thenReturn(now);

        BackupService service = new BackupService(artifactsDirHolder, configService, timeProvider, backupInfoRepository, systemEnvSpy, configRepository,
                databaseStrategy, null);
        service.startBackup(admin);

        String ipAddress = SystemUtil.getFirstLocalNonLoopbackIpAddress();
        String body = String.format("Backup of the Go server at '%s' was successfully completed. The backup is stored at location: %s. This backup was triggered by 'admin'.", ipAddress, backupDir(now).getAbsolutePath());

        verify(goMailSender).send(new SendEmailMessage("Server Backup Completed Successfully", body, "mail@admin.com"));
        verifyNoMoreInteractions(goMailSender);
    }

    @Test
    public void shouldNotSendEmailToAdminAfterTakingBackupIfEmailConfigIsNotSet() {
        GoConfigService configService = mock(GoConfigService.class);
        ServerConfig serverConfig = new ServerConfig();
        serverConfig.setBackupConfig(new BackupConfig());
        when(configService.serverConfig()).thenReturn(serverConfig);
        GoMailSender goMailSender = mock(GoMailSender.class);
        when(configService.getMailSender()).thenReturn(goMailSender);
        when(configService.adminEmail()).thenReturn("mail@admin.com");
        when(configService.isUserAdmin(admin)).thenReturn(true);

        TimeProvider timeProvider = mock(TimeProvider.class);
        DateTime now = new DateTime();
        when(timeProvider.currentDateTime()).thenReturn(now);

        BackupService service = new BackupService(artifactsDirHolder, configService, timeProvider, backupInfoRepository, systemEnvSpy, configRepository,
                databaseStrategy, null);
        service.startBackup(admin);

        verifyNoMoreInteractions(goMailSender);
    }

    @Test
    public void shouldSendEmailToAdminWhenTheBackupFails() throws Exception {
        GoConfigService configService = mock(GoConfigService.class);
        ServerConfig serverConfig = new ServerConfig();
        serverConfig.setBackupConfig(new BackupConfig().setEmailOnFailure(true));
        when(configService.serverConfig()).thenReturn(serverConfig);
        when(configService.adminEmail()).thenReturn("mail@admin.com");

        GoMailSender goMailSender = mock(GoMailSender.class);
        when(configService.getMailSender()).thenReturn(goMailSender);
        when(configService.isUserAdmin(admin)).thenReturn(true);

        DateTime now = new DateTime();
        TimeProvider timeProvider = mock(TimeProvider.class);
        when(timeProvider.currentDateTime()).thenReturn(now);

        Database databaseStrategyMock = mock(Database.class);
        doThrow(new RuntimeException("Oh no!")).when(databaseStrategyMock).backup(any(File.class));
        BackupService service = new BackupService(artifactsDirHolder, configService, timeProvider, backupInfoRepository, systemEnvSpy, configRepository,
                databaseStrategyMock, null);
        ServerBackup backup = service.startBackup(admin);

        String ipAddress = SystemUtil.getFirstLocalNonLoopbackIpAddress();
        String body = String.format("Backup of the Go server at '%s' has failed. The reason is: %s", ipAddress, "Oh no!");

        assertThat(backup.isSuccessful(), is(false));
        assertThat(backup.getMessage(), is("Failed to perform backup. Reason: Oh no!"));
        verify(goMailSender).send(new SendEmailMessage("Server Backup Failed", body, "mail@admin.com"));
        verifyNoMoreInteractions(goMailSender);

        assertThat(FileUtils.listFiles(backupsDirectory, TrueFileFilter.TRUE, TrueFileFilter.TRUE).isEmpty(), is(true));
    }

    @Test
    public void shouldNotSendEmailToAdminWhenTheBackupFailsAndEmailConfigIsNotSet() throws Exception {
        GoConfigService configService = mock(GoConfigService.class);
        ServerConfig serverConfig = new ServerConfig();
        serverConfig.setBackupConfig(new BackupConfig());
        when(configService.serverConfig()).thenReturn(serverConfig);
        when(configService.adminEmail()).thenReturn("mail@admin.com");

        GoMailSender goMailSender = mock(GoMailSender.class);
        when(configService.getMailSender()).thenReturn(goMailSender);
        when(configService.isUserAdmin(admin)).thenReturn(true);

        DateTime now = new DateTime();
        TimeProvider timeProvider = mock(TimeProvider.class);
        when(timeProvider.currentDateTime()).thenReturn(now);

        Database databaseStrategyMock = mock(Database.class);
        doThrow(new RuntimeException("Oh no!")).when(databaseStrategyMock).backup(any(File.class));
        BackupService service = new BackupService(artifactsDirHolder, configService, timeProvider, backupInfoRepository, systemEnvSpy, configRepository,
                databaseStrategyMock, null);
        ServerBackup backup = service.startBackup(admin);

        assertThat(backup.isSuccessful(), is(false));
        assertThat(backup.getMessage(), is("Failed to perform backup. Reason: Oh no!"));
        verifyNoMoreInteractions(goMailSender);

        assertThat(FileUtils.listFiles(backupsDirectory, TrueFileFilter.TRUE, TrueFileFilter.TRUE).isEmpty(), is(true));
    }

    @Test
    public void shouldReturnBackupRunningSinceValue_inISO8601_format() throws InterruptedException {
        assertThat(backupService.backupRunningSinceISO8601(), is(Optional.empty()));

        final Semaphore waitForBackupToStart = new Semaphore(1);
        final Semaphore waitForAssertionToCompleteWhileBackupIsOn = new Semaphore(1);
        final BackupUpdateListener backupUpdateListener = new BackupUpdateListener() {
            private boolean backupStarted = false;
            @Override
            public void updateStep(BackupProgressStatus step) {
                if (!backupStarted) {
                    backupStarted = true;
                    waitForBackupToStart.release();
                    try {
                        waitForAssertionToCompleteWhileBackupIsOn.acquire();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }

            @Override
            public void error(String message) {
            }

            @Override
            public void completed(String message) {
            }
        };

        waitForAssertionToCompleteWhileBackupIsOn.acquire();
        waitForBackupToStart.acquire();
        Thread backupThd = new Thread(() -> backupService.startBackup(admin, backupUpdateListener));

        backupThd.start();
        waitForBackupToStart.acquire();
        String backupStartedTimeString = backupService.backupRunningSinceISO8601().get();
        DateTimeFormatter dateTimeFormatter = ISODateTimeFormat.dateTime();
        DateTime backupTime = dateTimeFormatter.parseDateTime(backupStartedTimeString);

        ServerBackup runningBackup = (ServerBackup) ReflectionUtil.getField(backupService, "runningBackup");
        assertThat(new DateTime(runningBackup.getTime()), is(backupTime));
        waitForAssertionToCompleteWhileBackupIsOn.release();
        backupThd.join();
    }

    @Test
    public void shouldReturnBackupStartedBy() throws InterruptedException {
        assertThat(backupService.backupStartedBy(), is(Optional.empty()));

        final Semaphore waitForBackupToStart = new Semaphore(1);
        final Semaphore waitForAssertionToCompleteWhileBackupIsOn = new Semaphore(1);
        final BackupUpdateListener backupUpdateListener = new BackupUpdateListener() {
            private boolean backupStarted = false;
            @Override
            public void updateStep(BackupProgressStatus step) {
                if (!backupStarted) {
                    backupStarted = true;
                    waitForBackupToStart.release();
                    try {
                        waitForAssertionToCompleteWhileBackupIsOn.acquire();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }

            @Override
            public void error(String message) {
            }

            @Override
            public void completed(String message) {
            }
        };

        waitForAssertionToCompleteWhileBackupIsOn.acquire();
        waitForBackupToStart.acquire();
        Thread backupThd = new Thread(() -> backupService.startBackup(admin, backupUpdateListener));

        backupThd.start();
        waitForBackupToStart.acquire();
        String backupStartedBy = backupService.backupStartedBy().get();
        ServerBackup runningBackup = (ServerBackup) ReflectionUtil.getField(backupService, "runningBackup");

        assertThat(runningBackup.getUsername(), is(backupStartedBy));
        waitForAssertionToCompleteWhileBackupIsOn.release();
        backupThd.join();
    }

    @Test
    public void shouldExecutePostBackupScriptAndReturnResultOnSuccess() throws InterruptedException {
        final Semaphore waitForBackupToComplete = new Semaphore(1);
        GoConfigService configService = mock(GoConfigService.class);
        ServerConfig serverConfig = new ServerConfig();
        serverConfig.setBackupConfig(new BackupConfig().setSchedule(null).setPostBackupScript("jcmd"));
        when(configService.serverConfig()).thenReturn(serverConfig);
        GoMailSender goMailSender = mock(GoMailSender.class);
        when(configService.getMailSender()).thenReturn(goMailSender);
        when(configService.adminEmail()).thenReturn("mail@admin.com");
        when(configService.isUserAdmin(admin)).thenReturn(true);
        TimeProvider timeProvider = mock(TimeProvider.class);
        DateTime now = new DateTime();
        when(timeProvider.currentDateTime()).thenReturn(now);

        final MessageCollectingBackupUpdateListener backupUpdateListener = new MessageCollectingBackupUpdateListener(waitForBackupToComplete);

        waitForBackupToComplete.acquire();
        backupService = new BackupService(artifactsDirHolder, configService, timeProvider, backupInfoRepository, systemEnvSpy, configRepository,
                databaseStrategy, backupQueue);
        Thread backupThd = new Thread(() -> backupService.startBackup(admin, backupUpdateListener));

        backupThd.start();
        waitForBackupToComplete.acquire();
        assertThat(backupUpdateListener.getMessages().contains(BackupProgressStatus.POST_BACKUP_SCRIPT_COMPLETE.getMessage()), is(true));
        backupThd.join();
    }

    @Test
    public void shouldExecutePostBackupScriptAndReturnResultOnFailure() {
        GoConfigService configService = mock(GoConfigService.class);
        ServerConfig serverConfig = new ServerConfig();
        serverConfig.setBackupConfig(new BackupConfig().setSchedule(null).setPostBackupScript("non-existant"));
        when(configService.serverConfig()).thenReturn(serverConfig);
        GoMailSender goMailSender = mock(GoMailSender.class);
        when(configService.getMailSender()).thenReturn(goMailSender);
        when(configService.adminEmail()).thenReturn("mail@admin.com");
        when(configService.isUserAdmin(admin)).thenReturn(true);

        TimeProvider timeProvider = mock(TimeProvider.class);
        DateTime now = new DateTime();
        when(timeProvider.currentDateTime()).thenReturn(now);

        BackupService service = new BackupService(artifactsDirHolder, configService, timeProvider, backupInfoRepository, systemEnvSpy, configRepository,
                databaseStrategy, null);
        ServerBackup backup = service.startBackup(admin);

        assertThat(backup.hasFailed(), is(true));
        assertThat(backup.getMessage(), is("Post backup script exited with an error, check the server log for details."));
    }

    private void deleteWrapperConfigFileIfExists(String ...fileNames) {
        for (String fileName : fileNames) {
            FileUtils.deleteQuietly(new File(WRAPPER_CONFIG_DIR, fileName));
        }
    }

    private boolean fileExists(String fileName) {
        return !FileUtils.listFiles(backupsDirectory, new NameFileFilter(fileName), TrueFileFilter.TRUE).isEmpty();
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

    private void createWrapperConfigFile(String fileName, String content) throws IOException {
        FileOutputStream fos = null;
        try {
            File file = new File(WRAPPER_CONFIG_DIR, fileName);
            FileUtils.forceMkdir(file.getParentFile());
            fos = new FileOutputStream(file);
            fos.write(content.getBytes());
        } finally {
            if (fos != null) {
                fos.close();
            }
        }
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
    public void shouldReturnIfBackupIsInProgress() throws InterruptedException {
        final Semaphore waitForBackupToBegin = new Semaphore(1);
        final Semaphore waitForAssertion_whichHasToHappen_whileBackupIsRunning = new Semaphore(1);

        Database databaseStrategyMock = mock(Database.class);
        doAnswer((Answer<Object>) invocationOnMock -> {
            waitForBackupToBegin.release();
            waitForAssertion_whichHasToHappen_whileBackupIsRunning.acquire();
            return null;
        }).when(databaseStrategyMock).backup(any(File.class));


        final BackupService backupService = new BackupService(artifactsDirHolder, goConfigService, new TimeProvider(), backupInfoRepository, systemEnvSpy,
                configRepository, databaseStrategyMock, null);

        waitForBackupToBegin.acquire();
        Thread thd = new Thread(() -> backupService.startBackup(admin));
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
        return new ArrayList<>(FileUtils.listFiles(backupsDirectory, new NameFileFilter(filename), TrueFileFilter.TRUE)).get(0);
    }

    private void cleanupBackups() throws IOException {
        FileUtils.deleteQuietly(artifactsDirHolder.getArtifactsDir());
    }

    class MessageCollectingBackupUpdateListener implements BackupUpdateListener {

        private final List<String> messages;

        private final Semaphore backupComplete;
        MessageCollectingBackupUpdateListener(Semaphore backupComplete) {
            this.backupComplete = backupComplete;
            this.messages = new ArrayList<>();
        }

        @Override
        public void updateStep(BackupProgressStatus step) {
            messages.add(step.getMessage());
        }

        public List<String> getMessages() {
            return messages;
        }

        @Override
        public void error(String message) {
        }

        @Override
        public void completed(String message) {
            backupComplete.release();
        }
    }
}
