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

import {MithrilViewComponent} from "jsx/mithril-component";
import m from "mithril";
import {BackupProgressStatus, BackupStatus} from "models/backups/types";
import {FlashMessage, MessageType} from "views/components/flash_message";
import styles from "./progress_indicator.scss";

export interface Attrs {
  status: BackupStatus;
  progressStatus?: BackupProgressStatus;
  message: string;
}

export class ProgressIndicator extends MithrilViewComponent<Attrs> {

  view(vnode: m.Vnode<Attrs>) {
    const currentStatus = vnode.attrs.progressStatus || BackupProgressStatus.STARTING;
    const status        = vnode.attrs.status;
    return (
      <div class={styles.stepsContainer}>
        {this.backupInProgress(status)}
        <ul class={styles.backupSteps}>
          <BackupStep id={BackupProgressStatus.CREATING_DIR}
                      error={vnode.attrs.message}
                      status={this.getStatus(BackupProgressStatus.CREATING_DIR, currentStatus, status)}>Creating
            Backup Directory</BackupStep>
          <BackupStep id={BackupProgressStatus.BACKUP_VERSION_FILE}
                      error={vnode.attrs.message}
                      status={this.getStatus(BackupProgressStatus.BACKUP_VERSION_FILE, currentStatus, status)}>
            Backing up Version File</BackupStep>
          <BackupStep id={BackupProgressStatus.BACKUP_CONFIG}
                      error={vnode.attrs.message}
                      status={this.getStatus(BackupProgressStatus.BACKUP_CONFIG, currentStatus, status)}>
            Backing up Configuration</BackupStep>
          <BackupStep id={BackupProgressStatus.BACKUP_WRAPPER_CONFIG}
                      error={vnode.attrs.message}
                      status={this.getStatus(BackupProgressStatus.BACKUP_WRAPPER_CONFIG, currentStatus, status)}>
            Backing up Wrapper Configuration</BackupStep>
          <BackupStep id={BackupProgressStatus.BACKUP_CONFIG_REPO}
                      error={vnode.attrs.message}
                      status={this.getStatus(BackupProgressStatus.BACKUP_CONFIG_REPO, currentStatus, status)}>
            Backing up Configuration History</BackupStep>
          <BackupStep id={BackupProgressStatus.BACKUP_DATABASE}
                      error={vnode.attrs.message}
                      status={this.getStatus(BackupProgressStatus.BACKUP_DATABASE, currentStatus, status)}>
            Backing up Database</BackupStep>
          <BackupStep id={BackupProgressStatus.POST_BACKUP_SCRIPT_START}
                      error={vnode.attrs.message}
                      status={this.getStatus(BackupProgressStatus.POST_BACKUP_SCRIPT_START, currentStatus, status)}>
            Executing Post Backup Script</BackupStep>
        </ul>
        {this.backupComplete(status)}
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

  private backupInProgress(backupStatus: BackupStatus) {
    if (backupStatus === BackupStatus.IN_PROGRESS) {
      return <p class={styles.backupMessage}>Backup in progress...</p>;
    }
  }

  private backupComplete(backupStatus: BackupStatus) {
    if (backupStatus === BackupStatus.COMPLETED) {
      return <p class={styles.backupMessage}>Backup Completed</p>;
    }
  }
}

enum StepStatus {
  NOT_RUN, RUNNING, PASSED, FAILED
}

interface StepAttrs {
  id: BackupProgressStatus;
  status: StepStatus;
  error: string;
}

class BackupStep extends MithrilViewComponent<StepAttrs> {
  view(vnode: m.Vnode<StepAttrs>) {
    return <li data-test-id={`step-${vnode.attrs.id}`}
               class={this.indicatorClass(vnode.attrs.status)}>
      {this.maybeSpinner(vnode)}{vnode.children}{this.maybeError(vnode)}
    </li>;
  }

  private maybeSpinner(vnode: m.Vnode<StepAttrs>) {
    let maybeSpinner;
    if (vnode.attrs.status === StepStatus.RUNNING) {
      maybeSpinner = <span class={styles.spinner}/>;
    }
    return maybeSpinner;
  }

  private maybeError(vnode: m.Vnode<StepAttrs>) {
    let error;
    if (vnode.attrs.status === StepStatus.FAILED) {
      error = <div class={styles.errorContainer}>
        <FlashMessage type={MessageType.alert}>{vnode.attrs.error}</FlashMessage>
      </div>;
    }
    return error;
  }

  private indicatorClass(stepStatus: StepStatus) {
    if (stepStatus === StepStatus.RUNNING) {
      return styles.backingUp;
    }
    if (stepStatus === StepStatus.PASSED) {
      return styles.backedUp;
    }
    if (stepStatus === StepStatus.FAILED) {
      return styles.failed;
    }
    return "";
  }
}
