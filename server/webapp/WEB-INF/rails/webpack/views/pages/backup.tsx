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

import * as m from "mithril";
import {ServerBackupAPI} from "models/backups/server_backup_api";
import {BackupStatus} from "models/backups/types";
import {BackupWidget} from "views/pages/backup/backup_widget";
import {ToggleConfirmModal} from "views/pages/maintenance_mode/confirm_modal";
import {Page} from "./page";

interface State {
  lastBackupTime: Date | null | undefined;
  lastBackupUser: string | null | undefined;
  backupLocation: string;
  progressMessages: string[];
  availableDiskSpace: string;
  backupStatus: BackupStatus;
  backupInProgress: boolean;
  backupFailed: boolean;
  displayProgressConsole: boolean;
  onPerformBackup: () => void;
}

export class BackupPage extends Page<null, State> {
  oninit(vnode: m.Vnode<null, State>) {
    super.oninit(vnode);
    vnode.state.availableDiskSpace = this.getMeta().availableDiskSpace;
    vnode.state.lastBackupUser = this.getMeta().lastBackupUser;
    vnode.state.lastBackupTime = new Date(this.getMeta().lastBackupTime);
    vnode.state.backupLocation = this.getMeta().backupLocation;
    vnode.state.backupInProgress = false;
    vnode.state.displayProgressConsole = false;
    vnode.state.progressMessages = [];
    vnode.state.backupStatus = BackupStatus.NOT_STARTED;

    vnode.state.onPerformBackup = () => {
      const message = "Jobs that are building may get rescheduled if the backup process takes a long time. Proceed with backup?";
      const modal   = new ToggleConfirmModal(message, () => {
        modal.close();
        vnode.state.displayProgressConsole = true;
        vnode.state.progressMessages = [];
        ServerBackupAPI.start((backup) => {
          vnode.state.backupStatus = backup.status;
          this.addProgressMessage(vnode, `${backup.message}...`);
        }, (backup) => {
          this.addProgressMessage(vnode, backup.message);
          vnode.state.backupStatus = backup.status;
          vnode.state.lastBackupUser = backup.username;
          vnode.state.lastBackupTime = backup.time;
        }, (error) => {
          this.addProgressMessage(vnode, error);
          vnode.state.backupStatus = BackupStatus.ERROR;
        });
      }, "Server backup confirmation", "Confirm");
      modal.render();
    };
  }
  pageName() {
    return "Backup";
  }

  componentToDisplay(vnode: m.Vnode<null, State>) {
    return <BackupWidget
      lastBackupTime={vnode.state.lastBackupTime}
      lastBackupUser={vnode.state.lastBackupUser}
      availableDiskSpace={vnode.state.availableDiskSpace}
      backupLocation={vnode.state.backupLocation}
      backupProgressMessages={vnode.state.progressMessages}
      backupStatus={vnode.state.backupStatus}
      onPerformBackup={vnode.state.onPerformBackup}
      displayProgressConsole={vnode.state.displayProgressConsole}
    />;
  }

  fetchData() {
    return Promise.resolve({});
  }

  private addProgressMessage(vnode: m.Vnode<null, State>, message: string) {
    if (vnode.state.progressMessages[vnode.state.progressMessages.length - 1] !== message) {
      vnode.state.progressMessages.push(message);
    }
  }
}
