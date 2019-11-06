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

import Stream from "mithril/stream";
import {Agent, AgentJSON} from "models/environments/types";
import {Origin, OriginJSON} from "models/origin";

export interface EnvironmentAgentJSON extends AgentJSON {
  origin: OriginJSON;
}

export class AgentWithOrigin extends Agent {
  readonly origin: Stream<Origin>;

  constructor(uuid: string, origin: Origin) {
    super(uuid);
    this.origin = Stream(origin);
  }

  static fromJSON(data: EnvironmentAgentJSON) {
    return new AgentWithOrigin(data.uuid, Origin.fromJSON(data.origin));
  }

  clone() {
    return new AgentWithOrigin(this.uuid(), this.origin().clone());
  }
}

export class Agents extends Array<AgentWithOrigin> {
  constructor(...agents: AgentWithOrigin[]) {
    super(...agents);
    Object.setPrototypeOf(this, Object.create(Agents.prototype));
  }

  static fromJSON(agents: EnvironmentAgentJSON[]) {
    return new Agents(...agents.map(AgentWithOrigin.fromJSON));
  }
}
