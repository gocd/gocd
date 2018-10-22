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

import SparkRoutes from '../../../../webpack/helpers/spark_routes';

describe("Dashboard Widget", () => {
  const m               = require("mithril");
  const Stream          = require("mithril/stream");
  const $               = require("jquery");
  const _               = require("lodash");
  const simulateEvent   = require("simulate-event");
  const DashboardWidget = require("views/dashboard/dashboard_widget");
  const Dashboard       = require('models/dashboard/dashboard');
  const DashboardVM     = require("views/dashboard/models/dashboard_view_model");
  const Modal           = require("views/shared/new_modal");

  const PersonalizeVM   = require('views/dashboard/models/personalization_vm');
  const Personalization = require('models/dashboard/personalization');

  let $root, root, dashboard, dashboardJson, buildCauseJson, doCancelPolling, doRefreshImmediately;
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

    jasmine.Ajax.withMock(() => {
      jasmine.Ajax.stubRequest(SparkRoutes.pipelineSelectionPath(), undefined, 'GET');
    });

    [$root, root] = window.createDomElementForTest();
  });

  afterEach(() => {
    _.debounce = originalDebounce;
    window.destroyDomElementForTest();
  });

  beforeEach(mount);

  afterEach(unmount);

  it("should render dashboard pipeline search field", () => {
    expect(find('search-box')).toBeInDOM();
  });

  it("should show spinner before dashboard is loaded", () => {
    unmount();
    const pageSpinner = Stream(true);
    mount(true, pageSpinner);
    expect($root.find(".page-spinner")).toBeInDOM();

    pageSpinner(false);
    unmount();
    mount(true, pageSpinner);
    expect($root.find(".page-spinner")).not.toBeInDOM();
  });

  it('should render an info message', () => {
    expect($root.find('.dashboard-message')).not.toBeInDOM();
    dashboard.message({content: 'some message', type: 'info'});
    m.redraw();
    expect($root.find('.callout.info .dashboard-message')).toContainText('some message');
  });

  it('should render an alert message', () => {
    expect($root.find('.dashboard-message')).not.toBeInDOM();
    dashboard.message({content: 'some error message', type: 'alert'});
    m.redraw();
    expect($root.find('.callout.alert .dashboard-message')).toContainText('some error message');
  });

  it("should search for a pipeline", () => {
    const searchField        = find('search-box').get(0);
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
    const searchField = find('search-box').get(0);
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
    stubBuildCauseAjaxCall();

    const searchField = find('search-box').get(0);
    expect($root.find('.pipeline')).toHaveLength(2);

    $(searchField).val('up42');
    simulateEvent.simulate(searchField, 'input');
    m.redraw();

    expect($root.find('.pipeline')).toHaveLength(1);
    expect($root.find('.material_changes')).not.toBeInDOM();

    jasmine.Ajax.withMock(() => {
      jasmine.Ajax.stubRequest(SparkRoutes.buildCausePath('up42', '1'), undefined, 'GET').andReturn({
        responseText:    JSON.stringify(buildCauseJson),
        responseHeaders: {'Content-Type': 'application/vnd.go.cd.v1+json'},
        status:          200
      });
      $root.find('.info a').get(1).click();
    });

    expect($root.find('.material_changes')).toBeInDOM();

    $(searchField).val('up43');
    simulateEvent.simulate(searchField, 'input');
    m.redraw();

    expect($root.find('.pipeline')).toHaveLength(1);
    expect($root.find('.material_changes')).not.toBeInDOM();

    jasmine.Ajax.withMock(() => {
      jasmine.Ajax.stubRequest(SparkRoutes.buildCausePath('up42', '1'), undefined, 'GET').andReturn({
        responseText:    JSON.stringify(buildCauseJson),
        responseHeaders: {'Content-Type': 'application/vnd.go.cd.v1+json'},
        status:          200
      });
      $root.find('.info a').get(1).click();
    });

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

      const searchField = find('search-box').get(0);
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
    const pipelineGroups      = $root.find('.dashboard-group');

    expect(pipelineGroups.size()).toEqual(pipelineGroupsCount);
    expect(pipelineGroups.get(0)).toContainText(dashboardJson._embedded.pipeline_groups[0].name);
  });

  it("should render pipeline group title", () => {
    expect($root.find('.dashboard-group_title a').get(0)).toContainText(dashboardJson._embedded.pipeline_groups[0].name);
  });

  it("should show pipeline group name which links to pipeline group index page for admin users", () => {
    const pipelineGroupJSON = dashboardJson._embedded.pipeline_groups[0];

    const title = $root.find('.dashboard-group_title>a').get(0);
    expect(title.href.indexOf(`/go/admin/pipelines#group-${pipelineGroupJSON.name}`)).not.toEqual(-1);
  });

  it("should show pipeline group icon which links to pipeline group settings page for admin users", () => {
    const pipelineGroupJSON = dashboardJson._embedded.pipeline_groups[0];

    const title = $root.find('.dashboard-group_title>a').get(1);
    expect(title.href.indexOf(`/go/admin/pipeline_group/${pipelineGroupJSON.name}/edit`)).not.toEqual(-1);
  });

  it("should show disabled pipeline group settings icon showing tooltip for non admin users", () => {
    unmount();
    mount(false);
    expect($root.find('.dashboard-group_title .edit_config')).toHaveClass('disabled');
    expect($root.find('.dashboard-group_title .edit_config')).toHaveAttr('data-tooltip-id');
  });

  it("should show pipeline group name as a disabled link for non admin users", () => {
    unmount();
    mount(false);
    const pipelineGroupJSON = dashboardJson._embedded.pipeline_groups[0];

    const title = $root.find('.dashboard-group_title>a').get(0);
    expect(title).toContainText(pipelineGroupJSON.name);
    expect(title).toHaveClass('disabled');
  });


  it("should render pipelines within each pipeline group", () => {
    const pipelineName                      = dashboardJson._embedded.pipeline_groups[0].pipelines[0];
    const pipelinesWithinPipelineGroupCount = dashboardJson._embedded.pipeline_groups[0].pipelines.length;
    const pipelinesWithinPipelineGroup      = $root.find('.dashboard-group .pipeline');

    expect(pipelinesWithinPipelineGroup.size()).toEqual(pipelinesWithinPipelineGroupCount);
    expect(pipelinesWithinPipelineGroup).toContainText(pipelineName);
  });

  function mount(canAdminister = true, showSpinner = Stream(false)) {
    buildCauseJson = {
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
    };
    dashboardJson  = {
      "filter_name": "Default",
      "_embedded":   {
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

    dashboard           = new Dashboard();
    const personalizeVM = new PersonalizeVM(Stream("Default"));
    personalizeVM.model(new Personalization([{name: "Default", state: []}], []));
    dashboard.initialize(dashboardJson);

    const dashboardViewModel           = new DashboardVM(dashboard);
    dashboardViewModel._performRouting = _.noop;

    m.mount(root, {
      view() {
        return m(DashboardWidget, {
          personalizeVM,
          showSpinner,
          isQuickEditPageEnabled:     false,
          pluginsSupportingAnalytics: {},
          shouldShowAnalyticsIcon:    false,
          doCancelPolling,
          doRefreshImmediately,
          vm:                         dashboardViewModel
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

  function stubBuildCauseAjaxCall() {
    jasmine.Ajax.withMock(() => {
      jasmine.Ajax.stubRequest(SparkRoutes.buildCausePath('up42', '1'), undefined, 'GET').andReturn({
        responseText:    JSON.stringify(buildCauseJson),
        responseHeaders: {'Content-Type': 'application/vnd.go.cd.v1+json'},
        status:          200
      });
      jasmine.Ajax.stubRequest(SparkRoutes.buildCausePath('up43', '1'), undefined, 'GET').andReturn({
        responseText:    JSON.stringify(buildCauseJson),
        responseHeaders: {'Content-Type': 'application/vnd.go.cd.v1+json'},
        status:          200
      });
    });
  }

  function find(id) {
    return $root.find(`[data-test-id='${id}']`);
  }
});
