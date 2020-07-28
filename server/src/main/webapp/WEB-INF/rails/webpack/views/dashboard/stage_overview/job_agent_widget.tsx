/*
 * Copyright 2020 ThoughtWorks, Inc.
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

import m from "mithril";
import Stream from "mithril/stream";
import {MithrilComponent} from "../../../jsx/mithril-component";
import {Agent, Agents} from "../../../models/agents/agents";
import {JobJSON} from "./models/types";
import * as styles from "./index.scss";
import {Link} from "../../components/link";

export interface Attrs {
  job: JobJSON;
  agents: Stream<Agents>;
}

export interface State {
  getAgent: (job: JobJSON, agents: Stream<Agents>) => m.Child;
}

export class JobAgentWidget extends MithrilComponent<Attrs, State> {
  oninit(vnode: m.Vnode<Attrs, State>) {
    vnode.state.getAgent = (job: JobJSON, agents: Stream<Agents>) => {
      if (!job.agent_uuid) {
        return <span class={styles.unknownProperty}>unassigned</span>;
      }

      const agentDetailsPageLink = `/go/agents/${job.agent_uuid}/job_run_history`;
      const agent: Agent | undefined = agents().getAgent(job.agent_uuid);

      return <Link href={agentDetailsPageLink} target={"_blank"}>{agent ? agent.hostname : job.agent_uuid}</Link>
    };
  }

  view(vnode: m.Vnode<Attrs, State>): m.Children | void | null {
    return vnode.state.getAgent(vnode.attrs.job, vnode.attrs.agents);
  }

}
