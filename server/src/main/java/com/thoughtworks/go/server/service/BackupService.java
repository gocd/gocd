/*
 * Copyright 2018 ThoughtWorks, Inc.
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
import com.thoughtworks.go.database.Database;
import com.thoughtworks.go.security.AESCipherProvider;
import com.thoughtworks.go.security.DESCipherProvider;
import com.thoughtworks.go.server.domain.PostBackupScript;
import com.thoughtworks.go.server.domain.ServerBackup;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.messaging.EmailMessageDrafter;
import com.thoughtworks.go.server.persistence.ServerBackupRepository;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.server.web.BackupStatusProvider;
import com.thoughtworks.go.serverhealth.HealthStateType;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static com.thoughtworks.go.util.StringUtil.joinSentences;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.codec.binary.Hex.encodeHexString;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * @understands backing up db and config
 */
@Service
public class BackupService implements BackupStatusProvider {

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
    private final SystemEnvironment systemEnvironment;
    private final ConfigRepository configRepository;
    private final Database databaseStrategy;

    private volatile DateTime backupRunningSince;
    private volatile String backupStartedBy;

    private static final String CONFIG_BACKUP_ZIP = "config-dir.zip";
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
                         Database databaseStrategy) {
        this.artifactsDirHolder = artifactsDirHolder;
        this.goConfigService = goConfigService;
        this.serverBackupRepository = serverBackupRepository;
        this.systemEnvironment = systemEnvironment;
        this.configRepository = configRepository;
        this.databaseStrategy = databaseStrategy;
        this.timeProvider = timeProvider;
    }

    void backupViaTimer(HttpLocalizedOperationResult result) {
        performBackupWithoutAuthentication(Username.CRUISE_TIMER, result, BackupInitiator.TIMER);
    }

    public ServerBackup getServerBackup(String id) {
        return null;
    }

    public ServerBackup scheduleBackup(Username username, HttpLocalizedOperationResult result) {
        return null;
    }

    public ServerBackup startBackup(Username username, HttpLocalizedOperationResult result) {
        if (!goConfigService.isUserAdmin(username)) {
            result.forbidden("Unauthorized to initiate Go backup as you are not a Go administrator", HealthStateType.forbidden());
            return null;
        }
        return performBackupWithoutAuthentication(username, result, BackupInitiator.USER);
    }

    private ServerBackup doPerformBackup(Username username, HttpLocalizedOperationResult result, DateTime backupTime) {
        GoMailSender mailSender = goConfigService.getMailSender();
        synchronized (BACKUP_MUTEX) {
            final File destDir = new File(backupLocation(), BACKUP + backupTime.toString("YYYYMMdd-HHmmss"));
            if (!destDir.mkdirs()) {
                result.internalServerError("Failed to perform backup. Reason: Could not create the backup directory.");
                return null;
            }
            try {
                backupRunningSince = backupTime;
                backupStartedBy = username.getUsername().toString();
                backupVersion(destDir);
                backupConfig(destDir);
                configRepository.doLocked(new VoidThrowingFn<IOException>() {
                    @Override
                    public void run() throws IOException {
                        backupConfigRepository(destDir);
                    }
                });
                backupDb(destDir);
                ServerBackup serverBackup = new ServerBackup(destDir.getAbsolutePath(), backupTime.toDate(), username.getUsername().toString());
                serverBackupRepository.save(serverBackup);
                if (emailOnSuccess()) {
                    mailSender.send(EmailMessageDrafter.backupSuccessfullyCompletedMessage(destDir.getAbsolutePath(), goConfigService.adminEmail(), username));
                }
                result.setMessage("Backup was generated successfully.");
                return serverBackup;
            } catch (Exception e) {
                FileUtils.deleteQuietly(destDir);
                result.internalServerError("Failed to perform backup. Reason: " + e.getMessage());
                LOGGER.error("[Backup] Failed to backup Go.", e);
                if (emailOnFailure()) {
                    mailSender.send(EmailMessageDrafter.backupFailedMessage(e.getMessage(), goConfigService.adminEmail()));
                }
            } finally {
                backupRunningSince = null;
                backupStartedBy = null;
            }
            return null;
        }
    }

    private ServerBackup performBackupWithoutAuthentication(Username username,
                                                            HttpLocalizedOperationResult result,
                                                            BackupInitiator initiatedBy) {
        DateTime backupTime = timeProvider.currentDateTime();
        ServerBackup serverBackup = doPerformBackup(username, result, backupTime);

        String postBackupScriptFile = postBackupScriptFile();

        if (isNotBlank(postBackupScriptFile)) {
            PostBackupScript postBackupScript = new PostBackupScript(postBackupScriptFile, initiatedBy, username, serverBackup, backupLocation(), backupTime.toDate());
            if (postBackupScript.execute()) {
                // only set message, retain the original status
                result.setMessage(joinSentences(result.message(), "Post backup script executed successfully."));
            } else {
                result.internalServerError(joinSentences(result.message(), "Post backup script exited with an error, check the server log for details."));
            }
        }
        return serverBackup;
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

    private void backupVersion(File backupDir) throws IOException {
        File versionFile = new File(backupDir, VERSION_BACKUP_FILE);
        FileUtils.writeStringToFile(versionFile, CurrentGoCDVersion.getInstance().formatted(), UTF_8);
    }

    private void backupConfigRepository(File backupDir) throws IOException {
        File configRepoDir = systemEnvironment.getConfigRepoDir();
        try (ZipOutputStream configRepoZipStream = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(new File(backupDir, CONFIG_REPOSITORY_BACKUP_ZIP))))) {
            new DirectoryStructureWalker(configRepoDir.getAbsolutePath(), configRepoZipStream).walk();
        }
    }

    private void backupConfig(File backupDir) throws IOException {
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

    private void backupDb(File backupDir) {
        databaseStrategy.backup(backupDir);
    }

    public String backupLocation() {
        return artifactsDirHolder.getBackupsDir().getAbsolutePath();
    }

    public Date lastBackupTime() {
        ServerBackup serverBackup = serverBackupRepository.lastBackup();
        return serverBackup == null ? null : serverBackup.getTime();
    }

    public String lastBackupUser() {
        ServerBackup serverBackup = serverBackupRepository.lastBackup();
        return serverBackup == null ? null : serverBackup.getUsername();
    }

    public void deleteAll() {
        serverBackupRepository.deleteAll();
    }

    public boolean isBackingUp() {
        return backupRunningSince != null;
    }

    public String backupRunningSinceISO8601() {
        return !(backupRunningSince == null) ? backupRunningSince.toString() : null;
    }

    public String backupStartedBy() {
        return !(backupStartedBy == null) ? backupStartedBy : null;
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
