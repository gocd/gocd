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
import {MithrilViewComponent} from "jsx/mithril-component";
import m from "mithril";
import Stream from "mithril/stream";
import {CurrentUser} from "models/new_preferences/current_user";
import {ButtonGroup, Cancel, Primary} from "views/components/buttons";
import {Form, FormBody} from "views/components/forms/form";
import {CheckboxField, TextField} from "views/components/forms/input_fields";
import {EmailSettingsAttrs} from "views/pages/new_preferences";
import {OperationState} from "views/pages/page_operations";
import styles from "views/pages/server-configuration/index.scss";

export class EmailSettingsWidget extends MithrilViewComponent<EmailSettingsAttrs> {
  private ajaxOperationMonitor = Stream<OperationState>(OperationState.UNKNOWN);

  view(vnode: m.Vnode<EmailSettingsAttrs, this>): m.Children | void | null {
    const currentUser           = vnode.attrs.currentUserVM().entity();
    const disabledActionButtons = !vnode.attrs.currentUserVM().isModified();

    return <div data-test-id="email-settings-widget" class={styles.formContainer}>
      <FormBody>
        <div class={styles.formHeader}>
          <h2>Email Settings</h2>
        </div>
        <div class={styles.formFields}>
          <Form compactForm={true}>
            <TextField label="Email" type={"email"}
                       property={currentUser.email}
                       helpText={"The email to which the notification is send."}
                       docLink={docsUrl("configuration/dev_notifications.html")}
                       placeholder={"Email not set"}/>
            <CheckboxField dataTestId="enable-email-notification"
                           property={currentUser.emailMe}
                           label={"Enable email notification"}/>
          </Form>
        </div>
        <div class={styles.formFields}>
          <Form compactForm={true}>
            <TextField label="My check-in aliases"
                       property={this.checkinAliasesProxy.bind(this, currentUser)}
                       placeholder={"No matchers defined"}
                       helpText={"Usually the commits will be either in 'user' or 'username'. Specify both the values here."}/>
          </Form>
        </div>
        <div class={styles.buttons}>
          <ButtonGroup>
            <Cancel data-test-id={"cancel"}
                    disabled={disabledActionButtons}
                    ajaxOperationMonitor={this.ajaxOperationMonitor}
                    onclick={vnode.attrs.onCancel.bind(this, vnode.attrs.currentUserVM())}>Cancel</Cancel>
            <Primary data-test-id={"save-email-settings"}
                     disabled={disabledActionButtons}
                     ajaxOperation={vnode.attrs.onSaveEmailSettings.bind(this, vnode.attrs.currentUserVM())}
                     ajaxOperationMonitor={this.ajaxOperationMonitor}>Save</Primary>
          </ButtonGroup>
        </div>
      </FormBody>
    </div>;
  }

  private checkinAliasesProxy(currentUser: CurrentUser, newValue?: string) {
    if (newValue !== undefined) {
      const updatedValues = newValue.trim().split(',');
      currentUser.checkinAliases(updatedValues);
    }
    return currentUser.checkinAliases().join(',');
  }
}
