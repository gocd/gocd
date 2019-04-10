/*
 * Copyright 2019 ThoughtWorks, Inc.
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

public enum BackupProgressStatus {

    STARTING("Starting Backup"),
    CREATING_DIR("Creating backup directory"),
    BACKUP_VERSION_FILE("Backing up version file"),
    BACKUP_CONFIG("Backing up Config"),
    BACKUP_CONFIG_REPO("Backing up config repo"),
    BACKUP_DATABASE("Backing up Database"),
    POST_BACKUP_SCRIPT_START("Executing Post backup script"),
    POST_BACKUP_SCRIPT_COMPLETE("Post backup script executed successfully");

    private final String message;

    BackupProgressStatus(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
