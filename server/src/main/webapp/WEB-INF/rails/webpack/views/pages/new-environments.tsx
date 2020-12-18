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

import {ApiResult, ErrorResponse} from "helpers/api_request_builder";
import _ from "lodash";
import m from "mithril";
import Stream from "mithril/stream";
import {Agents} from "models/agents/agents";
import {AgentsCRUD} from "models/agents/agents_crud";
import {Environments, EnvironmentWithOrigin} from "models/new-environments/environments";
import {EnvironmentsAPIs} from "models/new-environments/environments_apis";
import {Permissions, SupportedEntity} from "models/shared/permissions";
import {AnchorVM, ScrollManager} from "views/components/anchor/anchor";
import {Primary} from "views/components/buttons";
import {FlashMessage, MessageType} from "views/components/flash_message";
import {SearchField} from "views/components/forms/input_fields";
import {HeaderPanel} from "views/components/header_panel";
import {DeleteConfirmModal} from "views/components/modal/delete_confirm_modal";
import configRepoStyles from "views/pages/config_repos/index.scss";
import {CreateEnvModal} from "views/pages/new-environments/create_env_modal";
import {EnvironmentsWidget} from "views/pages/new-environments/environments_widget";
import {Page, PageState} from "views/pages/page";
import {AddOperation, DeleteOperation, SaveOperation} from "views/pages/page_operations";

const sm: ScrollManager = new AnchorVM();

interface State extends AddOperation<EnvironmentWithOrigin>, SaveOperation, DeleteOperation<EnvironmentWithOrigin> {
  searchText: Stream<string>;
}

export class NewEnvironmentsPage extends Page<null, State> {
  private readonly environments: Stream<Environments> = Stream(new Environments());
  private readonly agents: Stream<Agents>             = Stream(new Agents());

  oninit(vnode: m.Vnode<null, State>) {
    super.oninit(vnode);

    vnode.state.searchText = Stream();

    vnode.state.onAdd = (e: MouseEvent) => {
      e.stopPropagation();
      this.flashMessage.clear();
      new CreateEnvModal(vnode.state.onSuccessfulSave, this.environments).render();
    };

    vnode.state.onSuccessfulSave = (msg: m.Children) => {
      this.flashMessage.setMessage(MessageType.success, msg);
      this.fetchData(vnode);
    };

    vnode.state.onError = (msg) => {
      this.flashMessage.alert(msg);
    };

    vnode.state.onDelete = (env: EnvironmentWithOrigin, e: MouseEvent) => {
      e.stopPropagation();
      this.flashMessage.clear();

      const message                   = <span>Are you sure you want to delete environment <em>{env.name()}</em>?</span>;
      const modal: DeleteConfirmModal = new DeleteConfirmModal(message, () => {
        const self = this;
        return env.delete()
                  .then((result: ApiResult<any>) => {
                    result.do(
                      () => {
                        self.flashMessage.setMessage(MessageType.success,
                                                     `The environment '${env.name()}' was deleted successfully!`);
                      }, (errorResponse: ErrorResponse) => {
                        self.flashMessage.setMessage(MessageType.alert, JSON.parse(errorResponse.body!).message);
                      }
                    );
                    //@ts-ignore
                  }).then(self.fetchData.bind(self))
                  .finally(modal.close.bind(modal));
      });
      modal.render();
    };
  }

  componentToDisplay(vnode: m.Vnode<null, State>): m.Children {
    this.parseEnvironmentLink(sm);
    const flashMessage                       = this.flashMessage.hasMessage() ?
      <FlashMessage type={this.flashMessage.type} message={this.flashMessage.message}/>
      : null;
    const filteredEnvs: Stream<Environments> = Stream();
    if (vnode.state.searchText()) {
      const results = _.filter(this.environments(), (env: EnvironmentWithOrigin) => env.matches(vnode.state.searchText()));

      if (_.isEmpty(results)) {
        return <div>
          <FlashMessage type={MessageType.info}>No Results for the search string: <em>{vnode.state.searchText()}</em></FlashMessage>
        </div>;
      }
      filteredEnvs(new Environments(...results));
    } else {
      filteredEnvs(this.environments());
    }
    return [
      flashMessage,
      <EnvironmentsWidget environments={filteredEnvs}
                          agents={this.agents}
                          onSuccessfulSave={vnode.state.onSuccessfulSave}
                          onDelete={vnode.state.onDelete.bind(vnode.state)}
                          sm={sm}/>
    ];
  }

  pageName(): string {
    return "Environments";
  }

  fetchData(vnode: m.Vnode<null, State>): Promise<any> {
    return Promise.all([EnvironmentsAPIs.all(), AgentsCRUD.all(), Permissions.all([SupportedEntity.environment])]).then((results) => {
      results[0].do((successResponse) => {
        results[2].do((permissionsResponse) => {
          this.mergePermissionsWithEnvironment(successResponse.body, permissionsResponse.body);
        }, this.setErrorState);

        this.pageState = PageState.OK;
        this.environments(successResponse.body);
      }, this.setErrorState);

      results[1].do((successResponse) => {
        this.agents(successResponse.body);
      }, this.setErrorState);
    });
  }

  headerPanel(vnode: m.Vnode<null, State>): any {
    const headerButtons = [];
    headerButtons.push(<Primary dataTestId="add-environment-button" onclick={vnode.state.onAdd}>Add
      Environment</Primary>);
    if (!_.isEmpty(this.environments())) {
      const searchBox = <div className={configRepoStyles.wrapperForSearchBox}>
        <SearchField property={vnode.state.searchText} dataTestId={"search-box"}
                     placeholder="Search for a environment name"/>
      </div>;
      headerButtons.splice(0, 0, searchBox);
    }
    return <HeaderPanel title="Environments" buttons={headerButtons} help={this.helpText()}/>;
  }

  helpText(): m.Children {
    return EnvironmentsWidget.helpText();
  }

  private parseEnvironmentLink(sm: ScrollManager) {
    sm.setTarget(m.route.param().name || "");
  }

  private mergePermissionsWithEnvironment(environments: Environments, permissions: Permissions) {
    const envPermissions = permissions.for(SupportedEntity.environment);
    environments.forEach((env) => {
      env.canAdminister(envPermissions.canAdminister(env.name()));
    });
  }
}
