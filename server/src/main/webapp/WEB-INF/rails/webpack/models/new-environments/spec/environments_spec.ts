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

import {EnvironmentVariableWithOrigin} from "models/environment_variables/types";
import {PipelineWithOrigin} from "models/internal_pipeline_structure/pipeline_structure";
import {AgentWithOrigin} from "models/new-environments/environment_agents";
import {Environments, EnvironmentWithOrigin} from "models/new-environments/environments";
import data from "models/new-environments/spec/test_data";
import {Origin, OriginType} from "models/origin";

const envJSON          = data.environment_json();
const environmentsJSON = {
  _embedded: {
    environments: [envJSON]
  }
};

describe("Environments Model - Environments", () => {
  it("should deserialize from json", () => {
    const environments = Environments.fromJSON(environmentsJSON);
    expect(environments.length).toEqual(1);

    expect(environments[0].name()).toEqual(envJSON.name);
    expect(environments[0].origins().length).toEqual(2);
    expect(environments[0].origins()[0].type()).toEqual(envJSON.origins[0].type);
    expect(environments[0].origins()[1].type()).toEqual(envJSON.origins[1].type);
    expect(environments[0].agents().length).toEqual(2);
    expect(environments[0].pipelines().length).toEqual(2);
    expect(environments[0].environmentVariables().length).toEqual(4);
  });

  it("should tell whether environment contains a pipeline", () => {
    const environment = EnvironmentWithOrigin.fromJSON(envJSON);

    expect(environment.containsPipeline(envJSON.pipelines[0].name)).toBe(true);
    expect(environment.containsPipeline(envJSON.pipelines[1].name)).toBe(true);

    expect(environment.containsPipeline("non-existing-pipeline")).toBe(false);
  });

  it("should tell whether environment contains an agent", () => {
    const environment = EnvironmentWithOrigin.fromJSON(envJSON);

    expect(environment.containsAgent(envJSON.agents[0].uuid)).toBe(true);
    expect(environment.containsAgent(envJSON.agents[1].uuid)).toBe(true);

    expect(environment.containsAgent("non-existing-agent")).toBe(false);
  });

  it("should add a pipeline to an environment", () => {
    const environment = EnvironmentWithOrigin.fromJSON(envJSON);

    const pipelineToAdd = data.pipeline_association_in_xml_json();

    expect(environment.containsPipeline(pipelineToAdd.name)).toBe(false);
    environment.addPipelineIfNotPresent(PipelineWithOrigin.fromJSON(pipelineToAdd));
    expect(environment.containsPipeline(pipelineToAdd.name)).toBe(true);
  });

  it("should add an agent to an environment", () => {
    const environment = EnvironmentWithOrigin.fromJSON(envJSON);

    const agentToAdd = data.agent_association_in_xml_json();

    expect(environment.containsAgent(agentToAdd.uuid)).toBe(false);
    environment.addAgentIfNotPresent(AgentWithOrigin.fromJSON(agentToAdd));
    expect(environment.containsAgent(agentToAdd.uuid)).toBe(true);
  });

  it("should not add a pipeline to an environment when one already exists", () => {
    const environment = EnvironmentWithOrigin.fromJSON(envJSON);

    const pipelineToAdd = data.pipeline_association_in_xml_json();

    expect(environment.pipelines().length).toBe(2);
    expect(environment.containsPipeline(pipelineToAdd.name)).toBe(false);

    environment.addPipelineIfNotPresent(PipelineWithOrigin.fromJSON(pipelineToAdd));

    expect(environment.pipelines().length).toBe(3);
    expect(environment.containsPipeline(pipelineToAdd.name)).toBe(true);

    environment.addPipelineIfNotPresent(PipelineWithOrigin.fromJSON(pipelineToAdd));

    expect(environment.pipelines().length).toBe(3);
    expect(environment.containsPipeline(pipelineToAdd.name)).toBe(true);
  });

  it("should not add an agent to an environment when one already exists", () => {
    const environment = EnvironmentWithOrigin.fromJSON(envJSON);

    const agentToAdd = data.agent_association_in_xml_json();

    expect(environment.agents().length).toBe(2);
    expect(environment.containsAgent(agentToAdd.uuid)).toBe(false);

    environment.addAgentIfNotPresent(AgentWithOrigin.fromJSON(agentToAdd));

    expect(environment.agents().length).toBe(3);
    expect(environment.containsAgent(agentToAdd.uuid)).toBe(true);

    environment.addAgentIfNotPresent(AgentWithOrigin.fromJSON(agentToAdd));

    expect(environment.agents().length).toBe(3);
    expect(environment.containsAgent(agentToAdd.uuid)).toBe(true);
  });

  it("should remove a pipeline from an environment", () => {
    const environment  = EnvironmentWithOrigin.fromJSON(envJSON);
    const pipelineJson = data.pipeline_association_in_xml_json();
    const pipeline     = PipelineWithOrigin.fromJSON(pipelineJson);
    environment.addPipelineIfNotPresent(pipeline);

    expect(environment.pipelines().length).toBe(3);
    expect(environment.containsPipeline(pipelineJson.name)).toBe(true);

    environment.removePipelineIfPresent(pipeline);

    expect(environment.pipelines().length).toBe(2);
    expect(environment.containsPipeline(pipelineJson.name)).toBe(false);
  });

  it("should remove an agent from an environment", () => {
    const environment = EnvironmentWithOrigin.fromJSON(envJSON);

    const agentJson = data.agent_association_in_xml_json();
    const agent     = AgentWithOrigin.fromJSON(agentJson);
    environment.addAgentIfNotPresent(agent);

    expect(environment.agents().length).toBe(3);
    expect(environment.containsAgent(agentJson.uuid)).toBe(true);

    environment.removeAgentIfPresent(agent);

    expect(environment.agents().length).toBe(2);
    expect(environment.containsAgent(agentJson.uuid)).toBe(false);
  });

  it("should not fail to remove a non-existent pipeline from an environment", () => {
    const environment  = EnvironmentWithOrigin.fromJSON(envJSON);
    const pipelineJson = data.pipeline_association_in_xml_json();
    const pipeline     = PipelineWithOrigin.fromJSON(pipelineJson);

    expect(environment.pipelines().length).toBe(2);
    expect(environment.containsPipeline(pipelineJson.name)).toBe(false);

    environment.removePipelineIfPresent(pipeline);

    expect(environment.pipelines().length).toBe(2);
    expect(environment.containsPipeline(pipelineJson.name)).toBe(false);
  });

  it("should not fail to remove a non-existent agent from an environment", () => {
    const environment = EnvironmentWithOrigin.fromJSON(envJSON);

    const agentJson = data.agent_association_in_xml_json();
    const agent     = AgentWithOrigin.fromJSON(agentJson);

    expect(environment.agents().length).toBe(2);
    expect(environment.containsAgent(agentJson.uuid)).toBe(false);

    environment.removeAgentIfPresent(agent);

    expect(environment.agents().length).toBe(2);
    expect(environment.containsAgent(agentJson.uuid)).toBe(false);
  });

  it("should answer the environment to which the specified pipeline is associated", () => {
    const environments         = Environments.fromJSON(environmentsJSON);
    const existingPipelineName = environments[0].pipelines()[0].name();

    expect(environments.findEnvironmentForPipeline(existingPipelineName)!.name()).toEqual(environments[0].name());
    expect(environments.findEnvironmentForPipeline("my-fancy-pipeline")).toEqual(undefined);
  });

  it("should answer whether the pipeline is defined in another environment apart from specified environment", () => {
    const env2JSON     = data.environment_json();
    const multiEnvJSON = {
      _embedded: {
        environments: [envJSON, env2JSON]
      }
    };

    const environments = Environments.fromJSON(multiEnvJSON);

    const env1Name     = environments[0].name();
    const env2Name     = environments[1].name();
    const pipelineName = environments[0].pipelines()[0].name();

    expect(environments.isPipelineDefinedInAnotherEnvironmentApartFrom(env1Name, pipelineName)).toEqual(false);
    expect(environments.isPipelineDefinedInAnotherEnvironmentApartFrom(env2Name, pipelineName)).toEqual(true);
  });

  it("should set origins as GoCD origin if not exist", () => {
    const env = data.environment_json();
    delete env.origins;
    const envWithOrigin = EnvironmentWithOrigin.fromJSON(env);

    expect(envWithOrigin.origins().length).toEqual(1);
    expect(envWithOrigin.origins()[0].type()).toEqual(OriginType.GoCD);
  });
});

describe("Environment Model - Environment", () => {
  it("toJSON()", () => {
    const env          = EnvironmentWithOrigin.fromJSON(envJSON);
    const expectedJSON = {
      name: env.name(),
      environment_variables: env.environmentVariables().toJSON()
    };

    expect(env.toJSON()).toEqual(expectedJSON);
  });

  it('should not give name error for environment variable when environment variable name is blank', () => {
    const env = EnvironmentWithOrigin.fromJSON(envJSON);
    env.environmentVariables().push(new EnvironmentVariableWithOrigin("", new Origin(OriginType.GoCD)));
    expect(env.isValid()).toBe(false);

    expect(env.environmentVariables()[4].errors().errors("name")).toEqual(["Name must be present"]);
  });

  it('should give error for environment variables with same name', () => {
    const env = EnvironmentWithOrigin.fromJSON(envJSON);
    env.environmentVariables().push(new EnvironmentVariableWithOrigin("foo", new Origin(OriginType.GoCD)));
    env.environmentVariables().push(new EnvironmentVariableWithOrigin("foo", new Origin(OriginType.GoCD)));
    expect(env.isValid()).toBe(false);
    expect(env.environmentVariables()[4].errors().errors("name")).toEqual(["Name is a duplicate"]);
    expect(env.environmentVariables()[5].errors().errors("name")).toEqual(["Name is a duplicate"]);
  });

  it('should answer whether the environment is locally defined', () => {
    const env = EnvironmentWithOrigin.fromJSON(envJSON);

    expect(env.isLocal()).toBe(false);

    env.origins().splice(1, 1);

    expect(env.isLocal()).toBe(true);
  });
});
