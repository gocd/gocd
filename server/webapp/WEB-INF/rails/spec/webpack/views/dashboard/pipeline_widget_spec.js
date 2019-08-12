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
import {TestHelper} from "views/pages/spec/test_helper";
import {Modal} from "views/shared/new_modal";
import {DashboardViewModel as DashboardVM} from "views/dashboard/models/dashboard_view_model";
import {Dashboard} from "models/dashboard/dashboard";
import {Pipelines} from "models/dashboard/pipelines";
import {PipelineWidget} from "views/dashboard/pipeline_widget";
import {timeFormatter} from "helpers/time_formatter";
import simulateEvent from "simulate-event";
import $ from "jquery";
import _ from "lodash";
import m from "mithril";
import "jasmine-ajax";

describe("Dashboard Pipeline Widget", () => {

  const pipelineName   = 'up42';

  let dashboardViewModel, dashboard, pipelinesJson, dashboardJSON, pipeline, doCancelPolling,
      doRefreshImmediately;
  let pipelineInstances;
  const helper = new TestHelper();

  beforeEach(() => {
    jasmine.Ajax.install();
    doCancelPolling      = jasmine.createSpy();
    doRefreshImmediately = jasmine.createSpy();
    pipelineInstances    = [{
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
    }];
  });

  afterEach(() => {
    jasmine.Ajax.uninstall();
  });

  describe("Pipeline Header", () => {
    beforeEach(mount);
    afterEach(helper.unmount.bind(helper));

    it("should render pipeline name", () => {
      expect(helper.find('.pipeline_name')).toContainText('up42');
    });

    it("should link history to pipeline history page", () => {
      expect(helper.find('.pipeline_header>div>a')).toContainText('History');
      const expectedPath = `/go/tab/pipeline/history/${pipelinesJson[0].name}`;
      expect(helper.find('.pipeline_header .pipeline_history').get(0).href.indexOf(expectedPath)).not.toEqual(-1);
    });

    it("should link to pipeline settings path", () => {
      const expectedPath = pipeline.settingsPath;
      expect(helper.find('.edit_config').get(0).href.indexOf(expectedPath)).not.toEqual(-1);
    });

    it('should disable pipeline settings showing tooltip information for non admin users', () => {
      helper.unmount();
      mount(false, {}, {}, true, true);

      expect(pipeline.canAdminister).toBe(false);
      expect(helper.find('.edit_config')).toHaveClass('disabled');
      expect(helper.find('.edit_config')).toHaveAttr('data-tooltip-id');
    });

    it('should disable pipeline settings for config repo pipelines', () => {
      helper.unmount();
      mount(true, {}, {}, true, true, true);

      expect(pipeline.isDefinedInConfigRepo()).toBe(true);
      expect(helper.find('.edit_config')).toHaveClass('disabled');
    });
  });

  describe("Pipeline Analytics", () => {
    beforeEach(() => {
      mount(false, {}, {}, true, true, false, {"plugin-x": "pipeline_duration"}, true);
    });

    afterEach(helper.unmount.bind(helper));

    afterEach(() => {
      Modal.destroyAll();
    });

    it("should link to pipeline analytics if there are any", () => {
      expect(helper.find('.pipeline-analytics')).toBeInDOM();
    });

    it("should open up a modal when the analytics icon is clicked", () => {
      helper.click('.pipeline-analytics');
      expect($('.reveal:visible')).toBeInDOM();
      expect($(".frame-container")).toBeInDOM();
      const modalTitle = $('.modal-title:visible');
      expect(modalTitle).toHaveText("Analytics for pipeline: up42");
    });

    it("should not display the analytics icon if the user is not an admin", () => {
      helper.unmount();
      mount(false, {}, {}, true, true, false, {"plugin-x": "pipeline_duration"}, false);
      expect(helper.find('.pipeline-analytics')).not.toBeInDOM();
    });
  });

  describe("Pipeline Operations", () => {
    describe("Settings", () => {
      beforeEach(mount);
      afterEach(() => {
        helper.unmount();
      });

      it("should not disable pipeline settings button for admin users", () => {
        expect(helper.find('.edit_config')).not.toHaveClass("disabled");
      });

      it("should disable pipeline settings button for non admin users", () => {
        helper.unmount();
        mount(false);
        expect(helper.find('.edit_config')).toHaveClass("disabled");
      });

      it("should render pipeline settings icon", () => {
        expect(helper.find('.edit_config')).toBeInDOM();
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

        mount(true, pauseInfo);
      });

      afterEach(() => {
        helper.unmount();
      });

      it("should render unpause pipeline button", () => {
        const pauseButton = helper.find('.unpause');
        expect(pauseButton).toBeInDOM();
        expect($(pauseButton).attr('title')).toBe("Pipeline Paused");
      });

      it('should disable pause button for non admin users', () => {
        helper.unmount();
        mount(true, pauseInfo, undefined, false);

        expect(helper.find('.unpause')).toHaveClass('disabled');
      });

      it('should add onclick handler for admin users', () => {
        expect(_.isFunction(helper.find('.unpause').get(0).onclick)).toBe(true);
      });

      it('should not add onclick handler for non admin users', () => {
        helper.unmount();
        mount(true, pauseInfo, undefined, false);

        expect(_.isFunction(helper.find('.unpause').get(0).onclick)).toBe(false);
      });

      it("should render the pipeline pause message", () => {
        expect(helper.find('.pipeline_pause-message')).toContainText('Paused by admin (under construction)');
      });

      it("should not render null in case of no pipeline pause message", () => {
        helper.unmount();
        pauseInfo = {
          "paused":       true,
          "paused_by":    "admin",
          "pause_reason": null
        };

        mount(true, pauseInfo, undefined, false);
        expect(helper.find('.pipeline_pause-message')).toContainText('Paused by admin ()');
      });

      it("should not render the pipeline flash message", () => {
        expect(helper.find('.pipeline_message')).not.toBeInDOM();
        expect(helper.find('.pipeline_message .success')).not.toBeInDOM();
        expect(helper.find('.pipeline_message .error')).not.toBeInDOM();
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
        helper.click('.unpause');

        expect(doCancelPolling).toHaveBeenCalled();
        expect(doRefreshImmediately).toHaveBeenCalled();

        expect(helper.find('.pipeline_pause-message')).not.toBeInDOM();
        expect(helper.find('.pipeline_message')).toContainText(responseMessage);
        expect(helper.find('.pipeline_message')).toHaveClass("success");
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

        helper.click('.unpause');

        expect(doCancelPolling).toHaveBeenCalled();
        expect(doRefreshImmediately).toHaveBeenCalled();

        expect(helper.find('.pipeline_message')).toContainText(responseMessage);
        expect(helper.find('.pipeline_message')).toHaveClass("error");
      });
    });

    describe("pause", () => {
      let pauseInfo, dashboard;
      beforeEach(() => {
        pauseInfo = {
          "paused":       false,
          "paused_by":    "admin",
          "pause_reason": "under construction",
          "paused_at":    "1970-01-01T00:00:12Z"
        };

        dashboard        = {};
        dashboard.reload = jasmine.createSpy();

        mount(true, pauseInfo, dashboard);
      });

      afterEach(() => {
        helper.unmount();
        Modal.destroyAll();
      });

      it("should render pause pipeline button", () => {
        expect(helper.find('.pause')).toBeInDOM();
      });

      it('should disable pause button for non admin users', () => {
        helper.unmount();
        mount(true, pauseInfo, dashboard, false);

        expect(helper.find('.pause')).toHaveClass('disabled');
      });

      it('should add onclick handler for admin users', () => {
        expect(_.isFunction(helper.find('.pause').get(0).onclick)).toBe(true);
      });

      it('should not add onclick handler for non admin users', () => {
        helper.unmount();
        mount(true, pauseInfo, dashboard, false);

        expect(_.isFunction(helper.find('.pause').get(0).onclick)).toBe(false);
      });

      it("should show modal to specify pause reason upon pausing a pipeline", () => {
        expect($('.reveal:visible')).not.toBeInDOM();

        helper.click('.pause');

        expect($('.reveal:visible')).toBeInDOM();
      });

      it("should show appropriate header for popup modal upon pause button click", () => {
        helper.click('.pause');

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

        helper.click('.pause');

        $('.reveal input').val("test");

        doRefreshImmediately.and.callFake(() => pipeline.isPaused = true);
        simulateEvent.simulate($('.reveal .primary').get(0), 'click');

        expect(doCancelPolling).toHaveBeenCalled();
        expect(doRefreshImmediately).toHaveBeenCalled();

        expect(helper.find('.pipeline_pause-message')).toBeInDOM();
        expect(helper.find('.pipeline_pause-message')).toContainText(`on ${timeFormatter.format(pauseInfo.paused_at)}`);
        expect(helper.find('.pipeline_pause-message div').get(0).title).toEqual(timeFormatter.formatInServerTime(pauseInfo.paused_at));
        expect(helper.find('.pipeline_message')).toContainText(responseMessage);
        expect(helper.find('.pipeline_message')).toHaveClass("success");
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

        helper.click('.pause');
        $('.reveal input').val("test");
        simulateEvent.simulate($('.reveal .primary').get(0), 'click');

        expect(doCancelPolling).toHaveBeenCalled();
        expect(doRefreshImmediately).toHaveBeenCalled();

        expect(helper.find('.pipeline_message')).toContainText(responseMessage);
        expect(helper.find('.pipeline_message')).toHaveClass("error");
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

        helper.click('.pause');
        expect($('.reveal:visible')).toBeInDOM();
        const pausePopupTextBox = $('.reveal input');
        pausePopupTextBox.val("test");
        const keydownEvent = $.Event("keydown");
        keydownEvent.key   = "Enter";
        pausePopupTextBox.trigger(keydownEvent);

        expect($('.reveal:visible')).not.toBeInDOM();
        expect(helper.find('.pipeline_message')).toContainText(responseMessage);
        expect(helper.find('.pipeline_message')).toHaveClass("success");
      });

      it("should close pause popup when escape is pressed", () => {
        helper.click('.pause');
        expect($('.reveal:visible')).toBeInDOM();
        const keydownEvent = $.Event("keydown");
        keydownEvent.key   = "Escape";
        $('body').trigger(keydownEvent);
        expect($('.reveal:visible')).not.toBeInDOM();
      });

      it("should not retain text entered when the pause popup is closed", () => {
        helper.click('.pause');
        expect($('.reveal:visible')).toBeInDOM();
        let pausePopupTextBox = $('.reveal input');
        pausePopupTextBox.val("test");
        $('.reveal .secondary').trigger('click');
        helper.click('.pause');
        pausePopupTextBox = $('.reveal input');

        expect(pausePopupTextBox).toHaveValue("");
      });

      it("should have tooltip for pause button when it is disabled", () => {
        helper.unmount();
        mount(true, pauseInfo, {}, false);
        const pauseButton = helper.find('.pause');
        expect(pauseButton).toHaveAttr('data-tooltip-id');
        const tooltipId = $(pauseButton).attr('data-tooltip-id');
        expect($(`#${tooltipId}`)).toHaveText("You do not have permission to pause the pipeline.");
      });

      it('should not show the paused time if it is null', () => {
        pauseInfo = {
          "paused":       false,
          "paused_by":    "admin",
          "pause_reason": "under construction",
          "paused_at":    null
        };

        dashboard        = {};
        dashboard.reload = jasmine.createSpy();

        helper.unmount();
        mount(true, pauseInfo, dashboard);

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

        helper.click('.pause');
        $('.reveal input').val("test");

        doRefreshImmediately.and.callFake(() => pipeline.isPaused = true);
        simulateEvent.simulate($('.reveal .primary').get(0), 'click');

        expect(doCancelPolling).toHaveBeenCalled();
        expect(doRefreshImmediately).toHaveBeenCalled();

        expect(helper.find('.pipeline_pause-message')).toBeInDOM();
        expect(helper.find('.pipeline_pause-message').text()).toBe('Paused by admin (under construction)');
        expect(helper.find('.pipeline_pause-message div')).not.toBeInDOM();
        expect(helper.find('.pipeline_message')).toContainText(responseMessage);
        expect(helper.find('.pipeline_message')).toHaveClass("success");
      });
    });

    describe("Unlock", () => {
      beforeEach(() => {
        const lockInfo = {
          "canUnlock": true,
          "locked":    true
        };
        mount(true, undefined, lockInfo);
      });

      afterEach(() => {
        helper.unmount();
      });

      it("should render unlock pipeline button", () => {
        expect(helper.find('.pipeline_locked')).toBeInDOM();
      });

      it("should enable unlock pipeline button when user can unlock a pipeline", () => {
        expect(helper.find('.pipeline_locked')).not.toHaveClass('disabled');
      });

      it("should disable unlock pipeline button when user can not unlock a pipeline", () => {
        helper.unmount();
        const lockInfo = {
          "canUnlock": false,
          "locked":    true
        };

        mount(true, undefined, lockInfo);
        expect(helper.find('.pipeline_locked')).toHaveClass('disabled');
      });

      it("should not render the pipeline flash message", () => {
        expect(helper.find('.pipeline_message')).not.toBeInDOM();
        expect(helper.find('.pipeline_message .success')).not.toBeInDOM();
        expect(helper.find('.pipeline_message .error')).not.toBeInDOM();
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

        helper.click('.pipeline_locked');

        expect(doCancelPolling).toHaveBeenCalled();
        expect(doRefreshImmediately).toHaveBeenCalled();

        expect(helper.find('.pipeline_message')).toContainText(responseMessage);
        expect(helper.find('.pipeline_message')).toHaveClass("success");
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

        helper.click('.pipeline_locked');

        expect(doCancelPolling).toHaveBeenCalled();
        expect(doRefreshImmediately).toHaveBeenCalled();

        expect(helper.find('.pipeline_message')).toContainText(responseMessage);
        expect(helper.find('.pipeline_message')).toHaveClass("error");
      });
    });

    describe("Trigger", () => {
      beforeEach(mount);

      afterEach(() => {
        helper.unmount();
        Modal.destroyAll();
      });

      it("should render trigger pipeline button", () => {
        expect(helper.find('.play')).toBeInDOM();
      });

      it('should disable trigger button for non admin users', () => {
        helper.unmount();
        mount(true, {}, {}, false, false);

        expect(helper.find('.play')).toHaveClass('disabled');
      });

      it('should disable trigger button when first stage is in progress', () => {
        helper.unmount();
        pipelineInstances[0]._embedded.stages[0].status = 'Building';
        mount();

        expect(helper.find('.play')).toHaveClass('disabled');
      });

      it('should not add onclick handler when first stage is in progess', () => {
        helper.unmount();
        pipelineInstances[0]._embedded.stages[0].status = 'Building';
        mount();

        expect(_.isFunction(helper.find('.play').get(0).onclick)).toBe(false);
      });

      it('should disable trigger button when pipeline is locked', () => {
        helper.unmount();
        mount(true, undefined, {"locked": true});

        expect(helper.find('.play')).toHaveClass('disabled');
      });

      it('should disable trigger button when pipeline is paused', () => {
        helper.unmount();
        mount(true, {
          "paused":       true,
          "paused_by":    "admin",
          "pause_reason": "under construction"
        });

        expect(helper.find('.play')).toHaveClass('disabled');
      });

      it('should not add onclick handler pipeline is locked', () => {
        helper.unmount();
        mount(true, undefined, {"locked": true});

        expect(_.isFunction(helper.find('.play').get(0).onclick)).toBe(false);
      });

      it('should add onclick handler for admin users', () => {
        expect(_.isFunction(helper.find('.play').get(0).onclick)).toBe(true);
      });

      it('should not add onclick handler for non admin users', () => {
        helper.unmount();
        mount(true, {}, {}, false, false);

        expect(_.isFunction(helper.find('.play').get(0).onclick)).toBe(false);
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

        helper.click('.play');

        expect(pipeline.triggerDisabled()).toBe(true);

        expect(helper.find('.pipeline_message')).toContainText(responseMessage);
        expect(helper.find('.pipeline_message')).toHaveClass("success");
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

        helper.click('.play');

        expect(pipeline.triggerDisabled()).toBe(false);

        expect(helper.find('.pipeline_message')).toContainText(responseMessage);
        expect(helper.find('.pipeline_message')).toHaveClass("error");
      });

      it("should have tooltips for trigger buttons when it is disabled", () => {
        helper.unmount();
        mount(true, {}, {}, true, false);
        const playButton = helper.find('.pipeline_operations .play');
        expect(playButton).toHaveAttr('data-tooltip-id');
        const tooltipId = $(playButton).attr('data-tooltip-id');
        expect($(`#${tooltipId}`)).toHaveText("You do not have permission to trigger the pipeline");
      });
    });

    describe("Trigger With Options", () => {
      beforeEach(mount);

      afterEach(() => {
        helper.unmount();
        Modal.destroyAll();
      });

      it("should render trigger with options pipeline button", () => {
        expect(helper.find('.play_with_options')).toBeInDOM();
      });

      it('should disable trigger with options button for non admin users', () => {
        helper.unmount();
        mount(true, {}, {}, false, false);

        expect(helper.find('.play_with_options')).toHaveClass('disabled');
      });

      it('should disable trigger with options button when first stage is in progress', () => {
        helper.unmount();
        pipelineInstances[0]._embedded.stages[0].status = 'Building';
        mount();

        expect(helper.find('.play_with_options')).toHaveClass('disabled');
      });

      it('should not add onclick handler when first stage is in progess', () => {
        helper.unmount();
        pipelineInstances[0]._embedded.stages[0].status = 'Building';
        mount();

        expect(_.isFunction(helper.find('.play_with_options').get(0).onclick)).toBe(false);
      });

      it('should disable trigger with options button when pipeline is locked', () => {
        helper.unmount();
        mount(true, undefined, {"locked": true});
        expect(helper.find('.play_with_options')).toHaveClass('disabled');
      });

      it('should disable trigger with options button when pipeline is paused', () => {
        helper.unmount();
        mount(true, {
          "paused":       true,
          "paused_by":    "admin",
          "pause_reason": "under construction"
        });
        const triggerWithOptsButton = helper.find('.play_with_options');
        expect(triggerWithOptsButton).toHaveClass('disabled');
        expect(triggerWithOptsButton).toHaveAttr('title');
        expect($(triggerWithOptsButton).attr('title')).toBe("Trigger with Options Disabled");
      });

      it('should not add onclick handler pipeline is locked', () => {
        helper.unmount();
        mount(true, undefined, {"locked": true});

        expect(_.isFunction(helper.find('.play_with_options').get(0).onclick)).toBe(false);
      });

      it('should add onclick handler for admin users', () => {
        expect(_.isFunction(helper.find('.play_with_options').get(0).onclick)).toBe(true);
      });

      it('should not add onclick handler for non admin users', () => {
        helper.unmount();
        mount(true, {}, {}, false, false);

        expect(_.isFunction(helper.find('.play_with_options').get(0).onclick)).toBe(false);
      });

      it("should show modal to specify trigger options for a pipeline", () => {
        stubTriggerOptions(pipelineName);

        expect($('.reveal:visible')).not.toBeInDOM();

        helper.click('.play_with_options');

        expect($('.reveal:visible')).toBeInDOM();
      });

      it('should render error message in modal when api response parsing fails', () => {
        stubTriggerOptionsWithInvalidData(pipelineName);

        expect($('.reveal:visible')).not.toBeInDOM();

        helper.click('.play_with_options');
        m.redraw();

        expect($('.reveal:visible')).toBeInDOM();
        expect($('.callout.alert')).toBeInDOM();
      });

      it("should show appropriate header for trigger with options popup modal", () => {

        helper.click('.play_with_options');

        const modalTitle = $('.modal-title:visible');
        expect(modalTitle).toHaveText(`${pipeline.name} - Trigger`);
      });

      it('should show modal appropriately when opened and closed multiple times', () => {
        stubTriggerOptions(pipelineName);

        //open trigger with options modal
        expect($('.reveal:visible')).not.toBeInDOM();
        expect($('.pipeline_options-heading')).not.toContainText('Materials');

        helper.click('.play_with_options');

        expect($('.reveal:visible')).toBeInDOM();
        expect($('.pipeline_options-heading')).toContainText('Materials');

        //close trigger with options modal
        $('.modal-buttons .button.save.secondary').click();

        //open again trigger with options modal
        expect($('.reveal:visible')).not.toBeInDOM();
        expect($('.pipeline_options-heading')).not.toContainText('Materials');

        helper.click('.play_with_options');

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

        helper.click('.play_with_options');

        $('.modal-buttons .button.save.primary').click();

        expect(pipeline.triggerDisabled()).toBe(true);

        expect(helper.find('.pipeline_message')).toContainText(responseMessage);
        expect(helper.find('.pipeline_message')).toHaveClass("success");
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

        helper.click('.play_with_options');

        helper.clickButtonOnActiveModal('.modal-buttons .button.save.primary');

        expect(pipeline.triggerDisabled()).toBe(false);

        expect(helper.find('.pipeline_message')).toContainText(responseMessage);
        expect(helper.find('.pipeline_message')).toHaveClass("error");
      });

      it("should have tooltips when it is disabled", () => {
        helper.unmount();
        mount(true, {}, {}, true, false);
        const playButton = helper.find('.pipeline_operations .play_with_options');
        expect(playButton).toHaveAttr('data-tooltip-id');
        const tooltipId = $(playButton).attr('data-tooltip-id');
        expect($(`#${tooltipId}`)).toHaveText("You do not have permission to trigger the pipeline");
      });
    });
  });

  describe("Pipeline Instances", () => {
    it("should render pipeline instances", () => {
      mount();

      expect(helper.find('.pipeline_instances')).toBeInDOM();
      expect(helper.find('.pipeline_instance')).toBeInDOM();

      helper.unmount();
    });

    it('should render no pipeline instance run message for no instance runs of a pipeline', () => {
      pipelineInstances = [];
      mount();

      expect(helper.find('.pipeline_instances')).toBeInDOM();

      expect(helper.find('.no_instance')).toBeInDOM();
      const pipelineNeverRunMessage = 'You haven\'t run this pipeline yet. Click the play button to run pipeline.';
      expect(helper.find('.no_instance')).toContainText(pipelineNeverRunMessage);

      helper.unmount();
    });
  });

  function mount(canAdminister = true, pauseInfo = {}, lockInfo = {}, canPause = true, canOperate = true, fromConfigRepo = false, pluginsSupportingAnalytics = {}, shouldShowAnalyticsIcon = false) {
    pipelinesJson = [{
      "_links":                 {
        "self": {
          "href": "http://localhost:8153/go/api/pipelines/up42/history"
        },
        "doc":  {
          "href": "https://api.go.cd/current/#pipelines"
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

    helper.mount(() => m(PipelineWidget, {
      pipeline,
      invalidateEtag:    () => {
      },
      pluginsSupportingAnalytics,
      shouldShowAnalyticsIcon,
      doCancelPolling,
      doRefreshImmediately,
      operationMessages: dashboardViewModel.operationMessages,
      dropdown:          dashboardViewModel.dropdown,
      buildCause:        dashboardViewModel.buildCause
    }));
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
