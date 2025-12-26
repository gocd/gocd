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
import org.apache.commons.io.filefilter.NameFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Semaphore;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static com.thoughtworks.go.util.TestUtils.doInterruptiblyQuietlyRethrowInterrupt;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.codec.binary.Hex.encodeHexString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

    private final GoConfigFileHelper configHelper = new GoConfigFileHelper();

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

        Files.writeString(new File(systemEnvironment.getConfigDir(), "cruise-config.xml").toPath(), "invalid crapy config", UTF_8);
        Files.writeString(new File(systemEnvironment.getConfigDir(), "cipher").toPath(), "invalid crapy cipher", UTF_8);
        Files.writeString(new File(systemEnvironment.getConfigDir(), "cipher.aes").toPath(), "invalid crapy cipher", UTF_8);

        systemEnvSpy = spy(systemEnvironment);
        when(systemEnvSpy.wrapperConfigDirPath()).thenReturn(Optional.of(WRAPPER_CONFIG_DIR));
        backupService = new BackupService(artifactsDirHolder, goConfigService, timeProvider, backupInfoRepository,
                systemEnvSpy, configRepository, databaseStrategy, null);
    }

    @AfterEach
    public void tearDown() throws Exception {
        dbHelper.onTearDown();
        cleanupBackups();
        Files.writeString(new File(systemEnvironment.getConfigDir(), "cruise-config.xml").toPath(), goConfigService.xml(), UTF_8);
        Files.write(systemEnvironment.getDESCipherFile().toPath(), originalCipher);
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
            assertThat(backup.isSuccessful()).isTrue();
            assertThat(backup.getMessage()).isEqualTo("Backup was generated successfully.");

            File configZip = backedUpFile("config-dir.zip");
            assertThat(fileContents(configZip, "foo")).isEqualTo("foo_foo");
            assertThat(fileContents(configZip, "bar")).isEqualTo("bar_bar");
            assertThat(fileContents(configZip, "baz")).isEqualTo("hazar_bar");

            assertThat(fileContents(configZip, FilenameUtils.separatorsToSystem("hello/world/file"))).isEqualTo("hello world!");
            assertThat(fileContents(configZip, FilenameUtils.separatorsToSystem("some_dir/cruise-config.xml"))).isEqualTo("some-other-cruise-config");
            assertThat(fileContents(configZip, FilenameUtils.separatorsToSystem("some_dir/cipher"))).isEqualTo("some-cipher");

            assertThat(fileContents(configZip, "cruise-config.xml")).isEqualTo(goConfigService.xml());

            byte[] realDesCipher = new DESCipherProvider(systemEnvironment).getKey();
            byte[] realAESCipher = new AESCipherProvider(systemEnvironment).getKey();
            assertThat(fileContents(configZip, "cipher")).isEqualTo(encodeHexString(realDesCipher));
            assertThat(fileContents(configZip, "cipher.aes")).isEqualTo(encodeHexString(realAESCipher));
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
            assertThat(backup.isSuccessful()).isTrue();
            assertThat(backup.getMessage()).isEqualTo("Backup was generated successfully.");

            File wrapperConfigZip = backedUpFile("wrapper-config-dir.zip");
            assertThat(fileContents(wrapperConfigZip, "foo")).isEqualTo("foo_foo");
            assertThat(fileContents(wrapperConfigZip, "bar")).isEqualTo("bar_bar");
            assertThat(fileContents(wrapperConfigZip, "baz")).isEqualTo("hazar_bar");

            assertThat(fileContents(wrapperConfigZip, FilenameUtils.separatorsToSystem("hello/world/file"))).isEqualTo("hello world!");
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

            assertThat(backup.isSuccessful()).isTrue();
            assertThat(backup.getMessage()).isEqualTo("Backup was generated successfully. Backup of wrapper configuration was skipped as the wrapper configuration directory path is unknown.");

            assertFalse(fileExists("wrapper-config-dir.zip"));
        } finally {
            deleteWrapperConfigFileIfExists("foo", "bar");
        }
    }

    @Test
    public void shouldBackupConfigRepository(@TempDir Path temporaryFolder) throws IOException {
        configHelper.addPipeline("too-unique-to-be-present", "stage-name");

        ServerBackup backup = backupService.startBackup(admin);
        assertThat(backup.isSuccessful()).isTrue();
        assertThat(backup.getMessage()).isEqualTo("Backup was generated successfully.");

        File repoZip = backedUpFile("config-repo.zip");
        File repoDir = TempDirUtils.createTempDirectoryIn(temporaryFolder, "expanded-config-repo-backup").toFile();
        new ZipUtil().unzip(repoZip, repoDir);
        File cloneDir = TempDirUtils.createTempDirectoryIn(temporaryFolder, "cloned-config-repo-backup").toFile();
        GitMaterial git = new GitMaterial(repoDir.getAbsolutePath());

        List<Modification> modifications = git.latestModification(cloneDir, subprocessExecutionContext);
        String latestChangeRev = modifications.get(0).getRevision();
        git.checkout(cloneDir, new StringRevision(latestChangeRev), subprocessExecutionContext);
        assertThat(Files.readString(new File(cloneDir, "cruise-config.xml").toPath(), UTF_8).indexOf("too-unique-to-be-present")).isGreaterThan(0);
        StringRevision revision = new StringRevision(latestChangeRev + "~1");
        git.updateTo(new InMemoryStreamConsumer(), cloneDir, new RevisionContext(revision), subprocessExecutionContext);
        assertThat(Files.readString(new File(cloneDir, "cruise-config.xml").toPath(), UTF_8).indexOf("too-unique-to-be-present")).isEqualTo(-1);
    }

    @Test
    public void shouldCaptureVersionForEveryBackup() throws IOException {
        BackupService backupService = new BackupService(artifactsDirHolder, goConfigService, timeProvider, backupInfoRepository, systemEnvSpy, configRepository, databaseStrategy, null);
        ServerBackup backup = backupService.startBackup(admin);
        assertThat(backup.isSuccessful()).isTrue();
        assertThat(backup.getMessage()).isEqualTo("Backup was generated successfully.");

        File version = backedUpFile("version.txt");
        assertThat(Files.readString(version.toPath(), UTF_8)).isEqualTo(CurrentGoCDVersion.getInstance().formatted());
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
        LocalDateTime now = LocalDateTime.of(2023, 10, 1, 12, 0, 4, 5600);
        when(timeProvider.currentLocalDateTime()).thenReturn(now);

        BackupService service = new BackupService(artifactsDirHolder, configService, timeProvider, backupInfoRepository, systemEnvSpy, configRepository,
                databaseStrategy, null);
        service.startBackup(admin);

        String ipAddress = SystemUtil.getFirstLocalNonLoopbackIpAddress();
        String body = String.format("Backup of the Go server at '%s' was successfully completed. The backup is stored at location: %s. This backup was triggered by 'admin'.",
            ipAddress,
            new File(backupsDirectory, BackupService.BACKUP + "20231001-120004").getAbsolutePath());

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
        LocalDateTime now = LocalDateTime.now();
        when(timeProvider.currentLocalDateTime()).thenReturn(now);

        BackupService service = new BackupService(artifactsDirHolder, configService, timeProvider, backupInfoRepository, systemEnvSpy, configRepository,
                databaseStrategy, null);
        service.startBackup(admin);

        verifyNoMoreInteractions(goMailSender);
    }

    @Test
    public void shouldSendEmailToAdminWhenTheBackupFails() {
        GoConfigService configService = mock(GoConfigService.class);
        ServerConfig serverConfig = new ServerConfig();
        serverConfig.setBackupConfig(new BackupConfig().setEmailOnFailure(true));
        when(configService.serverConfig()).thenReturn(serverConfig);
        when(configService.adminEmail()).thenReturn("mail@admin.com");

        GoMailSender goMailSender = mock(GoMailSender.class);
        when(configService.getMailSender()).thenReturn(goMailSender);
        when(configService.isUserAdmin(admin)).thenReturn(true);

        TimeProvider timeProvider = mock(TimeProvider.class);
        LocalDateTime now = LocalDateTime.now();
        when(timeProvider.currentLocalDateTime()).thenReturn(now);

        Database databaseStrategyMock = mock(Database.class);
        doThrow(new RuntimeException("Oh no!")).when(databaseStrategyMock).backup(any());
        BackupService service = new BackupService(artifactsDirHolder, configService, timeProvider, backupInfoRepository, systemEnvSpy, configRepository,
                databaseStrategyMock, null);
        ServerBackup backup = service.startBackup(admin);

        String ipAddress = SystemUtil.getFirstLocalNonLoopbackIpAddress();
        String body = String.format("Backup of the Go server at '%s' has failed. The reason is: %s", ipAddress, "Oh no!");

        assertThat(backup.isSuccessful()).isFalse();
        assertThat(backup.getMessage()).isEqualTo("Failed to perform backup. Reason: Oh no!");
        verify(goMailSender).send(new SendEmailMessage("Server Backup Failed", body, "mail@admin.com"));
        verifyNoMoreInteractions(goMailSender);

        assertThat(FileUtils.listFiles(backupsDirectory, TrueFileFilter.TRUE, TrueFileFilter.TRUE)).isEmpty();
    }

    @Test
    public void shouldNotSendEmailToAdminWhenTheBackupFailsAndEmailConfigIsNotSet() {
        GoConfigService configService = mock(GoConfigService.class);
        ServerConfig serverConfig = new ServerConfig();
        serverConfig.setBackupConfig(new BackupConfig());
        when(configService.serverConfig()).thenReturn(serverConfig);
        when(configService.adminEmail()).thenReturn("mail@admin.com");

        GoMailSender goMailSender = mock(GoMailSender.class);
        when(configService.getMailSender()).thenReturn(goMailSender);
        when(configService.isUserAdmin(admin)).thenReturn(true);

        TimeProvider timeProvider = mock(TimeProvider.class);
        LocalDateTime now = LocalDateTime.now();
        when(timeProvider.currentLocalDateTime()).thenReturn(now);

        Database databaseStrategyMock = mock(Database.class);
        doThrow(new RuntimeException("Oh no!")).when(databaseStrategyMock).backup(any());
        BackupService service = new BackupService(artifactsDirHolder, configService, timeProvider, backupInfoRepository, systemEnvSpy, configRepository,
                databaseStrategyMock, null);
        ServerBackup backup = service.startBackup(admin);

        assertThat(backup.isSuccessful()).isFalse();
        assertThat(backup.getMessage()).isEqualTo("Failed to perform backup. Reason: Oh no!");
        verifyNoMoreInteractions(goMailSender);

        assertThat(FileUtils.listFiles(backupsDirectory, TrueFileFilter.TRUE, TrueFileFilter.TRUE)).isEmpty();
    }

    @Test
    public void shouldReturnBackupRunningSinceValue_inISO8601_format() throws InterruptedException {
        assertThat(backupService.backupRunningSinceISO8601()).isEqualTo(Optional.empty());

        final Semaphore waitForBackupToStart = new Semaphore(1);
        final Semaphore waitForAssertionToCompleteWhileBackupIsOn = new Semaphore(1);
        final BackupUpdateListener backupUpdateListener = new BackupUpdateListener() {
            private boolean backupStarted = false;
            @Override
            public void updateStep(BackupProgressStatus step) {
                if (!backupStarted) {
                    backupStarted = true;
                    waitForBackupToStart.release();
                    doInterruptiblyQuietlyRethrowInterrupt(waitForAssertionToCompleteWhileBackupIsOn::acquire);
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
        try {
            String backupStartedTimeString = backupService.backupRunningSinceISO8601().get();
            Date backupTime = Dates.parseIso8601CompactOffset(backupStartedTimeString);

            ServerBackup runningBackup = ReflectionUtil.getField(backupService, "runningBackup");
            assertThat(runningBackup.getTime()).isCloseTo(backupTime, 1000L); // No millis in format
        } finally {
            waitForAssertionToCompleteWhileBackupIsOn.release();
            backupThd.join();
        }
    }

    @Test
    public void shouldReturnBackupStartedBy() throws InterruptedException {
        assertThat(backupService.backupStartedBy()).isEqualTo(Optional.empty());

        final Semaphore waitForBackupToStart = new Semaphore(1);
        final Semaphore waitForAssertionToCompleteWhileBackupIsOn = new Semaphore(1);
        final BackupUpdateListener backupUpdateListener = new BackupUpdateListener() {
            private boolean backupStarted = false;
            @Override
            public void updateStep(BackupProgressStatus step) {
                if (!backupStarted) {
                    backupStarted = true;
                    waitForBackupToStart.release();
                    doInterruptiblyQuietlyRethrowInterrupt(waitForAssertionToCompleteWhileBackupIsOn::acquire);
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
        ServerBackup runningBackup = ReflectionUtil.getField(backupService, "runningBackup");

        assertThat(runningBackup.getUsername()).isEqualTo(backupStartedBy);
        waitForAssertionToCompleteWhileBackupIsOn.release();
        backupThd.join();
    }

    @Test
    public void shouldFailFastOnBadPostBackupScriptLocation() throws InterruptedException {
        final Semaphore waitForBackupToComplete = new Semaphore(1);
        GoConfigService configService = mock(GoConfigService.class);
        ServerConfig serverConfig = new ServerConfig();
        String badPostBackupScriptLocation = CruiseConfig.WORKING_BASE_DIR + "artifacts/should-not-be-here.sh";
        serverConfig.setBackupConfig(new BackupConfig().setSchedule(null).setPostBackupScript(badPostBackupScriptLocation));
        when(configService.serverConfig()).thenReturn(serverConfig);
        GoMailSender goMailSender = mock(GoMailSender.class);
        when(configService.getMailSender()).thenReturn(goMailSender);
        when(configService.adminEmail()).thenReturn("mail@admin.com");
        when(configService.isUserAdmin(admin)).thenReturn(true);
        TimeProvider timeProvider = mock(TimeProvider.class);
        LocalDateTime now = LocalDateTime.now();
        when(timeProvider.currentLocalDateTime()).thenReturn(now);

        final MessageCollectingBackupUpdateListener backupUpdateListener = new MessageCollectingBackupUpdateListener(waitForBackupToComplete);

        waitForBackupToComplete.acquire();
        backupService = new BackupService(artifactsDirHolder, configService, timeProvider, backupInfoRepository, systemEnvSpy, configRepository,
            databaseStrategy, backupQueue);
        Thread backupThd = new Thread(() -> backupService.startBackup(admin, backupUpdateListener));

        backupThd.start();
        waitForBackupToComplete.acquire();
        assertThat(backupUpdateListener.messages).isEmpty();
        assertThat(backupUpdateListener.errors).containsExactly("Failed to perform backup. Reason: Post backup script cannot be executed when located within pipelines or artifact storage.");
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
        LocalDateTime now = LocalDateTime.now();
        when(timeProvider.currentLocalDateTime()).thenReturn(now);

        final MessageCollectingBackupUpdateListener backupUpdateListener = new MessageCollectingBackupUpdateListener(waitForBackupToComplete);

        waitForBackupToComplete.acquire();
        backupService = new BackupService(artifactsDirHolder, configService, timeProvider, backupInfoRepository, systemEnvSpy, configRepository,
                databaseStrategy, backupQueue);
        Thread backupThd = new Thread(() -> backupService.startBackup(admin, backupUpdateListener));

        backupThd.start();
        waitForBackupToComplete.acquire();
        assertThat(backupUpdateListener.messages).contains(BackupProgressStatus.POST_BACKUP_SCRIPT_COMPLETE.getMessage());
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
        LocalDateTime now = LocalDateTime.now();
        when(timeProvider.currentLocalDateTime()).thenReturn(now);

        BackupService service = new BackupService(artifactsDirHolder, configService, timeProvider, backupInfoRepository, systemEnvSpy, configRepository,
                databaseStrategy, null);
        ServerBackup backup = service.startBackup(admin);

        assertThat(backup.hasFailed()).isTrue();
        assertThat(backup.getMessage()).isEqualTo("Post backup script exited with an error, check the server log for details.");
    }

    private void deleteWrapperConfigFileIfExists(String ...fileNames) {
        for (String fileName : fileNames) {
            FileUtils.deleteQuietly(new File(WRAPPER_CONFIG_DIR, fileName));
        }
    }

    private boolean fileExists(@SuppressWarnings("SameParameterValue") String fileName) {
        return new File(backupsDirectory, fileName).exists();
    }

    private void deleteConfigFileIfExists(String ...fileNames) {
        for (String fileName : fileNames) {
            FileUtils.deleteQuietly(new File(configDir(), fileName));
        }
    }

    private String fileContents(File location, String filename) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipInputStream zipIn = new ZipInputStream(new FileInputStream(location))) {
            ZipEntry nextEntry;
            while ((nextEntry = zipIn.getNextEntry()) != null) {
                if (nextEntry.getName().equals(filename)) {
                    zipIn.transferTo(out);
                }
            }
        }
        return out.toString(UTF_8);
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
        }).when(databaseStrategyMock).backup(any());


        final BackupService backupService = new BackupService(artifactsDirHolder, goConfigService, new TimeProvider(), backupInfoRepository, systemEnvSpy,
                configRepository, databaseStrategyMock, null);

        waitForBackupToBegin.acquire();
        Thread thd = new Thread(() -> backupService.startBackup(admin));
        thd.start();

        waitForAssertion_whichHasToHappen_whileBackupIsRunning.acquire();
        waitForBackupToBegin.acquire();
        assertThat(backupService.isBackingUp()).isTrue();
        waitForAssertion_whichHasToHappen_whileBackupIsRunning.release();

        thd.join();
    }

    private File configDir() {
        return new File(new SystemEnvironment().getConfigDir());
    }

    private File backedUpFile(final String filename) {
        return new ArrayList<>(FileUtils.listFiles(backupsDirectory, new NameFileFilter(filename), TrueFileFilter.TRUE)).get(0);
    }

    private void cleanupBackups() {
        FileUtils.deleteQuietly(artifactsDirHolder.getArtifactsDir());
    }

    static class MessageCollectingBackupUpdateListener implements BackupUpdateListener {

        private final List<String> messages = new ArrayList<>();
        private final List<String> errors = new ArrayList<>();

        private final Semaphore backupComplete;

        MessageCollectingBackupUpdateListener(Semaphore backupComplete) {
            this.backupComplete = backupComplete;
        }

        @Override
        public void updateStep(BackupProgressStatus step) {
            messages.add(step.getMessage());
        }

        @Override
        public void error(String message) {
            errors.add(message);
            backupComplete.release();
        }

        @Override
        public void completed(String message) {
            backupComplete.release();
        }
    }
}
