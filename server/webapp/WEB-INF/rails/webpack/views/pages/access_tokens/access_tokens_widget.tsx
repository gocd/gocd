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
import {Ellipsize} from "views/components/ellipsize";
import {Tabs} from "views/components/tab";
import {Table} from "views/components/table";
import {AccessTokensWidget, Attrs} from "views/pages/access_tokens/commons/access_tokens_widget";

export class AccessTokensWidgetForCurrentUser extends AccessTokensWidget {

  protected getTabs(vnode: m.Vnode<Attrs>): m.Child {
    return <Tabs
      tabs={["Active Tokens", "Revoked Tokens"]}
      contents={[
        <Table headers={["Description", "Created at", " Last used", "Revoke"]}
               data={this.getActiveTokensData(vnode)}/>,
        <Table headers={["Description", "Created at", " Last used", "Revoked At", "Revoked Message"]}
               data={this.getRevokedTokensData(vnode)}/>]}
    />;
  }

  private getActiveTokensData(vnode: m.Vnode<Attrs>): m.Child[][] {
    return vnode.attrs.accessTokens().activeTokens().sortByCreateDate().map((accessToken) => {
      return [
        <Ellipsize text={accessToken().description()}/>,
        AccessTokensWidget.formatTimeInformation(accessToken().createdAt()),
        AccessTokensWidget.getLastUsedInformation(accessToken()),
        this.getRevokeButton(vnode, accessToken)
      ];
    });
  }

  private getRevokedTokensData(vnode: m.Vnode<Attrs>): m.Child[][] {
    return vnode.attrs.accessTokens().revokedTokens().sortByRevokeTime().map((accessToken) => {
      const revokedAt = accessToken().revokedAt();

      return [
        <Ellipsize text={accessToken().description()}/>,
        AccessTokensWidget.formatTimeInformation(accessToken().createdAt()),
        AccessTokensWidget.getLastUsedInformation(accessToken()),
        revokedAt ? AccessTokensWidget.formatTimeInformation(revokedAt) : null,
        <Ellipsize text={accessToken().revokeCause()}/>
      ];
    });
  }

}

export class AccessTokensWidgetForAdmin extends AccessTokensWidget {

  protected getTabs(vnode: m.Vnode<Attrs>): m.Child {
    return <Tabs
      tabs={["Active Tokens", "Revoked Tokens"]}
      contents={[
        <Table headers={["Created By", "Description", "Created at", " Last used", "Revoke"]}
               data={this.getActiveTokensData(vnode)}/>,
        <Table
          headers={["Created By", "Description", "Created at", " Last used", "Revoked At", "Revoked Message"]}
          data={this.getRevokedTokensData(vnode)}/>]}
    />;
  }

  private getActiveTokensData(vnode: m.Vnode<Attrs>): m.Child[][] {
    return vnode.attrs.accessTokens().activeTokens().sortByCreateDate().map((accessToken) => {
      return [
        accessToken().username,
        <Ellipsize text={accessToken().description()}/>,
        AccessTokensWidgetForAdmin.formatTimeInformation(accessToken().createdAt()),
        AccessTokensWidgetForAdmin.getLastUsedInformation(accessToken()),
        this.getRevokeButton(vnode, accessToken)
      ];
    });
  }

  private getRevokedTokensData(vnode: m.Vnode<Attrs>): m.Child[][] {
    return vnode.attrs.accessTokens().revokedTokens().sortByRevokeTime().map((accessToken) => {
      const revokedAt = accessToken().revokedAt();

      return [
        accessToken().username,
        <Ellipsize text={accessToken().description()}/>,
        AccessTokensWidgetForAdmin.formatTimeInformation(accessToken().createdAt()),
        AccessTokensWidgetForAdmin.getLastUsedInformation(accessToken()),
        revokedAt ? AccessTokensWidgetForAdmin.formatTimeInformation(revokedAt) : null,
        <Ellipsize text={accessToken().revokeCause()}/>
      ];
    });
  }
}
