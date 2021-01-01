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
import {Links} from "models/shared/links";
import {UserJSON} from "../users/users";

export interface ServerBackupJson {
  status: BackupStatus;
  message: string;
  time: Date;
  user: UserJSON;
  _links: any;
  progress_status?: BackupProgressStatus;
}

export enum BackupStatus {
  IN_PROGRESS,
  ERROR,
  COMPLETED,
  NOT_STARTED
}

export enum BackupProgressStatus {
  STARTING,
  CREATING_DIR,
  BACKUP_VERSION_FILE,
  BACKUP_CONFIG,
  BACKUP_WRAPPER_CONFIG,
  BACKUP_CONFIG_REPO,
  BACKUP_DATABASE,
  POST_BACKUP_SCRIPT_START,
  POST_BACKUP_SCRIPT_COMPLETE
}

export class ServerBackup {
  readonly status: BackupStatus;
  readonly message: string;
  readonly time: Date;
  readonly username: string;
  readonly links: Links;
  readonly progressStatus?: BackupProgressStatus;

  constructor(status: BackupStatus,
              message: string,
              time: Date,
              username: string,
              links: Links,
              progressStatus?: BackupProgressStatus) {
    this.status         = status;
    this.message        = message;
    this.time           = time;
    this.username       = username;
    this.links          = links;
    this.progressStatus = progressStatus;
  }

  static fromJSON(serverBackupJson: ServerBackupJson): ServerBackup {
    let progressStatus;
    if (serverBackupJson.progress_status) {
      progressStatus = BackupProgressStatus[serverBackupJson.progress_status];
    }
    // @ts-ignore
    return new ServerBackup(BackupStatus[serverBackupJson.status],
                            serverBackupJson.message,
                            new Date(serverBackupJson.time),
                            serverBackupJson.user.login_name,
                            Links.fromJSON(serverBackupJson._links),
                            progressStatus);
  }

  isInProgress(): boolean {
    return BackupStatus.IN_PROGRESS === this.status;
  }

  getStatus(): BackupStatus {
    return this.status;
  }

  getMessage(): string {
    return this.message;
  }

  isComplete(): boolean {
    return BackupStatus.COMPLETED === this.status;
  }
}
