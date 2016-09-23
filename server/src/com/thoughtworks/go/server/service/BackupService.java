/*
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
 */

package com.thoughtworks.go.server.service;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.sql.DataSource;

import com.thoughtworks.go.config.GoMailSender;
import com.thoughtworks.go.database.Database;
import com.thoughtworks.go.i18n.LocalizedMessage;
import com.thoughtworks.go.security.CipherProvider;
import com.thoughtworks.go.server.domain.ServerBackup;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.messaging.EmailMessageDrafter;
import com.thoughtworks.go.server.persistence.ServerBackupRepository;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.server.util.ServerVersion;
import com.thoughtworks.go.server.web.BackupStatusProvider;
import com.thoughtworks.go.serverhealth.HealthStateType;
import com.thoughtworks.go.service.ConfigRepository;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.TimeProvider;
import com.thoughtworks.go.util.VoidThrowingFn;
import org.apache.commons.io.DirectoryWalker;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.joda.time.DateTime;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

/**
 * @understands backing up db and config
 */
@Service
public class BackupService implements BackupStatusProvider {

    private org.slf4j.Logger LOGGER = LoggerFactory.getLogger(BackupService.class);

    public static final String BACKUP = "backup_";
    private final DataSource dataSource;
    private final ArtifactsDirHolder artifactsDirHolder;
    private final GoConfigService goConfigService;
    private ServerBackupRepository serverBackupRepository;
    private final TimeProvider timeProvider;
    private final SystemEnvironment systemEnvironment;
    private ServerVersion serverVersion;
    private final ConfigRepository configRepository;
    private final Database databaseStrategy;

    private GoMailSender mailSender;
    private volatile DateTime backupRunningSince;
    private volatile String backupStartedBy;

    final String CONFIG_BACKUP_ZIP = "config-dir.zip";
    private static final String CONFIG_REPOSITORY_BACKUP_ZIP = "config-repo.zip";
    private static final String VERSION_BACKUP_FILE = "version.txt";
    private static final String BACKUP_MUTEX = "GO-SERVER-BACKUP-IN-PROGRESS".intern();

    @Autowired
    public BackupService(DataSource dataSource, ArtifactsDirHolder artifactsDirHolder, GoConfigService goConfigService, TimeProvider timeProvider,
                         ServerBackupRepository serverBackupRepository, SystemEnvironment systemEnvironment, ServerVersion serverVersion, ConfigRepository configRepository, Database databaseStrategy) {
        this.dataSource = dataSource;
        this.artifactsDirHolder = artifactsDirHolder;
        this.goConfigService = goConfigService;
        this.serverBackupRepository = serverBackupRepository;
        this.systemEnvironment = systemEnvironment;
        this.serverVersion = serverVersion;
        this.configRepository = configRepository;
        this.databaseStrategy = databaseStrategy;
        this.timeProvider = timeProvider;
    }

    public void initialize() {
        mailSender = goConfigService.getMailSender();
    }

    public ServerBackup startBackup(Username username, HttpLocalizedOperationResult result) {
        if (!goConfigService.isUserAdmin(username)) {
            result.unauthorized(LocalizedMessage.string("UNAUTHORIZED_TO_BACKUP"), HealthStateType.unauthorised());
            return null;
        }
        synchronized (BACKUP_MUTEX) {
            DateTime now = timeProvider.currentDateTime();
            LOGGER.info(String.format("Backup started at %s by user '%s'", now, username.getDisplayName()));
            final File destDir = new File(backupLocation(), BACKUP + now.toString("YYYYMMdd-HHmmss"));
            if (!destDir.mkdirs()) {
                result.badRequest(LocalizedMessage.string("BACKUP_UNSUCCESSFUL", "Could not create the backup directory."));
                return null;
            }

            try {
                StopWatch stopWatch = new StopWatch("stop watch");
                backupRunningSince = now;
                backupStartedBy = username.getUsername().toString();
                LOGGER.info("Backing up GoCD version file.");
                stopWatch.start("GoCD version file");
                backupVersion(destDir);
                stopWatch.stop();
                System.out.println(stopWatch.getLastTaskName());
                LOGGER.info("Finished backing up {}. Took {} ms.", stopWatch.getLastTaskName(), stopWatch.getLastTaskTimeMillis());
                LOGGER.info("Backing up config directory.");
                stopWatch.start("config directory");
                backupConfig(destDir);
                stopWatch.stop();
                LOGGER.info("Finished backing up {}. Took {} ms.", stopWatch.getLastTaskName(), stopWatch.getLastTaskTimeMillis());
                LOGGER.info("Backing up config.git repository");
                stopWatch.start("config.git repository");
                configRepository.doLocked(new VoidThrowingFn<IOException>() {
                    @Override public void run() throws IOException {
                        backupConfigRepository(destDir);
                    }
                });
                stopWatch.stop();
                LOGGER.info("Finished backing up {}. Took {} ms.", stopWatch.getLastTaskName(), stopWatch.getLastTaskTimeMillis());
                LOGGER.info("Backing up the database.");
                stopWatch.start("database");
                backupDb(destDir);
                stopWatch.stop();
                LOGGER.info("Finished backing up {}. Took {} ms.", stopWatch.getLastTaskName(), stopWatch.getLastTaskTimeMillis());
                ServerBackup serverBackup = new ServerBackup(destDir.getAbsolutePath(), now.toDate(), username.getUsername().toString());
                serverBackupRepository.save(serverBackup);
                mailSender.send(EmailMessageDrafter.backupSuccessfullyCompletedMessage(destDir.getAbsolutePath(), goConfigService.adminEmail(), username));
                result.setMessage(LocalizedMessage.string("BACKUP_COMPLETED_SUCCESSFULLY"));
                return serverBackup;
            } catch (Exception e) {
                FileUtils.deleteQuietly(destDir);
                result.badRequest(LocalizedMessage.string("BACKUP_UNSUCCESSFUL", e.getMessage()));
                LOGGER.error("[Backup] Failed to backup Go.", e);
                mailSender.send(EmailMessageDrafter.backupFailedMessage(e.getMessage(), goConfigService.adminEmail()));
            } finally {
                backupRunningSince = null;
                backupStartedBy = null;
            }
            return null;
        }
    }

    private void backupVersion(File backupDir) throws IOException {
        File versionFile = new File(backupDir, VERSION_BACKUP_FILE);
        FileUtils.writeStringToFile(versionFile, serverVersion.version());
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
            File cipherFile = systemEnvironment.getCipherFile();
            new DirectoryStructureWalker(configDirectory, configZip, cruiseConfigFile, cipherFile).walk();
            configZip.putNextEntry(new ZipEntry(cruiseConfigFile.getName()));
            IOUtils.write(goConfigService.xml(), configZip);
            configZip.putNextEntry(new ZipEntry(cipherFile.getName()));
            IOUtils.write(new CipherProvider(systemEnvironment).getKey(), configZip);
        }
    }

    private void backupDb(File backupDir) throws SQLException {
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

    public DirectoryStructureWalker(String configDirectory, ZipOutputStream zipStream, File ...excludeFiles) {
        this.excludeFiles = new ArrayList<>();
        for (File excludeFile : excludeFiles) {
            this.excludeFiles.add(excludeFile.getAbsolutePath());
        }

        this.configDirectory = new File(configDirectory).getAbsolutePath();
        this.zipStream = zipStream;
    }

    @Override
    protected boolean handleDirectory(File directory, int depth, Collection results) throws IOException {
        if (! directory.getAbsolutePath().equals(configDirectory)) {
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
