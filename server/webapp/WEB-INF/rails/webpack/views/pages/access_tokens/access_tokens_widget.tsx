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

interface Attrs {
  accessTokens: Stream<AccessTokens>;
  onRevoke: (accessToken: Stream<AccessToken>, e: MouseEvent) => void;
}

export class AccessTokensWidget extends MithrilViewComponent<Attrs> {
  view(vnode: m.Vnode<Attrs>) {
    const data = vnode.attrs.accessTokens().map((accessToken, index) => {
      const lastUsedAt = accessToken().meta().lastUsedAt() ? accessToken().meta().lastUsedAt()!.toString() : "Never";
      return [index + 1,
        <span className={styles.description}>{accessToken().description()}</span>,
        accessToken().meta().createdAt().toString(),
        lastUsedAt,
        this.getRevokeButton(vnode, accessToken)
      ];
    });
    return <Table headers={["#", "Description", "Created at", " Last used on", ""]} data={data}/>;
  }

  private getRevokeButton(vnode: m.Vnode<Attrs>, accessToken: Stream<AccessToken>) {
    if (accessToken().meta().revoked()) {
      return <span className={styles.revoked}>Revoked</span>;
    }
    return <Buttons.Secondary data-test-id="button-revoke"
                              onclick={vnode.attrs.onRevoke.bind(this, accessToken)}>Revoke</Buttons.Secondary>;
  }
}
