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
import {EnvironmentVariables} from "models/environment_variables/types";
import {PipelineGroups, Pipelines, PipelineStructureJSON} from "models/internal_pipeline_structure/pipeline_structure";
import {Environments, EnvironmentWithOrigin} from "models/new-environments/environments";
import test_data from "models/new-environments/spec/test_data";
import {PipelinesViewModel} from "views/pages/new-environments/models/pipelines_view_model";

describe("Pipelines View Model", () => {
  let environment: EnvironmentWithOrigin;
  let environments: Environments;
  let pipelinesViewModel: PipelinesViewModel;
  let pipelineGroupsJSON: PipelineStructureJSON;

  beforeEach(() => {
    environments          = new Environments();
    const environmentJSON = test_data.environment_json();
    environment           = EnvironmentWithOrigin.fromJSON(environmentJSON);
    environments.push(environment);

    pipelinesViewModel = new PipelinesViewModel(environment, environments);
    pipelineGroupsJSON = test_data.pipeline_groups_json();
    pipelineGroupsJSON.groups[0].pipelines.push(environmentJSON.pipelines[0]);
    pipelineGroupsJSON.groups[1].pipelines.push(environmentJSON.pipelines[1]);

    pipelinesViewModel.updateModel(Stream(PipelineGroups.fromJSON(pipelineGroupsJSON.groups)));
  });

  it("should update search text", () => {
    expect(pipelinesViewModel.searchText()).toBe('');

    const searchText = "Test";
    pipelinesViewModel.searchText(searchText);

    expect(pipelinesViewModel.searchText()).toBe(searchText);
  });

  it("should update error message", () => {
    expect(pipelinesViewModel.errorMessage()).toBeUndefined();

    const errorMessage = "Boom!";
    pipelinesViewModel.errorMessage(errorMessage);

    expect(pipelinesViewModel.errorMessage()).toBe(errorMessage);
  });

  it("should filter pipelines based on search text", () => {
    let filteredPipelines = pipelinesViewModel.filteredPipelines();

    expect(filteredPipelines.length).toBe(6);
    expect(filteredPipelines[0].name()).toBe(pipelineGroupsJSON.groups[0].pipelines[0].name);
    expect(filteredPipelines[1].name()).toBe(pipelineGroupsJSON.groups[0].pipelines[1].name);
    expect(filteredPipelines[2].name()).toBe(pipelineGroupsJSON.groups[0].pipelines[2].name);
    expect(filteredPipelines[3].name()).toBe(pipelineGroupsJSON.groups[1].pipelines[0].name);
    expect(filteredPipelines[4].name()).toBe(pipelineGroupsJSON.groups[1].pipelines[1].name);
    expect(filteredPipelines[5].name()).toBe(pipelineGroupsJSON.groups[1].pipelines[2].name);

    const searchText = filteredPipelines[0].name();
    pipelinesViewModel.searchText(searchText);

    filteredPipelines = pipelinesViewModel.filteredPipelines();

    expect(filteredPipelines.length).toBe(1);
    expect(filteredPipelines[0].name()).toBe(pipelineGroupsJSON.groups[0].pipelines[0].name);
  });

  it("should filter pipelines based on partial search text match", () => {
    let filteredPipelines = pipelinesViewModel.filteredPipelines();

    expect(filteredPipelines.length).toBe(6);
    expect(filteredPipelines[0].name()).toBe(pipelineGroupsJSON.groups[0].pipelines[0].name);
    expect(filteredPipelines[1].name()).toBe(pipelineGroupsJSON.groups[0].pipelines[1].name);
    expect(filteredPipelines[2].name()).toBe(pipelineGroupsJSON.groups[0].pipelines[2].name);
    expect(filteredPipelines[3].name()).toBe(pipelineGroupsJSON.groups[1].pipelines[0].name);
    expect(filteredPipelines[4].name()).toBe(pipelineGroupsJSON.groups[1].pipelines[1].name);
    expect(filteredPipelines[5].name()).toBe(pipelineGroupsJSON.groups[1].pipelines[2].name);

    const searchText = "pipeline-";
    pipelinesViewModel.searchText(searchText);

    filteredPipelines = pipelinesViewModel.filteredPipelines();

    expect(filteredPipelines.length).toBe(6);
    expect(filteredPipelines[0].name()).toBe(pipelineGroupsJSON.groups[0].pipelines[0].name);
    expect(filteredPipelines[1].name()).toBe(pipelineGroupsJSON.groups[0].pipelines[1].name);
    expect(filteredPipelines[2].name()).toBe(pipelineGroupsJSON.groups[0].pipelines[2].name);
    expect(filteredPipelines[3].name()).toBe(pipelineGroupsJSON.groups[1].pipelines[0].name);
    expect(filteredPipelines[4].name()).toBe(pipelineGroupsJSON.groups[1].pipelines[1].name);
    expect(filteredPipelines[5].name()).toBe(pipelineGroupsJSON.groups[1].pipelines[2].name);
  });

  it("should filter pipelines from environment whose association is defined in config repository", () => {
    const configRepoEnvironmentPipelines = pipelinesViewModel.configRepoEnvironmentPipelines();

    expect(configRepoEnvironmentPipelines.length).toBe(1);
    expect(configRepoEnvironmentPipelines[0].name()).toBe(environment.pipelines()[1].name());
  });

  it("should filter unassociated pipelines defined in config repository", () => {
    const pipelines = pipelinesViewModel.unassociatedPipelinesDefinedInConfigRepository();

    expect(pipelines.length).toBe(2);
    expect(pipelines[0].name()).toBe(pipelineGroupsJSON.groups[0].pipelines[1].name);
    expect(pipelines[1].name()).toBe(pipelineGroupsJSON.groups[1].pipelines[1].name);
  });

  it("should filter pipelines defined in other environment", () => {
    const pipeline = pipelinesViewModel.allPipelines()[0];
    environments.push(new EnvironmentWithOrigin("another",
                                                true,
                                                [],
                                                [],
                                                new Pipelines(pipeline),
                                                new EnvironmentVariables()));

    pipelinesViewModel.updateModel(Stream(PipelineGroups.fromJSON(pipelineGroupsJSON.groups)));
    const pipelines = pipelinesViewModel.pipelinesDefinedInOtherEnvironment();

    expect(pipelines.length).toBe(1);
    expect(pipelines[0].name()).toBe(pipeline.name());
  });

  it('should give all pipelines', () => {
    const pipelines = pipelinesViewModel.allPipelines();
    expect(pipelines.length).toBe(6);
    expect(pipelines[0].name()).toBe(pipelineGroupsJSON.groups[0].pipelines[0].name);
    expect(pipelines[1].name()).toBe(pipelineGroupsJSON.groups[0].pipelines[1].name);
    expect(pipelines[2].name()).toBe(pipelineGroupsJSON.groups[0].pipelines[2].name);
    expect(pipelines[3].name()).toBe(pipelineGroupsJSON.groups[1].pipelines[0].name);
    expect(pipelines[4].name()).toBe(pipelineGroupsJSON.groups[1].pipelines[1].name);
    expect(pipelines[5].name()).toBe(pipelineGroupsJSON.groups[1].pipelines[2].name);
  });

  it('should list available pipelines', () => {
    const pipelines = pipelinesViewModel.availablePipelines();

    expect(pipelines.length).toBe(3);
    expect(pipelines[0].name()).toBe(pipelineGroupsJSON.groups[0].pipelines[0].name);
    expect(pipelines[1].name()).toBe(pipelineGroupsJSON.groups[0].pipelines[2].name);
    expect(pipelines[2].name()).toBe(pipelineGroupsJSON.groups[1].pipelines[0].name);
  });
});
