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
import m from "mithril";
import {Agent, Agents} from "models/new-agent/agents";
import {SearchField} from "views/components/forms/input_fields";
import {Table} from "views/components/table";

interface Attrs {
  agents: Agents;
}

export class AgentsWidget extends MithrilViewComponent<Attrs> {
  view(vnode: m.Vnode<Attrs>) {
    const tableData = vnode.attrs.agents.list().map((agent: Agent) => {
      return [
        <div data-test-id={`agent-hostname-of-${agent.uuid}`}>{agent.hostname}</div>,
        <div data-test-id={`agent-sandbox-of-${agent.uuid}`}>{agent.sandbox}</div>,
        <div data-test-id={`agent-operating-system-of-${agent.uuid}`}>{agent.operatingSystem}</div>,
        <div data-test-id={`agent-ip-address-of-${agent.uuid}`}>{agent.ipAddress}</div>,
        <div data-test-id={`agent-status-of-${agent.uuid}`}>{agent.status()}</div>,
        <div data-test-id={`agent-free-space-of-${agent.uuid}`}>{agent.readableFreeSpace()}</div>,
        <div
          data-test-id={`agent-resources-of-${agent.uuid}`}>{AgentsWidget.joinOrNoneSpecified(agent.resources)}</div>,
        <div
          data-test-id={`agent-environments-of-${agent.uuid}`}>{AgentsWidget.joinOrNoneSpecified(agent.environmentNames())}</div>,
      ];
    });

    return <div>
      <SearchField placeholder="Search"
                   label="Search for agents"
                   property={vnode.attrs.agents.filterText}/>
      <Table data={tableData}
             headers={["Agent Name", "Sandbox", "OS", "IP Address", "Status", "Free Space", "Resources", "Environments"]}
             sortHandler={vnode.attrs.agents}/>
    </div>;
  }

  static joinOrNoneSpecified(array: string[]): m.Children {
    if (array && array.length > 0) {
      return array.join(", ");
    } else {
      return (<em>none specified</em>);
    }
  }
}
