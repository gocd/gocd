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

import * as m from "mithril";
import {Stream} from "mithril/stream";
import * as stream from "mithril/stream";
import {AccessTokenCRUD} from "models/access_tokens/access_token_crud";
import {AccessToken, AccessTokens} from "models/access_tokens/types";
import * as Buttons from "views/components/buttons";
import {FlashMessage, MessageType} from "views/components/flash_message";
import {HeaderPanel} from "views/components/header_panel";
import {AccessTokensWidget} from "views/pages/access_tokens/access_tokens_widget";
import {GenerateTokenModal, RevokeTokenModal} from "views/pages/access_tokens/modals";
import {Page, PageState} from "views/pages/page";
import {AddOperation, SaveOperation} from "views/pages/page_operations";

interface State extends AddOperation<AccessToken>, SaveOperation {
  accessTokens: Stream<AccessTokens>;
  onRevoke: (accessToken: Stream<AccessToken>, e: MouseEvent) => void;
}

export class AccessTokensPage extends Page<null, State> {
  oninit(vnode: m.Vnode<null, State>) {
    vnode.state.accessTokens = stream();
    super.oninit(vnode);

    vnode.state.onAdd = (e: MouseEvent) => {
      e.stopPropagation();
      new GenerateTokenModal(vnode.state.accessTokens, vnode.state.onSuccessfulSave, vnode.state.onError).render();
    };

    vnode.state.onRevoke = (accessToken: Stream<AccessToken>, e: MouseEvent) => {
      e.stopPropagation();
      new RevokeTokenModal(vnode.state.accessTokens,
                           accessToken,
                           vnode.state.onSuccessfulSave,
                           vnode.state.onError).render();
    };

    vnode.state.onSuccessfulSave = (msg: m.Children) => {
      this.flashMessage.setMessage(MessageType.success, msg);
    };

    vnode.state.onError = (msg: m.Children) => {
      this.flashMessage.setMessage(MessageType.alert, msg);
    };
  }

  pageName(): string {
    return "Personal Access Tokens";
  }

  headerPanel(vnode: m.Vnode<null, State>): any {
    return <HeaderPanel title={this.pageName()} buttons={
      <Buttons.Primary onclick={vnode.state.onAdd.bind(vnode.state)}>Generate Token</Buttons.Primary>
    }/>;
  }

  componentToDisplay(vnode: m.Vnode<null, State>): m.Children {
    const flashMessage = this.flashMessage ?
      <FlashMessage message={this.flashMessage.message} type={this.flashMessage.type}/> : null;

    return [flashMessage, <AccessTokensWidget accessTokens={vnode.state.accessTokens}
                                              onRevoke={vnode.state.onRevoke}/>];
  }

  fetchData(vnode: m.Vnode<null, State>): Promise<any> {
    return AccessTokenCRUD.all().then((result) =>
                                        result.do((successResponse) => {
                                          vnode.state.accessTokens(successResponse.body);
                                          this.pageState = PageState.OK;
                                        }, this.setErrorState));
  }
}
