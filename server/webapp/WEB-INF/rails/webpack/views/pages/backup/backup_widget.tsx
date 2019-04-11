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

import {docsUrl} from "gen/gocd_version";
import {MithrilComponent} from "jsx/mithril-component";
import * as m from "mithril";
import {BackupProgressStatus, BackupStatus} from "models/backups/types";
import * as Buttons from "views/components/buttons";
import * as styles from "./index.scss";
import {ProgressIndicator} from "./progress_indicator";

interface Attrs {
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
      <div class={styles.content}>
        <div class={styles.performBackupContainer}>
          <div class={styles.performBackupSection}>
            <div class={styles.performBackupButtonContainer}>
              <Buttons.Primary onclick={this.startBackup.bind(this, vnode)}>
                <span className={vnode.attrs.backupStatus === BackupStatus.IN_PROGRESS ? styles.backupInProgress : ""}/>
                Perform Backup
              </Buttons.Primary>
            </div>
            <div className={styles.backupInfo}>
              <p className={styles.availableDiskSpace}>
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
      <h3 class={styles.helpHeading}>On performing a backup, Go will create a backup of:</h3>
      <ul class={styles.helpItem}>
        <li>
          <strong>Configuration</strong> - An archive named config-dir.zip containing XML configuration, Jetty server
          configuration,
          keystores and other Go internal configurations.
        </li>
        <li>
          <strong>Database</strong> - The database is archived to a file which could be used to restore Go's database.
        </li>
        <li>
          <strong>XML Configuration Version Repository</strong> - An archive named config-repo.zip containing a Git
          repository of the XML
          configuration file.
        </li>
        <li>
          <strong>Go Version</strong> - A flat file named version.txt containing the version of Go that the backup was
          taken against.
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
}
