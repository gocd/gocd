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

public enum BackupProgressStatus {

    STARTING("Starting Backup"),
    CREATING_DIR("Creating Backup Directory"),
    BACKUP_VERSION_FILE("Backing up Version File"),
    BACKUP_CONFIG("Backing up Configuration"),
    BACKUP_WRAPPER_CONFIG("Backing up Wrapper Configuration"),
    BACKUP_CONFIG_REPO("Backing up Configuration History"),
    BACKUP_DATABASE("Backing up Database"),
    POST_BACKUP_SCRIPT_START("Executing Post Backup Script"),
    POST_BACKUP_SCRIPT_COMPLETE("Post Backup Script executed successfully");

    private final String message;

    BackupProgressStatus(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
