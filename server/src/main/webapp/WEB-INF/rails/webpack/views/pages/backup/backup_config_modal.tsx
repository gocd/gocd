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

import {docsUrl} from "gen/gocd_version";
import {MithrilViewComponent} from "jsx/mithril-component";
import m from "mithril";
import Stream from "mithril/stream";
import {BackupConfig} from "models/backup_config/types";
import {ButtonIcon, Cancel, Primary} from "views/components/buttons";
import {FlashMessage, MessageType} from "views/components/flash_message";
import {Form, FormBody} from "views/components/forms/form";
import {CheckboxField, TextField} from "views/components/forms/input_fields";
import {Modal, Size} from "views/components/modal";

export interface Attrs {
  backupConfig: Stream<BackupConfig>;
}

const backupScheduleHelpText = (
  <span>
    A quartz cron-like specification to perform a backup. See the <a
    href="https://www.quartz-scheduler.org/documentation/quartz-2.3.0/tutorials/crontrigger.html"
    target="_blank"
    rel="noopener noreferrer">quartz documentation</a> for the syntax and <a
    href="https://www.quartz-scheduler.org/documentation/quartz-2.3.0/tutorials/crontrigger.html#examples"
    target="_blank"
    rel="noopener noreferrer">some examples</a>.
  </span>
);

const backupScriptHelpText = (
  <span>
    After a backup is completed, GoCD will invoke this script, allowing you to copy the backup to another machine or service.
    See the <a href={docsUrl("/advanced_usage/cron_backup.html")} target="_blank"
               rel="noopener noreferrer">help documentation</a> for more information.
  </span>
);

class BackupConfigWidget extends MithrilViewComponent<Attrs> {
  view(vnode: m.Vnode<Attrs>) {
    return (
      <div>
        <h1>
          Backup Configurations
        </h1>

        <FormBody>
          <Form>
            <TextField label="Backup schedule"
                       helpText={backupScheduleHelpText}
                       property={vnode.attrs.backupConfig().schedule}
                       errorText={vnode.attrs.backupConfig().errors().errorsForDisplay("schedule")}/>

            <TextField label="Post backup script"
                       property={vnode.attrs.backupConfig().postBackupScript}
                       helpText={backupScriptHelpText}
                       errorText={vnode.attrs.backupConfig().errors().errorsForDisplay("postBackupScript")}/>

            <CheckboxField label="Send email on backup failure"
                           property={vnode.attrs.backupConfig().emailOnFailure}
                           helpText="If checked, an email will be sent when backup fails, for any reason."/>

            <CheckboxField label="Send email on backup success"
                           property={vnode.attrs.backupConfig().emailOnSuccess}
                           helpText="If checked, an email will be sent when backup succeeds."/>
          </Form>
        </FormBody>
      </div>
    );
  }
}

type MouseEventHandler = (modal: BackupConfigModal) => void;

export class BackupConfigModal extends Modal {
  private readonly backupConfig: Stream<BackupConfig>;
  private readonly showSpinner: Stream<boolean>;

  private readonly errorMessage: Stream<string>;
  private readonly isSaving: Stream<boolean>;
  private onsave: MouseEventHandler;

  constructor(backupConfig: Stream<BackupConfig>,
              showSpinner: Stream<boolean>,
              errorMessage: Stream<string>,
              isSaving: Stream<boolean>,
              onsave: MouseEventHandler) {
    super(Size.medium);
    this.backupConfig = backupConfig;
    this.showSpinner  = showSpinner;
    this.errorMessage = errorMessage;
    this.isSaving     = isSaving;
    this.onsave       = onsave;
  }

  title(): string {
    return "Configure backup settings";
  }

  body() {
    return [
      <FlashMessage message={this.errorMessage()} type={MessageType.alert}/>,
      [<BackupConfigWidget backupConfig={this.backupConfig} key={"backup config"}/>]
    ];
  }

  buttons() {
    if (this.isLoading()) {
      return [<Primary data-test-id="button-ok" onclick={this.close.bind(this)}>Cancel</Primary>];
    } else if (this.errorMessage()) {
      return [<Primary data-test-id="button-ok" onclick={this.close.bind(this)}>Close</Primary>];
    } else {
      return [
        <Primary data-test-id="button-ok" icon={this.isSaving() ? ButtonIcon.SPINNER : undefined}
                 onclick={this.onsave.bind(this, this)}>Save</Primary>,
        <Cancel data-test-id="button-ok" onclick={this.close.bind(this)}>Cancel</Cancel>
      ];
    }
  }

  protected isLoading(): boolean {
    return this.showSpinner();
  }

}
