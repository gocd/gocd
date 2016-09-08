/*
 * Copyright 2016 ThoughtWorks, Inc.
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

define(["jquery", "mithril", 'lodash', "models/pipeline_configs/tracking_tool", "views/pipeline_configs/tracking_tool_widget"], function ($, m, _, TrackingTool, TrackingToolWidget) {
  describe("Tracking Tool Widget", function () {
    var $root = $('#mithril-mount-point'), root = $root.get(0);
    var trackingToolProp;

    beforeEach(function () {
      trackingToolProp = m.prop();

      m.mount(root,
        m.component(TrackingToolWidget, {trackingTool: trackingToolProp})
      );

      m.redraw(true);
      var accordion = $root.find('.tracking-tool .accordion-item > a').get(0);
      var evObj     = document.createEvent('MouseEvents');
      evObj.initEvent('click', true, false);
      accordion.onclick(evObj);
      m.redraw(true);
    });

    it("should select none tracking tool when none is selected", function () {
      expect($root.find('#tracking-tool-none')).toBeChecked();
    });

    it("should set proper tracking tool when it is selected", function () {
      _.each(['generic', 'mingle', 'generic', 'mingle'], function (type) {
        var radioButton = $root.find('#tracking-tool-' + type).get(0);
        var evObj       = document.createEvent('MouseEvents');
        evObj.initEvent('click', true, false);
        radioButton.onclick(evObj);
        m.redraw(true);

        expect(trackingToolProp().type()).toBe(type);
        expect(radioButton).toBeChecked();
      });
    });

  });
});
