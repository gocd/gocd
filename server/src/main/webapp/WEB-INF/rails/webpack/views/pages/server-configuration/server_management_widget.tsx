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
import {ButtonGroup, Cancel, Primary} from "views/components/buttons";
import {Form, FormBody} from "views/components/forms/form";
import {TextField} from "views/components/forms/input_fields";
import {ServerManagementAttrs} from "views/pages/server_configuration";
import {OperationState} from "../page_operations";
import styles from "./index.scss";

export class ServerManagementWidget extends MithrilViewComponent<ServerManagementAttrs> {
  private ajaxOperationMonitor = Stream<OperationState>(OperationState.UNKNOWN);

  view(vnode: m.Vnode<ServerManagementAttrs>) {
    const siteUrlsDocsLink      = "installation/configuring_server_details.html#configure-site-urls";
    const siteUrlHelpText       = "This entry will be used by Go Server to generate links for emails, feeds etc. Format: [protocol]://[host]:[port].";
    const serverSiteUrlHelpText = "If you wish that your primary site URL be HTTP, but still want to have HTTPS endpoints " +
      "for the features that require SSL, you can specify this attribute with a value of the base HTTPS URL. Format: https://[host]:[port].";

    return <div data-test-id={"server-management-widget"} class={styles.formContainer}>
      <FormBody>
        <div class={styles.formHeader}>
          <h2>Configure your server site urls</h2>
        </div>
        <div class={styles.formFields}>
          <Form compactForm={true}>
            <TextField label="Site URL"
                       property={vnode.attrs.siteUrls.siteUrl}
                       helpText={siteUrlHelpText}
                       docLink={siteUrlsDocsLink}
                       errorText={vnode.attrs.siteUrls.errors().errorsForDisplay("siteUrl")}/>
            <TextField label="Secure Site URL"
                       property={vnode.attrs.siteUrls.secureSiteUrl}
                       errorText={vnode.attrs.siteUrls.errors().errorsForDisplay("secureSiteUrl")}
                       helpText={serverSiteUrlHelpText}
                       docLink={siteUrlsDocsLink}/>
          </Form>
        </div>
        <div class={styles.buttons}>
          <ButtonGroup>
            <Cancel data-test-id={"cancel"} onclick={() => vnode.attrs.onCancel()} ajaxOperation={vnode.attrs.onCancel}
                    ajaxOperationMonitor={this.ajaxOperationMonitor}>Cancel</Cancel>
            <Primary data-test-id={"save"} onclick={() => this.onSave(vnode)} ajaxOperation={this.onSave.bind(this, vnode)}
                     ajaxOperationMonitor={this.ajaxOperationMonitor}>Save</Primary>
          </ButtonGroup>
        </div>
      </FormBody>
    </div>;
  }

  onSave(vnode: m.Vnode<ServerManagementAttrs>) {
    if (vnode.attrs.siteUrls.isValid()) {
      return vnode.attrs.onServerManagementSave(vnode.attrs.siteUrls);
    }
    return Promise.resolve();
  }
}
