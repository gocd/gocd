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

import Stream from "mithril/stream";
import {Origin, OriginJSON} from "models/origin";

export interface EnvironmentAgentJSON {
  uuid: string;
  hostname: string;
  origin: OriginJSON;
}

export class AgentWithOrigin {
  uuid: Stream<string>;
  hostname: Stream<string>;
  readonly origin: Stream<Origin>;

  constructor(uuid: string, hostname: string, origin: Origin) {
    this.uuid     = Stream(uuid);
    this.hostname = Stream(hostname);
    this.origin   = Stream(origin);
  }

  static fromJSON(data: EnvironmentAgentJSON) {
    return new AgentWithOrigin(data.uuid, data.hostname, Origin.fromJSON(data.origin));
  }

  clone() {
    return new AgentWithOrigin(this.uuid(), this.hostname(), this.origin().clone());
  }
}

export class Agents extends Array<AgentWithOrigin> {
  constructor(...agents: AgentWithOrigin[]) {
    super(...agents);
    Object.setPrototypeOf(this, Object.create(Agents.prototype));
  }

  static fromJSON(agents: EnvironmentAgentJSON[]) {
    if (agents) {
      return new Agents(...agents.map(AgentWithOrigin.fromJSON));
    } else {
      return new Agents();
    }
  }
}
