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
import * as simulateEvent from "simulate-event";
import {TestHelper} from "views/pages/spec/test_helper";
import * as Buttons from "../../buttons";
import {Modal} from "../index";
import styles from "../index.scss";

describe("Modal", () => {
  const helper = new TestHelper();
  const body = document.body;
  const q = (sel: string) => document.querySelector(sel)!;
  const qa = (sel: string) => document.querySelectorAll(sel)!;

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
    helper.redraw();

    expect(q(`.${styles.overlayHeader} h3`)).toContainText("Test Modal");
    expect(q(`.${styles.overlayContent} p`)).toContainText("Hello World!");
    expect(q(`.${styles.overlayFixedHeight}`)).not.toBeInDOM();
    const buttonsSelector = `.${styles.overlayFooter} button`;
    expect(qa(buttonsSelector).length).toBe(2);
    expect(qa(buttonsSelector).item(0)).toContainText("Cancel");
    expect(qa(buttonsSelector).item(1)).toContainText("OK");
    expect(body).toHaveClass(styles.fixed);
    testModal.close();
    expect(body).not.toHaveClass(styles.fixed);
  });

  it("should display a modal with fixed height", () => {
    const testModal = new (class TestModal extends Modal {
      constructor() {
        super();
        this.fixedHeight = true;
      }

      body(): m.Children {
        return m("p", "Hello World!");
      }

      title(): string {
        return "Test Modal";
      }

      buttons(): m.ChildArray {
        return [];
      }
    })();

    testModal.render();
    helper.redraw();
    expect(q(`.${styles.overlayFixedHeight} p`)).toContainText("Hello World!");
    testModal.close();
    expect(body).not.toHaveClass(styles.fixed);
  });

  it("should close modal when escape key is pressed", () => {
    const testModal = aModal();

    testModal.render();
    helper.redraw();

    const modalSelector = `.${styles.overlayHeader} h3`;
    expect(q(modalSelector)).toBeInDOM();
    simulateEvent.simulate(q(modalSelector), "keydown", {key: "Escape", keyCode: 27});
    helper.redraw();
    expect(q(modalSelector)).not.toExist();
    testModal.close();
  });

  it("should render an OK button by default when no buttons are supplied", () => {
    const testModal = aModal();

    testModal.render();
    helper.redraw();
    const buttonsSelector = `.${styles.overlayFooter} button`;
    expect(qa(buttonsSelector).length).toBe(1);
    expect(q(buttonsSelector)).toContainText("OK");
    testModal.close();
  });

  it("should close the modal when the overlay outside is clicked", () => {
    const testModal = aModal();
    testModal.render();
    helper.redraw();

    const modalSelector = `.${styles.overlayHeader} h3`;
    const bgSelector    = `.${styles.overlayBg}`;
    expect(q(modalSelector)).toBeInDOM();
    helper.click(q(bgSelector));

    expect(q(modalSelector)).not.toExist();
    testModal.close();
  });

  it("should not close the modal when clicked inside", () => {
    const testModal = aModal();
    testModal.render();
    helper.redraw();

    const modalSelector = `.${styles.overlayHeader} h3`;
    expect(q(modalSelector)).toBeInDOM();
    helper.click(q("#modal-inside"));

    expect(q(modalSelector)).toBeInDOM();
    testModal.close();
  });

  it("should not render the footer in absence of buttons", () => {
    const testModal = aModalWithoutFooter();
    testModal.render();
    helper.redraw();

    expect(q(`.${styles.overlayFooter}`)).not.toExist();
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

  function aModalWithoutFooter() {
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

      buttons(): m.ChildArray {
        return [];
      }
    })();
  }
});
