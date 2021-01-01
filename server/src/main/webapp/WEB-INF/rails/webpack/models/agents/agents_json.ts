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

interface OriginJSON {
  type: string;
}

interface AgentEnvironmentJSON {
  name: string;
  origin: OriginJSON;
}

interface UrlJSON {
  href: string;
}

interface LinkJSON {
  job: UrlJSON;
  stage: UrlJSON;
  pipeline: UrlJSON;
}

export interface BuildDetailsJSON {
  pipeline_name: string;
  stage_name: string;
  job_name: string;
  _links: LinkJSON;
}

export interface AgentJSON {
  uuid: string;
  hostname: string;
  ip_address: string;
  sandbox: string;
  operating_system: string;
  free_space: string | number;
  agent_config_state: string;
  agent_state: string;
  resources: string[];
  build_state: string;
  build_details?: BuildDetailsJSON;
  environments: AgentEnvironmentJSON[];
  elastic_plugin_id?: string;
  elastic_agent_id?: string;
}

interface EmbeddedJSON {
  agents: AgentJSON[];
}

export interface AgentsJSON {
  _embedded: EmbeddedJSON;
}
