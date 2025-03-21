/*
 * Copyright Thoughtworks, Inc.
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

import classNames from "classnames/bind";
import {MithrilViewComponent} from "jsx/mithril-component";
import m from "mithril";
import {ServerConfigVM} from "models/server-configuration/server_configuration_vm";
import style from "views/pages/agents/index.scss";
import {MailServerManagementWidget} from "views/pages/server-configuration/mail_server_management_widget";
import {ServerManagementWidget} from "views/pages/server-configuration/server_management_widget";
import {
  ArtifactManagementAttrs,
  JobTimeoutAttrs,
  MailServerManagementAttrs,
  Routing,
  ServerManagementAttrs
} from "views/pages/server_configuration";
import {ArtifactsManagementWidget} from "./artifacts_management_widget";
import styles from "./index.scss";
import {JobTimeoutConfigurationWidget} from "./job_timeout_configuration_widget";

const classnames = classNames.bind(style);

interface Attrs extends ArtifactManagementAttrs, ServerManagementAttrs, MailServerManagementAttrs, JobTimeoutAttrs, Routing {
}

export enum Sections {
  DEFAULT_JOB_TIMEOUT = "job-timeout",
  SERVER_MANAGEMENT   = "server-management",
  ARTIFACT_MANAGEMENT = "artifact-management",
  EMAIL_SERVER        = "email-server"
}

class ServerConfigurationRightPanel extends MithrilViewComponent<Attrs> {
  view(vnode: m.Vnode<Attrs>) {
    switch (vnode.attrs.activeConfiguration) {
      case Sections.SERVER_MANAGEMENT:
        return <ServerManagementWidget siteUrlsVM={vnode.attrs.siteUrlsVM}
                                       onServerManagementSave={vnode.attrs.onServerManagementSave}
                                       onCancel={vnode.attrs.onCancel}/>;
      case Sections.EMAIL_SERVER:
        return <MailServerManagementWidget mailServerVM={vnode.attrs.mailServerVM}
                                           onMailServerManagementSave={vnode.attrs.onMailServerManagementSave}
                                           onMailServerManagementDelete={vnode.attrs.onMailServerManagementDelete}
                                           sendTestMail={vnode.attrs.sendTestMail}
                                           testMailResponse={vnode.attrs.testMailResponse}
                                           onCancel={vnode.attrs.onCancel}/>;
      case Sections.ARTIFACT_MANAGEMENT:
        return <ArtifactsManagementWidget artifactConfigVM={vnode.attrs.artifactConfigVM}
                                          onArtifactConfigSave={vnode.attrs.onArtifactConfigSave}
                                          onCancel={vnode.attrs.onCancel}/>;
      case Sections.DEFAULT_JOB_TIMEOUT:
        return <JobTimeoutConfigurationWidget defaultJobTimeoutVM={vnode.attrs.defaultJobTimeoutVM}
                                              onDefaultJobTimeoutSave={vnode.attrs.onDefaultJobTimeoutSave}
                                              onCancel={vnode.attrs.onCancel}/>;
    }
  }
}

export class ServerConfigurationWidget extends MithrilViewComponent<Attrs> {
  view(vnode: m.Vnode<Attrs>) {
    let activeConfigurationVM: ServerConfigVM;
    switch (vnode.attrs.activeConfiguration) {
      case Sections.SERVER_MANAGEMENT:
        activeConfigurationVM = vnode.attrs.siteUrlsVM();
        break;
      case Sections.ARTIFACT_MANAGEMENT:
        activeConfigurationVM = vnode.attrs.artifactConfigVM();
        break;
      case Sections.EMAIL_SERVER:
        activeConfigurationVM = vnode.attrs.mailServerVM();
        break;
      case Sections.DEFAULT_JOB_TIMEOUT:
        activeConfigurationVM = vnode.attrs.defaultJobTimeoutVM();
        break;
    }
    return <div class={styles.serverConfigurationContainer}>
      <div class={styles.leftPanel}>
        <ul>
          <li data-test-id="server-management-link"
              class={classnames({[styles.active]: vnode.attrs.activeConfiguration === Sections.SERVER_MANAGEMENT})}
              onclick={() => vnode.attrs.route(Sections.SERVER_MANAGEMENT, activeConfigurationVM)}>
            Server Management
          </li>
          <li data-test-id="artifacts-management-link"
              class={classnames({[styles.active]: vnode.attrs.activeConfiguration === Sections.ARTIFACT_MANAGEMENT})}
              onclick={() => vnode.attrs.route(Sections.ARTIFACT_MANAGEMENT, activeConfigurationVM)}>
            Artifacts Management
          </li>
          <li data-test-id="email-server-link"
              class={classnames({[styles.active]: vnode.attrs.activeConfiguration === Sections.EMAIL_SERVER})}
              onclick={() => vnode.attrs.route(Sections.EMAIL_SERVER, activeConfigurationVM)}>
            Email server
          </li>
          <li data-test-id="job-timeout-link"
              class={classnames({[styles.active]: vnode.attrs.activeConfiguration === Sections.DEFAULT_JOB_TIMEOUT})}
              onclick={() => vnode.attrs.route(Sections.DEFAULT_JOB_TIMEOUT, activeConfigurationVM)}>
            Job Timeout Configuration
          </li>
        </ul>
      </div>
      <div class={styles.rightPanel}>
        <ServerConfigurationRightPanel {...vnode.attrs}/>
      </div>
    </div>;
  }
}
