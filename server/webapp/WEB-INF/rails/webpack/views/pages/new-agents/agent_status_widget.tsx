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

import {bind} from "classnames/bind";
import {MithrilComponent} from "jsx/mithril-component";
import {Agent, BuildDetails} from "models/new-agent/agents";
import Stream from "mithril/stream";
import m from "mithril";
import style from "views/pages/new-agents/index.scss";

const classnames = bind(style);

interface AgentStatusWidgetAttrs {
  agent: Agent;
  buildDetailsForAgent: Stream<string>;
}

export class AgentStatusWidget extends MithrilComponent<AgentStatusWidgetAttrs> {
  view(vnode: m.Vnode<AgentStatusWidgetAttrs>) {
    const agent        = vnode.attrs.agent;
    const buildDetails = agent.buildDetails as BuildDetails;

    if (!agent.isBuilding()) {
      return agent.status();
    }

    return (<div class={classnames(style.tableCell, {[style.building]: agent.isBuilding()})}
                 data-test-id={`agent-status-of-${agent.uuid}`}>
      <a href={"javascript:void(0)"}
         class={style.agentStatus}
         data-test-id={`agent-status-text-${agent.uuid}`}
         onclick={(event) => AgentStatusWidget.toggleBuildDetails(event, vnode, agent.uuid)}>{agent.status()}</a>
      <ul data-test-id={`agent-build-details-of-${agent.uuid}`}
          class={classnames(style.buildDetails, {[style.show]: vnode.attrs.buildDetailsForAgent() === agent.uuid})}>
        <li><a href={buildDetails.pipelineUrl}>Pipeline - {buildDetails.pipelineName}</a></li>
        <li><a href={buildDetails.stageUrl}>Stage - {buildDetails.stageName}</a></li>
        <li><a href={buildDetails.jobUrl}>Job - {buildDetails.jobName}</a></li>
      </ul>
    </div>);
  }

  static toggleBuildDetails(event: Event, vnode: m.Vnode<AgentStatusWidgetAttrs>, agentUUID: string) {
    event.stopImmediatePropagation();
    if (vnode.attrs.buildDetailsForAgent() === agentUUID) {
      vnode.attrs.buildDetailsForAgent("");
    } else {
      vnode.attrs.buildDetailsForAgent(agentUUID);
    }
  }
}
