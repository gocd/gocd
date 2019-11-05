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

import {JsonUtils} from "helpers/json_utils";
import m from "mithril";
import Stream from "mithril/stream";
import {
  ArtifactConfigCRUD,
  JobTimeoutManagementCRUD,
  MailServerCrud,
  ServerManagementCRUD
} from "models/server-configuration/server_configuartion_crud";
import {
  ArtifactConfig,
  DefaultJobTimeout,
  MailServer,
  SiteUrls
} from "models/server-configuration/server_configuration";
import {FlashMessage, MessageType} from "views/components/flash_message";
import {Page, PageState} from "views/pages/page";
import {OperationState} from "views/pages/page_operations";
import {Sections, ServerConfigurationWidget} from "views/pages/server-configuration/server_configuration_widget";

export interface ServerConfigurationPageOperations {
  onCancel: () => void;
}

export interface ServerManagementAttrs extends ServerConfigurationPageOperations {
  onServerManagementSave: (siteUrls: SiteUrls) => void;
  siteUrls: SiteUrls;
}

export interface JobTimeoutAttrs extends ServerConfigurationPageOperations {
  onDefaultJobTimeoutSave: (jobTimeout: DefaultJobTimeout) => void;
  defaultJobTimeout: Stream<DefaultJobTimeout>;
}

export interface ArtifactManagementAttrs extends ServerConfigurationPageOperations {
  onArtifactConfigSave: (artifactConfig: ArtifactConfig) => void;
  artifactConfig: ArtifactConfig;
}

export interface MailServerManagementAttrs extends ServerConfigurationPageOperations {
  mailServer: Stream<MailServer>;
  operationState: Stream<OperationState>;
  onMailServerManagementSave: (mailServer: MailServer) => void;
}

export interface Routing {
  activeConfiguration: Sections;
  route: (activeConfiguration: Sections) => void;
}

interface State extends ServerManagementAttrs, ArtifactManagementAttrs, MailServerManagementAttrs, JobTimeoutAttrs, Routing {
}

export class ServerConfigurationPage extends Page<null, State> {
  private siteUrlsEtag: string | undefined;
  private artifactConfigEtag: string | undefined;

  oninit(vnode: m.Vnode<null, State>) {
    super.oninit(vnode);
    vnode.state.activeConfiguration    = m.route.param().configuration || Sections.SERVER_MANAGEMENT;
    vnode.state.route                  = (activeConfiguration: Sections) => {
      vnode.state.activeConfiguration = activeConfiguration;
      m.route.set(activeConfiguration);
    };
    vnode.state.siteUrls               = new SiteUrls("", "");
    vnode.state.artifactConfig         = new ArtifactConfig("");
    vnode.state.mailServer             = Stream(new MailServer());
    vnode.state.defaultJobTimeout      = Stream(new DefaultJobTimeout(0));
    vnode.state.operationState         = Stream(OperationState.UNKNOWN) as Stream<OperationState>;
    vnode.state.onServerManagementSave = (siteUrls: SiteUrls) => {
      ServerManagementCRUD.put(siteUrls, this.siteUrlsEtag).then((result) => {
        result.do((successResponse) => {
          this.flashMessage.setMessage(MessageType.success, "Site urls updated successfully");
          this.fetchData(vnode);
        }, (errorResponse) => {
          this.flashMessage.setMessage(MessageType.alert, JSON.parse(errorResponse.body!).message);
        });
      });
    };

    vnode.state.onArtifactConfigSave = (artifactConfig: ArtifactConfig) => {
      ArtifactConfigCRUD.put(artifactConfig, this.artifactConfigEtag).then((result) => {
        result.do((successResponse) => {
          this.flashMessage.setMessage(MessageType.success, "Artifact config updated successfully");
          this.fetchData(vnode);
        }, (errorResponse) => {
          this.flashMessage.setMessage(MessageType.alert, JSON.parse(errorResponse.body!).message);
        });
      });
    };

    vnode.state.onCancel = () => {
      this.fetchData(vnode);
    };

    vnode.state.onMailServerManagementSave = (mailServer: MailServer) => {
      vnode.state.operationState(OperationState.IN_PROGRESS);
      MailServerCrud.createOrUpdate(mailServer).then((result) => {
        result.do(
          (successResponse) => {
            const msg = "Configuration was saved successfully!";
            vnode.state.operationState(OperationState.DONE);
            this.flashMessage.setMessage(MessageType.success, msg);
            this.fetchData(vnode);
          },
          (errorResponse) => {
            vnode.state.operationState(OperationState.DONE);
            if (result.getStatusCode() === 422 && errorResponse.body) {
              vnode.state.mailServer(MailServer.fromJSON(JsonUtils.toCamelCasedObject(JSON.parse(errorResponse.body)).data));
            } else {
              this.flashMessage.setMessage(MessageType.alert, JSON.parse(errorResponse.body!).message);
            }
          });
      });
    };

    vnode.state.onDefaultJobTimeoutSave = (jobTimeout: DefaultJobTimeout) => {
      JobTimeoutManagementCRUD.createOrUpdate(jobTimeout).then((result) => {
        result.do(((successResponse) => {
          const msg = "Configuration was saved successfully!";
          this.flashMessage.setMessage(MessageType.success, msg);
          this.fetchData(vnode);
        }), (errorResponse) => {
          this.flashMessage.setMessage(MessageType.alert, JSON.parse(errorResponse.body!).message);
        });
      });
    };
  }

  componentToDisplay(vnode: m.Vnode<null, State>): m.Children {
    const flashMessage = this.flashMessage.hasMessage() ?
      <FlashMessage type={this.flashMessage.type} message={this.flashMessage.message}/>
      : null;
    return [
      flashMessage,
      <ServerConfigurationWidget {...vnode.state}/>
    ];
  }

  pageName(): string {
    return "Server Configuration";
  }

  fetchData(vnode: m.Vnode<null, State>): Promise<any> {
    return Promise.all([ServerManagementCRUD.get(), ArtifactConfigCRUD.get(), MailServerCrud.get(), JobTimeoutManagementCRUD.get()])
      .then((results) => {
        results[0].do((successResponse) => {
          this.pageState       = PageState.OK;
          vnode.state.siteUrls = successResponse.body.object;
          this.siteUrlsEtag    = successResponse.body.etag;
        }, () => this.setErrorState());

        results[1].do((successResponse) => {
          this.pageState             = PageState.OK;
          vnode.state.artifactConfig = successResponse.body.object;
          this.artifactConfigEtag    = successResponse.body.etag;
        }, () => this.setErrorState());

        results[2].do((successResponse) => {
          vnode.state.mailServer(successResponse.body);
        }, () => this.setErrorState());

        results[3].do((successResponse) => {
          this.pageState = PageState.OK;
          vnode.state.defaultJobTimeout(successResponse.body);
        }, () => this.setErrorState());
      });
  }
}
