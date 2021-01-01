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

import m from "mithril";
import {BackupProgressStatus, BackupStatus} from "models/backups/types";
import {ProgressIndicator} from "views/pages/backup/progress_indicator";
import {TestHelper} from "views/pages/spec/test_helper";
import styles from "../progress_indicator.scss";

describe("Backup Progress Indicator Widget", () => {

  const helper = new TestHelper();

  afterEach(helper.unmount.bind(helper));

  it("should render not-run status before backup starts", () => {
    mount(BackupStatus.NOT_STARTED, "", BackupProgressStatus.STARTING);

    for (let key = BackupProgressStatus.CREATING_DIR; key < BackupProgressStatus.POST_BACKUP_SCRIPT_COMPLETE; key++) {
      expectStatusNotRun(key);
    }
    expect(helper.byClass(styles.stepsContainer)).not.toContainText("Backup in progress...");
    expect(helper.byClass(styles.stepsContainer)).not.toContainText("Backup Completed");
    expect(helper.byClass(styles.errorContainer)).not.toBeInDOM();
  });

  it("should render status while backup is running", () => {
    mount(BackupStatus.IN_PROGRESS, "Backing up version file", BackupProgressStatus.BACKUP_VERSION_FILE);

    expect(helper.byTestId(`step-${BackupProgressStatus.BACKUP_VERSION_FILE}`)).toHaveClass(styles.backingUp);
    expect(helper.byTestId(`step-${BackupProgressStatus.CREATING_DIR}`)).toHaveClass(styles.backedUp);
    for (let key = BackupProgressStatus.BACKUP_CONFIG; key < BackupProgressStatus.POST_BACKUP_SCRIPT_COMPLETE; key++) {
      expectStatusNotRun(key);
    }
    expect(helper.byClass(styles.stepsContainer)).toContainText("Backup in progress...");
    expect(helper.byClass(styles.stepsContainer)).not.toContainText("Backup Completed");
    expect(helper.byClass(styles.errorContainer)).not.toBeInDOM();
  });

  it("should render status after backup is complete", () => {
    mount(BackupStatus.COMPLETED, "Backup Completed", BackupProgressStatus.POST_BACKUP_SCRIPT_COMPLETE);

    for (let key = BackupProgressStatus.CREATING_DIR; key < BackupProgressStatus.POST_BACKUP_SCRIPT_COMPLETE; key++) {
      expect(helper.byTestId(`step-${key}`)).toHaveClass(styles.backedUp);
    }
    expect(helper.byClass(styles.stepsContainer)).not.toContainText("Backup in progress...");
    expect(helper.byClass(styles.stepsContainer)).toContainText("Backup Completed");
    expect(helper.byClass(styles.errorContainer)).not.toBeInDOM();
  });

  it("should render failed status", () => {
    mount(BackupStatus.ERROR, "Backup failed for some reason", BackupProgressStatus.BACKUP_DATABASE);
    for (let key = BackupProgressStatus.CREATING_DIR; key < BackupProgressStatus.BACKUP_DATABASE; key++) {
      expect(helper.byTestId(`step-${key}`)).toHaveClass(styles.backedUp);
    }
    expect(helper.byTestId(`step-${BackupProgressStatus.BACKUP_DATABASE}`)).toHaveClass(styles.failed);
    expect(helper.byClass(styles.errorContainer)).toHaveText("Backup failed for some reason");
    expectStatusNotRun(BackupProgressStatus.POST_BACKUP_SCRIPT_START);
  });

  function mount(status: BackupStatus,
                 message: string,
                 progressStatus?: BackupProgressStatus) {
    helper.mount(() => {
      return <ProgressIndicator status={status} progressStatus={progressStatus} message={message}/>;
    });
  }

  function expectStatusNotRun(key: BackupProgressStatus) {
    expect(helper.byTestId(`step-${key}`)).not.toHaveClass(styles.backingUp);
    expect(helper.byTestId(`step-${key}`)).not.toHaveClass(styles.backedUp);
    expect(helper.byTestId(`step-${key}`)).not.toHaveClass(styles.failed);
  }

});
