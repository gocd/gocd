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
import _ from "lodash";
import {DashboardViewModel as DashboardVM} from "views/dashboard/models/dashboard_view_model";
import {Dashboard} from "models/dashboard/dashboard";

describe("Dashboard View Model", () => {

  describe("FilterMixin", () => {
    let pipelinesCountMap, dashboard, dashboardVM;

    beforeEach(() => {
      pipelinesCountMap = {'up42': 2, 'up43': 2, 'down42': 1};
      dashboard = new Dashboard();
      dashboard.initialize(dashboardJsonForPipelines(pipelinesCountMap));
      dashboardVM = new DashboardVM(dashboard);
    });

    it("it should filter dashboard provided filter text", () => {
      dashboardVM._performRouting = _.noop;
      const filter = {
        acceptsStatusOf: () => { return true;}
      };

      expect(dashboardVM.filteredGroups(filter)[0].pipelines).toEqual(["up42", "up43", "down42"]);
      dashboardVM.searchText("up");
      expect(dashboardVM.filteredGroups(filter)[0].pipelines).toEqual(["up42", "up43"]);
      dashboardVM.searchText("42");
      expect(dashboardVM.filteredGroups(filter)[0].pipelines).toEqual(["up42", "down42"]);
      dashboardVM.searchText("up42");
      expect(dashboardVM.filteredGroups(filter)[0].pipelines).toEqual(["up42"]);
      dashboardVM.searchText("up42-some-more");
      expect(dashboardVM.filteredGroups(filter)).toEqual([]);
    });

    it("should peform routing when filter text is updated", () => {
      const performSpy = spyOn(dashboardVM, "_performRouting");

      expect(performSpy).not.toHaveBeenCalled();

      dashboardVM.searchText("up");

      expect(performSpy).toHaveBeenCalled();
    });

  });

  describe("Dropdown", () => {
    let pipelinesCountMap, dashboard, dashboardVM;
    beforeEach(() => {
      pipelinesCountMap = {'up42': 2, 'up43': 2};
      dashboard = new Dashboard();
      dashboard.initialize(dashboardJsonForPipelines(pipelinesCountMap));
      dashboardVM = new DashboardVM(dashboard);
    });

    it('should initialize dropdown state for each pipeline instance', () => {
      expect(dashboardVM.dropdown.isOpen('up42', 1)).toEqual(false);
      expect(dashboardVM.dropdown.isOpen('up42', 2)).toEqual(false);
      expect(dashboardVM.dropdown.isOpen('up43', 1)).toEqual(false);
      expect(dashboardVM.dropdown.isOpen('up43', 2)).toEqual(false);
    });

    it('should remove invalid pipeline instances from dropdown state', () => {
      expect(dashboardVM.dropdown.isOpen('up42', 1)).toEqual(false);
      expect(dashboardVM.dropdown.isOpen('up42', 2)).toEqual(false);
      expect(dashboardVM.dropdown.isOpen('up43', 1)).toEqual(false);
      expect(dashboardVM.dropdown.isOpen('up43', 2)).toEqual(false);

      dashboardVM.dropdown.show('up42', 1);

      dashboard.initialize(dashboardJsonForPipelines({'up42': 1, 'up43': 1}));

      expect(dashboardVM.dropdown.isOpen('up42', 1)).toEqual(true);
      expect(dashboardVM.dropdown.isOpen('up43', 1)).toEqual(false);
    });

    it('should initialize dropdown states as close initially', () => {
      expect(dashboardVM.dropdown.isOpen('up42', 1)).toEqual(false);
      expect(dashboardVM.dropdown.isOpen('up43', 1)).toEqual(false);
    });

    it('should toggle dropdown state', () => {
      expect(dashboardVM.dropdown.isOpen('up42', 1)).toEqual(false);
      dashboardVM.dropdown.show('up42', 1);
      expect(dashboardVM.dropdown.isOpen('up42', 1)).toEqual(true);
    });

    it('should close all other dropdowns incase of a dropdown is toggled', () => {
      dashboardVM.dropdown.show('up42', 1);

      expect(dashboardVM.dropdown.isOpen('up42', 1)).toEqual(true);
      expect(dashboardVM.dropdown.isOpen('up43', 1)).toEqual(false);
      dashboardVM.dropdown.show('up43', 1);
      expect(dashboardVM.dropdown.isOpen('up42', 1)).toEqual(false);
      expect(dashboardVM.dropdown.isOpen('up43', 1)).toEqual(true);
    });

    it('should hide all dropdowns when the first instance dropdown is open', () => {
      dashboardVM.dropdown.show('up42', 1);
      expect(dashboardVM.dropdown.isOpen('up42', 1)).toEqual(true);
      expect(dashboardVM.dropdown.isOpen('up42', 2)).toEqual(false);
      expect(dashboardVM.dropdown.isOpen('up43', 1)).toEqual(false);
      expect(dashboardVM.dropdown.isOpen('up43', 2)).toEqual(false);
      dashboardVM.dropdown.hide();
      expect(dashboardVM.dropdown.isOpen('up42', 1)).toEqual(false);
      expect(dashboardVM.dropdown.isOpen('up42', 2)).toEqual(false);
      expect(dashboardVM.dropdown.isOpen('up43', 1)).toEqual(false);
      expect(dashboardVM.dropdown.isOpen('up43', 2)).toEqual(false);
    });

    it('should hide all dropdowns when any dropdown other than the first instance\'s is open', () => {
      dashboardVM.dropdown.show('up42', 2);
      expect(dashboardVM.dropdown.isOpen('up42', 1)).toEqual(false);
      expect(dashboardVM.dropdown.isOpen('up42', 2)).toEqual(true);
      expect(dashboardVM.dropdown.isOpen('up43', 1)).toEqual(false);
      expect(dashboardVM.dropdown.isOpen('up43', 2)).toEqual(false);
      dashboardVM.dropdown.hide();
      expect(dashboardVM.dropdown.isOpen('up42', 1)).toEqual(false);
      expect(dashboardVM.dropdown.isOpen('up42', 2)).toEqual(false);
      expect(dashboardVM.dropdown.isOpen('up43', 1)).toEqual(false);
      expect(dashboardVM.dropdown.isOpen('up43', 2)).toEqual(false);
    });

    it('should hide other changes dropdown for the same pipeline but different instance', () => {
      dashboardVM.dropdown.show('up42', 1);
      expect(dashboardVM.dropdown.isOpen('up42', 1)).toEqual(true);
      expect(dashboardVM.dropdown.isOpen('up42', 2)).toEqual(false);
      dashboardVM.dropdown.show('up42', 2);
      expect(dashboardVM.dropdown.isOpen('up42', 1)).toEqual(false);
      expect(dashboardVM.dropdown.isOpen('up42', 2)).toEqual(true);
    });
  });

  describe('Operation Messages', () => {
    let pipelinesCountMap, dashboard, dashboardVM;
    beforeEach(() => {
      pipelinesCountMap = {'up42': 2, 'up43': 2};
      dashboardVM       = new DashboardVM(dashboard);
      dashboard = new Dashboard();
      dashboard.initialize(dashboardJsonForPipelines(pipelinesCountMap));
      jasmine.clock().install();
    });

    afterEach(() => {
      jasmine.clock().uninstall();
    });

    it('should set pipeline operation success messages', () => {
      expect(dashboardVM.operationMessages.get('up42')).toEqual(undefined);

      const message = 'message';
      dashboardVM.operationMessages.success('up42', message);

      expect(dashboardVM.operationMessages.get('up42')).toEqual({message, type: 'success'});
    });

    it('should set pipeline operation failure messages', () => {
      expect(dashboardVM.operationMessages.get('up42')).toEqual(undefined);

      const message = 'message';
      dashboardVM.operationMessages.failure('up42', message);

      expect(dashboardVM.operationMessages.get('up42')).toEqual({message, type: 'error'});
    });

    it('should clear message after timeout interval', () => {
      expect(dashboardVM.operationMessages.get('up42')).toEqual(undefined);

      const message = 'message';
      dashboardVM.operationMessages.failure('up42', message);

      expect(dashboardVM.operationMessages.get('up42')).toEqual({message, type: 'error'});

      jasmine.clock().tick(5001);

      expect(dashboardVM.operationMessages.get('up42')).toEqual(undefined);
    });
  });

  const dashboardJsonForPipelines = (pipelines) => {
    return {
      "_embedded": {
        "pipeline_groups": [
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
            pipelines:        _.keys(pipelines),
            "can_administer": true
          }
        ],
        "pipelines":       pipelinesJsonForPipelines(pipelines)
      }
    };
  };

  const getPipelineInstance = (counter) => {
    //need to increment by 1 as pipeline counter starts with 1
    counter++;

    return {
      "_links":       {
        "self":            {
          "href": "http://localhost:8153/go/api/pipelines/up42/instance/1"
        },
        "doc":             {
          "href": "https://api.go.cd/current/#get-pipeline-instance"
        }
      },
      "label":        counter,
      counter,
      "scheduled_at": "2017-11-10T07:25:28.539Z",
      "triggered_by": "changes",
      "_embedded":    {
        "stages": [
          {
            "_links":       {
              "self": {
                "href": "http://localhost:8153/go/api/stages/up42/1/up42_stage/1"
              },
              "doc":  {
                "href": "https://api.go.cd/current/#get-stage-instance"
              }
            },
            "name":         "up42_stage",
            "counter":      "1",
            "status":       "Failed",
            "approved_by":  "changes",
            "scheduled_at": "2017-11-10T07:25:28.539Z"
          }
        ]
      }
    };
  };

  const pipelinesJsonForPipelines = (pipelines) => {
    return _.map((pipelines), (instanceCount, pipelineName) => {
      const instances = _.times(instanceCount, getPipelineInstance);

      return ({
        "_links":                 {
          "self":                 {
            "href": "http://localhost:8153/go/api/pipelines/up42/history"
          },
          "doc":                  {
            "href": "https://api.go.cd/current/#pipelines"
          }
        },
        "name":                   pipelineName,
        "last_updated_timestamp": 1510299695473,
        "locked":                 false,
        "can_pause":              true,
        "template_info": {
          "is_using_template": false,
          "template_name":     null
        },
        "pause_info":             {
          "paused":       false,
          "paused_by":    null,
          "pause_reason": null
        },
        "_embedded":              {instances}
      });
    });

  };
});
