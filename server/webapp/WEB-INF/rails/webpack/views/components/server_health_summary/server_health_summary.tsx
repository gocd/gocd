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

import {AjaxPoller} from "helpers/ajax_poller";
import * as m from "mithril";
import * as stream from "mithril/stream";
import {ServerHealthMessages} from "models/shared/server_health_messages/server_health_messages";
import {ServerHealthMessagesCountWidget} from "./server_health_messages_count_widget";

const serverHealthMessages = stream(new ServerHealthMessages([]));

function createRepeater() {
  const options = {
    repeaterFn: () => {
      return ServerHealthMessages.all()
                                 .then((result) => {
                                   result.do(
                                     (successResponse) => serverHealthMessages(successResponse.body),
                                     (errorResponse) => {
                                       // tslint:disable-next-line:no-console
                                       console.log("There was an error fetching server health messages!");
                                     }
                                   );
                                 });
    }
  };
  return new AjaxPoller(options);
}

let repeater: AjaxPoller<void>;

const ServerHealthSummary = {
  oninit() {
    repeater = createRepeater();
    repeater.start();
  },
  onbeforeremove() {
    repeater.stop();
  },
  view() {
    return (<ServerHealthMessagesCountWidget serverHealthMessages={serverHealthMessages}/>);
  }
};

module.exports = ServerHealthSummary;
