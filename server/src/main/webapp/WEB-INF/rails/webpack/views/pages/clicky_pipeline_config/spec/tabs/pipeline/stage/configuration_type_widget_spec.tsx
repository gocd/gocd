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
import * as simulateEvent from "simulate-event";
import {FlashMessageModelWithTimeout} from "views/components/flash_message";
import {EntityReOrderHandler} from "views/pages/clicky_pipeline_config/tabs/common/re_order_entity_widget";
import {ConfigurationTypeWidget} from "views/pages/clicky_pipeline_config/tabs/pipeline/stage/configuration_type_widget";
import {TestHelper} from "views/pages/spec/test_helper";

describe("Pipeline Template Configuration Type Widget", () => {
  const helper = new TestHelper();
  afterEach(helper.unmount.bind(helper));

  it("should render configuration type as define from stages", () => {
    mount();

    expect(helper.byTestId("radio-template")).toBeInDOM();
    expect(helper.q("label", helper.byTestId("input-field-for-template")).innerText).toEqual("Use Template");
    expect(helper.byTestId("radio-template")).not.toBeChecked();

    expect(helper.byTestId("radio-stage")).toBeInDOM();
    expect(helper.q("label", helper.byTestId("input-field-for-stage")).innerText).toEqual("Define Stages");
    expect(helper.byTestId("radio-stage")).toBeChecked();
  });

  it("should toggle configuration type to from template", () => {
    mount();

    expect(helper.byTestId("radio-template")).not.toBeChecked();
    expect(helper.byTestId("radio-stage")).toBeChecked();

    simulateEvent.simulate(helper.q("input", helper.byTestId("input-field-for-template")), "click");

    expect(helper.byTestId("radio-template")).toBeChecked();
    expect(helper.byTestId("radio-stage")).not.toBeChecked();
  });

  it("should not disable configurations type switch when defined from stages", () => {
    mount();

    expect(helper.byTestId("radio-template")).not.toBeChecked();
    expect(helper.byTestId("radio-stage")).toBeChecked();

    expect(helper.byTestId("radio-template")).not.toBeDisabled();
    expect(helper.byTestId("radio-stage")).not.toBeDisabled();
  });

  it("should disable configurations type switch when defined from templates", () => {
    mount("template", true);

    expect(helper.byTestId("radio-template")).toBeChecked();
    expect(helper.byTestId("radio-stage")).not.toBeChecked();

    expect(helper.byTestId("radio-template")).toBeDisabled();
    expect(helper.byTestId("radio-stage")).toBeDisabled();
  });

  describe("Read Only", () => {
    beforeEach(() => {
      mount("template", false, true);
    });

    it("should render disabled configuration radio boxes", () => {
      expect(helper.byTestId("radio-template")).toBeDisabled();
      expect(helper.byTestId("radio-stage")).toBeDisabled();
    });
  });

  function mount(propertyVal: string = "stage", fromTemplate: boolean = false, readonly: boolean = false) {
    helper.mount(() => {
      const onSave               = jasmine.createSpy().and.returnValue(Promise.resolve());
      const onReset              = jasmine.createSpy().and.returnValue(Promise.resolve());
      const flashMessage         = new FlashMessageModelWithTimeout();
      const entityReOrderHandler = new EntityReOrderHandler("", flashMessage, onSave, onReset, () => false);

      return <ConfigurationTypeWidget property={Stream<string>(propertyVal)}
                                      entityReOrderHandler={entityReOrderHandler}
                                      readonly={readonly}
                                      isPipelineDefinedOriginallyFromTemplate={Stream<boolean>(fromTemplate)}/>;
    });
  }
});
