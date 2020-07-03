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
import m from "mithril";
import Stream from "mithril/stream";
import {AccessToken, AccessTokens} from "models/access_tokens/types";
import {AdminAccessTokenCRUD} from "models/admin_access_tokens/admin_access_token_crud";
import {FlashMessage, MessageType} from "views/components/flash_message";
import {Link} from "views/components/link";
import {RevokeAccessTokenByAdmin} from "views/pages/access_tokens/modals";
import {Page, PageState} from "views/pages/page";
import {AccessTokensWidgetForAdmin} from "./access_tokens/access_tokens_widget";

interface State {
  accessTokens: Stream<AccessTokens>;
  searchText: Stream<string>;
  onRevoke: (accessToken: Stream<AccessToken>, e: MouseEvent) => void;
}

export class AdminAccessTokensPage extends Page<null, State> {
  oninit(vnode: m.Vnode<null, State>) {
    vnode.state.accessTokens = Stream();
    vnode.state.searchText   = Stream();
    super.oninit(vnode);

    vnode.state.onRevoke = (accessToken: Stream<AccessToken>, e: MouseEvent) => {
      e.stopPropagation();
      new RevokeAccessTokenByAdmin(vnode.state.accessTokens,
                                   accessToken,
                                   (msg: m.Children) => {
                                     this.flashMessage.setMessage(MessageType.success, msg);
                                   },
                                   (msg: m.Children) => {
                                     this.flashMessage.setMessage(MessageType.alert, msg);
                                   }).render();
    };
  }

  componentToDisplay(vnode: m.Vnode<null, State>): m.Children {
    const flashMessage = this.flashMessage ?
      <FlashMessage message={this.flashMessage.message} type={this.flashMessage.type}/> : null;
    let widget;
    if (vnode.state.accessTokens().length === 0) {
      widget = this.helpText();
    } else {
      widget = <AccessTokensWidgetForAdmin accessTokens={vnode.state.accessTokens}
                                           onRevoke={vnode.state.onRevoke}
                                           searchText={vnode.state.searchText}/>;
    }
    return [flashMessage, widget];
  }

  pageName(): string {
    return "Manage access tokens";
  }

  fetchData(vnode: m.Vnode<null, State>): Promise<any> {
    return AdminAccessTokenCRUD
      .all()
      .then((result) =>
              result.do(
                (successResponse) => {
                  vnode.state.accessTokens(successResponse.body);
                  this.pageState = PageState.OK;
                },
                this.setErrorState));
  }

  helpText(): m.Children {
    return (<ol data-test-id="access_token_info">
      <li>Navigate to <a href="/go/access_tokens">Personal Access Tokens</a></li>
      <li>Click on "Generate Token" to create new personal access token.</li>
      <li>The generated token can be used to access the GoCD API.
        <Link href={docsUrl('configuration/access_tokens.html')} externalLinkIcon={true}> Learn More</Link>
      </li>
    </ol>);
  }
}
