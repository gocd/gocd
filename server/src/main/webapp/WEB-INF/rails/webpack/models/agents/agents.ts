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

import _ from "lodash";
import Stream from "mithril/stream";
import {AgentJSON, AgentsJSON, BuildDetailsJSON} from "models/agents/agents_json";

const filesize = require("filesize");

export enum AgentConfigState {
  Enabled, Disabled, Pending
}

export enum AgentState {
  Idle, Building, LostContact, Missing, Cancelled, Unknown
}

export enum BuildState {
  Idle, Building, Cancelled, Unknown
}

export class AgentsEnvironment {
  public readonly name: string;
  public readonly originType: string;

  constructor(name: string, originType: string) {
    this.name       = name;
    this.originType = originType;
  }

  isAssociatedFromConfigRepo(): boolean {
    return this.originType === "config-repo";
  }

  isUnknown(): boolean {
    return this.originType === "unknown";
  }
}

export class BuildDetails {
  public readonly pipelineName: string;
  public readonly stageName: string;
  public readonly jobName: string;
  public readonly pipelineUrl: string;
  public readonly stageUrl: string;
  public readonly jobUrl: string;

  constructor(pipelineName: string,
              stageName: string,
              jobName: string,
              pipelineUrl: string,
              stageUrl: string,
              jobUrl: string) {
    this.pipelineName = pipelineName;
    this.stageName    = stageName;
    this.jobName      = jobName;
    this.pipelineUrl  = pipelineUrl;
    this.stageUrl     = stageUrl;
    this.jobUrl       = jobUrl;
  }

  static fromJSON(json?: BuildDetailsJSON) {
    return json ? new BuildDetails(json.pipeline_name,
                                   json.stage_name,
                                   json.job_name,
                                   json._links.pipeline.href,
                                   json._links.stage.href,
                                   json._links.job.href
    ) : undefined;
  }
}

export class Agent {
  public readonly uuid: string;
  public readonly hostname: string;
  public readonly ipAddress: string;
  public readonly freeSpace: string | number;
  public readonly sandbox: string;
  public readonly operatingSystem: string;
  public readonly resources: string[];
  public readonly environments: AgentsEnvironment[];
  public readonly agentConfigState: AgentConfigState;
  public readonly agentState: AgentState;
  public readonly buildState: BuildState;
  public readonly buildDetails?: BuildDetails;
  public readonly elasticAgentId?: string;
  public readonly elasticPluginId?: string;
  public readonly selected: Stream<boolean> = Stream();

  constructor(uuid: string,
              hostname: string,
              ipAddress: string,
              freeSpace: string | number,
              sandbox: string,
              operatingSystem: string,
              agentConfigState: AgentConfigState,
              agentState: AgentState,
              buildState: BuildState,
              resources: string[],
              environments: AgentsEnvironment[],
              buildDetails?: BuildDetails,
              elasticAgentId?: string,
              elasticPluginId?: string) {
    this.uuid             = uuid;
    this.hostname         = hostname;
    this.ipAddress        = ipAddress;
    this.freeSpace        = freeSpace;
    this.sandbox          = sandbox;
    this.operatingSystem  = operatingSystem;
    this.agentConfigState = agentConfigState;
    this.agentState       = agentState;
    this.resources        = resources || [];
    this.buildState       = buildState;
    this.environments     = environments;
    this.buildDetails     = buildDetails;
    this.elasticAgentId   = elasticAgentId;
    this.elasticPluginId  = elasticPluginId;
  }

  static fromJSON(data: AgentJSON) {
    return new Agent(data.uuid,
                     data.hostname,
                     data.ip_address,
                     data.free_space,
                     data.sandbox,
                     data.operating_system,
                     (AgentConfigState as any)[data.agent_config_state],
                     (AgentState as any)[data.agent_state],
                     (BuildState as any)[data.build_state],
                     data.resources,
                     data.environments.map((envJson) => new AgentsEnvironment(envJson.name, envJson.origin.type)),
                     BuildDetails.fromJSON(data.build_details),
                     data.elastic_agent_id,
                     data.elastic_plugin_id);
  }

  readableFreeSpace() {
    try {
      if (_.isNumber(this.freeSpace)) {
        return filesize(this.freeSpace);
      } else {
        return "Unknown";
      }
    } catch (e) {
      return "Unknown";
    }
  }

  environmentNames() {
    if (!this.environments || this.environments.length === 0) {
      return [];
    }
    return this.environments.map((env) => env.name);
  }

  isBuilding() {
    return !!this.buildDetails;
  }

  status() {
    if (this.agentConfigState === AgentConfigState.Pending) {
      return "Pending";
    }

    if (this.agentConfigState === AgentConfigState.Disabled) {
      if (this.buildState === BuildState.Building) {
        return "Disabled (Building)";
      } else if (this.buildState === BuildState.Cancelled) {
        return "Disabled (Cancelled)";
      }
      return "Disabled";
    }

    if (this.agentState === AgentState.Building) {
      return this.buildState === BuildState.Cancelled ? "Building (Cancelled)" : "Building";
    }
    return AgentState[this.agentState];
  }

  isElastic() {
    return this.elasticPluginId && this.elasticAgentId;
  }
}

export class Agents extends Array<Agent> {
  constructor(...agents: Agent[]) {
    super(...agents);
    Object.setPrototypeOf(this, Object.create(Agents.prototype));
  }

  static fromJSON(data: AgentsJSON): Agents {
    const agents: Agent[] = data._embedded.agents.map((json: AgentJSON) => {
      return Agent.fromJSON(json);
    });
    return new Agents(...agents);
  }

  hasAgent(uuid: string) {
    return this.findIndex((agent) => agent.uuid === uuid) > -1;
  }

  getAgent(uuid: string): Agent | undefined {
    return this.find((agent) => agent.uuid === uuid);
  }
}
