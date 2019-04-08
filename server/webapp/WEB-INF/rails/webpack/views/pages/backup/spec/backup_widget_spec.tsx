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
import {BackupStatus} from "models/backups/types";
import {TestHelper} from "views/pages/artifact_stores/spec/test_helper";
import {BackupWidget} from "views/pages/backup/backup_widget";
import * as progressConsoleStyles from "../../../components/progress_console/index.scss";
import * as styles from "../index.scss";

describe("AuthorizationConfigurationWidget", () => {
  const performBackup = jasmine.createSpy("onPerformBackup");
  const helper         = new TestHelper();

  afterEach(helper.unmount.bind(helper));

  it("should render available disk space and last backup details", () => {
    mount("2000 GB", BackupStatus.NOT_STARTED, [],  new Date("2019-02-27T00:15:00"), "admin-person");
    expect(helper.findByClass(styles.availableDiskSpace)).toContainText("Available disk space in backup directory: 2000 GB");
    expect(helper.findByClass(styles.backupInfo)).toContainText(`Last backup was taken by 'admin-person' at ${new Date("2019-02-27T00:15:00").toLocaleString()}`);
  });

  it("should not render last backup details if not set", () => {
    mount("2000 GB", BackupStatus.NOT_STARTED, []);
    expect(helper.findByClass(styles.backupInfo)).not.toContainText("Last backup was taken");
  });

  it("should render backup help", () => {
    mount("2000 GB", BackupStatus.NOT_STARTED, []);
    expect(helper.findByClass(styles.backupHelp)).toContainText("On performing a backup, Go will create a backup of:");
    expect(helper.findByClass(styles.backupHelp)).toContainText("Go Version - A flat file named version.txt containing the version of Go that the backup was taken against.");
    expect(helper.findByClass(styles.backupHelp)).toContainText("Database - The database is archived to a file which could be used to restore Go's database.");
    expect(helper.findByClass(styles.backupConfigHelp)).toContainText("Backups are stored in /path/to/backup/directory");
  });

  it("should render progress messages when backup in progress", () => {
    mount("2000 GB", BackupStatus.NOT_STARTED, ["first message", "second message"]);
    expect(helper.findByClass(progressConsoleStyles.message)).toHaveLength(2);
    expect(helper.findByClass(progressConsoleStyles.message).get(0)).toContainText("first message");
    expect(helper.findByClass(progressConsoleStyles.message).get(1)).toContainText("second message");
  });

  it("should not render progress spinner when backup not in progress", () => {
    mount("2000 GB", BackupStatus.NOT_STARTED, []);
    expect(helper.findByClass(styles.backupInProgress)).toHaveLength(0);
  });

  it("should render progress spinner only when backup in progress", () => {
    mount("200 GB", BackupStatus.IN_PROGRESS, []);
    expect(helper.findByClass(styles.backupInProgress)).toHaveLength(1);
  });

  function mount(availableDiskSpace: string, status: BackupStatus, progressMsgs: string[], lastBackupTime?: Date | null, lastBackupUser?: string | null) {
    helper.mount(() => {
        return <BackupWidget lastBackupTime={lastBackupTime}
                             lastBackupUser={lastBackupUser}
                             availableDiskSpace={availableDiskSpace}
                             backupLocation={"/path/to/backup/directory"}
                             backupProgressMessages={progressMsgs}
                             displayProgressConsole={true}
                             backupStatus={status}
                             onPerformBackup={performBackup}/>
          ;
      });
  }
});
