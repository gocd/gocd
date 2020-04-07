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
import {FlashMessageModelWithTimeout} from "views/components/flash_message";
import {EntityReOrderHandler} from "views/pages/clicky_pipeline_config/tabs/common/re_order_entity_widget";
import {TestHelper} from "views/pages/spec/test_helper";

describe("ReOrder Entity", () => {
  const helper = new TestHelper();
  let reorderHandler: EntityReOrderHandler;
  let flashMessage: FlashMessageModelWithTimeout;
  let onPipelineConfigSave: () => any;
  let onPipelineConfigReset: () => any;

  beforeEach(() => {
    flashMessage          = new FlashMessageModelWithTimeout();
    onPipelineConfigSave  = jasmine.createSpy().and.returnValue(Promise.resolve());
    onPipelineConfigReset = jasmine.createSpy().and.returnValue(Promise.resolve());

    reorderHandler = new EntityReOrderHandler("stage", flashMessage, onPipelineConfigSave, onPipelineConfigReset);

    helper.mount(() => reorderHandler.getReOrderConfirmationView());
  });

  afterEach(helper.unmount.bind(helper));

  it("should not render the reorder confirmation view by default", () => {
    expect(helper.byTestId("reorder-confirmation")).not.toBeInDOM();
  });

  it("should render the reorder confirmation view when the entities are reodered", () => {
    expect(helper.byTestId("reorder-confirmation")).not.toBeInDOM();

    reorderHandler.onReOder();
    m.redraw.sync();

    expect(helper.byTestId("reorder-confirmation")).toBeInDOM();
  });

  it("should revert the reorder", () => {
    expect(helper.byTestId("reorder-confirmation")).not.toBeInDOM();

    reorderHandler.onReOder();
    m.redraw.sync();
    expect(helper.byTestId("reorder-confirmation")).toBeInDOM();

    helper.clickByTestId("revert-btn");

    expect(helper.byTestId("reorder-confirmation")).not.toBeInDOM();
    expect(onPipelineConfigReset).toHaveBeenCalled();
    expect(onPipelineConfigSave).not.toHaveBeenCalled();
  });

  it("should save the re order", () => {
    expect(helper.byTestId("reorder-confirmation")).not.toBeInDOM();

    reorderHandler.onReOder();
    m.redraw.sync();
    expect(helper.byTestId("reorder-confirmation")).toBeInDOM();

    helper.clickByTestId("save-btn");

    expect(onPipelineConfigReset).not.toHaveBeenCalled();
    expect(onPipelineConfigSave).toHaveBeenCalled();
  });
});
