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
import {AjaxPoller} from "helpers/ajax_poller";
import {MithrilComponent} from "jsx/mithril-component";
import m from "mithril";
import Stream from "mithril/stream";
import {ServerHealthMessages} from "models/shared/server_health_messages/server_health_messages";
import {ServerHealthMessagesCountWidget} from "./server_health_messages_count_widget";

interface State {
  serverHealthMessages: Stream<ServerHealthMessages>;
  repeater: AjaxPoller<void>;
}

export class ServerHealthSummary extends MithrilComponent<{}, State> {
  oninit(vnode: m.Vnode<{}, State>): any {
    vnode.state.repeater             = this.createRepeater(vnode);
    vnode.state.serverHealthMessages = Stream(new ServerHealthMessages([]));
    vnode.state.repeater.start();
  }

  onremove(vnode: m.VnodeDOM<{}, State>): any {
    vnode.state.repeater.stop();
  }

  view(vnode: m.Vnode<{}, State>) {
    return (<ServerHealthMessagesCountWidget serverHealthMessages={vnode.state.serverHealthMessages}/>);
  }

  private createRepeater(vnode: m.Vnode<{}, State>) {
    const options = {
      repeaterFn: () => {
        return ServerHealthMessages.all()
                                   .then((result) => {
                                     result.do(
                                       (successResponse) => vnode.state.serverHealthMessages(successResponse.body),
                                       (errorResponse) => {
                                         // tslint:disable-next-line:no-console
                                         console.log(`There was an unknown error fetching server health messages: ${errorResponse.body}`);
                                       }
                                     );
                                   });
      }
    };
    return new AjaxPoller(options);
  }
}
