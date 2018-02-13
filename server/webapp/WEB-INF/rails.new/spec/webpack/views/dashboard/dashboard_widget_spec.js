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
describe("Dashboard Widget", () => {
  const m               = require("mithril");
  const $               = require('jquery');
  const _               = require('lodash');
  const simulateEvent   = require('simulate-event');
  const DashboardWidget = require("views/dashboard/dashboard_widget");
  const Dashboard       = require('models/dashboard/dashboard');
  const DashboardVM     = require("views/dashboard/models/dashboard_view_model");
  const Modal           = require('views/shared/new_modal');

  let $root, root, dashboard, dashboardJson, doCancelPolling, doRefreshImmediately;
  const originalDebounce = _.debounce;
  beforeEach(() => {
    doCancelPolling      = jasmine.createSpy();
    doRefreshImmediately = jasmine.createSpy();

    //override debounce function for tests to be called synchronously
    spyOn(_, 'debounce').and.callFake((func) => {
      return function () {
        func.apply(this, arguments);
      };
    });

    [$root, root] = window.createDomElementForTest();
  });
  afterEach(() => {
    _.debounce = originalDebounce;
    window.destroyDomElementForTest();
  });

  beforeEach(mount);

  afterEach(unmount);

  it("should render dashboard pipelines header", () => {
    expect($root.find('.page-header')).toContainText('Pipelines');
  });

  it("should render dashboard pipeline search field", () => {
    expect($root.find('.pipeline_search')).toBeInDOM();
  });

  it("should search for a pipeline", () => {
    const searchField        = $root.find('#pipeline_search').get(0);
    let pipelinesCountOnPage = $root.find('.pipeline');
    expect(pipelinesCountOnPage).toHaveLength(2);

    $(searchField).val('foo');
    simulateEvent.simulate(searchField, 'input');

    m.redraw();

    pipelinesCountOnPage = $root.find('.pipeline');
    expect(pipelinesCountOnPage).toHaveLength(0);

    $(searchField).val('up42');
    simulateEvent.simulate(searchField, 'input');

    m.redraw();

    pipelinesCountOnPage = $root.find('.pipeline');
    expect(pipelinesCountOnPage).toHaveLength(1);

    $(searchField).val('up43');
    simulateEvent.simulate(searchField, 'input');

    m.redraw();

    pipelinesCountOnPage = $root.find('.pipeline');
    expect(pipelinesCountOnPage).toHaveLength(1);

    $(searchField).val('UP43');
    simulateEvent.simulate(searchField, 'input');

    m.redraw();

    pipelinesCountOnPage = $root.find('.pipeline');
    expect(pipelinesCountOnPage).toHaveLength(1);

    $(searchField).val('up');
    simulateEvent.simulate(searchField, 'input');

    m.redraw();

    pipelinesCountOnPage = $root.find('.pipeline');
    expect(pipelinesCountOnPage).toHaveLength(2);

    $(searchField).val('');
    simulateEvent.simulate(searchField, 'input');

    m.redraw();

    pipelinesCountOnPage = $root.find('.pipeline');
    expect(pipelinesCountOnPage).toHaveLength(2);
  });

  it("should show appropriate modal for pausing a searched pipeline", () => {
    const searchField = $root.find('#pipeline_search').get(0);
    expect($root.find('.pipeline')).toHaveLength(2);

    $(searchField).val('up42');
    simulateEvent.simulate(searchField, 'input');
    m.redraw();

    expect($root.find('.pipeline')).toHaveLength(1);

    simulateEvent.simulate($root.find('.pause').get(0), 'click');
    expect($('.modal-body').text()).toEqual('Specify a reason for pausing schedule on pipeline up42');

    //close specify pause cause popup for up42 pipeline
    Modal.destroyAll();

    $(searchField).val('up43');
    simulateEvent.simulate(searchField, 'input');
    m.redraw();

    expect($root.find('.pipeline')).toHaveLength(1);

    simulateEvent.simulate($root.find('.pause').get(0), 'click');
    expect($('.modal-body').text()).toEqual('Specify a reason for pausing schedule on pipeline up43');

    //close specify pause cause popup for up43 pipeline
    Modal.destroyAll();
  });

  it("should clear the pause message after the modal is closed", () => {
    jasmine.Ajax.withMock(() => {
      const responseMessage = `Pipeline 'up42' paused successfully.`;
      const pauseCause      = "test";

      jasmine.Ajax.stubRequest(`/go/api/pipelines/up42/pause`, undefined, 'POST').andReturn({
        responseText:    JSON.stringify({"message": responseMessage}),
        responseHeaders: {
          'Content-Type': 'application/vnd.go.cd.v1+json'
        },
        status:          200
      });

      simulateEvent.simulate($root.find('.pause').get(0), 'click');
      expect($('.modal-body').text()).toEqual('Specify a reason for pausing schedule on pipeline up42');
      $('.reveal input').val(pauseCause);
      expect($('.reveal input')).toHaveValue(pauseCause);

      simulateEvent.simulate($('.reveal .primary').get(0), 'click');

      expect($root.find('.pipeline_message')).toContainText(responseMessage);
      expect($root.find('.pipeline_message')).toHaveClass("success");

      simulateEvent.simulate($root.find('.pause').get(0), 'click');
      expect($('.reveal input')).toHaveValue('');

      Modal.destroyAll();
    });
  });

  it("should show changes popup for a searched pipeline", () => {
    const searchField = $root.find('#pipeline_search').get(0);
    expect($root.find('.pipeline')).toHaveLength(2);

    $(searchField).val('up42');
    simulateEvent.simulate(searchField, 'input');
    m.redraw();

    expect($root.find('.pipeline')).toHaveLength(1);
    expect($root.find('.material_changes')).not.toBeInDOM();

    $root.find('.info a').get(1).click();

    expect($root.find('.material_changes')).toBeInDOM();

    $(searchField).val('up43');
    simulateEvent.simulate(searchField, 'input');
    m.redraw();

    expect($root.find('.pipeline')).toHaveLength(1);
    expect($root.find('.material_changes')).not.toBeInDOM();

    $root.find('.info a').get(1).click();

    expect($root.find('.material_changes')).toBeInDOM();
  });

  it("should unlock a searched pipeline", () => {
    jasmine.Ajax.withMock(() => {
      const responseMessage = `Pipeline 'up43' unlocked successfully.`;
      jasmine.Ajax.stubRequest(`/go/api/pipelines/up43/unlock`, undefined, 'POST').andReturn({
        responseText:    JSON.stringify({"message": responseMessage}),
        responseHeaders: {
          'Content-Type': 'application/vnd.go.cd.v1+json'
        },
        status:          200
      });

      const searchField = $root.find('#pipeline_search').get(0);
      expect($root.find('.pipeline')).toHaveLength(2);

      $(searchField).val('up43');
      simulateEvent.simulate(searchField, 'input');
      m.redraw();

      expect($root.find('.pipeline')).toHaveLength(1);

      expect(doCancelPolling).not.toHaveBeenCalled();
      expect(doRefreshImmediately).not.toHaveBeenCalled();

      simulateEvent.simulate($root.find('.pipeline_locked').get(0), 'click');

      expect(doCancelPolling).toHaveBeenCalled();
      expect(doRefreshImmediately).toHaveBeenCalled();

      expect($root.find('.pipeline_message')).toContainText(responseMessage);
      expect($root.find('.pipeline_message')).toHaveClass("success");
    });
  });

  it("should render pipeline groups", () => {
    const pipelineGroupsCount = dashboardJson._embedded.pipeline_groups.length;
    const pipelineGroups      = $root.find('.pipeline-group');

    expect(pipelineGroups.size()).toEqual(pipelineGroupsCount);
    expect(pipelineGroups.get(0)).toContainText(dashboardJson._embedded.pipeline_groups[0].name);
  });

  it("should render pipeline group title", () => {
    expect($root.find('.pipeline-group_title span').get(0)).toContainText("pipeline group");
    expect($root.find('.pipeline-group_title strong').get(0)).toContainText(dashboardJson._embedded.pipeline_groups[0].name);
  });

  it("should show pipeline group settings icon which links to pipeline group index page for admin users", () => {
    const pipelineGroupJSON = dashboardJson._embedded.pipeline_groups[0];

    const title = $root.find('.pipeline-group_title a').get(0);
    expect(title.href.indexOf(`/go/admin/pipelines#group-${pipelineGroupJSON.name}`)).not.toEqual(-1);
  });

  it("should show disabled pipeline group settings icon for non admin users", () => {
    unmount();
    mount(false);

    expect($root.find('.pipeline-group_title a')).toHaveClass('disabled');
  });

  it("should render pipelines within each pipeline group", () => {
    const pipelineName                      = dashboardJson._embedded.pipeline_groups[0].pipelines[0];
    const pipelinesWithinPipelineGroupCount = dashboardJson._embedded.pipeline_groups[0].pipelines.length;
    const pipelinesWithinPipelineGroup      = $root.find('.pipeline-group .pipeline');

    expect(pipelinesWithinPipelineGroup.size()).toEqual(pipelinesWithinPipelineGroupCount);
    expect(pipelinesWithinPipelineGroup).toContainText(pipelineName);
  });

  it("should close all dropdowns when a user clicks on any portion of the dashboard widget", () => {
    const dashboard   = $root.find('.pipeline_wrapper');
    const changesLink = $root.find('.info a')[1];
    expect(dashboard.find('.material_changes')).not.toBeInDOM();
    $(changesLink).click();
    m.redraw();

    expect(dashboard.find('.material_changes')).toBeInDOM();

    $(dashboard).click();
    m.redraw();

    expect(dashboard.find('.material_changes')).not.toBeInDOM();
  });

  function mount(canAdminister = true) {
    dashboardJson = {
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
            "pipelines":      ["up42", "up43"],
            "can_administer": canAdminister
          }
        ],
        "pipelines":       [
          {
            "_links":                 {
              "self":                 {
                "href": "http://localhost:8153/go/api/pipelines/up42/history"
              },
              "doc":                  {
                "href": "https://api.go.cd/current/#pipelines"
              },
              "settings_path":        {
                "href": "http://localhost:8153/go/admin/pipelines/up42/general"
              },
              "trigger":              {
                "href": "http://localhost:8153/go/api/pipelines/up42/schedule"
              },
              "trigger_with_options": {
                "href": "http://localhost:8153/go/api/pipelines/up42/schedule"
              },
              "pause":                {
                "href": "http://localhost:8153/go/api/pipelines/up42/pause"
              },
              "unpause":              {
                "href": "http://localhost:8153/go/api/pipelines/up42/unpause"
              }
            },
            "name":                   "up42",
            "last_updated_timestamp": 1510299695473,
            "locked":                 false,
            "can_pause":              true,
            "pause_info":             {
              "paused":       false,
              "paused_by":    null,
              "pause_reason": null
            },
            "build_cause":            {
              "approver":           "",
              "is_forced":          false,
              "trigger_message":    "modified by GoCD Test User <devnull@example.com>",
              "material_revisions": [
                {
                  "material_type": "Git",
                  "material_name": "test-repo",
                  "changed":       true,
                  "modifications": [
                    {
                      "_links":        {
                        "vsm": {
                          "href": "http://localhost:8153/go/materials/value_stream_map/4879d548de8a9d7122ceb71e7809c1f91a0876afa534a4f3ba7ed4a532bc1b02/9c86679eefc3c5c01703e9f1d0e96b265ad25691"
                        }
                      },
                      "user_name":     "GoCD Test User <devnull@example.com>",
                      "revision":      "9c86679eefc3c5c01703e9f1d0e96b265ad25691",
                      "modified_time": "2017-12-19T05:30:32.000Z",
                      "comment":       "Initial commit"
                    }
                  ]
                }
              ]
            },
            "_embedded":              {
              "instances": [
                {
                  "_links":       {
                    "self":            {
                      "href": "http://localhost:8153/go/api/pipelines/up42/instance/1"
                    },
                    "doc":             {
                      "href": "https://api.go.cd/current/#get-pipeline-instance"
                    },
                    "history_url":     {
                      "href": "http://localhost:8153/go/api/pipelines/up42/history"
                    },
                    "vsm_url":         {
                      "href": "http://localhost:8153/go/pipelines/value_stream_map/up42/1"
                    },
                    "compare_url":     {
                      "href": "http://localhost:8153/go/compare/up42/0/with/1"
                    },
                    "build_cause_url": {
                      "href": "http://localhost:8153/go/pipelines/up42/1/build_cause"
                    }
                  },
                  "label":        "1",
                  "counter":      "1",
                  "scheduled_at": "2017-11-10T07:25:28.539Z",
                  "triggered_by": "changes",
                  "build_cause":  {
                    "approver":           "",
                    "is_forced":          false,
                    "trigger_message":    "modified by GoCD Test User <devnull@example.com>",
                    "material_revisions": [
                      {
                        "material_type": "Git",
                        "material_name": "test-repo",
                        "changed":       true,
                        "modifications": [
                          {
                            "_links":        {
                              "vsm": {
                                "href": "http://localhost:8153/go/materials/value_stream_map/4879d548de8a9d7122ceb71e7809c1f91a0876afa534a4f3ba7ed4a532bc1b02/9c86679eefc3c5c01703e9f1d0e96b265ad25691"
                              }
                            },
                            "user_name":     "GoCD Test User <devnull@example.com>",
                            "revision":      "9c86679eefc3c5c01703e9f1d0e96b265ad25691",
                            "modified_time": "2017-12-19T05:30:32.000Z",
                            "comment":       "Initial commit"
                          }
                        ]
                      }
                    ]
                  },
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
                }
              ]
            }
          },
          {
            "_links":                 {
              "self":                 {
                "href": "http://localhost:8153/go/api/pipelines/up43/history"
              },
              "doc":                  {
                "href": "https://api.go.cd/current/#pipelines"
              },
              "settings_path":        {
                "href": "http://localhost:8153/go/admin/pipelines/up43/general"
              },
              "trigger":              {
                "href": "http://localhost:8153/go/api/pipelines/up43/schedule"
              },
              "trigger_with_options": {
                "href": "http://localhost:8153/go/api/pipelines/up43/schedule"
              },
              "pause":                {
                "href": "http://localhost:8153/go/api/pipelines/up43/pause"
              },
              "unpause":              {
                "href": "http://localhost:8153/go/api/pipelines/up43/unpause"
              }
            },
            "name":                   "up43",
            "last_updated_timestamp": 1510299695473,
            "locked":                 true,
            "can_unlock":             true,
            "can_pause":              true,
            "pause_info":             {
              "paused":       false,
              "paused_by":    null,
              "pause_reason": null
            },
            "build_cause":            {
              "approver":           "",
              "is_forced":          false,
              "trigger_message":    "modified by GoCD Test User <devnull@example.com>",
              "material_revisions": [
                {
                  "material_type": "Git",
                  "material_name": "test-repo",
                  "changed":       true,
                  "modifications": [
                    {
                      "_links":        {
                        "vsm": {
                          "href": "http://localhost:8153/go/materials/value_stream_map/4879d548de8a9d7122ceb71e7809c1f91a0876afa534a4f3ba7ed4a532bc1b02/9c86679eefc3c5c01703e9f1d0e96b265ad25691"
                        }
                      },
                      "user_name":     "GoCD Test User <devnull@example.com>",
                      "revision":      "9c86679eefc3c5c01703e9f1d0e96b265ad25691",
                      "modified_time": "2017-12-19T05:30:32.000Z",
                      "comment":       "Initial commit"
                    }
                  ]
                }
              ]
            },
            "_embedded":              {
              "instances": [
                {
                  "_links":       {
                    "self":            {
                      "href": "http://localhost:8153/go/api/pipelines/up43/instance/1"
                    },
                    "doc":             {
                      "href": "https://api.go.cd/current/#get-pipeline-instance"
                    },
                    "history_url":     {
                      "href": "http://localhost:8153/go/api/pipelines/up43/history"
                    },
                    "vsm_url":         {
                      "href": "http://localhost:8153/go/pipelines/value_stream_map/up43/1"
                    },
                    "compare_url":     {
                      "href": "http://localhost:8153/go/compare/up43/0/with/1"
                    },
                    "build_cause_url": {
                      "href": "http://localhost:8153/go/pipelines/up43/1/build_cause"
                    }
                  },
                  "label":        "1",
                  "counter":      "1",
                  "scheduled_at": "2017-11-10T07:25:28.539Z",
                  "triggered_by": "changes",
                  "build_cause":  {
                    "approver":           "",
                    "is_forced":          false,
                    "trigger_message":    "modified by GoCD Test User <devnull@example.com>",
                    "material_revisions": [
                      {
                        "material_type": "Git",
                        "material_name": "test-repo",
                        "changed":       true,
                        "modifications": [
                          {
                            "_links":        {
                              "vsm": {
                                "href": "http://localhost:8153/go/materials/value_stream_map/4879d548de8a9d7122ceb71e7809c1f91a0876afa534a4f3ba7ed4a532bc1b02/9c86679eefc3c5c01703e9f1d0e96b265ad25691"
                              }
                            },
                            "user_name":     "GoCD Test User <devnull@example.com>",
                            "revision":      "9c86679eefc3c5c01703e9f1d0e96b265ad25691",
                            "modified_time": "2017-12-19T05:30:32.000Z",
                            "comment":       "Initial commit"
                          }
                        ]
                      }
                    ]
                  },
                  "_embedded":    {
                    "stages": [
                      {
                        "_links":       {
                          "self": {
                            "href": "http://localhost:8153/go/api/stages/up43/1/up43_stage/1"
                          },
                          "doc":  {
                            "href": "https://api.go.cd/current/#get-stage-instance"
                          }
                        },
                        "name":         "up43_stage",
                        "counter":      "1",
                        "status":       "Failed",
                        "approved_by":  "changes",
                        "scheduled_at": "2017-11-10T07:25:28.539Z"
                      }
                    ]
                  }
                }
              ]
            }
          }
        ]
      }
    };

    dashboard                = Dashboard.fromJSON(dashboardJson);
    const dashboardViewModel = new DashboardVM();
    dashboardViewModel.initialize(_.map(dashboardJson._embedded.pipelines, (p) => p.name));

    m.mount(root, {
      view() {
        return m(DashboardWidget, {
          dashboard,
          doCancelPolling,
          doRefreshImmediately,
          vm: dashboardViewModel
        });
      }
    });
    m.redraw(true);
  }

  function unmount() {
    $("body").off();
    m.mount(root, null);
    m.redraw();
  }
});
