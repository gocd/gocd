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

import {docsUrl} from "gen/gocd_version";
import {MithrilComponent} from "jsx/mithril-component";
import m from "mithril";
import {BackupProgressStatus, BackupStatus} from "models/backups/types";
import * as Buttons from "views/components/buttons";
import {FlashMessage, MessageType} from "views/components/flash_message";
import styles from "./index.scss";
import {ProgressIndicator} from "./progress_indicator";

export interface Attrs {
  lastBackupTime: Date | null | undefined;
  lastBackupUser: string | null | undefined;
  availableDiskSpace: string;
  backupLocation: string;
  message: string;
  backupStatus: BackupStatus;
  backupProgressStatus?: BackupProgressStatus;
  onPerformBackup: () => void;
  displayProgressIndicator: boolean;
}

export class BackupWidget extends MithrilComponent<Attrs> {
  view(vnode: m.Vnode<Attrs>) {
    let progressIndicator;
    if (vnode.attrs.displayProgressIndicator) {
      progressIndicator = <ProgressIndicator progressStatus={vnode.attrs.backupProgressStatus}
                                             status={vnode.attrs.backupStatus}
                                             message={vnode.attrs.message}/>;
    }
    return <div class={styles.backupContainer}>
      {this.topLevelError(vnode)}
      <div class={styles.content}>
        <div class={styles.performBackupContainer}>
          <div class={styles.performBackupSection}>
            <div class={styles.performBackupButtonContainer}>
              <Buttons.Primary data-test-id="perform-backup" onclick={this.startBackup.bind(this, vnode)}
                               disabled={vnode.attrs.backupStatus === BackupStatus.IN_PROGRESS}>
                Perform Backup
              </Buttons.Primary>
            </div>
            <div class={styles.backupInfo}>
              <p class={styles.availableDiskSpace}>
                <span>Available disk space in backup directory:</span> {vnode.attrs.availableDiskSpace}
              </p>
              {this.lastBackupDetails(vnode)}
            </div>
          </div>
          {progressIndicator}
          {this.backupConfigHelp(vnode)}
        </div>
        {this.backupHelp()}
      </div>
    </div>;
  }

  private lastBackupDetails(vnode: m.Vnode<Attrs>) {
    if (vnode.attrs.lastBackupTime && vnode.attrs.lastBackupUser) {
      return <p>Last backup was taken
        by <span>'{vnode.attrs.lastBackupUser}'</span> at <span>{vnode.attrs.lastBackupTime.toLocaleString()}</span>
      </p>;
    }
  }

  private backupHelp() {
    return <div class={styles.backupHelp}>
      <h3 class={styles.helpHeading}>On performing a backup, GoCD will create a backup of:</h3>
      <ul class={styles.helpItem}>
        <li>
          <strong>Configuration</strong> - An archive named <code>config-dir.zip</code> containing XML configuration, Jetty server
          configuration, keystores and other GoCD internal configurations.
        </li>
        <li>
          <strong>Wrapper Configuration</strong> - An archive named <code>wrapper-config-dir.zip</code> containing tanuki wrapper configuration.
        </li>
        <li>
          <strong>Database</strong> - The database is archived to a file named <code>db.zip</code> which is used to restore GoCD's database.
        </li>
        <li>
          <strong>Configuration History</strong> - An archive named <code>config-repo.zip</code> containing a Git repository of the
          XML configuration file.
        </li>
        <li>
          <strong>GoCD Version</strong> - A flat file named <code>version.txt</code> containing the version of GoCD that the backup was
          taken with.
        </li>
      </ul>
    </div>;
  }

  private startBackup(vnode: m.Vnode<Attrs>) {
    if (vnode.attrs.backupStatus === BackupStatus.IN_PROGRESS) {
      return;
    }
    vnode.attrs.onPerformBackup();
  }

  private backupConfigHelp(vnode: m.Vnode<Attrs>) {
    return <div class={styles.backupConfigHelp}>
      <p>Backups are stored in <strong class={styles.backupLocation}>{vnode.attrs.backupLocation}</strong></p>
      <p>To configure backups, please see <a target="_blank" href={docsUrl("advanced_usage/cron_backup.html")}>backup
        configuration documentation</a></p>
    </div>;
  }

  private topLevelError(vnode: m.Vnode<Attrs>) {
    if (vnode.attrs.backupStatus === BackupStatus.ERROR
      && (vnode.attrs.backupProgressStatus === undefined || vnode.attrs.backupProgressStatus < 1)) {
      return <FlashMessage dataTestId="top-level-error" type={MessageType.alert} message={vnode.attrs.message}/>;
    }
  }
}
