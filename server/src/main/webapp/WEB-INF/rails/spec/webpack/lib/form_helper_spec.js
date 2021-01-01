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
import {TestHelper} from "views/pages/spec/test_helper";
import {f} from "helpers/form_helper";
import Stream from "mithril/stream";
import m from "mithril";
import * as simulateEvent from "simulate-event";

describe("Form Helper", () => {


  const helper = new TestHelper();

  afterEach((done) => {
    helper.unmount(done);
  });

  it("buttonWithTooltip should add a tooltip to the button", () => {
    const model = Stream(true);
    helper.mount(() =>
      m(f.buttonWithTooltip, {
        model,
        'tooltipText': "show me on hover",
      }, "Fancy button with tooltip")
    );
    const buttonWithTooltip = helper.q('button');
    expect(buttonWithTooltip).toExist();

    const tooltipId = buttonWithTooltip.getAttribute("data-tooltip-id");
    const tooltip   = document.getElementById(tooltipId);

    expect(tooltip).toExist();
    expect(tooltip).toHaveText("show me on hover");
    expect(tooltip).not.toBeVisible();

    simulateEvent.simulate(buttonWithTooltip, 'mouseover');
    helper.redraw();

    expect(tooltip).toBeVisible();

    simulateEvent.simulate(buttonWithTooltip, 'mouseout');
    helper.redraw();

    expect(tooltip).not.toBeVisible();
  });

  it("linkWithTooltip should add a tooltip to the link", () => {
    const model = Stream(true);
    helper.mount(() => m(f.linkWithTooltip, {
      model,
      'tooltipText': "show me on hover",
    }, "Fancy link with tooltip"));

    const linkWithTooltip = helper.q('a');
    expect(linkWithTooltip).toExist();

    const tooltipId = linkWithTooltip.getAttribute("data-tooltip-id");
    const tooltip   = document.getElementById(tooltipId);

    expect(tooltip).toExist();
    expect(tooltip).toHaveText("show me on hover");
    expect(tooltip).not.toBeVisible();

    simulateEvent.simulate(linkWithTooltip, 'mouseover');
    helper.redraw();

    expect(tooltip).toBeVisible();

    simulateEvent.simulate(linkWithTooltip, 'mouseout');
    helper.redraw();

    expect(tooltip).not.toBeVisible();
  });
});
