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
  const Dashboard      = require('models/dashboard/dashboard');
  const DashboardVM    = require("views/dashboard/models/dashboard_view_model");
  const Modal          = require('views/shared/new_modal');
  const pipelineName   = 'up42';

  let $root, root, dashboardViewModel, dashboard, pipelinesJson, dashboardJSON, pipeline, doCancelPolling,
      doRefreshImmediately;
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

    it("should link history to pipeline history page", () => {
      expect($root.find('.pipeline_header>div>a')).toContainText('History');
      const expectedPath = `/go/tab/pipeline/history/${pipelinesJson[0].name}`;
      expect($root.find('.pipeline_header .pipeline_history').get(0).href.indexOf(expectedPath)).not.toEqual(-1);
    });

    it("should link to pipeline settings path", () => {
      const expectedPath = pipeline.settingsPath;
      expect($root.find('.edit_config').get(0).href.indexOf(expectedPath)).not.toEqual(-1);
    });

    it('should disable pipeline settings showing tooltip information for non admin users', () => {
      unmount();
      mount(false, false, {}, {}, true, true);

      expect(pipeline.canAdminister).toBe(false);
      expect($root.find('.edit_config')).toHaveClass('disabled');
      expect($root.find('.edit_config')).toHaveAttr('data-tooltip-id');
    });

    it('should disable pipeline settings for config repo pipelines', () => {
      unmount();
      mount(false, true, {}, {}, true, true, true);

      expect(pipeline.isDefinedInConfigRepo()).toBe(true);
      expect($root.find('.edit_config')).toHaveClass('disabled');
    });
  });

  describe("Pipeline Analytics", () => {
    beforeEach(() => {
      mount(false, false, {}, {}, true, true, false, {"plugin-x": "pipeline_duration"}, true);
    });

    afterEach(() => {
      unmount();
      Modal.destroyAll();
    });

    it("should link to pipeline analytics if there are any", () => {
      expect($root.find('.pipeline-analytics')).toBeInDOM();
    });

    it("should open up a modal when the analytics icon is clicked", () => {
      simulateEvent.simulate($root.find('.pipeline-analytics').get(0), 'click');
      m.redraw();
      expect($('.reveal:visible')).toBeInDOM();
      expect($(".frame-container")).toBeInDOM();
      const modalTitle = $('.modal-title:visible');
      expect(modalTitle).toHaveText("Analytics for pipeline: up42");
    });

    it("should not display the analytics icon if the user is not an admin", () => {
      unmount();
      mount(false, false, {}, {}, true, true, false, {"plugin-x": "pipeline_duration"}, false);
      expect($root.find('.pipeline-analytics')).not.toBeInDOM();
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

        doRefreshImmediately.and.callFake(() => pipeline.isPaused = false);
        simulateEvent.simulate($root.find('.unpause').get(0), 'click');

        expect(doCancelPolling).toHaveBeenCalled();
        expect(doRefreshImmediately).toHaveBeenCalled();

        expect($root.find('.pipeline_pause-message')).not.toBeInDOM();
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

        doRefreshImmediately.and.callFake(() => pipeline.isPaused = true);
        simulateEvent.simulate($('.reveal .primary').get(0), 'click');

        expect(doCancelPolling).toHaveBeenCalled();
        expect(doRefreshImmediately).toHaveBeenCalled();

        expect($root.find('.pipeline_pause-message')).toBeInDOM();
        expect($root.find('.pipeline_message')).toContainText(responseMessage);
        expect($root.find('.pipeline_message')).toHaveClass("success");
      });

      it("should not pause a pipeline", () => {
        const responseMessage = `Pipeline '${pipeline.name}' could not be paused.`;
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

      it("should pause pipeline and close popup when enter is pressed inside the pause popup", () => {
        const responseMessage = `Pipeline '${pipeline.name}' paused successfully.`;
        jasmine.Ajax.stubRequest(`/go/api/pipelines/${pipeline.name}/pause`, undefined, 'POST').andReturn({
          responseText:    JSON.stringify({"message": responseMessage}),
          responseHeaders: {
            'Content-Type': 'application/vnd.go.cd.v1+json'
          },
          status:          200
        });

        simulateEvent.simulate($root.find('.pause').get(0), 'click');
        expect($('.reveal:visible')).toBeInDOM();
        const pausePopupTextBox = $('.reveal input');
        pausePopupTextBox.val("test");
        const keydownEvent = $.Event("keydown");
        keydownEvent.key   = "Enter";
        pausePopupTextBox.trigger(keydownEvent);

        expect($('.reveal:visible')).not.toBeInDOM();
        expect($root.find('.pipeline_message')).toContainText(responseMessage);
        expect($root.find('.pipeline_message')).toHaveClass("success");
      });

      it("should close pause popup when escape is pressed", () => {
        simulateEvent.simulate($root.find('.pause').get(0), 'click');
        expect($('.reveal:visible')).toBeInDOM();
        const keydownEvent = $.Event("keydown");
        keydownEvent.key   = "Escape";
        $('body').trigger(keydownEvent);
        expect($('.reveal:visible')).not.toBeInDOM();
      });

      it("should not retain text entered when the pause popup is closed", () => {
        simulateEvent.simulate($root.find('.pause').get(0), 'click');
        expect($('.reveal:visible')).toBeInDOM();
        let pausePopupTextBox = $('.reveal input');
        pausePopupTextBox.val("test");
        $('.reveal .secondary').trigger('click');
        simulateEvent.simulate($root.find('.pause').get(0), 'click');
        pausePopupTextBox = $('.reveal input');

        expect(pausePopupTextBox).toHaveValue("");
      });

      it("should have tooltip for pause button when it is disabled", () => {
        unmount();
        mount(false, true, pauseInfo, {}, false);
        const pauseButton = $root.find('.pause');
        expect(pauseButton).toHaveAttr('data-tooltip-id');
        const tooltipId = $(pauseButton).attr('data-tooltip-id');
        expect($(`#${tooltipId}`)).toHaveText("You do not have permission to pause the pipeline.");
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

    describe("Trigger", () => {
      beforeEach(mount);

      afterEach(() => {
        unmount();
        Modal.destroyAll();
      });

      it("should render trigger pipeline button", () => {
        expect($root.find('.play')).toBeInDOM();
      });

      it('should disable trigger button for non admin users', () => {
        unmount();
        mount(false, true, {}, {}, false, false);

        expect($root.find('.play')).toHaveClass('disabled');
      });

      it('should disable trigger button when first stage is in progress', () => {
        unmount();
        pipelineInstances[0]._embedded.stages[0].status = 'Building';
        mount();

        expect($root.find('.play')).toHaveClass('disabled');
      });

      it('should not add onclick handler when first stage is in progess', () => {
        unmount();
        pipelineInstances[0]._embedded.stages[0].status = 'Building';
        mount();

        expect(_.isFunction($root.find('.play').get(0).onclick)).toBe(false);
      });

      it('should disable trigger button when pipeline is locked', () => {
        unmount();
        mount(false, true, undefined, {"locked": true});

        expect($root.find('.play')).toHaveClass('disabled');
      });

      it('should disable trigger button when pipeline is paused', () => {
        unmount();
        mount(false, true, {
          "paused":       true,
          "paused_by":    "admin",
          "pause_reason": "under construction"
        });

        expect($root.find('.play')).toHaveClass('disabled');
      });

      it('should not add onclick handler pipeline is locked', () => {
        unmount();
        mount(false, true, undefined, {"locked": true});

        expect(_.isFunction($root.find('.play').get(0).onclick)).toBe(false);
      });

      it('should add onclick handler for admin users', () => {
        expect(_.isFunction($root.find('.play').get(0).onclick)).toBe(true);
      });

      it('should not add onclick handler for non admin users', () => {
        unmount();
        mount(false, true, {}, {}, false, false);

        expect(_.isFunction($root.find('.play').get(0).onclick)).toBe(false);
      });

      it("should trigger a pipeline", () => {
        const responseMessage = `Request for scheduling pipeline '${pipeline.name}' accepted successfully.`;
        jasmine.Ajax.stubRequest(`/go/api/pipelines/${pipeline.name}/schedule`, undefined, 'POST').andReturn({
          responseText:    JSON.stringify({"message": responseMessage}),
          responseHeaders: {
            'Content-Type': 'application/vnd.go.cd.v1+json'
          },
          status:          200
        });

        expect(pipeline.triggerDisabled()).toBe(false);

        simulateEvent.simulate($root.find('.play').get(0), 'click');

        expect(pipeline.triggerDisabled()).toBe(true);

        expect($root.find('.pipeline_message')).toContainText(responseMessage);
        expect($root.find('.pipeline_message')).toHaveClass("success");
      });

      it("should show error when triggering a pipeline fails", () => {
        const responseMessage = `Can not trigger pipeline. Some stages of pipeline are in progress.`;
        jasmine.Ajax.stubRequest(`/go/api/pipelines/${pipeline.name}/schedule`, undefined, 'POST').andReturn({
          responseText:    JSON.stringify({"message": responseMessage}),
          responseHeaders: {
            'Content-Type': 'application/vnd.go.cd.v1+json'
          },
          status:          409
        });

        expect(pipeline.triggerDisabled()).toBe(false);

        simulateEvent.simulate($root.find('.play').get(0), 'click');

        expect(pipeline.triggerDisabled()).toBe(false);

        expect($root.find('.pipeline_message')).toContainText(responseMessage);
        expect($root.find('.pipeline_message')).toHaveClass("error");
      });

      it("should have tooltips for trigger buttons when it is disabled", () => {
        unmount();
        mount(false, true, {}, {}, true, false);
        const playButton = $root.find('.pipeline_operations .play');
        expect(playButton).toHaveAttr('data-tooltip-id');
        const tooltipId = $(playButton).attr('data-tooltip-id');
        expect($(`#${tooltipId}`)).toHaveText("You do not have permission to trigger the pipeline");
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
        const pipelineButton = $root.find('.button .pipeline_btn');
        expect(pipelineButton).toHaveAttr('title');
        const tooltipId = $(pipelineButton).attr('title');
        expect($(`#${tooltipId}`)).toHaveText("Pipeline Paused");
        expect($root.find('.play_with_options')).toHaveClass('disabled');
      });

      it('should disable trigger with options button when pipeline is paused', () => {
        unmount();
        mount(false, true, {
          "paused":       true,
          "paused_by":    "admin",
          "pause_reason": "under construction"
        });
const pipelineButton = $root.find('.button .pipeline_btn');
expect(pipelineButton).toHaveAttr('title');
const tooltipId = $(pipelineButton).attr('title');
expect($(`#${tooltipId}`)).toHaveText("Pipeline Paused");
        expect($root.find('.play_with_options')).toHaveClass('disabled');
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

      it('should render error message in modal when api response parsing fails', () => {
        stubTriggerOptionsWithInvalidData(pipelineName);
        const triggerWithOptionsButton = $root.find('.play_with_options');

        expect($('.reveal:visible')).not.toBeInDOM();

        simulateEvent.simulate(triggerWithOptionsButton.get(0), 'click');
        m.redraw();

        expect($('.reveal:visible')).toBeInDOM();
        expect($('.callout.alert')).toBeInDOM();
      });

      it("should show appropriate header for trigger with options popup modal", () => {
        const pauseButton = $root.find('.play_with_options');

        simulateEvent.simulate(pauseButton.get(0), 'click');
        m.redraw();

        const modalTitle = $('.modal-title:visible');
        expect(modalTitle).toHaveText(`${pipeline.name} - Trigger`);
      });

      it('should show modal appropriately when opened and closed multiple times', () => {
        stubTriggerOptions(pipelineName);
        const triggerWithOptionsButton = $root.find('.play_with_options');

        //open trigger with options modal
        expect($('.reveal:visible')).not.toBeInDOM();
        expect($('.pipeline_options-heading')).not.toContainText('Materials');

        simulateEvent.simulate(triggerWithOptionsButton.get(0), 'click');
        m.redraw();

        expect($('.reveal:visible')).toBeInDOM();
        expect($('.pipeline_options-heading')).toContainText('Materials');

        //close trigger with options modal
        $('.modal-buttons .button.save.secondary').click();

        //open again trigger with options modal
        expect($('.reveal:visible')).not.toBeInDOM();
        expect($('.pipeline_options-heading')).not.toContainText('Materials');

        simulateEvent.simulate(triggerWithOptionsButton.get(0), 'click');
        m.redraw();

        expect($('.reveal:visible')).toBeInDOM();
        expect($('.pipeline_options-heading')).toContainText('Materials');
      });

      it("should trigger a pipeline", () => {
        stubTriggerOptions(pipelineName);
        const responseMessage = `Request for scheduling pipeline '${pipeline.name}' accepted successfully.`;
        jasmine.Ajax.stubRequest(`/go/api/pipelines/${pipeline.name}/schedule`, undefined, 'POST').andReturn({
          responseText:    JSON.stringify({"message": responseMessage}),
          responseHeaders: {
            'Content-Type': 'application/vnd.go.cd.v1+json'
          },
          status:          200
        });

        expect(pipeline.triggerDisabled()).toBe(false);

        simulateEvent.simulate($root.find('.play_with_options').get(0), 'click');
        m.redraw();

        $('.modal-buttons .button.save.primary').click();

        expect(pipeline.triggerDisabled()).toBe(true);

        expect($root.find('.pipeline_message')).toContainText(responseMessage);
        expect($root.find('.pipeline_message')).toHaveClass("success");
      });

      it("should show error when triggering a pipeline fails", () => {
        stubTriggerOptions(pipelineName);
        const responseMessage = `Can not trigger pipeline. Some stages of pipeline are in progress.`;
        jasmine.Ajax.stubRequest(`/go/api/pipelines/${pipeline.name}/schedule`, undefined, 'POST').andReturn({
          responseText:    JSON.stringify({"message": responseMessage}),
          responseHeaders: {
            'Content-Type': 'application/vnd.go.cd.v1+json'
          },
          status:          409
        });

        expect(pipeline.triggerDisabled()).toBe(false);

        simulateEvent.simulate($root.find('.play_with_options').get(0), 'click');
        m.redraw();

        $('.modal-buttons .button.save.primary').click();

        expect(pipeline.triggerDisabled()).toBe(false);

        expect($root.find('.pipeline_message')).toContainText(responseMessage);
        expect($root.find('.pipeline_message')).toHaveClass("error");
      });

      it("should have tooltips when it is disabled", () => {
        unmount();
        mount(false, true, {}, {}, true, false);
        const playButton = $root.find('.pipeline_operations .play_with_options');
        expect(playButton).toHaveAttr('data-tooltip-id');
        const tooltipId = $(playButton).attr('data-tooltip-id');
        expect($(`#${tooltipId}`)).toHaveText("You do not have permission to trigger the pipeline");
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

  function mount(isQuickEditPageEnabled = false, canAdminister = true, pauseInfo = {}, lockInfo = {}, canPause = true, canOperate = true, fromConfigRepo = false, pluginsSupportingAnalytics = {}, shouldShowAnalyticsIcon = false) {
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
      "from_config_repo":       fromConfigRepo,
      "_embedded":              {
        "instances": pipelineInstances
      }
    }];

    dashboardJSON = {
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
            "pipelines":      ["up42"],
            "can_administer": true
          }
        ],
        "pipelines":       pipelinesJson
      }
    };

    pipeline = Pipelines.fromJSON(pipelinesJson).pipelines[pipelineName];

    dashboardViewModel = new DashboardVM();
    dashboard          = new Dashboard();
    dashboard.initialize(dashboardJSON);

    //stub trigger_with_options api call
    pipeline.viewInformationForTriggerWithOptions = () => {
      return Promise.resolve({});
    };

    m.mount(root, {
      view() {
        return m(PipelineWidget, {
          pipeline,
          isQuickEditPageEnabled,
          pluginsSupportingAnalytics,
          shouldShowAnalyticsIcon,
          doCancelPolling,
          doRefreshImmediately,
          operationMessages: dashboardViewModel.operationMessages,
          dropdown: dashboardViewModel.dropdown,
          buildCause: dashboardViewModel.buildCause
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

  function stubTriggerOptionsWithInvalidData(pipelineName) {
    const invalidJsonResponse = "{ : { : There was an unknown error. Please check the server logs for more information.}}";
    jasmine.Ajax.stubRequest(`/go/api/pipelines/${pipelineName}/trigger_options`, undefined, 'GET').andReturn({
      responseText:    invalidJsonResponse,
      responseHeaders: {
        'Content-Type': 'application/vnd.go.cd.v1+json'
      },
      status:          200
    });
  }

});
