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

import {AjaxPoller} from "helpers/ajax_poller";
import m from "mithril";
import {Agents} from "models/new-agent/agents";
import {AgentsCRUD} from "models/new-agent/agents_crud";
import {AgentsWidget} from "views/pages/new-agents/agents_widget";
import {Page, PageState} from "views/pages/page";

interface State {
  agents: Agents;
  repeater: AjaxPoller<void>;
}

export class NewAgentPage extends Page<null, State> {
  oninit(vnode: m.Vnode<null, State>) {
    new AjaxPoller({
                     repeaterFn: this.fetchData.bind(this, vnode),
                     intervalSeconds: 10
                   }).start();
  }

  componentToDisplay(vnode: m.Vnode<null, State>): m.Children {
    return <AgentsWidget agents={vnode.state.agents}/>;
  }

  pageName(): string {
    return "Agents";
  }

  fetchData(vnode: m.Vnode<null, State>): Promise<any> {
    return AgentsCRUD.all().then((result) =>
                                   result.do((successResponse) => {
                                     vnode.state.agents = successResponse.body;
                                     this.pageState     = PageState.OK;
                                   }, this.setErrorState));
  }
}
