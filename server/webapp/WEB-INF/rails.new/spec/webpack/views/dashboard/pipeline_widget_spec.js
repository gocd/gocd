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
  const _             = require("lodash");
  const $             = require('jquery');
  const simulateEvent = require('simulate-event');
  require('jasmine-ajax');

  const PipelineWidget = require("views/dashboard/pipeline_widget");
  const Pipelines      = require('models/dashboard/pipelines');
  const DashboardVM    = require("views/dashboard/models/dashboard_view_model");
  const Modal          = require('views/shared/new_modal');
  const pipelineName   = 'up42';


  let $root, root, dashboardViewModel, pipelinesJson, pipeline, doCancelPolling, doRefreshImmediately;
  let pipelineInstances;
  beforeEach(() => {
    jasmine.Ajax.install();
    doCancelPolling      = jasmine.createSpy();
    doRefreshImmediately = jasmine.createSpy();
    [$root, root]        = window.createDomElementForTest();
    pipelineInstances    = [{
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
    }];
  });
  afterEach(() => {
    jasmine.Ajax.uninstall();
    window.destroyDomElementForTest();
  });

  describe("Pipeline Header", () => {
    beforeEach(mount);
    afterEach(unmount);

    it("should render pipeline name", () => {
      expect($root.find('.pipeline_name')).toContainText('up42');
    });

    it("should show pipeline name on hover", () => {
      expect($root.find('.pipeline_name').get(0).title).toEqual('up42');
    });

    it("should link history to pipeline history page", () => {
      expect($root.find('.pipeline_header>div>a')).toContainText('History');
      const expectedPath = `/go/tab/pipeline/history/${pipelinesJson[0].name}`;
      expect($root.find('.pipeline_header>div>a').get(1).href.indexOf(expectedPath)).not.toEqual(-1);
    });

    it("should link to pipeline settings path", () => {
      const expectedPath = pipeline.settingsPath;
      expect($root.find('.edit_config').get(0).href.indexOf(expectedPath)).not.toEqual(-1);
    });

  });


  describe("Pipeline Operations", () => {
    describe("Settings", () => {
      beforeEach(mount);
      afterEach(unmount);

      it("should not disable pipeline settings button for admin users", () => {
        expect($root.find('.edit_config')).not.toHaveClass("disabled");
      });

      it("should disable pipeline settings button for non admin users", () => {
        unmount();
        mount(true, false);
        expect($root.find('.edit_config')).toHaveClass("disabled");
      });

      it("should link to pipeline settings quick edit path when toggles are enabled", () => {
        unmount();
        mount(true);
        const expectedPath = pipeline.quickEditPath;
        expect($root.find('.edit_config').get(0).href.indexOf(expectedPath)).not.toEqual(-1);
      });

      it("should render pipeline settings icon", () => {
        expect($root.find('.edit_config')).toBeInDOM();
      });
    });

    describe("Unpause", () => {
      let pauseInfo;
      beforeEach(() => {
        pauseInfo = {
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

      it('should disable pause button for non admin users', () => {
        unmount();
        mount(false, true, pauseInfo, undefined, false);

        expect($root.find('.unpause')).toHaveClass('disabled');
      });

      it('should add onclick handler for admin users', () => {
        expect(_.isFunction($root.find('.unpause').get(0).onclick)).toBe(true);
      });

      it('should not add onclick handler for non admin users', () => {
        unmount();
        mount(false, true, pauseInfo, undefined, false);

        expect(_.isFunction($root.find('.unpause').get(0).onclick)).toBe(false);
      });

      it("should render the pipeline pause message", () => {
        expect($root.find('.pipeline_pause-message')).toContainText('Paused by admin (under construction)');
      });

      it("should not render null in case of no pipeline pause message", () => {
        unmount();
        pauseInfo = {
          "paused":       true,
          "paused_by":    "admin",
          "pause_reason": null
        };

        mount(false, true, pauseInfo, undefined, false);
        expect($root.find('.pipeline_pause-message')).toContainText('Paused by admin ()');
      });

      it("should not render the pipeline flash message", () => {
        expect($root.find('.pipeline_message')).not.toBeInDOM();
        expect($root.find('.pipeline_message .success')).not.toBeInDOM();
        expect($root.find('.pipeline_message .error')).not.toBeInDOM();
      });

      it("should unpause a pipeline", () => {
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

      it("should show error when unpause a pipeline fails", () => {
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

    describe("pause", () => {
      let pauseInfo, dashboard;
      beforeEach(() => {
        pauseInfo = {
          "paused":       false,
          "paused_by":    "admin",
          "pause_reason": "under construction"
        };

        dashboard        = {};
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

      it('should disable pause button for non admin users', () => {
        unmount();
        mount(false, true, pauseInfo, dashboard, false);

        expect($root.find('.pause')).toHaveClass('disabled');
      });

      it('should add onclick handler for admin users', () => {
        expect(_.isFunction($root.find('.pause').get(0).onclick)).toBe(true);
      });

      it('should not add onclick handler for non admin users', () => {
        unmount();
        mount(false, true, pauseInfo, dashboard, false);

        expect(_.isFunction($root.find('.pause').get(0).onclick)).toBe(false);
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

      it("should not pause a pipeline", () => {
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

      it("should show error when unlocking a pipeline fails", () => {
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

    describe("Trigger With Options", () => {
      beforeEach(mount);

      afterEach(() => {
        unmount();
        Modal.destroyAll();
      });

      it("should render trigger with options pipeline button", () => {
        expect($root.find('.play_with_options')).toBeInDOM();
      });

      it('should disable trigger with options button for non admin users', () => {
        unmount();
        mount(false, true, {}, {}, false, false);

        expect($root.find('.play_with_options')).toHaveClass('disabled');
      });

      it('should disable trigger with options button when first stage is in progress', () => {
        unmount();
        pipelineInstances[0]._embedded.stages[0].status = 'Building';
        mount();

        expect($root.find('.play_with_options')).toHaveClass('disabled');
      });

      it('should not add onclick handler when first stage is in progess', () => {
        unmount();
        pipelineInstances[0]._embedded.stages[0].status = 'Building';
        mount();

        expect(_.isFunction($root.find('.play_with_options').get(0).onclick)).toBe(false);
      });

      it('should disable trigger with options button when pipeline is locked', () => {
        unmount();
        mount(false, true, undefined, {"locked": true});

        expect($root.find('.play_with_options')).toHaveClass('disabled');
      });

      it('should not add onclick handler pipeline is locked', () => {
        unmount();
        mount(false, true, undefined, {"locked": true});

        expect(_.isFunction($root.find('.play_with_options').get(0).onclick)).toBe(false);
      });

      it('should add onclick handler for admin users', () => {
        expect(_.isFunction($root.find('.play_with_options').get(0).onclick)).toBe(true);
      });

      it('should not add onclick handler for non admin users', () => {
        unmount();
        mount(false, true, {}, {}, false, false);

        expect(_.isFunction($root.find('.play_with_options').get(0).onclick)).toBe(false);
      });

      it("should show modal to specify trigger options for a pipeline", () => {
        stubTriggerOptions(pipelineName);
        const triggerWithOptionsButton = $root.find('.play_with_options');

        expect($('.reveal:visible')).not.toBeInDOM();

        simulateEvent.simulate(triggerWithOptionsButton.get(0), 'click');
        m.redraw();

        expect($('.reveal:visible')).toBeInDOM();
      });

      it("should show appropriate header for trigger with options popup modal", () => {
        const pauseButton = $root.find('.play_with_options');

        simulateEvent.simulate(pauseButton.get(0), 'click');
        m.redraw();

        const modalTitle = $('.modal-title:visible');
        expect(modalTitle).toHaveText(`${pipeline.name} - Trigger`);
      });
    });
  });

  describe("Pipeline Instances", () => {
    it("should render pipeline instances", () => {
      mount();

      expect($root.find('.pipeline_instances')).toBeInDOM();
      expect($root.find('.pipeline_instance')).toBeInDOM();

      unmount();
    });

    it('should render no pipeline instance run message for no instance runs of a pipeline', () => {
      pipelineInstances = [];
      mount();

      expect($root.find('.pipeline_instances')).toBeInDOM();

      expect($root.find('.no_instance')).toBeInDOM();
      const pipelineNeverRunMessage = 'You haven\'t run this pipeline yet. Click the play button to run pipeline.';
      expect($root.find('.no_instance')).toContainText(pipelineNeverRunMessage);

      unmount();
    });
  });

  function mount(isQuickEditPageEnabled = false, canAdminister = true, pauseInfo = {}, lockInfo = {}, canPause = true, canOperate = true) {
    pipelinesJson = [{
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
      "can_operate":            canOperate,
      "can_pause":              canPause,
      "pause_info":             pauseInfo,
      "_embedded":              {
        "instances": pipelineInstances
      }
    }];

    pipeline = Pipelines.fromJSON(pipelinesJson).pipelines[pipelineName];

    dashboardViewModel = new DashboardVM();
    dashboardViewModel.initialize([pipelineName]);

    //stub trigger_with_options api call
    pipeline.viewInformationForTriggerWithOptions = () => {
      return Promise.resolve({});
    };

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

  function stubTriggerOptions(pipelineName) {
    jasmine.Ajax.stubRequest(`/go/api/pipelines/${pipelineName}/trigger_options`, undefined, 'GET').andReturn({
      responseText:    JSON.stringify({
        variables: [], materials: [{
          "name":        "https://github.com/ganeshspatil/gocd",
          "fingerprint": "3dcc10e7943de637211a4742342fe456ffbe832577bb377173007499434fd819",
          "revision":    {
            "date":              "2018-02-08T04:32:11Z",
            "user":              "Ganesh S Patil <ganeshpl@thoughtworks.com>",
            "comment":           "Refactor Pipeline Widget (#4311)\n\n* Extract out PipelineHeaderWidget and PipelineOperationsWidget into seperate msx files",
            "last_run_revision": "a2d23c5505ac571d9512bdf08d6287e47dcb52d5"
          }
        }]
      }),
      responseHeaders: {
        'Content-Type': 'application/vnd.go.cd.v1+json'
      },
      status:          200
    });

  }
});
