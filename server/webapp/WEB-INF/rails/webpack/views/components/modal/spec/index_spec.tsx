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
import m from "mithril";
import * as simulateEvent from "simulate-event";
import * as Buttons from "../../buttons";
import {Modal} from "../index";
import styles from "../index.scss";

describe("Modal", () => {
  it("should display a modal", () => {
    const testModal = new (class TestModal extends Modal {
      constructor() {
        super();
      }

      body(): m.Children {
        return m("p", "Hello World!");
      }

      title(): string {
        return "Test Modal";
      }

      buttons(): m.ChildArray {
        return [
          <Buttons.Primary>OK</Buttons.Primary>,
          <Buttons.Cancel>Cancel</Buttons.Cancel>
        ];
      }

    })();

    testModal.render();
    expect($(`.${styles.overlayHeader} h3`)).toContainText("Test Modal");
    expect($(`.${styles.overlayContent} p`)).toContainText("Hello World!");
    const buttonsSelector = `.${styles.overlayFooter} button`;
    expect($(buttonsSelector).length).toBe(2);
    expect($(buttonsSelector).get(0)).toContainText("Cancel");
    expect($(buttonsSelector).get(1)).toContainText("OK");
    expect($("body")).toHaveClass(styles.fixed);
    testModal.close();
    expect($("body")).not.toHaveClass(styles.fixed);
  });

  it("should close modal when escape key is pressed", () => {
    const testModal = aModal();

    testModal.render();

    const modalSelector = `.${styles.overlayHeader} h3`;
    expect($(modalSelector)).toBeInDOM();
    simulateEvent.simulate($(modalSelector).get(0), "keydown", {key: "Escape", keyCode: 27});
    expect($(modalSelector)).not.toBeInDOM();
    testModal.close();
  });

  it("should render an OK button by default when no buttons are supplied", () => {
    const testModal = aModal();

    testModal.render();
    const buttonsSelector = `.${styles.overlayFooter} button`;
    expect($(buttonsSelector).length).toBe(1);
    expect($(buttonsSelector).get(0)).toContainText("OK");
    testModal.close();
  });

  it("should close the modal when the overlay outside is clicked", () => {
    const testModal = aModal();
    testModal.render();

    const modalSelector = `.${styles.overlayHeader} h3`;
    const bgSelector    = `.${styles.overlayBg}`;
    expect($(modalSelector)).toBeInDOM();
    simulateEvent.simulate($(bgSelector).get(0), "click");
    expect($(modalSelector)).not.toBeInDOM();
    testModal.close();
  });

  it("should not close the modal when clicked inside", () => {
    const testModal = aModal();
    testModal.render();

    const modalSelector = `.${styles.overlayHeader} h3`;
    expect($(modalSelector)).toBeInDOM();
    simulateEvent.simulate($("#modal-inside").get(0), "click");
    expect($(modalSelector)).toBeInDOM();
    testModal.close();
  });

  function aModal() {
    return new (class TestModal extends Modal {
      constructor() {
        super();
      }

      body(): m.Children {
        return m("p", {id: "modal-inside"}, "Hello World!");
      }

      title(): string {
        return "Test Modal";
      }
    })();
  }

});
