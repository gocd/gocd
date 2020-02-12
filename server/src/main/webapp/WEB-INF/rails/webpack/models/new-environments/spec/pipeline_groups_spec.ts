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

import {PipelineGroups} from "models/internal_pipeline_structure/pipeline_structure";
import data from "models/new-environments/spec/test_data";

const pipelineGroupsJSON = data.pipeline_groups_json();

describe("Environments Model - Pipeline Groups", () => {
  it("should deserialize from json", () => {
    const pipelineGroups = PipelineGroups.fromJSON(pipelineGroupsJSON.groups);

    expect(pipelineGroups.length).toEqual(2);
    expect(pipelineGroups[0].name()).toEqual(pipelineGroupsJSON.groups[0].name);
    expect(pipelineGroups[0].pipelines().length).toEqual(2);
    expect(pipelineGroups[0].pipelines()[0].name()).toEqual(pipelineGroupsJSON.groups[0].pipelines[0].name);
    expect(pipelineGroups[0].pipelines()[1].name()).toEqual(pipelineGroupsJSON.groups[0].pipelines[1].name);

    expect(pipelineGroups[1].name()).toEqual(pipelineGroupsJSON.groups[1].name);
    expect(pipelineGroups[1].pipelines().length).toEqual(2);
    expect(pipelineGroups[1].pipelines()[0].name()).toEqual(pipelineGroupsJSON.groups[1].pipelines[0].name);
    expect(pipelineGroups[1].pipelines()[1].name()).toEqual(pipelineGroupsJSON.groups[1].pipelines[1].name);
  });
});
