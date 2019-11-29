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
import Stream from "mithril/stream";
import {EnvironmentWithOrigin} from "models/new-environments/environments";
import {KeyValuePair} from "views/components/key_value_pair";
import styles from "views/pages/new-environments/index.scss";
import {Agents} from "../../../models/agents/agents";

interface EnvironmentHeaderAttrs {
  environment: EnvironmentWithOrigin;
  agents: Stream<Agents>;
}

export class EnvironmentHeader extends MithrilViewComponent<EnvironmentHeaderAttrs> {
  view(vnode: m.Vnode<EnvironmentHeaderAttrs>) {
    const environment = vnode.attrs.environment;
    const agentCount  = environment.agents().filter((agent) => vnode.attrs.agents().hasAgent(agent.uuid())).length;
    return <div class={styles.envHeader} data-test-id={`environment-header-for-${environment.name()}`}>
      <span title={environment.name()}
            class={styles.envName} data-test-id="env-name">{environment.name()}</span>
      <KeyValuePair inline={true} data={new Map([["Pipeline Count", environment.pipelines().length]])}/>
      <KeyValuePair inline={true} data={new Map([["Agent Count", agentCount]])}/>
    </div>;
  }
}
