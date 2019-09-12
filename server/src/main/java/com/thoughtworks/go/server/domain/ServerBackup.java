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
package com.thoughtworks.go.server.domain;

import com.thoughtworks.go.domain.HibernatePersistedObject;
import lombok.*;
import lombok.experimental.Accessors;

import javax.persistence.*;
import java.util.Date;
import java.util.Optional;

@EqualsAndHashCode(doNotUseGetters = true, callSuper = true)
@ToString(callSuper = true)
@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
@Entity
@Table(name = "serverBackups")
@Cacheable
public class ServerBackup extends HibernatePersistedObject {
    private Date time;
    private String path;
    private String username;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BackupStatus status;
    private String message;
    @Column(name = "progressStatus")
    @Enumerated(EnumType.STRING)
    private BackupProgressStatus backupProgressStatus;

    public ServerBackup(String path, Date time, String username, String message) {
        this(path, time, username, message, BackupStatus.IN_PROGRESS);
        this.backupProgressStatus = BackupProgressStatus.STARTING;
    }

    public ServerBackup(String path, Date time, String username, String message, BackupStatus status) {
        this.path = path;
        this.time = time;
        this.username = username;
        this.message = message;
        this.status = status;
    }

    public ServerBackup(String path, Date time, String username, BackupStatus status, String message, long id) {
        this.path = path;
        this.time = time;
        this.username = username;
        this.message = message;
        this.status = status;
        this.id = id;
    }

    public Optional<BackupProgressStatus> getBackupProgressStatus() {
        return Optional.ofNullable(backupProgressStatus);
    }

    public boolean isSuccessful() {
        return BackupStatus.COMPLETED.equals(status);
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public ServerBackup setProgressStatus(BackupProgressStatus status) {
        this.backupProgressStatus = status;
        this.message = status.getMessage();
        return this;
    }

    public void markCompleted() {
        this.status = BackupStatus.COMPLETED;
    }

    public void markError(String message) {
        this.status = BackupStatus.ERROR;
        this.message = message;
    }

    public Boolean hasFailed() {
        return BackupStatus.ERROR.equals(status);
    }
}
