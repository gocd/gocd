/*
 * Copyright 2020 ThoughtWorks, Inc.
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
import com.thoughtworks.go.config.BackupConfig;
import com.thoughtworks.go.config.GoMailSender;
import com.thoughtworks.go.config.exceptions.EntityType;
import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.database.Database;
import com.thoughtworks.go.security.AESCipherProvider;
import com.thoughtworks.go.security.DESCipherProvider;
import com.thoughtworks.go.server.domain.BackupProgressStatus;
import com.thoughtworks.go.server.domain.PostBackupScript;
import com.thoughtworks.go.server.domain.ServerBackup;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.messaging.EmailMessageDrafter;
import com.thoughtworks.go.server.messaging.ServerBackupQueue;
import com.thoughtworks.go.server.messaging.StartServerBackupMessage;
import com.thoughtworks.go.server.persistence.ServerBackupRepository;
import com.thoughtworks.go.server.service.backup.BackupStatusUpdater;
import com.thoughtworks.go.server.service.backup.BackupUpdateListener;
import com.thoughtworks.go.server.web.BackupStatusProvider;
import com.thoughtworks.go.service.ConfigRepository;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.TimeProvider;
import com.thoughtworks.go.util.VoidThrowingFn;
import org.apache.commons.io.DirectoryWalker;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.apache.commons.codec.binary.Hex.encodeHexString;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * @understands backing up db and config
 */
@Service
public class BackupService implements BackupStatusProvider {

    public static final String ABORTED_BACKUPS_MESSAGE = "Server shut down while backup in progress.";

    // Don't change these enums. These are an API contract, and are used by post backup script.
    public enum BackupInitiator {
        TIMER,
        USER
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(BackupService.class);
    static final String BACKUP = "backup_";

    private final ArtifactsDirHolder artifactsDirHolder;

    private final GoConfigService goConfigService;
    private ServerBackupRepository serverBackupRepository;
    private final TimeProvider timeProvider;
    private ServerBackupQueue backupQueue;
    private final SystemEnvironment systemEnvironment;
    private final ConfigRepository configRepository;
    private final Database databaseStrategy;
    private volatile ServerBackup runningBackup;
    private static final String CONFIG_BACKUP_ZIP = "config-dir.zip";
    private static final String WRAPPER_CONFIG_BACKUP_ZIP = "wrapper-config-dir.zip";

    private static final String CONFIG_REPOSITORY_BACKUP_ZIP = "config-repo.zip";
    private static final String VERSION_BACKUP_FILE = "version.txt";

    private static final Object BACKUP_MUTEX = new Object();

    @Autowired
    public BackupService(ArtifactsDirHolder artifactsDirHolder,
                         GoConfigService goConfigService,
                         TimeProvider timeProvider,
                         ServerBackupRepository serverBackupRepository,
                         SystemEnvironment systemEnvironment,
                         ConfigRepository configRepository,
                         Database databaseStrategy, ServerBackupQueue backupQueue) {
        this.artifactsDirHolder = artifactsDirHolder;
        this.goConfigService = goConfigService;
        this.serverBackupRepository = serverBackupRepository;
        this.systemEnvironment = systemEnvironment;
        this.configRepository = configRepository;
        this.databaseStrategy = databaseStrategy;
        this.timeProvider = timeProvider;
        this.backupQueue = backupQueue;
    }

    public void initialize() {
        if (systemEnvironment.isServerInStandbyMode()) {
            LOGGER.info("GoCD server in 'standby' mode, not changing 'in-progress' backups to 'aborted'.");
        } else {
            serverBackupRepository.markInProgressBackupsAsAborted(ABORTED_BACKUPS_MESSAGE);
        }
    }

    public ServerBackup scheduleBackup(Username username) {
        ServerBackup serverBackup = createServerBackup(username);
        backupQueue.post(new StartServerBackupMessage(serverBackup.getId()));
        return serverBackup;
    }

    public ServerBackup runningBackup() {
        if (runningBackup == null) {
            throw new RecordNotFoundException(EntityType.Backup);
        }
        return runningBackup;
    }

    public ServerBackup getServerBackup(long id) {
        return serverBackupRepository.getBackup(id);
    }

    public Optional<ServerBackup> startBackupWithId(long id) {
        ServerBackup backup = serverBackupRepository.getBackup(id);
        ServerBackup serverBackup = performBackup(backup, singletonList(new BackupStatusUpdater(backup, serverBackupRepository)), BackupInitiator.USER);
        return Optional.of(serverBackup);
    }

    ServerBackup backupViaTimer() {
        ServerBackup serverBackup = createServerBackup(Username.CRUISE_TIMER);
        return performBackup(serverBackup, singletonList(new BackupStatusUpdater(serverBackup, serverBackupRepository)), BackupInitiator.TIMER);
    }

    public ServerBackup startBackup(Username username) {
        ServerBackup serverBackup = createServerBackup(username);
        return performBackup(serverBackup, singletonList(new BackupStatusUpdater(serverBackup, serverBackupRepository)), BackupInitiator.USER);
    }

    ServerBackup startBackup(Username username, BackupUpdateListener backupUpdateListener) {
        ServerBackup serverBackup = createServerBackup(username);
        return performBackup(serverBackup, asList(new BackupStatusUpdater(serverBackup, serverBackupRepository), backupUpdateListener), BackupInitiator.USER);
    }

    private ServerBackup performBackup(ServerBackup backup, List<BackupUpdateListener> backupUpdateListeners, BackupInitiator initiatedBy) {
        GoMailSender mailSender = goConfigService.getMailSender();
        File destDir = new File(backup.getPath());
        synchronized (BACKUP_MUTEX) {
            try {
                runningBackup = backup;
                notifyUpdateToListeners(backupUpdateListeners, BackupProgressStatus.CREATING_DIR);
                if (!destDir.mkdirs()) {
                    notifyErrorToListeners(backupUpdateListeners, "Failed to perform backup. Reason: Could not create the backup directory.");
                    return backup;
                }
                backupVersion(destDir, backupUpdateListeners);
                backupConfig(destDir, backupUpdateListeners);
                backupWrapperConfig(destDir, backupUpdateListeners);
                backupConfigRepo(backupUpdateListeners, destDir);
                backupDb(destDir, backupUpdateListeners);
                boolean passed = executePostBackupScript(backup.getUsername(), initiatedBy, backup, backupUpdateListeners);
                if (passed) {
                    sendBackupSuccessEmail(backup.getUsername(), mailSender, destDir);
                    notifyCompletionToListeners(backupUpdateListeners);
                    LOGGER.debug("Backup Completed Successfully");
                }
            } catch (Exception e) {
                FileUtils.deleteQuietly(destDir);
                sendBackupFailedEmail(mailSender, e);
                notifyErrorToListeners(backupUpdateListeners, String.format("Failed to perform backup. Reason: %s", e.getMessage()));
                LOGGER.error("[Backup] Failed to backup Go.", e);
            } finally {
                runningBackup = null;
            }
        }
        return backup;
    }

    private ServerBackup createServerBackup(Username username) {
        DateTime backupTime = timeProvider.currentDateTime();
        ServerBackup serverBackup = new ServerBackup(getBackupDir(backupTime).getAbsolutePath(), backupTime.toDate(), username.getUsername().toString(), "Backup scheduled");
        serverBackup = serverBackupRepository.saveOrUpdate(serverBackup);
        return serverBackup;
    }

    private void backupConfigRepo(List<BackupUpdateListener> backupUpdateListeners, File destDir) throws IOException {
        notifyUpdateToListeners(backupUpdateListeners, BackupProgressStatus.BACKUP_CONFIG_REPO);
        configRepository.doLocked(new VoidThrowingFn<IOException>() {
            @Override
            public void run() throws IOException {
                File configRepoDir = systemEnvironment.getConfigRepoDir();
                try (ZipOutputStream configRepoZipStream = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(new File(destDir, CONFIG_REPOSITORY_BACKUP_ZIP))))) {
                    new DirectoryStructureWalker(configRepoDir.getAbsolutePath(), configRepoZipStream).walk();
                }
            }
        });
    }

    private void notifyUpdateToListeners(List<BackupUpdateListener> listeners, BackupProgressStatus status) {
        LOGGER.debug(status.getMessage());
        listeners.forEach(backupUpdateListener -> backupUpdateListener.updateStep(status));
    }

    private void notifyErrorToListeners(List<BackupUpdateListener> listeners, String message) {
        LOGGER.debug(message);
        listeners.forEach(backupUpdateListener -> backupUpdateListener.error(message));
    }

    private void notifyCompletionToListeners(List<BackupUpdateListener> listeners) {
        listeners.forEach(BackupUpdateListener::completed);
    }

    private File getBackupDir(DateTime backupTime) {
        return new File(backupLocation(), BACKUP + backupTime.toString("YYYYMMdd-HHmmss"));
    }

    private void sendBackupFailedEmail(GoMailSender mailSender, Exception e) {
        if (emailOnFailure()) {
            LOGGER.debug("Backup failed. Sending email...");
            mailSender.send(EmailMessageDrafter.backupFailedMessage(e.getMessage(), goConfigService.adminEmail()));
        }
    }

    private void sendBackupSuccessEmail(String username, GoMailSender mailSender, File destDir) {
        if (emailOnSuccess()) {
            LOGGER.debug("Backup successful. Sending email...");
            mailSender.send(EmailMessageDrafter.backupSuccessfullyCompletedMessage(destDir.getAbsolutePath(), goConfigService.adminEmail(), username));
        }
    }

    private boolean executePostBackupScript(String username, BackupInitiator initiatedBy, ServerBackup serverBackup, List<BackupUpdateListener> notifyUpdateToListeners) {
        String postBackupScriptFile = postBackupScriptFile();
        if (isNotBlank(postBackupScriptFile)) {
            notifyUpdateToListeners(notifyUpdateToListeners, BackupProgressStatus.POST_BACKUP_SCRIPT_START);
            PostBackupScript postBackupScript = new PostBackupScript(postBackupScriptFile, initiatedBy, username, serverBackup, backupLocation(), serverBackup.getTime());
            if (postBackupScript.execute()) {
                notifyUpdateToListeners(notifyUpdateToListeners, BackupProgressStatus.POST_BACKUP_SCRIPT_COMPLETE);
                return true;
            } else {
                notifyErrorToListeners(notifyUpdateToListeners, "Post backup script exited with an error, check the server log for details.");
                return false;
            }
        }
        return true;
    }

    private BackupConfig backupConfig() {
        return goConfigService.serverConfig().getBackupConfig();
    }

    private String postBackupScriptFile() {
        BackupConfig backupConfig = backupConfig();
        if (backupConfig != null) {
            String postBackupScript = backupConfig.getPostBackupScript();
            return StringUtils.stripToNull(postBackupScript);
        }
        return null;
    }

    private boolean emailOnFailure() {
        BackupConfig backupConfig = backupConfig();

        return backupConfig != null && backupConfig.isEmailOnFailure();
    }

    private boolean emailOnSuccess() {
        BackupConfig backupConfig = backupConfig();
        return backupConfig != null && backupConfig.isEmailOnSuccess();
    }

    private void backupVersion(File backupDir, List<BackupUpdateListener> backupUpdateListeners) throws IOException {
        notifyUpdateToListeners(backupUpdateListeners, BackupProgressStatus.BACKUP_VERSION_FILE);
        File versionFile = new File(backupDir, VERSION_BACKUP_FILE);
        FileUtils.writeStringToFile(versionFile, CurrentGoCDVersion.getInstance().formatted(), UTF_8);
    }

    private void backupWrapperConfig(File backupDir, List<BackupUpdateListener> backupUpdateListeners) throws IOException {
        notifyUpdateToListeners(backupUpdateListeners, BackupProgressStatus.BACKUP_WRAPPER_CONFIG);
        var wrapperConfigDirPath = systemEnvironment.wrapperConfigDirPath()
                .orElseThrow(() -> new RuntimeException("Could not find wrapper-config directory"));

        try (ZipOutputStream configZip = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(new File(backupDir, WRAPPER_CONFIG_BACKUP_ZIP))))) {
            new DirectoryStructureWalker(wrapperConfigDirPath, configZip).walk();
        }
    }

    private void backupConfig(File backupDir, List<BackupUpdateListener> backupUpdateListeners) throws IOException {
        notifyUpdateToListeners(backupUpdateListeners, BackupProgressStatus.BACKUP_CONFIG);
        String configDirectory = systemEnvironment.getConfigDir();
        try (ZipOutputStream configZip = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(new File(backupDir, CONFIG_BACKUP_ZIP))))) {
            File cruiseConfigFile = new File(systemEnvironment.getCruiseConfigFile());
            File desCipherFile = systemEnvironment.getDESCipherFile();
            File aesCipherFile = systemEnvironment.getAESCipherFile();
            new DirectoryStructureWalker(configDirectory, configZip, cruiseConfigFile, desCipherFile, aesCipherFile).walk();

            configZip.putNextEntry(new ZipEntry(cruiseConfigFile.getName()));
            IOUtils.write(goConfigService.xml(), configZip, UTF_8);

            if (desCipherFile.exists()) {
                configZip.putNextEntry(new ZipEntry(desCipherFile.getName()));
                IOUtils.write(encodeHexString(new DESCipherProvider(systemEnvironment).getKey()), configZip, UTF_8);
            }

            configZip.putNextEntry(new ZipEntry(aesCipherFile.getName()));
            IOUtils.write(encodeHexString(new AESCipherProvider(systemEnvironment).getKey()), configZip, UTF_8);
        }
    }

    private void backupDb(File backupDir, List<BackupUpdateListener> backupUpdateListener) {
        notifyUpdateToListeners(backupUpdateListener, BackupProgressStatus.BACKUP_DATABASE);
        databaseStrategy.backup(backupDir);
    }

    public String backupLocation() {
        return artifactsDirHolder.getBackupsDir().getAbsolutePath();
    }

    public Optional<Date> lastBackupTime() {
        return serverBackupRepository.lastSuccessfulBackup().map((ServerBackup::getTime));
    }

    public Optional<String> lastBackupUser() {
        return serverBackupRepository.lastSuccessfulBackup().map((ServerBackup::getUsername));
    }

    public void deleteAll() {
        serverBackupRepository.deleteAll();
    }

    @Override
    public boolean isBackingUp() {
        return runningBackup != null;
    }

    @Override
    public Optional<String> backupRunningSinceISO8601() {
        if (runningBackup != null) {
            return Optional.of(new DateTime(runningBackup.getTime()).toString());
        }
        return Optional.empty();
    }

    @Override
    public Optional<String> backupStartedBy() {
        if (runningBackup != null) {
            return Optional.of(runningBackup.getUsername());
        }
        return Optional.empty();
    }

    public String availableDiskSpace() {
        File artifactsDir = artifactsDirHolder.getArtifactsDir();
        return FileUtils.byteCountToDisplaySize(artifactsDir.getUsableSpace());
    }
}


class DirectoryStructureWalker extends DirectoryWalker {
    private final String configDirectory;
    private final ZipOutputStream zipStream;
    private final ArrayList<String> excludeFiles;

    public DirectoryStructureWalker(String configDirectory, ZipOutputStream zipStream, File... excludeFiles) {
        this.excludeFiles = new ArrayList<>();
        for (File excludeFile : excludeFiles) {
            this.excludeFiles.add(excludeFile.getAbsolutePath());
        }

        this.configDirectory = new File(configDirectory).getAbsolutePath();
        this.zipStream = zipStream;
    }

    @Override
    protected boolean handleDirectory(File directory, int depth, Collection results) throws IOException {
        if (!directory.getAbsolutePath().equals(configDirectory)) {
            ZipEntry e = new ZipEntry(fromRoot(directory) + "/");
            zipStream.putNextEntry(e);
        }
        return true;
    }

    @Override
    protected void handleFile(File file, int depth, Collection results) throws IOException {
        if (excludeFiles.contains(file.getAbsolutePath())) {
            return;
        }
        zipStream.putNextEntry(new ZipEntry(fromRoot(file)));
        try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(file))) {
            IOUtils.copy(in, zipStream);
        }
    }

    private String fromRoot(File directory) {
        return directory.getAbsolutePath().substring(configDirectory.length() + 1);
    }

    public void walk() throws IOException {
        walk(new File(this.configDirectory), null);
    }
}
