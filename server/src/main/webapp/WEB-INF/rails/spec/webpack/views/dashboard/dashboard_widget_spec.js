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
import {SparkRoutes} from "helpers/spark_routes";
import {TestHelper} from "views/pages/spec/test_helper";
import css from "views/components/buttons/index.scss";
import {asSelector} from "helpers/css_proxies";
import {Personalization} from "models/dashboard/personalization";
import {PersonalizationVM} from "views/dashboard/models/personalization_vm";
import {Modal} from "views/shared/new_modal";
import {DashboardViewModel as DashboardVM} from "views/dashboard/models/dashboard_view_model";
import {Dashboard} from "models/dashboard/dashboard";
import {DashboardWidget} from "views/dashboard/dashboard_widget";
import _ from "lodash";
import $ from "jquery";
import Stream from "mithril/stream";
import m from "mithril";

describe("Dashboard Widget", () => {

  const sel = asSelector(css);
  const body = document.body;

  let dashboard, dashboardJson, buildCauseJson, doCancelPolling, doRefreshImmediately, vm;
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
    expect(helper.byTestId('search-box')).toBeInDOM();
  });

  it("should show spinner before dashboard is loaded", () => {
    unmount();
    const pageSpinner = Stream(true);
    mount(true, pageSpinner);
    expect(helper.q(".page-spinner")).toBeInDOM();

    pageSpinner(false);
    unmount();
    mount(true, pageSpinner);
    expect(helper.q(".page-spinner")).toBeFalsy();
  });

  it('should render an info message', () => {
    expect(helper.q('.dashboard-message')).toBeFalsy();
    dashboard.message({content: 'some message', type: 'info'});
    helper.redraw();

    expect(helper.text('.callout.info .dashboard-message')).toContain('some message');
  });

  it('should render an alert message', () => {
    expect(helper.q('.dashboard-message')).toBeFalsy();
    dashboard.message({content: 'some error message', type: 'alert'});
    helper.redraw();

    expect(helper.text('.callout.alert .dashboard-message')).toContain('some error message');
  });

  it("should search for a pipeline", () => {
    const searchField        = helper.byTestId('search-box');
    let pipelinesCountOnPage = helper.qa('.pipeline');
    expect(pipelinesCountOnPage).toHaveLength(2);

    helper.oninput(searchField, "foo");

    pipelinesCountOnPage = helper.qa('.pipeline');
    expect(pipelinesCountOnPage).toHaveLength(0);

    helper.oninput(searchField, "up42");

    pipelinesCountOnPage = helper.qa('.pipeline');
    expect(pipelinesCountOnPage).toHaveLength(1);

    helper.oninput(searchField, "up43");

    pipelinesCountOnPage = helper.qa('.pipeline');
    expect(pipelinesCountOnPage).toHaveLength(1);

    helper.oninput(searchField, "UP43");

    pipelinesCountOnPage = helper.qa('.pipeline');
    expect(pipelinesCountOnPage).toHaveLength(1);

    helper.oninput(searchField, "up");

    pipelinesCountOnPage = helper.qa('.pipeline');
    expect(pipelinesCountOnPage).toHaveLength(2);

    helper.oninput(searchField, "");

    pipelinesCountOnPage = helper.qa('.pipeline');
    expect(pipelinesCountOnPage).toHaveLength(2);
  });

  it("should show appropriate modal for pausing a searched pipeline", () => {
    const searchField = helper.byTestId('search-box');
    expect(helper.qa('.pipeline')).toHaveLength(2);

    helper.oninput(searchField, "up42");

    expect(helper.qa('.pipeline')).toHaveLength(1);

    helper.click('.pause');

    expect(helper.text('.modal-body', body)).toBe('Specify a reason for pausing schedule on pipeline up42');

    //close specify pause cause popup for up42 pipeline
    Modal.destroyAll();

    helper.oninput(searchField, "up43");
    m.redraw.sync();

    expect(helper.qa('.pipeline')).toHaveLength(1);

    helper.click('.pause');
    expect(helper.text('.modal-body', body)).toBe('Specify a reason for pausing schedule on pipeline up43');

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

      helper.click('.pause');

      expect(helper.text('.modal-body', body)).toBe('Specify a reason for pausing schedule on pipeline up42');
      helper.q('.reveal input', body).value = pauseCause;
      helper.click('.reveal .primary', body);

      expect(helper.text('.pipeline_message')).toContain(responseMessage);
      expect(helper.q('.pipeline_message')).toHaveClass("success");

      helper.click('.pause');

      expect(helper.q('.reveal input', body)).toHaveValue('');

      Modal.destroyAll();
    });
  });

  it("should show changes popup for a searched pipeline", () => {
    stubBuildCauseAjaxCall();

    const searchField = helper.byTestId('search-box');
    expect(helper.qa('.pipeline')).toHaveLength(2);

    helper.oninput(searchField, "up42");

    expect(helper.qa('.pipeline')).toHaveLength(1);
    expect(helper.q('.material_changes')).toBeFalsy();

    jasmine.Ajax.withMock(() => {
      jasmine.Ajax.stubRequest(SparkRoutes.buildCausePath('up42', '1'), undefined, 'GET').andReturn({
        responseText:    JSON.stringify(buildCauseJson),
        responseHeaders: {'Content-Type': 'application/vnd.go.cd.v1+json'},
        status:          200
      });

      helper.click(helper.qa('.info a').item(1));
    });

    expect(helper.q('.material_changes')).toBeInDOM();

    helper.oninput(searchField, "up43");

    expect(helper.qa('.pipeline')).toHaveLength(1);
    expect(helper.q('.material_changes')).toBeFalsy();

    jasmine.Ajax.withMock(() => {
      jasmine.Ajax.stubRequest(SparkRoutes.buildCausePath('up42', '1'), undefined, 'GET').andReturn({
        responseText:    JSON.stringify(buildCauseJson),
        responseHeaders: {'Content-Type': 'application/vnd.go.cd.v1+json'},
        status:          200
      });

      helper.click(helper.qa('.info a').item(1));
    });

    expect(helper.q('.material_changes')).toBeInDOM();
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

      const searchField = helper.byTestId('search-box');
      expect(helper.qa('.pipeline')).toHaveLength(2);

      helper.oninput(searchField, "up43");

      expect(helper.qa('.pipeline')).toHaveLength(1);

      expect(doCancelPolling).not.toHaveBeenCalled();
      expect(doRefreshImmediately).not.toHaveBeenCalled();

      helper.click('.pipeline_locked');

      expect(doCancelPolling).toHaveBeenCalled();
      expect(doRefreshImmediately).toHaveBeenCalled();

      expect(helper.text('.pipeline_message')).toContain(responseMessage);
      expect(helper.q('.pipeline_message')).toHaveClass("success");
    });
  });

  it("should render pipeline groups", () => {
    const pipelineGroupsCount = dashboardJson._embedded.pipeline_groups.length;
    const pipelineGroups      = helper.qa('.dashboard-group');

    expect(pipelineGroups).toHaveLength(pipelineGroupsCount);
    expect(pipelineGroups.item(0)).toContainText(dashboardJson._embedded.pipeline_groups[0].name);
  });

  it("should render pipeline group title", () => {
    expect(helper.text('.dashboard-group_title a')).toContain(dashboardJson._embedded.pipeline_groups[0].name);
  });

  it("should show pipeline group name which links to pipeline group index page for admin users", () => {
    const pipelineGroupJSON = dashboardJson._embedded.pipeline_groups[0];

    const title = helper.q('.dashboard-group_title>a');
    expect(title.href.indexOf(`/go/admin/pipelines/#!${pipelineGroupJSON.name}`)).not.toEqual(-1);
  });

  it("should show pipeline group icon which links to pipeline group settings page for admin users", () => {
    const pipelineGroupJSON = dashboardJson._embedded.pipeline_groups[0];

    const title = helper.qa('.dashboard-group_title>a').item(1);
    expect(title.href.indexOf(`/go/admin/pipelines/#!${pipelineGroupJSON.name}/edit`)).not.toEqual(-1);
  });

  it("should show pipeline add icon when grouped by pipeline group for admin users", () => {
    expect(helper.q(sel.btnPrimary, helper.q(".dashboard-group_title"))).toBeInDOM();
  });

  it("should not show pipeline add icon when grouped by environments for admin users", () => {
    vm.groupByEnvironment(true);
    m.redraw.sync();
    expect(helper.q(sel.btnPrimary, helper.q(".dashboard-group_title"))).toBeFalsy();
  });

  it("should show disabled pipeline group settings icon showing tooltip for non admin users", () => {
    unmount();
    mount(false);
    expect(helper.q('.dashboard-group_title .edit_config')).toHaveClass('disabled');
    expect(helper.q('.dashboard-group_title .edit_config')).toHaveAttr('data-tooltip-id');
  });

  it("should show pipeline group name as a disabled link for non admin users", () => {
    unmount();
    mount(false);
    const pipelineGroupJSON = dashboardJson._embedded.pipeline_groups[0];

    const title = helper.q('.dashboard-group_title>a');
    expect(title).toContainText(pipelineGroupJSON.name);
    expect(title).toHaveClass('disabled');
  });


  it("should render pipelines within each pipeline group", () => {
    const pipelineName                      = dashboardJson._embedded.pipeline_groups[0].pipelines[0];
    const pipelinesWithinPipelineGroupCount = dashboardJson._embedded.pipeline_groups[0].pipelines.length;
    const pipelinesWithinPipelineGroup      = helper.qa('.dashboard-group .pipeline');

    expect(pipelinesWithinPipelineGroup).toHaveLength(pipelinesWithinPipelineGroupCount);
    expect(helper.textAll(pipelinesWithinPipelineGroup).join(" ")).toContain(pipelineName);
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
        "environments":    [
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
            "template_info": {
              "is_using_template": false,
              "template_name":     null
            },
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
            "template_info": {
              "is_using_template": false,
              "template_name":     null
            },
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
    const personalizeVM = new PersonalizationVM(Stream("Default"));
    personalizeVM.model(new Personalization([{name: "Default", state: []}], []));
    dashboard.initialize(dashboardJson);

    vm                 = new DashboardVM(dashboard);
    vm._performRouting = _.noop;
    helper.mount(() => m(DashboardWidget, {
      personalizeVM,
      showSpinner,
      pluginsSupportingAnalytics: {},
      shouldShowAnalyticsIcon:    false,
      doCancelPolling,
      doRefreshImmediately,
      vm
    }));
  }

  function unmount() {
    $(body).off();
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
