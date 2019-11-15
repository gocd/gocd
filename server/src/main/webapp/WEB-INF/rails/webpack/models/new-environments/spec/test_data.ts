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

import {PipelineJSON, PipelineStructureJSON} from "models/internal_pipeline_structure/pipeline_structure";
import {EnvironmentJSON} from "models/new-environments/environments";
import {OriginType} from "models/origin";
import {Agent} from "../../new_agent/agents";
import {AgentJSON} from "../../new_agent/agents_json";
import {AgentWithOrigin} from "../environment_agents";

function randomString(): string {
  return Math.random().toString(36).slice(2);
}

function fileOriginJson() {
  return {
    type: OriginType.GoCD,
  };
}

function configRepoOrigin() {
  return {
    type: OriginType.ConfigRepo,
    id: `config-repo-id-${randomString()}`
  };
}

function pipelineAssociationInXmlJson(): PipelineJSON {
  return {
    name: `pipeline-${randomString()}`,
    origin: fileOriginJson(),
    stages: []
  };
}

function pipelineAssociationInConfigRepoJson(): PipelineJSON {
  return {
    name: `pipeline-${randomString()}`,
    origin: configRepoOrigin(),
    stages: []
  };
}

function agentAssociationInXmlJson() {
  return {
    uuid: `agent-uuid-${randomString()}`,
    hostname: `hostname-${randomString()}`,
    origin: fileOriginJson()
  };
}

function agentAssociationInConfigRepoJson() {
  return {
    uuid: `agent-uuid-${randomString()}`,
    hostname: `hostname-${randomString()}`,
    origin: configRepoOrigin()
  };
}

function environmentVariableAssociationInXmlJson(secure: boolean = false) {
  return {
    name: `env-var-name-${randomString()}`,
    value: secure ? undefined : `env-var-value-${randomString()}`,
    encrypted_value: secure ? `env-var-secure-value-${randomString()}` : undefined,
    origin: fileOriginJson(),
    secure
  };
}

function environmentVariableAssociationInConfigRepoJson(secure: boolean = false) {
  return {
    name: `env-var-name-${randomString()}`,
    value: secure ? undefined : `env-var-value-${randomString()}`,
    encrypted_value: secure ? `env-var-secure-value-${randomString()}` : undefined,
    origin: configRepoOrigin(),
    secure
  };
}

function convert_to_agent(envAgent: AgentWithOrigin): Agent {
  return Agent.fromJSON({
                          uuid: `${envAgent.uuid()}`,
                          hostname: `${envAgent.uuid()}-hostname`,
                          ip_address: "",
                          sandbox: "",
                          operating_system: "OS",
                          agent_config_state: "Idle",
                          agent_state: "Idle",
                          environments: [],
                          build_state: "",
                          free_space: 5,
                          resources: []
                        } as AgentJSON);
}

export default {
  file_origin: fileOriginJson,
  config_repo_origin: configRepoOrigin,
  pipeline_association_in_xml_json: pipelineAssociationInXmlJson,
  pipeline_association_in_config_repo_json: pipelineAssociationInConfigRepoJson,
  agent_association_in_xml_json: agentAssociationInXmlJson,
  agent_association_in_config_repo_json: agentAssociationInConfigRepoJson,
  environment_variable_association_in_xml_json: environmentVariableAssociationInXmlJson,
  environment_variable_association_in_config_repo_json: environmentVariableAssociationInConfigRepoJson,
  environment_json: (): EnvironmentJSON => ({
    name: `environment-name-${randomString()}`,
    can_administer: true,
    origins: [fileOriginJson(), configRepoOrigin()],
    pipelines: [pipelineAssociationInXmlJson(), pipelineAssociationInConfigRepoJson()],
    agents: [agentAssociationInXmlJson(), agentAssociationInConfigRepoJson()],
    environment_variables: [
      environmentVariableAssociationInXmlJson(),
      environmentVariableAssociationInConfigRepoJson(),
      environmentVariableAssociationInXmlJson(true),
      environmentVariableAssociationInConfigRepoJson(true)
    ]
  }),
  xml_environment_json: (): EnvironmentJSON => ({
    name: `xml-environment-name-${randomString()}`,
    can_administer: true,
    origins: [fileOriginJson()],
    pipelines: [pipelineAssociationInXmlJson()],
    agents: [agentAssociationInXmlJson()],
    environment_variables: [environmentVariableAssociationInXmlJson(), environmentVariableAssociationInXmlJson(true)]
  }),
  config_repo_environment_json: (): EnvironmentJSON => ({
    name: `config-repo-environment-name-${randomString()}`,
    can_administer: true,
    origins: [fileOriginJson(), configRepoOrigin()],
    pipelines: [pipelineAssociationInConfigRepoJson()],
    agents: [agentAssociationInConfigRepoJson()],
    environment_variables: [
      environmentVariableAssociationInConfigRepoJson(),
      environmentVariableAssociationInConfigRepoJson(true)
    ]
  }),
  pipeline_groups_json: (): PipelineStructureJSON => ({
    groups: [
      {
        name: `pipeline-group-${randomString()}`,
        pipelines: [
          pipelineAssociationInXmlJson(),
          pipelineAssociationInConfigRepoJson()
        ]
      },
      {
        name: `pipeline-group-${randomString()}`,
        pipelines: [
          pipelineAssociationInXmlJson(),
          pipelineAssociationInConfigRepoJson()
        ]
      }
    ],
    templates: [
      {
        name: `template-${randomString()}`,
        parameters: ["foo", "bar"],
        stages: [
          {
            name: "stage1",
            jobs: [
              {name: "job1", is_elastic: false}
            ]
          }
        ]
      }
    ]
  }),
  convert_to_agent
};
