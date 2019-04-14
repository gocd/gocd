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

import {bind} from "classnames/bind";
import * as m from "mithril";
import {Stream} from "mithril/stream";
import {
  ServerHealthMessage,
  ServerHealthMessages
} from "models/shared/server_health_messages/server_health_messages";
import * as s from "underscore.string";
import {Modal, Size} from "../modal";
import * as styles from "./server_health_messages_count_widget.scss";

const classnames    = bind(styles);
const TimeFormatter = require("helpers/time_formatter");

export class ServerHealthMessagesModal extends Modal {

  private readonly messages: Stream<ServerHealthMessages>;

  constructor(messages: Stream<ServerHealthMessages>) {
    super(Size.large);
    this.messages = messages;
  }

  body(): m.Children {
    return <ul className={styles.serverHealthStatuses}>
      {
        this.messages().collect((msg: ServerHealthMessage) => {
                                  return this.messageView(msg);
                                }
        )
      }
    </ul>;
  }

  title(): string {
    return "Error and warning messages";
  }

  private messageView(message: ServerHealthMessage) {
    const messageId = `server-health-message-for-${s.slugify(message.message)}`;

    return <li data-test-id={messageId}
               data-test-message-level={message.level.toLowerCase()}
               className={classnames(styles.serverHealthStatus, message.level.toLowerCase())}>
      <span data-test-class="server-health-message_message" className={styles.message}>{m.trust(message.message)}</span>
      <span data-test-class="server-health-message_timestamp"
            className={styles.timestamp}>{TimeFormatter.format(message.time)}</span>
      <p data-test-class="server-health-message_detail" className={styles.detail}>{m.trust(message.detail)}</p>
    </li>;
  }
}
