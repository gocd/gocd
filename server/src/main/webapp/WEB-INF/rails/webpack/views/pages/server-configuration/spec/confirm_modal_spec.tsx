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

import _ from "lodash";
import m from "mithril";
import {ModalManager} from "views/components/modal/modal_manager";
import "views/components/modal/spec/modal_matchers";
import {TestHelper} from "views/pages/spec/test_helper";
import {ConfirmModal} from "../confirm_modal";

describe("ConfirmModal", () => {
  let modal: ConfirmModal;
  let spy: any;
  let testHelper: TestHelper;

  beforeEach(() => {
    spy   = jasmine.createSpy("cancel callback").and.returnValue(new Promise(_.noop));
    modal = new ConfirmModal("Do you want to cancel?", spy, "Some modal title");
    modal.render();
    m.redraw.sync();
    testHelper = new TestHelper().forModal();
  });

  afterEach(() => {
    ModalManager.closeAll();
  });

  it("should render", () => {
    expect(modal).toContainTitle("Some modal title");
    expect(modal).toContainButtons(["Yes", "No"]);
    expect(modal).toContainBody("Do you want to cancel?");
  });

  it("should send callback on clicking cancel button", () => {
    expect(spy).not.toHaveBeenCalled();

    testHelper.clickByTestId('button-cancel');

    expect(spy).toHaveBeenCalled();
  });

  it("should close the modal and not send cancel callback when 'No` button is clicked", () => {

    expect(spy).not.toHaveBeenCalled();

    testHelper.clickByTestId('button-no-cancel');

    expect(spy).not.toHaveBeenCalled();

    // @ts-ignore
    expect(document.querySelector(".component-modal-container").children.length).toBe(0);
  });

});
