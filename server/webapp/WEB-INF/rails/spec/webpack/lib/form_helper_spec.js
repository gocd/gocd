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
import {f} from "helpers/form_helper";
import Stream from "mithril/stream";
import m from "mithril";
import $ from "jquery";

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
    const buttonWithTooltip = helper.find('button');
    expect(buttonWithTooltip).toExist();
    const tooltipId = $(buttonWithTooltip).attr("data-tooltip-id");
    const tooltip   = helper.find(`#${tooltipId}`);
    expect(tooltip).toExist();
    expect(tooltip).toHaveText("show me on hover");
    expect(tooltip).not.toBeVisible();
    $(buttonWithTooltip).trigger('mouseover');
    expect(tooltip).toBeVisible();
    $(buttonWithTooltip).trigger('mouseout');
    expect(tooltip).not.toBeVisible();
  });

  it("linkWithTooltip should add a tooltip to the link", () => {
    const model = Stream(true);
    helper.mount(() => m(f.linkWithTooltip, {
      model,
      'tooltipText': "show me on hover",
    }, "Fancy link with tooltip"));
    const linkWithTooltip = helper.find('a');
    expect(linkWithTooltip).toExist();
    const tooltipId = $(linkWithTooltip).attr("data-tooltip-id");
    const tooltip   = helper.find(`#${tooltipId}`);
    expect(tooltip).toExist();
    expect(tooltip).toHaveText("show me on hover");
    expect(tooltip).not.toBeVisible();
    $(linkWithTooltip).trigger('mouseover');
    expect(tooltip).toBeVisible();
    $(linkWithTooltip).trigger('mouseout');
    expect(tooltip).not.toBeVisible();
  });
});
