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
import {docsUrl} from "gen/gocd_version";
import {MithrilComponent} from "jsx/mithril-component";
import m from "mithril";
import Stream from "mithril/stream";
import {ButtonGroup, Cancel, Primary} from "views/components/buttons";
import {Form, FormBody} from "views/components/forms/form";
import {TextField} from "views/components/forms/input_fields";
import {Link} from "views/components/link";
import {OperationState} from "views/pages/page_operations";
import {ServerManagementAttrs} from "views/pages/server_configuration";
import styles from "./index.scss";

interface State {
  isAllowedToCancel: boolean;
}

export class ServerManagementWidget extends MithrilComponent<ServerManagementAttrs, State> {

  private ajaxOperationMonitor = Stream<OperationState>(OperationState.UNKNOWN);

  view(vnode: m.Vnode<ServerManagementAttrs, State>) {
    const helpTextSiteUrl = <div data-test-id={"help-text-for-siteurl"}>
      This entry will be used by Go Server to generate links for emails, feeds etc. Format: [protocol]://[host]:[port].
      <Link href={docsUrl("/installation/configuring_server_details.html#configure-site-urls")}>more...</Link>
    </div>;

    const helpTextForSecureSiteUrl = <div data-test-id={"help-text-for-secure-siteurl"}>
      If you wish that your primary site URL be HTTP, but still want to have HTTPS endpoints for the features that
      require SSL, you can specify this attribute with a value of the base HTTPS URL. Format: https://[host]:[port].
      <Link href={docsUrl("/installation/configuring_server_details.html#configure-site-urls")}>more...</Link>
    </div>;

    return <div data-test-id={"server-management-widget"} class={styles.formContainer}>
      <FormBody>
        <div class={styles.formHeader}>
          <h2>Configure your server site urls</h2>
        </div>
        <div class={styles.formFields}>
          <Form compactForm={true}>
            <TextField label="Site URL" property={vnode.attrs.siteUrls.siteUrl} helpText={helpTextSiteUrl}
                       errorText={vnode.attrs.siteUrls.errors().errorsForDisplay("siteUrl")}
                       onchange={() => vnode.state.isAllowedToCancel = true}/>
            <TextField label="Secure Site URL" property={vnode.attrs.siteUrls.secureSiteUrl}
                       errorText={vnode.attrs.siteUrls.errors().errorsForDisplay("secureSiteUrl")}
                       helpText={helpTextForSecureSiteUrl}
                       onchange={() => vnode.state.isAllowedToCancel = true}/>
          </Form>
        </div>
        <div class={styles.buttons}>
          <ButtonGroup>
            <Cancel data-test-id={"cancel"} ajaxOperation={this.onCancel.bind(this, vnode)}
                    ajaxOperationMonitor={this.ajaxOperationMonitor}
                    disabled={!vnode.state.isAllowedToCancel}>Cancel</Cancel>
            <Primary data-test-id={"save"} ajaxOperation={this.onSave.bind(this, vnode)}
                     ajaxOperationMonitor={this.ajaxOperationMonitor}>Save</Primary>
          </ButtonGroup>
        </div>
      </FormBody>
    </div>;
  }

  onCancel(vnode: m.Vnode<ServerManagementAttrs, State>): Promise<any> {
    vnode.state.isAllowedToCancel = false;
    return vnode.attrs.onCancel();
  }

  onSave(vnode: m.Vnode<ServerManagementAttrs, State>): Promise<any> {
    if (vnode.attrs.siteUrls.isValid()) {
      vnode.state.isAllowedToCancel = false;
      return vnode.attrs.onServerManagementSave(vnode.attrs.siteUrls);
    }
    return Promise.resolve();
  }
}
