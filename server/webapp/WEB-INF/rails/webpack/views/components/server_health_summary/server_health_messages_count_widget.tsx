/*
 * Copyright 2018 ThoughtWorks, Inc.
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

import * as m from 'mithril';
import {Stream} from 'mithril/stream';

import * as styles from './server_health_messages_count_widget.scss';
import {MithrilComponent} from "../../../jsx/mithril-component";
import {
  ServerHealthMessage,
  ServerHealthMessages
} from "../../../models/shared/server_health_messages/server_health_messages";

const Modal         = require('views/shared/new_modal');
const TimeFormatter = require('helpers/time_formatter');
const classnames    = require('classnames/bind').bind(styles);

class HealthMessageWidget extends MithrilComponent<ServerHealthMessage> {
  view(vnode: m.Vnode<ServerHealthMessage>) {
    return (
      <li class={classnames(styles.serverHealthStatus, vnode.attrs.level.toLowerCase())}>
        <span class={styles.message}>{vnode.attrs.message}</span>
        <span class={styles.timestamp}>{TimeFormatter.format(vnode.attrs.time)}</span>
        <p class={styles.detail}>{m.trust(vnode.attrs.detail)}</p>
      </li>
    );
  }
}

interface Attrs {
  serverHealthMessages: Stream<ServerHealthMessages>
}

interface State {
  openServerHealthMessagesModal: () => void;
}

export class ServerHealthMessagesCountWidget implements m.Component<Attrs, State> {
  // @ts-ignore
  private __tsx_attrs: Attrs<Header, Actions> & m.Lifecycle<Attrs<Header, Actions>, this> & { key?: string | number };

  oninit(vnode: m.Vnode<Attrs, State>) {
    const modal = new Modal({
      size:    'large',
      title:   'Error and warning messages',
      body:    () => (
        <ul class="server-health-statuses">
          {
            vnode.attrs.serverHealthMessages().collect((msg: ServerHealthMessage) => {
              return <HealthMessageWidget {...msg}/>;
            })
          }
        </ul>
      ),
      onclose: () => modal.destroy(),
      buttons: [
        {
          text:    "OK",
          class:   'close',
          onclick: () => {
            modal.destroy();
          }
        }
      ]
    });

    vnode.state.openServerHealthMessagesModal = () => {
      modal.render();
    };
  }

  view(vnode: m.Vnode<Attrs, State>) {
    if (vnode.attrs.serverHealthMessages().hasMessages()) {
      return (
        <a class={styles.serverHealthMessagesContainer}
           onclick={vnode.state.openServerHealthMessagesModal.bind(vnode.state)}> {vnode.attrs.serverHealthMessages().summaryMessage()}</a>
      );
    }
  }
}

