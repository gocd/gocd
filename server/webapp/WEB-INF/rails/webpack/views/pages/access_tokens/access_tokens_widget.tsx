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
import {Table} from "views/components/table";
import * as styles from "./index.scss";

const TimeFormatter = require("helpers/time_formatter");

interface Attrs {
  accessTokens: Stream<AccessTokens>;
  onRevoke: (accessToken: Stream<AccessToken>, e: MouseEvent) => void;
}

export class AccessTokensWidget extends MithrilViewComponent<Attrs> {
  view(vnode: m.Vnode<Attrs>) {
    const accessTokens = vnode.attrs.accessTokens();
    if (accessTokens.length === 0) {
      return (<ul>
        <li>Click on "Generate Token" to create new personal access token.</li>
        <li>A Generated token can be used to access the GoCD API.</li>
      </ul>);
    }

    const data = accessTokens.sortByCreateDate().map((accessToken) => {
      return [
        <span className={styles.description}>{accessToken().description()}</span>,
        AccessTokensWidget.formatTimeInformation(accessToken().createdAt()),
        AccessTokensWidget.getLastUsedInformation(accessToken()),
        this.getRevokeButton(vnode, accessToken)
      ];
    });
    return <Table headers={["Description", "Created at", " Last used", "Revoke"]} data={data}/>;
  }

  private static getLastUsedInformation(accessToken: AccessToken) {
    const lastUsedAt = accessToken.lastUsedAt();
    if (!lastUsedAt) {
      return "Never";
    }

    return AccessTokensWidget.formatTimeInformation(lastUsedAt);
  }

  private static formatTimeInformation(date: Date) {
    const dateStr = TimeFormatter.format(date);

    if (date.toDateString() === new Date().toDateString()) {
      return `Today ${dateStr.substr(dateStr.indexOf("at"))}`;
    }

    return dateStr;
  }

  private getRevokeButton(vnode: m.Vnode<Attrs>, accessToken: Stream<AccessToken>) {
    if (accessToken().revoked()) {
      return <span className={styles.revoked}>Revoked</span>;
    }
    return <Buttons.Default data-test-id="button-revoke"
                            onclick={vnode.attrs.onRevoke.bind(this, accessToken)}>Revoke</Buttons.Default>;
  }
}
