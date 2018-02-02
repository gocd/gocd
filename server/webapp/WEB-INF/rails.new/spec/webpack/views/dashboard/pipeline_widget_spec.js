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
describe("Dashboard Pipeline Widget", () => {
  const m             = require("mithril");
  const $             = require('jquery');
  const simulateEvent = require('simulate-event');

  const PipelineWidget = require("views/dashboard/pipeline_widget");
  const Pipelines      = require('models/dashboard/pipelines');
  const DashboardVM    = require("views/dashboard/models/dashboard_view_model");
  const Modal          = require('views/shared/new_modal');


  let $root, root, dashboardViewModel, pipelinesJson, pipeline, doCancelPolling, doRefreshImmediately;

  beforeEach(() => {
    doCancelPolling      = jasmine.createSpy();
    doRefreshImmediately = jasmine.createSpy();
    [$root, root]        = window.createDomElementForTest();
  });
  afterEach(window.destroyDomElementForTest);

  describe("Pipeline Header", () => {
    beforeEach(mount);
    afterEach(unmount);

    it("should render pipeline name", () => {
      expect($root.find('.pipeline_name')).toContainText('up42');
    });

    it("should link history to pipeline history page", () => {
      expect($root.find('.pipeline_header a')).toContainText('History');
      const expectedPath = `/go/tab/pipeline/history/${pipelinesJson[0].name}`;
      expect($root.find('.pipeline_header a').get(0).href.indexOf(expectedPath)).not.toEqual(-1);
    });

    it("should link to pipeline settings path", () => {
      const expectedPath = pipeline.settingsPath;
      expect($root.find('.pipeline_edit').get(0).href.indexOf(expectedPath)).not.toEqual(-1);
    });

  });


  describe("Pipeline Operations", () => {
    describe("Settings", () => {
      beforeEach(mount);
      afterEach(unmount);

      it("should not disable pipeline settings button for admin users", () => {
        expect($root.find('.pipeline_edit')).not.toHaveClass("disabled");
      });

      it("should disable pipeline settings button for non admin users", () => {
        unmount();
        mount(true, false);
        expect($root.find('.pipeline_edit')).toHaveClass("disabled");
      });

      it("should link to pipeline settings quick edit path when toggles are enabled", () => {
        unmount();
        mount(true);
        const expectedPath = pipeline.quickEditPath;
        expect($root.find('.pipeline_edit').get(0).href.indexOf(expectedPath)).not.toEqual(-1);
      });

      it("should render pipeline settings icon", () => {
        expect($root.find('.pipeline_edit')).toBeInDOM();
      });
    });

    describe("Unpause", () => {
      beforeEach(() => {
        const pauseInfo = {
          "paused":       true,
          "paused_by":    "admin",
          "pause_reason": "under construction"
        };

        mount(false, true, pauseInfo);
      });

      afterEach(unmount);

      it("should render unpause pipeline button", () => {
        expect($root.find('.unpause')).toBeInDOM();
      });

      it("should render the pipeline pause message", () => {
        expect($root.find('.pipeline_pause-message')).toContainText('Paused by admin (under construction)');
      });

      it("should not render the pipeline flash message", () => {
        expect($root.find('.pipeline_message')).not.toBeInDOM();
        expect($root.find('.pipeline_message .success')).not.toBeInDOM();
        expect($root.find('.pipeline_message .error')).not.toBeInDOM();
      });

      it("should unpause a pipeline", () => {
        jasmine.Ajax.withMock(() => {
          const responseMessage = `Pipeline '${pipeline.name}' unpaused successfully.`;
          jasmine.Ajax.stubRequest(`/go/api/pipelines/${pipeline.name}/unpause`, undefined, 'POST').andReturn({
            responseText:    JSON.stringify({"message": responseMessage}),
            responseHeaders: {
              'Content-Type': 'application/vnd.go.cd.v1+json'
            },
            status:          200
          });

          expect(doCancelPolling).not.toHaveBeenCalled();
          expect(doRefreshImmediately).not.toHaveBeenCalled();

          simulateEvent.simulate($root.find('.unpause').get(0), 'click');

          expect(doCancelPolling).toHaveBeenCalled();
          expect(doRefreshImmediately).toHaveBeenCalled();

          expect($root.find('.pipeline_message')).toContainText(responseMessage);
          expect($root.find('.pipeline_message')).toHaveClass("success");
        });
      });

      it("should show error when unpause a pipeline fails", () => {
        jasmine.Ajax.withMock(() => {
          const responseMessage = `Can not unpuase pipeline. Pipeline '${pipeline.name}' is already unpaused.`;
          jasmine.Ajax.stubRequest(`/go/api/pipelines/${pipeline.name}/unpause`, undefined, 'POST').andReturn({
            responseText:    JSON.stringify({"message": responseMessage}),
            responseHeaders: {
              'Content-Type': 'application/vnd.go.cd.v1+json'
            },
            status:          409
          });

          expect(doCancelPolling).not.toHaveBeenCalled();
          expect(doRefreshImmediately).not.toHaveBeenCalled();

          simulateEvent.simulate($root.find('.unpause').get(0), 'click');

          expect(doCancelPolling).toHaveBeenCalled();
          expect(doRefreshImmediately).toHaveBeenCalled();

          expect($root.find('.pipeline_message')).toContainText(responseMessage);
          expect($root.find('.pipeline_message')).toHaveClass("error");
        });
      });
    });

    describe("pause", () => {
      beforeEach(() => {
        const pauseInfo = {
          "paused":       false,
          "paused_by":    "admin",
          "pause_reason": "under construction"
        };

        const dashboard  = {};
        dashboard.reload = jasmine.createSpy();
        mount(false, true, pauseInfo, dashboard);
      });

      afterEach(() => {
        unmount();
        Modal.destroyAll();
      });

      it("should render pause pipeline button", () => {
        expect($root.find('.pause')).toBeInDOM();
      });

      it("should show modal to specify pause reason upon pausing a pipeline", () => {
        const pauseButton = $root.find('.pause');

        expect($('.reveal:visible')).not.toBeInDOM();

        simulateEvent.simulate(pauseButton.get(0), 'click');
        m.redraw();

        expect($('.reveal:visible')).toBeInDOM();
      });

      it("should show appropriate header for popup modal upon pause button click", () => {
        const pauseButton = $root.find('.pause');

        simulateEvent.simulate(pauseButton.get(0), 'click');
        m.redraw();

        const modalTitle = $('.modal-title:visible');
        expect(modalTitle).toHaveText(`Pause pipeline ${pipeline.name}`);
      });

      it("should pause a pipeline", () => {
        jasmine.Ajax.withMock(() => {
          const responseMessage = `Pipeline '${pipeline.name}' paused successfully.`;
          jasmine.Ajax.stubRequest(`/go/api/pipelines/${pipeline.name}/pause`, undefined, 'POST').andReturn({
            responseText:    JSON.stringify({"message": responseMessage}),
            responseHeaders: {
              'Content-Type': 'application/vnd.go.cd.v1+json'
            },
            status:          200
          });

          expect(doCancelPolling).not.toHaveBeenCalled();
          expect(doRefreshImmediately).not.toHaveBeenCalled();

          simulateEvent.simulate($root.find('.pause').get(0), 'click');
          $('.reveal input').val("test");
          simulateEvent.simulate($('.reveal .primary').get(0), 'click');

          expect(doCancelPolling).toHaveBeenCalled();
          expect(doRefreshImmediately).toHaveBeenCalled();

          expect($root.find('.pipeline_message')).toContainText(responseMessage);
          expect($root.find('.pipeline_message')).toHaveClass("success");
        });
      });

      it("should not pause a pipeline", () => {
        jasmine.Ajax.withMock(() => {
          const responseMessage = `Pipeline '${pipeline.name}' paused successfully.`;
          jasmine.Ajax.stubRequest(`/go/api/pipelines/${pipeline.name}/pause`, undefined, 'POST').andReturn({
            responseText:    JSON.stringify({"message": responseMessage}),
            responseHeaders: {
              'Content-Type': 'application/vnd.go.cd.v1+json'
            },
            status:          409
          });
          expect(doCancelPolling).not.toHaveBeenCalled();
          expect(doRefreshImmediately).not.toHaveBeenCalled();

          simulateEvent.simulate($root.find('.pause').get(0), 'click');
          $('.reveal input').val("test");
          simulateEvent.simulate($('.reveal .primary').get(0), 'click');

          expect(doCancelPolling).toHaveBeenCalled();
          expect(doRefreshImmediately).toHaveBeenCalled();

          expect($root.find('.pipeline_message')).toContainText(responseMessage);
          expect($root.find('.pipeline_message')).toHaveClass("error");
        });
      });

    });

    describe("Unlock", () => {
      beforeEach(() => {
        const lockInfo = {
          "canUnlock": true,
          "locked":    true
        };
        mount(false, true, undefined, lockInfo);
      });

      afterEach(unmount);

      it("should render unlock pipeline button", () => {
        expect($root.find('.pipeline_locked')).toBeInDOM();
      });

      it("should enable unlock pipeline button when user can unlock a pipeline", () => {
        expect($root.find('.pipeline_locked')).not.toHaveClass('disabled');
      });

      it("should disable unlock pipeline button when user can not unlock a pipeline", () => {
        unmount();
        const lockInfo = {
          "canUnlock": false,
          "locked":    true
        };

        mount(false, true, undefined, lockInfo);
        expect($root.find('.pipeline_locked')).toHaveClass('disabled');
      });

      it("should not render the pipeline flash message", () => {
        expect($root.find('.pipeline_message')).not.toBeInDOM();
        expect($root.find('.pipeline_message .success')).not.toBeInDOM();
        expect($root.find('.pipeline_message .error')).not.toBeInDOM();
      });

      it("should unlock a pipeline", () => {
        jasmine.Ajax.withMock(() => {
          const responseMessage = `Pipeline '${pipeline.name}' unlocked successfully.`;
          jasmine.Ajax.stubRequest(`/go/api/pipelines/${pipeline.name}/unlock`, undefined, 'POST').andReturn({
            responseText:    JSON.stringify({"message": responseMessage}),
            responseHeaders: {
              'Content-Type': 'application/vnd.go.cd.v1+json'
            },
            status:          200
          });

          expect(doCancelPolling).not.toHaveBeenCalled();
          expect(doRefreshImmediately).not.toHaveBeenCalled();

          simulateEvent.simulate($root.find('.pipeline_locked').get(0), 'click');

          expect(doCancelPolling).toHaveBeenCalled();
          expect(doRefreshImmediately).toHaveBeenCalled();

          expect($root.find('.pipeline_message')).toContainText(responseMessage);
          expect($root.find('.pipeline_message')).toHaveClass("success");
        });
      });

      it("should show error when unlocking a pipeline fails", () => {
        jasmine.Ajax.withMock(() => {
          const responseMessage = `Can not unlock pipeline. Some stages of pipeline are in progress.`;
          jasmine.Ajax.stubRequest(`/go/api/pipelines/${pipeline.name}/unlock`, undefined, 'POST').andReturn({
            responseText:    JSON.stringify({"message": responseMessage}),
            responseHeaders: {
              'Content-Type': 'application/vnd.go.cd.v1+json'
            },
            status:          409
          });

          expect(doCancelPolling).not.toHaveBeenCalled();
          expect(doRefreshImmediately).not.toHaveBeenCalled();

          simulateEvent.simulate($root.find('.pipeline_locked').get(0), 'click');

          expect(doCancelPolling).toHaveBeenCalled();
          expect(doRefreshImmediately).toHaveBeenCalled();

          expect($root.find('.pipeline_message')).toContainText(responseMessage);
          expect($root.find('.pipeline_message')).toHaveClass("error");
        });
      });
    });
  });


  describe("Pipeline Instances", () => {
    beforeEach(mount);
    afterEach(unmount);

    it("should render pipeline instances", () => {
      expect($root.find('.pipeline_instances')).toBeInDOM();
    });
  });

  function mount(isQuickEditPageEnabled = false, canAdminister = true, pauseInfo = {}, lockInfo = {}) {
    const pipelineName = 'up42';
    pipelinesJson      = [{
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
      "name":                   pipelineName,
      "last_updated_timestamp": 1510299695473,
      "locked":                 lockInfo.locked,
      "can_unlock":             lockInfo.canUnlock,
      "can_administer":         canAdminister,
      "pause_info":             pauseInfo,
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
    }];

    pipeline = Pipelines.fromJSON(pipelinesJson).pipelines[pipelineName];

    dashboardViewModel = new DashboardVM();
    dashboardViewModel.initialize([pipelineName]);

    m.mount(root, {
      view() {
        return m(PipelineWidget, {
          pipeline,
          isQuickEditPageEnabled,
          doCancelPolling,
          doRefreshImmediately,
          vm: dashboardViewModel
        });
      }
    });
    m.redraw(true);
  }

  function unmount() {
    m.mount(root, null);
    m.redraw();
  }
});
