/*
 * Copyright 2018 ThoughtWorks, Inc.
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

describe("Dashboard", () => {
  describe('Pipeline Group Model', () => {

    const PipelineGroups = require('models/dashboard/pipeline_groups');

    it("should deserialize from json", () => {
      const pipelineGroups = PipelineGroups.fromJSON(pipelineGroupsData);

      expect(pipelineGroups.groups.length).toBe(1);
      expect(pipelineGroups.groups[0].name).toBe(pipelineGroupsData[0].name);
      expect(pipelineGroups.groups[0].canAdminister).toBe(pipelineGroupsData[0].can_administer);
      expect(pipelineGroups.groups[0].path).toBe('/go/admin/pipelines#group-first');
      expect(pipelineGroups.groups[0].pipelines).toEqual(pipelineGroupsData[0].pipelines);
    });

    const pipelineGroupsData = [
      {
        "_links":         {
          "self": {
            "href": "http://localhost:8153/go/api/config/pipeline_groups/first"
          },
          "doc":  {
            "href": "https://api.go.cd/current/#pipeline-groups"
          }
        },
        "name":           "first",
        "pipelines":      ["up42"],
        "can_administer": true
      }
    ];
  });
});
