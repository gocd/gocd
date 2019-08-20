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
import {ApiResult, ErrorResponse} from "helpers/api_request_builder";
import m from "mithril";
import {Agents} from "models/new-agent/agents";
import {AgentsCRUD} from "models/new-agent/agents_crud";
import {MessageType} from "views/components/flash_message";
import {AgentsWidget} from "views/pages/new-agents/agents_widget";
import {Page, PageState} from "views/pages/page";

interface State {
  agents: Agents;
  repeater: AjaxPoller<void>;
  onEnable: (e: MouseEvent) => void;
  onDisable: (e: MouseEvent) => void;
  onDelete: (e: MouseEvent) => void;
}

export class NewAgentPage extends Page<null, State> {
  oninit(vnode: m.Vnode<null, State>) {
    const self         = this;
    vnode.state.agents = new Agents([]);


    vnode.state.onEnable = function () {
      const uuids = this.agents.getSelectedAgentsUUID();
      AgentsCRUD.agentsToEnable(uuids)
                .then((result) => self.onResult(result, "Enabled", uuids.length))
                .then(this.agents.unselectAll.bind(this.agents))
                .finally(self.fetchData.bind(self, vnode));
    };

    vnode.state.onDisable = function () {
      const uuids = this.agents.getSelectedAgentsUUID();
      AgentsCRUD.agentsToDisable(uuids)
                .then((result) => self.onResult(result, "Disabled", uuids.length))
                .then(this.agents.unselectAll.bind(this.agents))
                .finally(self.fetchData.bind(self, vnode));
    };

    vnode.state.onDelete = function () {
      const uuids = this.agents.getSelectedAgentsUUID();
      AgentsCRUD.delete(uuids)
                .then((result) => self.onResult(result, "Deleted", uuids.length))
                .finally(self.fetchData.bind(self, vnode));
    };

    new AjaxPoller({
                     repeaterFn: this.fetchData.bind(this, vnode),
                     intervalSeconds: 10
                   }).start();
  }

  componentToDisplay(vnode: m.Vnode<null, State>): m.Children {
    return <AgentsWidget agents={vnode.state.agents}
                         onEnable={vnode.state.onEnable.bind(vnode.state)}
                         onDisable={vnode.state.onDisable.bind(vnode.state)}
                         onDelete={vnode.state.onDelete.bind(vnode.state)}
                         flashMessage={this.flashMessage}/>;
  }

  pageName(): string {
    return "Agents";
  }

  fetchData(vnode: m.Vnode<null, State>): Promise<any> {
    return AgentsCRUD.all().then((result) =>
                                   result.do((successResponse) => {
                                     vnode.state.agents.initializeWith(successResponse.body);
                                     this.pageState = PageState.OK;
                                   }, this.setErrorState));
  }

  private onResult(result: ApiResult<string>, action: string, count: number) {
    result.do(this.onSuccess.bind(this, action, count), this.onFailure.bind(this));
  }

  private onSuccess(action: string, count: number) {
    this.flashMessage.setMessage(MessageType.success, `${action} ${count} ${NewAgentPage.pluralizeAgent(count)}`);
  }

  private onFailure(errorResponse: ErrorResponse) {
    this.flashMessage.setMessage(MessageType.alert, errorResponse.message);
  }

  private static pluralizeAgent(count: number) {
    return count > 1 ? "agents" : "agent";
  }
}
