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

import {bind} from "classnames/bind";
import {MithrilViewComponent} from "jsx/mithril-component";
import * as m from "mithril";
import {BackupProgressStatus, BackupStatus} from "models/backups/types";
import {FlashMessage, MessageType} from "views/components/flash_message";
import * as styles from "./progress_indicator.scss";

const classnames = bind(styles);

export interface Attrs {
  status: BackupStatus;
  progressStatus?: BackupProgressStatus;
  message: string;
}

export class ProgressIndicator extends MithrilViewComponent<Attrs> {

  view(vnode: m.Vnode<Attrs>) {
    const backupInProgress = vnode.attrs.status === BackupStatus.IN_PROGRESS ? "Backup in progress..." : undefined;
    const backupComplete   = vnode.attrs.status === BackupStatus.COMPLETED ? "Backup Completed" : undefined;
    const currentStatus    = vnode.attrs.progressStatus || BackupProgressStatus.STARTING;
    const status           = vnode.attrs.status;
    return (
      <div class={styles.stepsContainer}>
        {backupInProgress}
        <BackupStep id={BackupProgressStatus.CREATING_DIR}
                    error={vnode.attrs.message}
                    status={this.getStatus(BackupProgressStatus.CREATING_DIR, currentStatus, status)}>Creating
          backup directory</BackupStep>
        <BackupStep id={BackupProgressStatus.BACKUP_VERSION_FILE}
                    error={vnode.attrs.message}
                    status={this.getStatus(BackupProgressStatus.BACKUP_VERSION_FILE, currentStatus, status)}>
          Backing up version file</BackupStep>
        <BackupStep id={BackupProgressStatus.BACKUP_CONFIG}
                    error={vnode.attrs.message}
                    status={this.getStatus(BackupProgressStatus.BACKUP_CONFIG, currentStatus, status)}>
          Backing up Config</BackupStep>
        <BackupStep id={BackupProgressStatus.BACKUP_CONFIG_REPO}
                    error={vnode.attrs.message}
                    status={this.getStatus(BackupProgressStatus.BACKUP_CONFIG_REPO, currentStatus, status)}>
          Backing up config repo</BackupStep>
        <BackupStep id={BackupProgressStatus.BACKUP_DATABASE}
                    error={vnode.attrs.message}
                    status={this.getStatus(BackupProgressStatus.BACKUP_DATABASE, currentStatus, status)}>
          Backing up Database</BackupStep>
        <BackupStep id={BackupProgressStatus.POST_BACKUP_SCRIPT_START}
                    error={vnode.attrs.message}
                    status={this.getStatus(BackupProgressStatus.POST_BACKUP_SCRIPT_START, currentStatus, status)}>
          Executing Post backup script</BackupStep>
        {backupComplete}
      </div>
    );
  }

  private getStatus(step: BackupProgressStatus, currentStatus: BackupProgressStatus, status: BackupStatus) {
    if (status === BackupStatus.COMPLETED) {
      return StepStatus.PASSED;
    }
    if (currentStatus > step) {
      return StepStatus.PASSED;
    } else if (step === currentStatus && status === BackupStatus.ERROR) {
      return StepStatus.FAILED;
    } else if (step === currentStatus) {
      return StepStatus.RUNNING;
    } else {
      return StepStatus.NOT_RUN;
    }
  }
}

enum StepStatus {
  NOT_RUN = "notRun", RUNNING = "running", PASSED = "passed", FAILED = "failed"
}

interface StepAttrs {
  id: BackupProgressStatus;
  status: StepStatus;
  error: string;
}

class BackupStep extends MithrilViewComponent<StepAttrs> {
  view(vnode: m.Vnode<StepAttrs>) {
    let error;
    if (vnode.attrs.status === StepStatus.FAILED) {
      error = <div class={styles.errorContainer}>
        <FlashMessage type={MessageType.alert}>{vnode.attrs.error}</FlashMessage>
      </div>;
    }
    return <div class={styles.stepContainer}>
      <span data-test-id={`step-${vnode.attrs.id}`}
            class={classnames(styles.indicator, styles[vnode.attrs.status])}/>
      <span class={styles.stepText}>{vnode.children}</span>
      {error}
    </div>;
  }
}
