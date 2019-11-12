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

import {MithrilViewComponent} from "jsx/mithril-component";
import m from "mithril";
import Stream from "mithril/stream";
import {Form, FormBody} from "views/components/forms/form";
import {CheckboxField, NumberField, PasswordField, TextField} from "views/components/forms/input_fields";
import {Delete, IconGroup} from "views/components/icons";
import {OperationState} from "views/pages/page_operations";
import {MailServerManagementAttrs} from "views/pages/server_configuration";
import {ButtonGroup, Cancel, Primary} from "../../components/buttons";
import styles from "./index.scss";

const senderEmailHelpText = (
  <span>Emails will be sent from this email address. This will be used as the <code>From:</code> field of the email.</span>
);

const portHelpText = (
  <span>Specify the port number of your SMTP server. You SMTP server will usually run on port <em>25</em>, <em>465</em> (if using SSL) or <em>587</em> (if using TLS).</span>
);

const smtpsHelpText = (
  <span>
    This changes the protocol used to send the mail. It switches between <em>SMTP</em> and <em>SMTPS</em>.
    To enable <code>STARTLS</code> support, for providers such as GMail and Office 365,
  </span>
);

export class MailServerManagementWidget extends MithrilViewComponent<MailServerManagementAttrs> {
  private ajaxOperationMonitor = Stream<OperationState>(OperationState.UNKNOWN);

  view(vnode: m.Vnode<MailServerManagementAttrs>) {
    const mailServer = vnode.attrs.mailServer();

    return <div data-test-id="mail-server-management-widget" class={styles.formContainer}>
      <FormBody>
        <div class={styles.formHeader}>
          <h2>Configure your email server settings</h2>
          <div class={styles.deleteIcon}><IconGroup>
            <Delete data-test-id={"Delete"}
                    disabled={!vnode.attrs.canDeleteMailServer()}
                    onclick={() => vnode.attrs.onMailServerManagementDelete()}>
              Delete</Delete>
          </IconGroup>
          </div>
        </div>
        <div class={styles.formFields}>
          <Form compactForm={true}>
            <TextField
              label="SMTP hostname"
              errorText={mailServer.errors().errorsForDisplay("hostname")}
              onchange={() => mailServer.validate("hostname")}
              property={mailServer.hostname}
              helpText={"Specify the hostname or ip address of your SMTP server."}
              required={true}/>

            <NumberField
              label="SMTP port"
              errorText={mailServer.errors().errorsForDisplay("port")}
              onchange={() => mailServer.validate("port")}
              property={mailServer.port}
              helpText={portHelpText}
              required={true}/>

            <CheckboxField
              label={"Use SMTPS"}
              property={mailServer.tls}
              helpText={smtpsHelpText}
              docLink="/configuration/admin_mailhost_info.html#smtps-and-tls"
            />

            <TextField
              label="SMTP username"
              errorText={mailServer.errors().errorsForDisplay("username")}
              onchange={() => mailServer.validate("password")}
              property={mailServer.username}
              helpText={"Specify the username, if the SMTP server requires authentication."}/>

            <PasswordField
              label="SMTP password"
              errorText={mailServer.errors().errorsForDisplay("password")}
              onchange={() => mailServer.validate("password")}
              property={mailServer.password}
              helpText={"Specify the password, if the SMTP server requires authentication."}
            />

            <TextField
              label="Send email using address"
              errorText={mailServer.errors().errorsForDisplay("senderEmail")}
              onchange={() => mailServer.validate("senderEmail")}
              property={mailServer.senderEmail}
              required={true}
              helpText={senderEmailHelpText}/>

            <TextField
              label="Administrator email"
              errorText={mailServer.errors().errorsForDisplay("adminEmail")}
              onchange={() => mailServer.validate("adminEmail")}
              property={mailServer.adminEmail}
              required={true}
              helpText={"One or more email address of GoCD system administrators. This email will be notified if the server runs out of disk space, or if backups fail."}/>
          </Form>
        </div>
        <div class={styles.buttons}>
          <ButtonGroup>
            <Cancel data-test-id={"cancel"}
                    ajaxOperation={vnode.attrs.onCancel}
                    ajaxOperationMonitor={this.ajaxOperationMonitor}
                    onclick={() => vnode.attrs.onCancel()}>Cancel</Cancel>
            <Primary data-test-id={"save"}
                     ajaxOperationMonitor={this.ajaxOperationMonitor}
                     ajaxOperation={() => vnode.attrs.onMailServerManagementSave(vnode.attrs.mailServer())}
                     onclick={() => vnode.attrs.onMailServerManagementSave(vnode.attrs.mailServer())}>Save</Primary>
          </ButtonGroup>
        </div>
      </FormBody>
    </div>;
  }
}
