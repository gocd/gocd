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
import {AgentJSON, AgentsJSON} from "models/new_agent/agents";
import uuid from "uuid/v4";

export class AgentsTestData {
  static list() {
    return {
      _embedded: {
        agents: [
          this.agent("37af8492-d64d-4a3f-b1f2-4ea37dd0d201",
                     "Aaaaa",
                     "Zzzzz",
                     "Windows 10",
                     "Building",
                     undefined,
                     "10.1.0.5"),
          this.agent("45583959-97e2-4b8e-8eab-2f98d73f2e23",
                     "Bbbbb",
                     "Xxxxx",
                     "Centos 7",
                     "LostContact",
                     2859,
                     "10.1.0.4"),
          this.agent("40c2e45a-3d1f-47e5-8a3e-94d2e4ba5266",
                     "Ccccc",
                     "Yyyyy",
                     "Mac",
                     "Idle",
                     9854,
                     "10.1.1.7")
        ]
      }
    } as AgentsJSON;
  }

  static agentWithResources(resources: string[]) {
    return this.agent("37af8492-d64d-4a3f-b1f2-4ea37dd0d201",
                      "Aaaaa",
                      "Zzzzz",
                      "Windows 10",
                      "Building",
                      undefined,
                      "10.1.0.5",
                      resources);
  }

  static agentWithEnvironments(...envs: string[]) {
    const agent        = this.agent("37af8492-d64d-4a3f-b1f2-4ea37dd0d201", "Aaaaa");
    agent.environments = envs.map((env) => {
      return {
        name: env,
        origin: {type: "gocd"}
      };
    });
    return agent;
  }

  static agentWithFreespace(freespace: number | string) {
    return this.agent(uuid(),
                      "Ccccc",
                      "Yyyyy",
                      "Mac",
                      "Idle",
                      freespace);
  }

  static pendingAgent() {
    return this.agentWithState("Pending", "Unknown");
  }

  static disabledAgent() {
    return this.agentWithState("Disabled", "Idle", "Idle");
  }

  static disabledBuildingAgent() {
    return this.agentWithState("Disabled", "Building", "Building");
  }

  static disabledCancelledAgent() {
    return this.agentWithState("Disabled", "Building", "Cancelled");
  }

  static buildingAgent() {
    return this.agentWithState("Enabled", "Building", "Building");
  }

  static buildingCancelledAgent() {
    return this.agentWithState("Enabled", "Building", "Cancelled");
  }

  static idleAgent() {
    return this.agentWithState("Enabled", "Idle", "Idle");
  }

  static missingAgent() {
    return this.agentWithState("Enabled", "Missing");
  }

  static lostContactAgent() {
    return this.agentWithState("Enabled", "LostContact");
  }

  static agent(uuid: string, hostname: string,
               sandbox: string            = "go",
               os: string                 = "windows",
               agentState: string         = "Idle",
               freeSpace: number | string = 93259825152,
               ipAddr: string             = "10.1.0." + AgentsTestData.getRandomIntegerInRange(1, 255),
               resources                  = ["dev", "fat", "ie9", "firefox"]) {
    return {
      selected: Stream(false),
      uuid,
      hostname,
      ip_address: ipAddr,
      sandbox,
      operating_system: os,
      agent_config_state: "Enabled",
      agent_state: agentState,
      environments: [{
        name: "gocd",
        origin: {type: "gocd"}
      }, {
        name: "internal",
        origin: {type: "gocd"}
      }],
      build_state: "Idle",
      free_space: freeSpace,
      resources
    } as AgentJSON;

  }

  static elasticAgent(hostname: string) {
    return {
      uuid: uuid(),
      hostname,
      ip_address: "10.1.0." + AgentsTestData.getRandomIntegerInRange(1, 255),
      sandbox: "go",
      operating_system: "windows",
      agent_config_state: "Enabled",
      agent_state: "Idle",
      environments: [{
        name: "gocd",
        origin: {type: "gocd"}
      }, {
        name: "internal",
        origin: {type: "gocd"}
      }],
      resources: [],
      build_state: "Idle",
      free_space: 93259825152,
      elastic_plugin_id: "cd.go.elastic-agents.docker",
      elastic_agent_id: `ea-${uuid()}`
    } as AgentJSON;
  }

  private static getRandomIntegerInRange(min: number, max: number) {
    return Math.ceil(Math.random() * (max - min) + min);
  }

  private static agentWithState(configState: string, agentState: string = "", buildState: string = "") {
    const agent              = this.agent(uuid(), "Aaaaa");
    agent.agent_config_state = configState;
    agent.build_state        = buildState;
    agent.agent_state        = agentState;

    if (agentState === "Building") {
      agent.build_details = {
        pipeline_name: "up42",
        stage_name: "up42_stage",
        job_name: "up42_job",
        _links: {
          job: {href: "job_url"},
          stage: {href: "stage_url"},
          pipeline: {href: "pipeline_url"}
        }
      };
    }
    return agent;
  }
}
