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

import {AgentJSON, AgentsJSON} from "models/agents/agents_json";
import uuid from "uuid/v4";

export class AgentsTestData {
  static list() {
    return {
      _embedded: {
        agents: [
          new AgentJSONBuilder().withHostname("Hostname-A").build(),
          new AgentJSONBuilder().withHostname("Hostname-B").build(),
          new AgentJSONBuilder().withHostname("Hostname-C").build()
        ]
      }
    } as AgentsJSON;
  }

  static withHostname(hostname: string) {
    return new AgentJSONBuilder().withHostname(hostname).build();
  }

  static withOs(os: string) {
    return new AgentJSONBuilder().withOs(os).build();
  }

  static withSandbox(sandbox: string) {
    return new AgentJSONBuilder().withSandbox(sandbox).build();
  }

  static withIP(ip: string) {
    return new AgentJSONBuilder().withIP(ip).build();
  }

  static withFreespace(freespace: number | string) {
    return new AgentJSONBuilder().withFreespace(freespace).build();
  }

  static pendingAgent() {
    return new AgentJSONBuilder().withConfigState("Pending").withAgentState("Unknown").build();
  }

  static disabledAgent() {
    return new AgentJSONBuilder()
      .withConfigState("Disabled")
      .withAgentState("Idle")
      .withBuildState("Idle")
      .build();
  }

  static disabledBuildingAgent() {
    return new AgentJSONBuilder()
      .withConfigState("Disabled")
      .withAgentState("Building")
      .withBuildState("Building")
      .build();
  }

  static disabledCancelledAgent() {
    return new AgentJSONBuilder()
      .withConfigState("Disabled")
      .withAgentState("Building")
      .withBuildState("Cancelled")
      .build();
  }

  static buildingElasticAgent() {
    const agentJSON             = new AgentJSONBuilder()
      .withConfigState("Enabled")
      .withAgentState("Building")
      .withBuildState("Building")
      .build();
    agentJSON.elastic_plugin_id = "cd.go.elastic-agent.docker";
    agentJSON.elastic_agent_id  = `ea-${agentJSON.uuid}`;
    return agentJSON;
  }

  static buildingAgent() {
    return new AgentJSONBuilder()
      .withConfigState("Enabled")
      .withAgentState("Building")
      .withBuildState("Building")
      .build();
  }

  static buildingCancelledAgent() {
    return new AgentJSONBuilder()
      .withConfigState("Enabled")
      .withAgentState("Building")
      .withBuildState("Cancelled")
      .build();
  }

  static idleElasticAgent() {
    const agentJSON             = new AgentJSONBuilder()
      .withConfigState("Enabled")
      .withAgentState("Idle")
      .withBuildState("Idle")
      .build();
    agentJSON.elastic_plugin_id = "cd.go.elastic-agent.docker";
    agentJSON.elastic_agent_id  = `ea-${agentJSON.uuid}`;
    return agentJSON;
  }

  static idleAgent() {
    return new AgentJSONBuilder()
      .withConfigState("Enabled")
      .withAgentState("Idle")
      .withBuildState("Idle")
      .build();
  }

  static missingAgent() {
    return new AgentJSONBuilder()
      .withConfigState("Enabled")
      .withAgentState("Missing")
      .build();
  }

  static lostContactAgent() {
    return new AgentJSONBuilder()
      .withConfigState("Enabled")
      .withAgentState("LostContact")
      .build();
  }

  static elasticAgent() {
    const agentJSON             = new AgentJSONBuilder().build();
    agentJSON.elastic_plugin_id = "cd.go.elastic-agent.docker";
    agentJSON.elastic_agent_id  = `ea-${uuid()}`;
    return agentJSON;
  }

  static elasticAgentWithEnvironments(...environments: string[]) {
    const agentJSON             = new AgentJSONBuilder().withEnvironments(...environments).build();
    agentJSON.elastic_plugin_id = "cd.go.elastic-agent.docker";
    agentJSON.elastic_agent_id  = `ea-${agentJSON.uuid}`;
    return agentJSON;
  }

  static withResources(...resources: string[]) {
    return new AgentJSONBuilder().withResources(...resources).build();
  }

  static withEnvironments(...environments: string[]) {
    return new AgentJSONBuilder().withEnvironments(...environments).build();
  }
}

class AgentJSONBuilder {
  private agentJson = {
    uuid: `${uuid()}`,
    hostname: AgentJSONBuilder.randomHostname(),
    ip_address: AgentJSONBuilder.randomIP(),
    sandbox: AgentJSONBuilder.randomSandbox(),
    operating_system: AgentJSONBuilder.randomOS(),
    agent_config_state: AgentJSONBuilder.randomAgentConfigState(),
    agent_state: AgentJSONBuilder.randomAgentState(),
    environments: [{
      name: "gocd",
      origin: {type: "gocd"}
    }, {
      name: "internal",
      origin: {type: "gocd"}
    }],
    build_state: AgentJSONBuilder.randomBuildState(),
    free_space: AgentJSONBuilder.getRandomIntegerInRange(0, Number.MAX_SAFE_INTEGER),
    resources: AgentJSONBuilder.randomResources()
  } as AgentJSON;

  build() {
    return this.agentJson;
  }

  withHostname(hostname: string) {
    this.agentJson.hostname = hostname;
    return this;
  }

  withOs(os: string) {
    this.agentJson.operating_system = os;
    return this;
  }

  withSandbox(sandbox: string) {
    this.agentJson.sandbox = sandbox;
    return this;
  }

  withIP(ip: string) {
    this.agentJson.ip_address = ip;
    return this;
  }

  withFreespace(freespace: number | string) {
    this.agentJson.free_space = freespace;
    return this;
  }

  withConfigState(agentConfigState: string) {
    this.agentJson.agent_config_state = agentConfigState;
    return this;
  }

  withAgentState(agentState: string) {
    this.agentJson.agent_state = agentState;
    return this;
  }

  withBuildState(buildState: string) {
    this.agentJson.build_state = buildState;
    if (buildState.toLowerCase() === "building") {
      this.addBuildDetails();
    }
    return this;
  }

  withResources(...resources: string[]) {
    this.agentJson.resources = resources;
    return this;
  }

  withEnvironments(...environments: string[]) {
    this.agentJson.environments = environments.map((env) => {
      return {
        name: env,
        origin: {type: "gocd"}
      };
    });
    return this;
  }

  private static randomOS() {
    const osList = ["MacOS", "Widows 10", "Windows 7", "RedHat", "CentOS"];
    return osList[this.getRandomIntegerInRange(0, osList.length - 1)];
  }

  private static randomHostname() {
    const list = ["fountain", "franklin", "hollywood", "jefferson", "melrose", "olympic", "pico", "sunset"];
    return list[this.getRandomIntegerInRange(0, list.length - 1)];
  }

  private static randomSandbox() {
    const list = ["/var/lib/agent", "c://go/agent", "/Users/bob/Applications/go/agent"];
    return list[this.getRandomIntegerInRange(0, list.length - 1)];
  }

  private static getRandomIntegerInRange(min: number, max: number) {
    return Math.ceil(Math.random() * (max - min) + min);
  }

  private static randomIP() {
    return `10.1.${this.getRandomIntegerInRange(0, 255)}.${this.getRandomIntegerInRange(1, 250)}`;
  }

  private static randomAgentConfigState() {
    return ["Disabled", "Enabled", "Pending"][this.getRandomIntegerInRange(0, 2)];
  }

  private static randomAgentState() {
    return ["Idle", "Building", "LostContact", "Missing", "Cancelled", "Unknown"][this.getRandomIntegerInRange(0, 5)];
  }

  private static randomBuildState() {
    return ["Idle", "Building", "Cancelled", "Unknown"][this.getRandomIntegerInRange(0, 3)];
  }

  private static randomResources() {
    return ["Chrome", "Firefox", "Safari", "PSQL", "Java", "Node", "Ruby"]
      .splice(this.getRandomIntegerInRange(0, 6), 3);
  }

  private addBuildDetails() {
    this.agentJson.build_details = {
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
}
