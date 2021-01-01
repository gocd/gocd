/*
 * Copyright 2021 ThoughtWorks, Inc.
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
import {MithrilComponent} from "jsx/mithril-component";
import m from "mithril";
import Stream from "mithril/stream";
import {ServerHealthMessages} from "models/shared/server_health_messages/server_health_messages";
import styles from "./server_health_messages_count_widget.scss";
import {ServerHealthMessagesModal} from "./server_health_messages_modal";

interface Attrs {
  serverHealthMessages: Stream<ServerHealthMessages>;
}

interface State {
  openServerHealthMessagesModal: () => void;
}

export class ServerHealthMessagesCountWidget extends MithrilComponent<Attrs, State> {
  oninit(vnode: m.Vnode<Attrs, State>) {
    const modal                               = new ServerHealthMessagesModal(vnode.attrs.serverHealthMessages);
    vnode.state.openServerHealthMessagesModal = () => {
      modal.render();
    };
  }

  view(vnode: m.Vnode<Attrs, State>) {
    if (vnode.attrs.serverHealthMessages().hasMessages()) {
      return (
        <a data-test-id="server-health-messages-count"
           aria-label={`Server Health: ${vnode.attrs.serverHealthMessages().summaryMessage()}`}
           class={styles.serverHealthMessagesContainer}
           onclick={vnode.state.openServerHealthMessagesModal.bind(vnode.state)}>
          {vnode.attrs.serverHealthMessages().summaryMessage()}
        </a>
      );
    }
  }
}
