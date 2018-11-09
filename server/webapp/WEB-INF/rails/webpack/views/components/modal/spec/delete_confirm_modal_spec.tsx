/*
 * Copyright 2018 ThoughtWorks, Inc.
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

import * as simulateEvent from "simulate-event";
import {DeleteConfirmModal} from "views/components/modal/delete_confirm_modal";
import {ModalManager} from "views/components/modal/modal_manager";
import "views/components/modal/spec/modal_matchers";

describe("DeleteConfirmModal", () => {

  afterEach(ModalManager.closeAll);

  it("should render", () => {
    const spy = jasmine.createSpy("delete callback");

    const modal = new DeleteConfirmModal("You will no longer be able to time travel!", spy, "Delete flux capacitor?");
    modal.render();

    expect(modal).toContainTitle("Delete flux capacitor?");
    expect(modal).toContainButtons(["No", "Yes Delete"]);
    expect(modal).toContainBody("You will no longer be able to time travel!");
  });

  it("should close when pressing escape key", () => {
    const spy = jasmine.createSpy("delete callback");

    const modal = new DeleteConfirmModal("You will no longer be able to time travel!", spy, "Delete flux capacitor?");
    modal.render();

    // @ts-ignore
    expect(document.querySelector(".component-modal-container").children.length).toBe(1);
    simulateEvent.simulate(document.body, "keydown", {key: "Escape", keyCode: 27});
    // @ts-ignore
    expect(document.querySelector(".component-modal-container").children.length).toBe(0);
  });

  it("should send callback on clicking delete button", () => {
    const spy = jasmine.createSpy("delete callback");

    const modal = new DeleteConfirmModal("You will no longer be able to time travel!", spy, "Delete flux capacitor?");
    modal.render();

    expect(spy).not.toHaveBeenCalled();

    simulateEvent.simulate(document.querySelector("[data-test-id='button-delete']") as Element, "click");

    expect(spy).toHaveBeenCalled();
  });

  it("should close the modal and not send delete callback when 'No` button is clicked", () => {
    const spy = jasmine.createSpy("delete callback");

    const modal = new DeleteConfirmModal("You will no longer be able to time travel!", spy, "Delete flux capacitor?");
    modal.render();

    expect(spy).not.toHaveBeenCalled();

    simulateEvent.simulate(document.querySelector("[data-test-id='button-no-delete']") as Element, "click");

    expect(spy).not.toHaveBeenCalled();
    // @ts-ignore
    expect(document.querySelector(".component-modal-container").children.length).toBe(0);
  });

});
