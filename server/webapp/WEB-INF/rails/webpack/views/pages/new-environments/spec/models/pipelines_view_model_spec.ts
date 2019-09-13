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

import {EnvironmentWithOrigin} from "models/new-environments/environments";
import {PipelineGroups} from "models/new-environments/pipeline_groups";
import test_data from "models/new-environments/spec/test_data";
import {PipelinesViewModel} from "views/pages/new-environments/models/pipelines_view_model";

describe("Pipelines View Model", () => {
  let environment: EnvironmentWithOrigin;
  let pipelinesViewModel: PipelinesViewModel;
  beforeEach(() => {
    environment        = EnvironmentWithOrigin.fromJSON(test_data.xml_environment_json());
    pipelinesViewModel = new PipelinesViewModel(environment);
  });

  it("should update search text", () => {
    expect(pipelinesViewModel.searchText()).toBeUndefined();

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

  it("should update pipeline groups", () => {
    expect(pipelinesViewModel.pipelineGroups()).toBeUndefined();

    const pipelineGroupsJSON = test_data.pipeline_groups_json();
    pipelinesViewModel.updatePipelineGroups(PipelineGroups.fromJSON(pipelineGroupsJSON));

    expect(pipelinesViewModel.pipelineGroups()).not.toBeUndefined();
    expect(pipelinesViewModel.pipelineGroups()!.length).toBe(2);
    expect(pipelinesViewModel.pipelineGroups()![0].name()).toBe(pipelineGroupsJSON.groups[0].name);
  });

  it("should tell whether pipeline group is expanded", () => {
    const pipelineGroupsJSON = test_data.pipeline_groups_json();
    pipelinesViewModel.updatePipelineGroups(PipelineGroups.fromJSON(pipelineGroupsJSON));

    const groupName = pipelineGroupsJSON.groups[0].name;

    expect(pipelinesViewModel.isPipelineGroupExpanded(groupName)).toBe(false);

    pipelinesViewModel.togglePipelineGroupState(groupName);

    expect(pipelinesViewModel.isPipelineGroupExpanded(groupName)).toBe(true);
  });

  it("should toggle pipeline group state", () => {
    const pipelineGroupsJSON = test_data.pipeline_groups_json();
    pipelinesViewModel.updatePipelineGroups(PipelineGroups.fromJSON(pipelineGroupsJSON));

    const groupName = pipelineGroupsJSON.groups[0].name;

    expect(pipelinesViewModel.isPipelineGroupExpanded(groupName)).toBe(false);

    pipelinesViewModel.togglePipelineGroupState(groupName);

    expect(pipelinesViewModel.isPipelineGroupExpanded(groupName)).toBe(true);
  });

  it("should should filter pipelines based on search text", () => {
    const pipelineGroupsJSON = test_data.pipeline_groups_json();
    pipelinesViewModel.updatePipelineGroups(PipelineGroups.fromJSON(pipelineGroupsJSON));

    let filteredPipelineGroups = pipelinesViewModel.filteredPipelineGroups()!;
    expect(filteredPipelineGroups.length).toBe(2);
    expect(filteredPipelineGroups[0].name()).toBe(pipelineGroupsJSON.groups[0].name);
    expect(filteredPipelineGroups[1].name()).toBe(pipelineGroupsJSON.groups[1].name);
    expect(filteredPipelineGroups[0].pipelines().length).toBe(2);
    expect(filteredPipelineGroups[1].pipelines().length).toBe(2);

    const searchText = filteredPipelineGroups[0].pipelines()[0].name();
    pipelinesViewModel.searchText(searchText);

    filteredPipelineGroups = pipelinesViewModel.filteredPipelineGroups()!;
    expect(filteredPipelineGroups.length).toBe(1);
    expect(filteredPipelineGroups[0].name()).toBe(pipelineGroupsJSON.groups[0].name);
    expect(filteredPipelineGroups[0].pipelines().length).toBe(1);
    expect(filteredPipelineGroups[0].pipelines()[0].name()).toBe(searchText);
  });

  it("should should filter pipelines based on partial search text match", () => {
    const pipelineGroupsJSON = test_data.pipeline_groups_json();
    pipelinesViewModel.updatePipelineGroups(PipelineGroups.fromJSON(pipelineGroupsJSON));

    let filteredPipelineGroups = pipelinesViewModel.filteredPipelineGroups()!;
    expect(filteredPipelineGroups.length).toBe(2);
    expect(filteredPipelineGroups[0].name()).toBe(pipelineGroupsJSON.groups[0].name);
    expect(filteredPipelineGroups[1].name()).toBe(pipelineGroupsJSON.groups[1].name);
    expect(filteredPipelineGroups[0].pipelines().length).toBe(2);
    expect(filteredPipelineGroups[1].pipelines().length).toBe(2);

    const searchText = "pipeline-";
    pipelinesViewModel.searchText(searchText);

    filteredPipelineGroups = pipelinesViewModel.filteredPipelineGroups()!;
    expect(filteredPipelineGroups.length).toBe(2);
    expect(filteredPipelineGroups[0].name()).toBe(pipelineGroupsJSON.groups[0].name);
    expect(filteredPipelineGroups[1].name()).toBe(pipelineGroupsJSON.groups[1].name);
    expect(filteredPipelineGroups[0].pipelines().length).toBe(2);
    expect(filteredPipelineGroups[1].pipelines().length).toBe(2);
  });
});
