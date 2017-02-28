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
describe("Tracking Tool Widget", function () {

  var m             = require('mithril');
  var Stream        = require('mithril/stream');
  var _             = require('lodash');
  var simulateEvent = require('simulate-event');

  require("jasmine-jquery");

  var TrackingToolWidget = require("views/pipeline_configs/tracking_tool_widget");

  var $root, root;
  beforeEach(() => {
    [$root, root] = window.createDomElementForTest();
  });
  afterEach(window.destroyDomElementForTest);
  var trackingToolProp;

  beforeEach(function () {
    trackingToolProp = Stream();

    m.mount(root, {
      view: function () {
        return m(TrackingToolWidget, {trackingTool: trackingToolProp});
      }
    });

    m.redraw();
    simulateEvent.simulate($root.find('.tracking-tool .accordion-item > a').get(0), 'click');
    m.redraw();
  });

  afterEach(function () {
    m.mount(root, null);
    m.redraw();
  });

  it("should select none tracking tool when none is selected", function () {
    expect($root.find('#tracking-tool-none')).toBeChecked();
  });

  it("should set proper tracking tool when it is selected", function () {
    _.each(['generic', 'mingle', 'generic', 'mingle'], function (type) {
      var radioButton = $root.find('#tracking-tool-' + type).get(0);
      simulateEvent.simulate(radioButton, 'click');
      m.redraw();

      expect(trackingToolProp().type()).toBe(type);
      expect(radioButton).toBeChecked();
    });
  });

});
