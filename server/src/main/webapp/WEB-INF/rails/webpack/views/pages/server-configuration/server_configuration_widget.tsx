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
import m from "mithril";
import style from "views/pages/agents/index.scss";
import {MailServerManagementWidget} from "views/pages/server-configuration/mail_server_management_widget";
import {ServerManagementWidget} from "views/pages/server-configuration/server_management_widget";
import {
  ArtifactManagementAttrs,
  JobTimeoutAttrs,
  MailServerManagementAttrs, Routing,
  ServerManagementAttrs
} from "views/pages/server_configuration";
import {ArtifactsManagementWidget} from "./artifacts_management_widget";
import styles from "./index.scss";
import {JobTimeoutConfigurationWidget} from "./job_timeout_configuration_widget";

const classnames = bind(style);

interface Attrs extends ArtifactManagementAttrs, ServerManagementAttrs, MailServerManagementAttrs, JobTimeoutAttrs, Routing {
}

export enum Sections {
  DEFAULT_JOB_TIMEOUT = "job-timeout",
  SERVER_MANAGEMENT   = "server-management",
  ARTIFACT_MANAGEMENT = "artifact-management",
  EMAIL_SERVER        = "email-server"
}

export class ServerConfigurationWidget extends MithrilViewComponent<Attrs> {
  view(vnode: m.Vnode<Attrs>) {
    return <div class={styles.serverConfigurationContainer}>
      <div class={styles.leftPanel}>
        <ul>
          <li data-test-id="server-management-link"
              class={classnames({[styles.active]: vnode.attrs.activeConfiguration === Sections.SERVER_MANAGEMENT})}
              onclick={() => vnode.attrs.route(Sections.SERVER_MANAGEMENT)}>
            Server Management
          </li>
          <li data-test-id="artifacts-management-link"
              class={classnames({[styles.active]: vnode.attrs.activeConfiguration === Sections.ARTIFACT_MANAGEMENT})}
              onclick={() => vnode.attrs.route(Sections.ARTIFACT_MANAGEMENT)}>
            Artifacts Management
          </li>
          <li data-test-id="email-server-link"
              class={classnames({[styles.active]: vnode.attrs.activeConfiguration === Sections.EMAIL_SERVER})}
              onclick={() => vnode.attrs.route(Sections.EMAIL_SERVER)}>
            Email server
          </li>
          <li data-test-id="job-timeout-link"
              class={classnames({[styles.active]: vnode.attrs.activeConfiguration === Sections.DEFAULT_JOB_TIMEOUT})}
              onclick={() => vnode.attrs.route(Sections.DEFAULT_JOB_TIMEOUT)}>
            Job Timeout Configuration
          </li>
        </ul>
      </div>
      <div class={styles.rightPanel}>
        {ServerConfigurationWidget.renderWidget(vnode)}
      </div>
    </div>;
  }

  private static renderWidget(vnode: m.Vnode<Attrs>) {
    switch (vnode.attrs.activeConfiguration) {
      case Sections.SERVER_MANAGEMENT:
        return <ServerManagementWidget siteUrls={vnode.attrs.siteUrls}
                                       onServerManagementSave={vnode.attrs.onServerManagementSave}
                                       onCancel={vnode.attrs.onCancel}/>;
      case Sections.EMAIL_SERVER:
        return <MailServerManagementWidget mailServer={vnode.attrs.mailServer}
                                           onMailServerManagementSave={vnode.attrs.onMailServerManagementSave}
                                           onMailServerManagementDelete={vnode.attrs.onMailServerManagementDelete}
                                           onCancel={vnode.attrs.onCancel}
                                           canDeleteMailServer={vnode.attrs.canDeleteMailServer}/>;

      case Sections.ARTIFACT_MANAGEMENT:
        return <ArtifactsManagementWidget artifactConfig={vnode.attrs.artifactConfig}
                                          onArtifactConfigSave={vnode.attrs.onArtifactConfigSave}
                                          onCancel={vnode.attrs.onCancel}/>;
      case Sections.DEFAULT_JOB_TIMEOUT:
        return <JobTimeoutConfigurationWidget defaultJobTimeout={vnode.attrs.defaultJobTimeout}
                                              onCancel={vnode.attrs.onCancel}
                                              onDefaultJobTimeoutSave={vnode.attrs.onDefaultJobTimeoutSave}
        />;
    }
  }
}
