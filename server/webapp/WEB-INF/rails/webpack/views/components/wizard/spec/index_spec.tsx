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
    wizard = new Wizard();
  });

  afterEach(() => {
    wizard.close();
    expect(findByClass(styles.wizard)).not.toBeInDOM();
  });

  function findByClass(className: string) {
    return $(`.${className}`);
  }

  function findIn(parent: string, childClass: string) {
    return $(`.${parent} .${childClass}`);
  }

  it("should display wizard", () => {
    wizard.addStep("Step 1", "This is step one");

    expect(findByClass(styles.wizard)).not.toBeInDOM();

    wizard.render();

    expect(findByClass(styles.wizard)).toBeInDOM();
  });

  describe("Wizard steps", () => {
    it("should list all the wizard steps in the header", () => {
      wizard
        .addStep("Step 1", "This is step one")
        .addStep("Step 2", "This is step two");

      wizard.render();

      expect(findByClass(styles.wizard)).toBeInDOM();
      expect(findIn(styles.wizardHeader, styles.stepHeader).eq(0)).toHaveText("Step 1");
      expect(findIn(styles.wizardHeader, styles.stepHeader).eq(1)).toHaveText("Step 2");
    });

    it("should show content of the first step by default", () => {
      wizard
        .addStep("Step 1", "This is step one")
        .addStep("Step 2", "This is step two");

      wizard.render();

      expect(findByClass(styles.wizard)).toBeInDOM();
      expect(findIn(styles.wizardBody, styles.stepBody).length).toBe(1);
      expect(findIn(styles.wizardBody, styles.stepBody).eq(0)).toHaveText("This is step one");
      expect(findIn(styles.wizardBody, styles.stepBody).eq(0)).toBeVisible();
    });

    it("should switch to step on click of the step name", () => {
      wizard
        .addStep("Step 1", "This is step one")
        .addStep("Step 2", "This is step two");

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
    });

    it("should close wizard on click", () => {
      wizard.render();

      expect(findByClass(styles.wizard)).toBeInDOM();
      expect(findByClass(btnCss.btnCancel)).toBeInDOM();

      simulateEvent.simulate(findByClass(btnCss.btnCancel).get(0), "click");

      expect(findByClass(styles.wizard)).not.toBeInDOM();
    });
  });

});
