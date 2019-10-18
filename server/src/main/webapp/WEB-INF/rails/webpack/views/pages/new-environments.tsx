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

import {ApiResult, ErrorResponse} from "helpers/api_request_builder";
import m from "mithril";
import Stream from "mithril/stream";
import {Environments, EnvironmentWithOrigin} from "models/new-environments/environments";
import {EnvironmentsAPIs} from "models/new-environments/environments_apis";
import * as Buttons from "views/components/buttons";
import {FlashMessage, MessageType} from "views/components/flash_message";
import {HeaderPanel} from "views/components/header_panel";
import {CreateEnvModal} from "views/pages/new-environments/create_env_modal";
import {EnvironmentsWidget} from "views/pages/new-environments/environments_widget";
import {Page, PageState} from "views/pages/page";
import {AddOperation, DeleteOperation, SaveOperation} from "views/pages/page_operations";
import {DeleteConfirmModal} from "../components/modal/delete_confirm_modal";

interface State extends AddOperation<EnvironmentWithOrigin>, SaveOperation, DeleteOperation<EnvironmentWithOrigin> {
  // onSuccessfulSave: (msg: m.Children) => void;
}

export class NewEnvironmentsPage extends Page<null, State> {
  private readonly environments: Stream<Environments> = Stream(new Environments());

  oninit(vnode: m.Vnode<null, State>) {
    super.oninit(vnode);

    vnode.state.onAdd = (e: MouseEvent) => {
      e.stopPropagation();
      this.flashMessage.clear();
      new CreateEnvModal(vnode.state.onSuccessfulSave).render();
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

      const message = ["Are you sure you want to delete environment ", m("strong", env.name()), "?"];
      const modal   = new DeleteConfirmModal(message, () => {
        const self = this;
        env.delete()
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
    const flashMessage = this.flashMessage.hasMessage() ?
      <FlashMessage type={this.flashMessage.type} message={this.flashMessage.message}/>
      : null;
    return [
      flashMessage,
      <EnvironmentsWidget environments={this.environments}
                          onSuccessfulSave={vnode.state.onSuccessfulSave}
                          onDelete={vnode.state.onDelete.bind(vnode.state)}/>
    ];
  }

  pageName(): string {
    return "Environments";
  }

  fetchData(vnode: m.Vnode<null, State>): Promise<any> {
    return EnvironmentsAPIs.all().then((result) =>
                                         result.do((successResponse) => {
                                           this.pageState = PageState.OK;
                                           this.environments(successResponse.body);
                                         }, this.setErrorState));
  }

  headerPanel(vnode: m.Vnode<null, State>): any {
    const headerButtons = [];
    headerButtons.push(<Buttons.Primary onclick={vnode.state.onAdd.bind(vnode.state)}>Add
      Environment</Buttons.Primary>);
    return <HeaderPanel title="Environments" buttons={headerButtons}/>;
  }
}
