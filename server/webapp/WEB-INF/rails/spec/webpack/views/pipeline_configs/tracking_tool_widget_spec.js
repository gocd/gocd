/*
 * Copyright 2017 ThoughtWorks, Inc.
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

describe("Tracking Tool Widget", () => {

  const m             = require('mithril');
  const Stream        = require('mithril/stream');
  const _             = require('lodash');
  const simulateEvent = require('simulate-event');

  require("jasmine-jquery");

  const TrackingToolWidget = require("views/pipeline_configs/tracking_tool_widget");

  let $root, root;
  beforeEach(() => {
    [$root, root] = window.createDomElementForTest();
  });
  afterEach(window.destroyDomElementForTest);
  let trackingToolProp;

  beforeEach(() => {
    trackingToolProp = Stream();

    m.mount(root, {
      view() {
        return m(TrackingToolWidget, {trackingTool: trackingToolProp});
      }
    });

    m.redraw();
    simulateEvent.simulate($root.find('.tracking-tool .accordion-item > a').get(0), 'click');
    m.redraw();
  });

  afterEach(() => {
    m.mount(root, null);
    m.redraw();
  });

  it("should select none tracking tool when none is selected", () => {
    expect($root.find('#tracking-tool-none')).toBeChecked();
  });

  it("should set proper tracking tool when it is selected", () => {
    _.each(['generic', 'mingle', 'generic', 'mingle'], (type) => {
      const radioButton = $root.find(`#tracking-tool-${type}`).get(0);
      simulateEvent.simulate(radioButton, 'click');
      m.redraw();

      expect(trackingToolProp().type()).toBe(type);
      expect(radioButton).toBeChecked();
    });
  });

});
