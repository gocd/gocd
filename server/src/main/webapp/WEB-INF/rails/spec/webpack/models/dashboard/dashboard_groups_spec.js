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
import {DashboardGroups} from "models/dashboard/dashboard_groups";

describe("DashboardGroups", () => {


  it("should deserialize pipeline groups from json", () => {
    const pipelineGroups = DashboardGroups.fromPipelineGroupsJSON(pipelineGroupsData);

    expect(pipelineGroups.groups.length).toBe(1);
    expect(pipelineGroups.groups[0].name).toBe(pipelineGroupsData[0].name);
    expect(pipelineGroups.groups[0].canAdminister).toBe(pipelineGroupsData[0].can_administer);
    expect(pipelineGroups.groups[0].pipelines).toEqual(pipelineGroupsData[0].pipelines);
  });

  it("should deserialize environment groups from json", () => {
    const environments = DashboardGroups.fromEnvironmentsJSON(environmentGroupsData);

    expect(environments.groups.length).toBe(1);
    expect(environments.groups[0].name).toBe(pipelineGroupsData[0].name);
    expect(environments.groups[0].canAdminister).toBe(pipelineGroupsData[0].can_administer);
    expect(environments.groups[0].pipelines).toEqual(pipelineGroupsData[0].pipelines);
  });

  describe('tooltip, title and aria-label', () => {

    it('should set values for a pipeline group when a user can administer it', () => {
      const pipelineGroups = DashboardGroups.fromPipelineGroupsJSON(pipelineGroupsData);
      expect(pipelineGroups.groups[0].label()).toBe("Pipeline Group 'first'");
      expect(pipelineGroups.groups[0].tooltipForEdit()).toBe("");
      expect(pipelineGroups.groups[0].titleForEdit()).toBe("Edit Pipeline Group 'first'");
      expect(pipelineGroups.groups[0].ariaLabelForEdit()).toBe("Edit Pipeline Group 'first'");
      expect(pipelineGroups.groups[0].tooltipForNewPipeline()).toBe("");
      expect(pipelineGroups.groups[0].titleForNewPipeline()).toBe("Create a new pipeline within this group");
      expect(pipelineGroups.groups[0].ariaLabelForNewPipeline()).toBe("Create a new pipeline within this group");
    });

    it('should set values for an environment when a user can administer it', () => {
      const environments = DashboardGroups.fromEnvironmentsJSON(environmentGroupsData);
      expect(environments.groups[0].label()).toBe("Environment 'first'");
      expect(environments.groups[0].tooltipForEdit()).toBe("");
      expect(environments.groups[0].titleForEdit()).toBe("Edit Environment 'first'");
      expect(environments.groups[0].ariaLabelForEdit()).toBe("Edit Environment 'first'");
      expect(environments.groups[0].tooltipForNewPipeline()).toBe("");
      expect(environments.groups[0].titleForNewPipeline()).toBe("");
      expect(environments.groups[0].ariaLabelForNewPipeline()).toBe("");
    });

    it('should set values for a pipeline group when a user cannot administer it', () => {
      const pipelineGroups = DashboardGroups.fromPipelineGroupsJSON([{
        "name":           "first",
        "pipelines":      ["up42"],
        "can_administer": false
      }]);
      expect(pipelineGroups.groups[0].label()).toBe("Pipeline Group 'first'");
      expect(pipelineGroups.groups[0].tooltipForEdit()).toBe("You don't have permission to edit this pipeline group");
      expect(pipelineGroups.groups[0].titleForEdit()).toBe("");
      expect(pipelineGroups.groups[0].ariaLabelForEdit()).toBe("You don't have permission to edit this pipeline group");
      expect(pipelineGroups.groups[0].tooltipForNewPipeline()).toBe("You don't have permission to create new pipeline within this pipeline group");
      expect(pipelineGroups.groups[0].titleForNewPipeline()).toBe("");
      expect(pipelineGroups.groups[0].ariaLabelForNewPipeline()).toBe("You don't have permission to create new pipeline within this pipeline group");
    });

    it('should set values for an environment when a user cannot administer it', () => {
      const environments = DashboardGroups.fromEnvironmentsJSON([{
        "name":           "first",
        "pipelines":      ["up42"],
        "can_administer": false
      }]);
      expect(environments.groups[0].label()).toBe("Environment 'first'");
      expect(environments.groups[0].tooltipForEdit()).toBe("You don't have permission to edit this environment");
      expect(environments.groups[0].titleForEdit()).toBe("");
      expect(environments.groups[0].ariaLabelForEdit()).toBe("You don't have permission to edit this environment");
      expect(environments.groups[0].tooltipForNewPipeline()).toBe("");
      expect(environments.groups[0].titleForNewPipeline()).toBe("");
      expect(environments.groups[0].ariaLabelForNewPipeline()).toBe("");
    });

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

  const environmentGroupsData = [
    {
      "_links":         {
        "self": {
          "href": "http://localhost:8153/go/api/config/environments/first/show"
        },
        "doc":  {
          "href": "https://api.go.cd/current/#environments"
        }
      },
      "name":           "first",
      "pipelines":      ["up42"],
      "can_administer": true
    }
  ];

});
