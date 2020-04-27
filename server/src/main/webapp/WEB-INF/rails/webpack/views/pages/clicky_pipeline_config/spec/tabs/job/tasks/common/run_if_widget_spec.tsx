/*
 * Copyright 2020 ThoughtWorks, Inc.
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

import m from "mithril";
import Stream from "mithril/stream";
import {RunIfCondition} from "models/pipeline_configs/task";
import {RunIfConditionWidget} from "views/pages/clicky_pipeline_config/tabs/job/tasks/common/run_if_widget";

import {TestHelper} from "views/pages/spec/test_helper";

describe("Run If Condition Widget", () => {
  const helper = new TestHelper();

  afterEach(helper.unmount.bind(helper));
  it("should render run if condition header", () => {
    const runIf = Stream(["passed"] as RunIfCondition[]);
    mount(runIf);

    expect(helper.byTestId("run-if-condition")).toContainText("Run If Conditions");
  });

  it("should select passed by default", () => {
    const runIf = Stream([] as RunIfCondition[]);
    mount(runIf);

    expect(helper.byTestId("form-field-input-passed")).toBeChecked();
    expect(helper.byTestId("form-field-input-failed")).not.toBeChecked();
    expect(helper.byTestId("form-field-input-any")).not.toBeChecked();
    expect(runIf()).toEqual(["passed"]);
  });

  it("should render passed selection", () => {
    const runIf = Stream(["passed"] as RunIfCondition[]);
    mount(runIf);

    expect(helper.byTestId("form-field-input-passed")).toBeChecked();
    expect(helper.byTestId("form-field-input-failed")).not.toBeChecked();
    expect(helper.byTestId("form-field-input-any")).not.toBeChecked();
  });

  it("should render failed selection", () => {
    const runIf = Stream(["failed"] as RunIfCondition[]);
    mount(runIf);

    expect(helper.byTestId("form-field-input-passed")).not.toBeChecked();
    expect(helper.byTestId("form-field-input-failed")).toBeChecked();
    expect(helper.byTestId("form-field-input-any")).not.toBeChecked();
  });

  it("should render any selection", () => {
    const runIf = Stream(["any"] as RunIfCondition[]);
    mount(runIf);

    expect(helper.byTestId("form-field-input-passed")).not.toBeChecked();
    expect(helper.byTestId("form-field-input-failed")).not.toBeChecked();
    expect(helper.byTestId("form-field-input-any")).toBeChecked();
  });

  it("should select passed selection", () => {
    const runIf = Stream(["failed"] as RunIfCondition[]);
    mount(runIf);

    expect(helper.byTestId("form-field-input-passed")).not.toBeChecked();
    expect(helper.byTestId("form-field-input-failed")).toBeChecked();
    expect(helper.byTestId("form-field-input-any")).not.toBeChecked();
    expect(runIf()).toEqual(["failed"]);

    //uncheck failed
    helper.clickByTestId("form-field-input-failed");

    //check passed
    helper.clickByTestId("form-field-input-passed");

    expect(helper.byTestId("form-field-input-passed")).toBeChecked();
    expect(helper.byTestId("form-field-input-failed")).not.toBeChecked();
    expect(helper.byTestId("form-field-input-any")).not.toBeChecked();
    expect(runIf()).toEqual(["passed"]);
  });

  it("should select failed selection", () => {
    const runIf = Stream(["passed"] as RunIfCondition[]);
    mount(runIf);

    expect(helper.byTestId("form-field-input-passed")).toBeChecked();
    expect(helper.byTestId("form-field-input-failed")).not.toBeChecked();
    expect(helper.byTestId("form-field-input-any")).not.toBeChecked();
    expect(runIf()).toEqual(["passed"]);

    //uncheck passed
    helper.clickByTestId("form-field-input-passed");

    //check passed
    helper.clickByTestId("form-field-input-failed");

    expect(helper.byTestId("form-field-input-passed")).not.toBeChecked();
    expect(helper.byTestId("form-field-input-failed")).toBeChecked();
    expect(helper.byTestId("form-field-input-any")).not.toBeChecked();
    expect(runIf()).toEqual(["failed"]);
  });

  it("should select any selection", () => {
    const runIf = Stream(["passed"] as RunIfCondition[]);
    mount(runIf);

    expect(helper.byTestId("form-field-input-passed")).toBeChecked();
    expect(helper.byTestId("form-field-input-failed")).not.toBeChecked();
    expect(helper.byTestId("form-field-input-any")).not.toBeChecked();
    expect(runIf()).toEqual(["passed"]);

    helper.clickByTestId("form-field-input-any");

    expect(helper.byTestId("form-field-input-passed")).not.toBeChecked();
    expect(helper.byTestId("form-field-input-failed")).not.toBeChecked();
    expect(helper.byTestId("form-field-input-any")).toBeChecked();
    expect(runIf()).toEqual(["any"]);
  });

  it("should clear existing selection when any is selected", () => {
    const runIf = Stream(["passed", "failed"] as RunIfCondition[]);
    mount(runIf);

    expect(helper.byTestId("form-field-input-passed")).toBeChecked();
    expect(helper.byTestId("form-field-input-failed")).toBeChecked();
    expect(helper.byTestId("form-field-input-any")).not.toBeChecked();
    expect(runIf()).toEqual(["passed", "failed"]);

    helper.clickByTestId("form-field-input-any");

    expect(helper.byTestId("form-field-input-passed")).not.toBeChecked();
    expect(helper.byTestId("form-field-input-passed")).toBeDisabled();
    expect(helper.byTestId("form-field-input-failed")).not.toBeChecked();
    expect(helper.byTestId("form-field-input-failed")).toBeDisabled();
    expect(helper.byTestId("form-field-input-any")).toBeChecked();

    expect(runIf()).toEqual(["any"]);
  });

  describe("Read Only", () => {
    it("should render readonly run if condition checkboxes", () => {
      const runIf = Stream(["passed", "failed"] as RunIfCondition[]);
      mount(runIf, true);

      expect(helper.byTestId("form-field-input-passed")).toBeDisabled();
      expect(helper.byTestId("form-field-input-failed")).toBeDisabled();
      expect(helper.byTestId("form-field-input-any")).toBeDisabled();
    });
  });

  function mount(runIf: Stream<RunIfCondition[]>, readonly: boolean = false) {
    helper.mount(() => {
      return <RunIfConditionWidget runIf={runIf} readonly={readonly}/>;
    });
  }
});
