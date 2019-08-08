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

import * as simulateEvent from "simulate-event";
import {Wizard} from "views/components/wizard/index";
import * as btnCss from "../../buttons/index.scss";
import * as styles from "../index.scss";

describe("WizardSpec", () => {
  let wizard: Wizard;
  beforeEach(() => {
    wizard = new Wizard()
      .addStep("Step 1", "This is step one")
      .addStep("Step 2", "This is step two")
      .addStep("Step 3", "This is step three");
  });

  afterEach(() => {
    wizard.close();
    expect(findByClass(styles.wizard)).not.toBeInDOM();
  });

  function findByClass(className: string) {
    return $(`.${className}`);
  }

  function findByDataTestId(id: string) {
    return $(`[data-test-id='${id}']`);
  }

  function findIn(parent: string, childClass: string) {
    return $(`.${parent} .${childClass}`);
  }

  it("should display wizard", () => {
    expect(findByClass(styles.wizard)).not.toBeInDOM();

    wizard.render();

    expect(findByClass(styles.wizard)).toBeInDOM();
  });

  it("should select the given step", () => {
    wizard
      .defaultStepIndex(2)
      .render();

    expect(findIn(styles.wizardHeader, styles.stepHeader).eq(1)).toHaveClass(styles.selected);
    expect(findIn(styles.wizardBody, styles.stepBody)).toHaveText("This is step two");
  });

  describe("Wizard steps", () => {
    it("should list all the wizard steps in the header", () => {
      wizard.render();

      expect(findByClass(styles.wizard)).toBeInDOM();
      expect(findIn(styles.wizardHeader, styles.stepHeader).eq(0)).toHaveText("Step 1");
      expect(findIn(styles.wizardHeader, styles.stepHeader).eq(1)).toHaveText("Step 2");
    });

    it("should show content of the first step by default", () => {
      wizard.render();

      expect(findByClass(styles.wizard)).toBeInDOM();
      expect(findIn(styles.wizardBody, styles.stepBody).length).toBe(1);
      expect(findIn(styles.wizardBody, styles.stepBody).eq(0)).toHaveText("This is step one");
      expect(findIn(styles.wizardBody, styles.stepBody).eq(0)).toBeVisible();
    });

    it("should switch to step on click of the step name", () => {
      wizard.render();

      expect(findByClass(styles.wizard)).toBeInDOM();
      expect(findIn(styles.wizardBody, styles.stepBody).length).toBe(1);
      expect(findIn(styles.wizardBody, styles.stepBody).eq(0)).toHaveText("This is step one");

      simulateEvent.simulate($(`.${styles.wizardHeader} .${styles.stepHeader}`).get(1), "click");

      expect(findIn(styles.wizardBody, styles.stepBody).eq(0)).toHaveText("This is step two");
    });
  });

  describe("Cancel button", () => {
    it("should show cancel button", () => {
      wizard.render();

      expect(findByClass(btnCss.btnCancel)).toBeInDOM();
      expect(findByClass(btnCss.btnCancel)).toHaveText("Cancel");
    });

    it("should close wizard on click", () => {
      wizard.render();

      expect(findByClass(styles.wizard)).toBeInDOM();
      expect(findByClass(btnCss.btnCancel)).toBeInDOM();

      simulateEvent.simulate(findByClass(btnCss.btnCancel).get(0), "click");

      expect(findByClass(styles.wizard)).not.toBeInDOM();
    });
  });

  describe("Previous button", () => {
    it("should show previous button", () => {
      wizard.render();

      expect(findByDataTestId("previous")).toBeInDOM();
      expect(findByDataTestId("previous")).toHaveText("Previous");
    });

    it("should go to previous step when clicked", () => {
      wizard.defaultStepIndex(2).render();
      expect(findIn(styles.wizardBody, styles.stepBody)).toHaveText("This is step two");

      simulateEvent.simulate(findByDataTestId("previous").get(0), "click");

      expect(findIn(styles.wizardBody, styles.stepBody)).toHaveText("This is step one");
    });

    it("should disable previous button on first step", () => {
      wizard.defaultStepIndex(1).render();
      expect(findByDataTestId("previous")).toBeDisabled();
    });
  });

  describe("Next Button", () => {
    it("should show next button", () => {
      wizard.render();

      expect(findByDataTestId("next")).toBeInDOM();
      expect(findByDataTestId("next")).toHaveText("Next");
    });

    it("should go to next step when clicked", () => {
      wizard.defaultStepIndex(1).render();
      expect(findIn(styles.wizardBody, styles.stepBody)).toHaveText("This is step one");

      simulateEvent.simulate(findByDataTestId("next").get(0), "click");

      expect(findIn(styles.wizardBody, styles.stepBody)).toHaveText("This is step two");
    });

    it("should disable next button on last step", () => {
      wizard.defaultStepIndex(3).render();
      expect(findByDataTestId("next")).toBeDisabled();
    });
  });
});
