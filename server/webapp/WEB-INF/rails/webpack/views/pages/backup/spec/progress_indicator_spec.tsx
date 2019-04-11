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
import {ProgressIndicator} from "views/pages/backup/progress_indicator";
import * as styles from "../progress_indicator.scss";

describe("Backup Progress Indicator Widget", () => {

  const helper = new TestHelper();

  afterEach(helper.unmount.bind(helper));

  it("should render not-run status before backup starts", () => {
    mount(BackupStatus.NOT_STARTED, BackupProgressStatus.STARTING);

    for (let key = BackupProgressStatus.CREATING_DIR; key < BackupProgressStatus.POST_BACKUP_SCRIPT_COMPLETE; key++) {
      expect(helper.findByDataTestId(`step-${key}`)).toHaveClass(styles.notRun);
    }
    expect(helper.findByClass(styles.stepsContainer)).not.toContainText("Backup in progress...");
    expect(helper.findByClass(styles.stepsContainer)).not.toContainText("Backup Completed");
  });

  it("should render status while backup is running", () => {
    mount(BackupStatus.IN_PROGRESS, BackupProgressStatus.BACKUP_VERSION_FILE);

    expect(helper.findByDataTestId(`step-${BackupProgressStatus.BACKUP_VERSION_FILE}`)).toHaveClass(styles.running);
    expect(helper.findByDataTestId(`step-${BackupProgressStatus.CREATING_DIR}`)).toHaveClass(styles.passed);
    for (let key = BackupProgressStatus.BACKUP_CONFIG; key < BackupProgressStatus.POST_BACKUP_SCRIPT_COMPLETE; key++) {
      expect(helper.findByDataTestId(`step-${key}`)).toHaveClass(styles.notRun);
    }
    expect(helper.findByClass(styles.stepsContainer)).toContainText("Backup in progress...");
    expect(helper.findByClass(styles.stepsContainer)).not.toContainText("Backup Completed");
  });

  it("should render status after backup is complete", () => {
    mount(BackupStatus.COMPLETED, BackupProgressStatus.POST_BACKUP_SCRIPT_COMPLETE);

    for (let key = BackupProgressStatus.CREATING_DIR; key < BackupProgressStatus.POST_BACKUP_SCRIPT_COMPLETE; key++) {
      expect(helper.findByDataTestId(`step-${key}`)).toHaveClass(styles.passed);
    }
    expect(helper.findByClass(styles.stepsContainer)).not.toContainText("Backup in progress...");
    expect(helper.findByClass(styles.stepsContainer)).toContainText("Backup Completed");
  });

  it("should render failed status", () => {
    mount(BackupStatus.ERROR, BackupProgressStatus.BACKUP_DATABASE);
    for (let key = BackupProgressStatus.CREATING_DIR; key < BackupProgressStatus.BACKUP_DATABASE; key++) {
      expect(helper.findByDataTestId(`step-${key}`)).toHaveClass(styles.passed);
    }
    expect(helper.findByDataTestId(`step-${BackupProgressStatus.BACKUP_DATABASE}`)).toHaveClass(styles.failed);
  });

  function mount(status: BackupStatus,
                 progressStatus?: BackupProgressStatus) {
    helper.mount(() => {
      return <ProgressIndicator status={status} progressStatus={progressStatus}/>;
    });
  }

});
