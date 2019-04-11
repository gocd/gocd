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
import {BackupProgressStatus, BackupStatus} from "models/backups/types";
import {TestHelper} from "views/pages/artifact_stores/spec/test_helper";
import {BackupWidget} from "views/pages/backup/backup_widget";
import * as styles from "../index.scss";

describe("Backup Widget", () => {
  const performBackup = jasmine.createSpy("onPerformBackup");
  const helper        = new TestHelper();

  afterEach(helper.unmount.bind(helper));

  it("should render available disk space and last backup details", () => {
    mount("2000 GB", BackupStatus.NOT_STARTED, "", undefined, new Date("2019-02-27T00:15:00"), "admin-person");
    expect(helper.findByClass(styles.availableDiskSpace))
      .toContainText("Available disk space in backup directory: 2000 GB");
    expect(helper.findByClass(styles.backupInfo))
      .toContainText(`Last backup was taken by 'admin-person' at ${new Date("2019-02-27T00:15:00").toLocaleString()}`);
  });

  it("should not render last backup details if not set", () => {
    mount("2000 GB", BackupStatus.NOT_STARTED, "");
    expect(helper.findByClass(styles.backupInfo)).not.toContainText("Last backup was taken");
  });

  it("should render backup help", () => {
    mount("2000 GB", BackupStatus.NOT_STARTED, "");
    expect(helper.findByClass(styles.backupHelp)).toContainText("On performing a backup, Go will create a backup of:");
    expect(helper.findByClass(styles.backupHelp))
      .toContainText(
        "Go Version - A flat file named version.txt containing the version of Go that the backup was taken against.");
    expect(helper.findByClass(styles.backupHelp))
      .toContainText("Database - The database is archived to a file which could be used to restore Go's database.");
    expect(helper.findByClass(styles.backupConfigHelp))
      .toContainText("Backups are stored in /path/to/backup/directory");
  });

  it("should not render progress spinner when backup not in progress", () => {
    mount("2000 GB", BackupStatus.NOT_STARTED, "");
    expect(helper.findByClass(styles.backupInProgress)).toHaveLength(0);
  });

  it("should render progress spinner only when backup in progress", () => {
    mount("200 GB", BackupStatus.IN_PROGRESS, "");
    expect(helper.findByClass(styles.backupInProgress)).toHaveLength(1);
  });

  it("should render top level error if backup fails to start", () => {
    mount("200 GB", BackupStatus.ERROR, "Something went wrong");
    expect(helper.findByDataTestId(`flash-message-alert`)).toHaveText("Something went wrong");
  });

  it("should not render top level error if backup has already started", () => {
    mount("200 GB", BackupStatus.ERROR, "Something went wrong", BackupProgressStatus.BACKUP_VERSION_FILE);
    expect(helper.findByDataTestId(`flash-message-alert`)).not.toBeInDOM();
  });

  function mount(availableDiskSpace: string,
                 status: BackupStatus,
                 message: string,
                 backupProgressStatus?: BackupProgressStatus,
                 lastBackupTime?: Date | null,
                 lastBackupUser?: string | null) {
    helper.mount(() => {
      return <BackupWidget lastBackupTime={lastBackupTime}
                           lastBackupUser={lastBackupUser}
                           availableDiskSpace={availableDiskSpace}
                           message={message}
                           backupStatus={status}
                           displayProgressIndicator={true}
                           backupProgressStatus={backupProgressStatus}
                           backupLocation={"/path/to/backup/directory"}
                           onPerformBackup={performBackup}/>;
    });
  }
});
