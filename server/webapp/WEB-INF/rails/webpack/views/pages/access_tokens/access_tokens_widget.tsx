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
import * as m from "mithril";
import {Stream} from "mithril/stream";
import {AccessToken, AccessTokens} from "models/access_tokens/types";
import * as Buttons from "views/components/buttons";
import {Ellipsize} from "views/components/ellipsize";
import {Tabs} from "views/components/tab";
import {Table} from "views/components/table";
import * as styles from "./index.scss";

const TimeFormatter = require("helpers/time_formatter");

interface Attrs {
  accessTokens: Stream<AccessTokens>;
  onRevoke: (accessToken: Stream<AccessToken>, e: MouseEvent) => void;
}

export class AccessTokensWidget extends MithrilViewComponent<Attrs> {

  public static formatTimeInformation(date: Date) {
    const dateStr = TimeFormatter.format(date);

    if (date.toDateString() === new Date().toDateString()) {
      return `Today ${dateStr.substr(dateStr.indexOf("at"))}`;
    }

    return dateStr;
  }

  public static getLastUsedInformation(accessToken: AccessToken) {
    const lastUsedAt = accessToken.lastUsedAt();
    if (!lastUsedAt) {
      return "Never";
    }

    return AccessTokensWidget.formatTimeInformation(lastUsedAt);
  }

  view(vnode: m.Vnode<Attrs>) {
    const accessTokens = vnode.attrs.accessTokens();
    if (accessTokens.length === 0) {
      return (<ul data-test-id="access-token-info">
        <li>Click on "Generate Token" to create new personal access token.</li>
        <li>A Generated token can be used to access the GoCD API.</li>
      </ul>);
    }

    return <Tabs
      tabs={["Active Tokens", "Revoked Tokens"]}
      contents={[
        this.getActiveTokensView(accessTokens, vnode),
        this.getRevokedTokensView(accessTokens)
      ]}/>;

  }

  private getActiveTokensView(accessTokens: AccessTokens, vnode: m.Vnode<Attrs>) {
    const activeTokensData = this.getActiveTokensData(accessTokens, vnode);

    if (activeTokensData.length === 0) {
      return <p>
        You don't have any active tokens.
        Click on 'Generate Token' button to create a new token.
      </p>;
    }

    return <Table headers={["Description", "Created At", "Last Used", "Revoke"]}
                  data={activeTokensData}/>;
  }

  private getRevokedTokensView(accessTokens: AccessTokens) {
    const revokedTokensData = this.getRevokedTokensData(accessTokens);

    if (revokedTokensData.length === 0) {
      return <p>You don't have any revoked tokens.</p>;
    }

    return <Table data={revokedTokensData}
                  headers={["Description", "Created At", "Last Used", "Revoked At", "Revoked Message"]}/>;
  }

  private getActiveTokensData(accessTokens: AccessTokens, vnode: m.Vnode<Attrs>) {
    return accessTokens.activeTokens().sortByCreateDate().map((accessToken) => {
      return [
        <Ellipsize text={accessToken().description()}/>,
        AccessTokensWidget.formatTimeInformation(accessToken().createdAt()),
        AccessTokensWidget.getLastUsedInformation(accessToken()),
        this.getRevokeButton(vnode, accessToken)
      ];
    });
  }

  private getRevokedTokensData(accessTokens: AccessTokens) {
    return accessTokens.revokedTokens().sortByRevokeTime().map((accessToken) => {
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

  private getRevokeButton(vnode: m.Vnode<Attrs>, accessToken: Stream<AccessToken>) {
    if (accessToken().revoked()) {
      return <span className={styles.revoked}>Revoked</span>;
    }
    return <Buttons.Default data-test-id="button-revoke"
                            onclick={vnode.attrs.onRevoke.bind(this, accessToken)}>Revoke</Buttons.Default>;
  }
}
