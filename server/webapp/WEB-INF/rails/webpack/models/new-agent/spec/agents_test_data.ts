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

import {AgentJSON, AgentsJSON} from "models/new-agent/agents";

export class AgentsTestData {
  static list() {
    return {
      _embedded: {
        agents: [
          this.agent("37af8492-d64d-4a3f-b1f2-4ea37dd0d201", "Aaaaa", "Zzzzz", "Windows 10", undefined, "10.1.0.5"),
          this.agent("45583959-97e2-4b8e-8eab-2f98d73f2e23", "Bbbbb", "Xxxxx", "Centos 7", 2859, "10.1.0.4"),
          this.agent("40c2e45a-3d1f-47e5-8a3e-94d2e4ba5266", "Ccccc", "Yyyyy", "Mac", undefined, "10.1.1.7")
        ]
      }
    } as AgentsJSON;
  }

  static agent(uuid: string, hostname: string,
               sandbox: string   = "go",
               os: string        = "windows",
               freeSpace: number = 93259825152,
               ipAddr: string    = "10.1.0." + AgentsTestData.getRandomIntegerInRange(1, 255)) {
    // @ts-ignore
    return {
      uuid: uuid,
      hostname: hostname,
      ip_address: ipAddr,
      sandbox: sandbox,
      operating_system: os,
      agent_config_state: "Enabled",
      agent_state: "Idle",
      environments: [{
        name: "gocd",
        origin: {type: "gocd"}
      }, {
        name: "internal",
        origin: {type: "gocd"}
      }],
      build_state: "Idle",
      free_space: freeSpace,
      resources: ["dev", "fat", "ie9", "firefox"]
    } as AgentJSON;
  }

  private static getRandomIntegerInRange(min: number, max: number) {
    return Math.ceil(Math.random() * (max - min) + min);
  }
}