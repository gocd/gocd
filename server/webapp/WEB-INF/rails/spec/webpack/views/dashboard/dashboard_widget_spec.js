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
import SparkRoutes from "helpers/spark_routes";
import {TestHelper} from "views/pages/spec/test_helper";

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

  let dashboard, dashboardJson, buildCauseJson, doCancelPolling, doRefreshImmediately;
  const originalDebounce = _.debounce;

  const helper = new TestHelper();

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
  });

  afterEach(() => {
    _.debounce = originalDebounce;
  });

  beforeEach(mount);

  afterEach(unmount);

  it("should render dashboard pipeline search field", () => {
    expect(helper.findByDataTestId('search-box')).toBeInDOM();
  });

  it("should show spinner before dashboard is loaded", () => {
    unmount();
    const pageSpinner = Stream(true);
    mount(true, pageSpinner);
    expect(helper.find(".page-spinner")).toBeInDOM();

    pageSpinner(false);
    unmount();
    mount(true, pageSpinner);
    expect(helper.find(".page-spinner")).not.toBeInDOM();
  });

  it('should render an info message', () => {
    expect(helper.find('.dashboard-message')).not.toBeInDOM();
    dashboard.message({content: 'some message', type: 'info'});
    m.redraw();
    expect(helper.find('.callout.info .dashboard-message')).toContainText('some message');
  });

  it('should render an alert message', () => {
    expect(helper.find('.dashboard-message')).not.toBeInDOM();
    dashboard.message({content: 'some error message', type: 'alert'});
    m.redraw();
    expect(helper.find('.callout.alert .dashboard-message')).toContainText('some error message');
  });

  it("should search for a pipeline", () => {
    const searchField        = helper.findByDataTestId('search-box').get(0);
    let pipelinesCountOnPage = helper.find('.pipeline');
    expect(pipelinesCountOnPage).toHaveLength(2);

    $(searchField).val('foo');
    simulateEvent.simulate(searchField, 'input');

    m.redraw();

    pipelinesCountOnPage = helper.find('.pipeline');
    expect(pipelinesCountOnPage).toHaveLength(0);

    $(searchField).val('up42');
    simulateEvent.simulate(searchField, 'input');

    m.redraw();

    pipelinesCountOnPage = helper.find('.pipeline');
    expect(pipelinesCountOnPage).toHaveLength(1);

    $(searchField).val('up43');
    simulateEvent.simulate(searchField, 'input');

    m.redraw();

    pipelinesCountOnPage = helper.find('.pipeline');
    expect(pipelinesCountOnPage).toHaveLength(1);

    $(searchField).val('UP43');
    simulateEvent.simulate(searchField, 'input');

    m.redraw();

    pipelinesCountOnPage = helper.find('.pipeline');
    expect(pipelinesCountOnPage).toHaveLength(1);

    $(searchField).val('up');
    simulateEvent.simulate(searchField, 'input');

    m.redraw();

    pipelinesCountOnPage = helper.find('.pipeline');
    expect(pipelinesCountOnPage).toHaveLength(2);

    $(searchField).val('');
    simulateEvent.simulate(searchField, 'input');

    m.redraw();

    pipelinesCountOnPage = helper.find('.pipeline');
    expect(pipelinesCountOnPage).toHaveLength(2);
  });

  it("should show appropriate modal for pausing a searched pipeline", () => {
    const searchField = helper.findByDataTestId('search-box').get(0);
    expect(helper.find('.pipeline')).toHaveLength(2);

    $(searchField).val('up42');
    simulateEvent.simulate(searchField, 'input');
    m.redraw();

    expect(helper.find('.pipeline')).toHaveLength(1);

    simulateEvent.simulate(helper.find('.pause').get(0), 'click');
    expect($('.modal-body').text()).toEqual('Specify a reason for pausing schedule on pipeline up42');

    //close specify pause cause popup for up42 pipeline
    Modal.destroyAll();

    $(searchField).val('up43');
    simulateEvent.simulate(searchField, 'input');
    m.redraw();

    expect(helper.find('.pipeline')).toHaveLength(1);

    simulateEvent.simulate(helper.find('.pause').get(0), 'click');
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

      simulateEvent.simulate(helper.find('.pause').get(0), 'click');
      expect($('.modal-body').text()).toEqual('Specify a reason for pausing schedule on pipeline up42');
      $('.reveal input').val(pauseCause);
      expect($('.reveal input')).toHaveValue(pauseCause);

      simulateEvent.simulate($('.reveal .primary').get(0), 'click');

      expect(helper.find('.pipeline_message')).toContainText(responseMessage);
      expect(helper.find('.pipeline_message')).toHaveClass("success");

      simulateEvent.simulate(helper.find('.pause').get(0), 'click');
      expect($('.reveal input')).toHaveValue('');

      Modal.destroyAll();
    });
  });

  it("should show changes popup for a searched pipeline", () => {
    stubBuildCauseAjaxCall();

    const searchField = helper.findByDataTestId('search-box').get(0);
    expect(helper.find('.pipeline')).toHaveLength(2);

    $(searchField).val('up42');
    simulateEvent.simulate(searchField, 'input');
    m.redraw();

    expect(helper.find('.pipeline')).toHaveLength(1);
    expect(helper.find('.material_changes')).not.toBeInDOM();

    jasmine.Ajax.withMock(() => {
      jasmine.Ajax.stubRequest(SparkRoutes.buildCausePath('up42', '1'), undefined, 'GET').andReturn({
        responseText:    JSON.stringify(buildCauseJson),
        responseHeaders: {'Content-Type': 'application/vnd.go.cd.v1+json'},
        status:          200
      });
      helper.find('.info a').get(1).click();
    });

    expect(helper.find('.material_changes')).toBeInDOM();

    $(searchField).val('up43');
    simulateEvent.simulate(searchField, 'input');
    m.redraw();

    expect(helper.find('.pipeline')).toHaveLength(1);
    expect(helper.find('.material_changes')).not.toBeInDOM();

    jasmine.Ajax.withMock(() => {
      jasmine.Ajax.stubRequest(SparkRoutes.buildCausePath('up42', '1'), undefined, 'GET').andReturn({
        responseText:    JSON.stringify(buildCauseJson),
        responseHeaders: {'Content-Type': 'application/vnd.go.cd.v1+json'},
        status:          200
      });
      helper.find('.info a').get(1).click();
    });

    expect(helper.find('.material_changes')).toBeInDOM();
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

      const searchField = helper.findByDataTestId('search-box').get(0);
      expect(helper.find('.pipeline')).toHaveLength(2);

      $(searchField).val('up43');
      simulateEvent.simulate(searchField, 'input');
      m.redraw();

      expect(helper.find('.pipeline')).toHaveLength(1);

      expect(doCancelPolling).not.toHaveBeenCalled();
      expect(doRefreshImmediately).not.toHaveBeenCalled();

      simulateEvent.simulate(helper.find('.pipeline_locked').get(0), 'click');

      expect(doCancelPolling).toHaveBeenCalled();
      expect(doRefreshImmediately).toHaveBeenCalled();

      expect(helper.find('.pipeline_message')).toContainText(responseMessage);
      expect(helper.find('.pipeline_message')).toHaveClass("success");
    });
  });

  it("should render pipeline groups", () => {
    const pipelineGroupsCount = dashboardJson._embedded.pipeline_groups.length;
    const pipelineGroups      = helper.find('.dashboard-group');

    expect(pipelineGroups.size()).toEqual(pipelineGroupsCount);
    expect(pipelineGroups.get(0)).toContainText(dashboardJson._embedded.pipeline_groups[0].name);
  });

  it("should render pipeline group title", () => {
    expect(helper.find('.dashboard-group_title a').get(0)).toContainText(dashboardJson._embedded.pipeline_groups[0].name);
  });

  it("should show pipeline group name which links to pipeline group index page for admin users", () => {
    const pipelineGroupJSON = dashboardJson._embedded.pipeline_groups[0];

    const title = helper.find('.dashboard-group_title>a').get(0);
    expect(title.href.indexOf(`/go/admin/pipelines#group-${pipelineGroupJSON.name}`)).not.toEqual(-1);
  });

  it("should show pipeline group icon which links to pipeline group settings page for admin users", () => {
    const pipelineGroupJSON = dashboardJson._embedded.pipeline_groups[0];

    const title = helper.find('.dashboard-group_title>a').get(1);
    expect(title.href.indexOf(`/go/admin/pipeline_group/${pipelineGroupJSON.name}/edit`)).not.toEqual(-1);
  });

  it("should show pipeline add icon when grouped by pipeline group for admin users", () => {
    const pipelineGroupJSON = dashboardJson._embedded.pipeline_groups[0];

    const title = helper.find('.dashboard-group_add_pipeline>a').get(0);
    expect(title.href.indexOf(`/go/admin/pipeline/new?group=${pipelineGroupJSON.name}`)).not.toEqual(-1);

  });

  it("should not show pipeline add icon when grouped by environments for admin users", () => {
    const pipelineGroupJSON = dashboardJson._embedded.environments[0];

    const title = helper.find('.dashboard-group_add_pipeline>a').get(0);
    expect(title.href.indexOf(`/go/admin/pipeline/new?group=${pipelineGroupJSON.name}`)).toEqual(-1);
  });

  it("should show disabled pipeline group settings icon showing tooltip for non admin users", () => {
    unmount();
    mount(false);
    expect(helper.find('.dashboard-group_title .edit_config')).toHaveClass('disabled');
    expect(helper.find('.dashboard-group_title .edit_config')).toHaveAttr('data-tooltip-id');
  });

  it("should show pipeline group name as a disabled link for non admin users", () => {
    unmount();
    mount(false);
    const pipelineGroupJSON = dashboardJson._embedded.pipeline_groups[0];

    const title = helper.find('.dashboard-group_title>a').get(0);
    expect(title).toContainText(pipelineGroupJSON.name);
    expect(title).toHaveClass('disabled');
  });


  it("should render pipelines within each pipeline group", () => {
    const pipelineName                      = dashboardJson._embedded.pipeline_groups[0].pipelines[0];
    const pipelinesWithinPipelineGroupCount = dashboardJson._embedded.pipeline_groups[0].pipelines.length;
    const pipelinesWithinPipelineGroup      = helper.find('.dashboard-group .pipeline');

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
        "environments": [
          {
            "_links":         {
              "self": {
                "href": "http://localhost:8153/go/api/admin/environments/e1"
              },
              "doc":  {
                "href": "https://api.gocd.org/current/#environment-config"
              }
            },
            "name":           "e1",
            "pipelines":      ["up42"],
            "can_administer": canAdminister
          }
        ],
        "pipelines":       [
          {
            "_links":                 {
              "self": {
                "href": "http://localhost:8153/go/api/pipelines/up42/history"
              },
              "doc":  {
                "href": "https://api.go.cd/current/#pipelines"
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
                    "self": {
                      "href": "http://localhost:8153/go/api/pipelines/up42/instance/1"
                    },
                    "doc":  {
                      "href": "https://api.go.cd/current/#get-pipeline-instance"
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
              "self": {
                "href": "http://localhost:8153/go/api/pipelines/up43/history"
              },
              "doc":  {
                "href": "https://api.go.cd/current/#pipelines"
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
                    "self": {
                      "href": "http://localhost:8153/go/api/pipelines/up43/instance/1"
                    },
                    "doc":  {
                      "href": "https://api.go.cd/current/#get-pipeline-instance"
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
    helper.mount(() => m(DashboardWidget, {
      personalizeVM,
      showSpinner,
      pluginsSupportingAnalytics: {},
      shouldShowAnalyticsIcon:    false,
      doCancelPolling,
      doRefreshImmediately,
      vm:                         dashboardViewModel
    }));
  }

  function unmount() {
    $("body").off();
    helper.unmount();
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

});
