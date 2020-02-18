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
import * as simulateEvent from "simulate-event";
import style from "views/components/buttons/index.scss";
import {DeleteConfirmModal} from "views/components/modal/delete_confirm_modal";
import {ModalManager} from "views/components/modal/modal_manager";
import "views/components/modal/spec/modal_matchers";
import {TestHelper} from "views/pages/spec/test_helper";

describe("DeleteConfirmModal", () => {
  let modal: DeleteConfirmModal;
  let spy: any;
  let testHelper: TestHelper;

  beforeEach(() => {
    spy        = jasmine.createSpy("delete callback").and.returnValue(new Promise(_.noop));
    modal      = new DeleteConfirmModal("You will no longer be able to time travel!", spy, "Delete flux capacitor?");
    modal.render();
    m.redraw.sync();
    testHelper = new TestHelper().forModal();
  });

  afterEach(() => {
    ModalManager.closeAll();
  });

  it("should render", () => {
    expect(modal).toContainTitle("Delete flux capacitor?");
    expect(modal).toContainButtons(["No", "Yes Delete"]);
    expect(modal).toContainBody("You will no longer be able to time travel!");
  });

  it("should send callback on clicking delete button", () => {
    expect(spy).not.toHaveBeenCalled();

    testHelper.clickByTestId('button-delete');

    expect(spy).toHaveBeenCalled();
  });

  it("should close when pressing escape key", () => {
    // @ts-ignore
    expect(document.querySelector(".component-modal-container").children.length).toBe(1);
    simulateEvent.simulate(document.body, "keydown", {key: "Escape", keyCode: 27});
    m.redraw.sync();
    // @ts-ignore
    expect(document.querySelector(".component-modal-container").children.length).toBe(0);
  });

  it('should disable the No and Yes delete button when operation state is in progress', () => {
    testHelper.clickByTestId("button-delete");
    expect(testHelper.byTestId("button-delete")).toHaveClass(style.iconSpinner);
    expect(testHelper.byTestId("button-delete")).toBeDisabled();
    expect(testHelper.byTestId("button-no-delete")).toBeDisabled();
    expect(testHelper.byTestId("button-no-delete")).not.toHaveClass(style.iconSpinner);
  });

  it("should close the modal and not send delete callback when 'No` button is clicked", () => {

    expect(spy).not.toHaveBeenCalled();

    testHelper.clickByTestId('button-no-delete');

    expect(spy).not.toHaveBeenCalled();
    // @ts-ignore
    expect(document.querySelector(".component-modal-container").children.length).toBe(0);
  });

});
